package com.jeremysu0818.igthreadsdl.data.resolver

import com.jeremysu0818.igthreadsdl.data.session.InstagramSessionManager
import com.jeremysu0818.igthreadsdl.domain.download.FilenameGenerator
import com.jeremysu0818.igthreadsdl.domain.model.ManifestType
import com.jeremysu0818.igthreadsdl.domain.model.MediaItem
import com.jeremysu0818.igthreadsdl.domain.model.MediaItemType
import com.jeremysu0818.igthreadsdl.domain.model.MediaManifest
import com.jeremysu0818.igthreadsdl.domain.model.MediaPlatform
import com.jeremysu0818.igthreadsdl.domain.resolver.MediaResolver
import com.jeremysu0818.igthreadsdl.domain.resolver.ResolverError
import com.jeremysu0818.igthreadsdl.domain.resolver.ResolverResult
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Locale

/**
 * Resolves a shared Instagram Story with the session saved by the in-app
 * Instagram login WebView. No password is handled by this resolver and no
 * third-party Story index is contacted.
 */
class InstagramStoryResolver(
    private val client: OkHttpClient = defaultResolverClient,
    private val sessionProvider: () -> String? = InstagramSessionManager::cookieHeader,
) : MediaResolver {
    override fun supports(url: String): Boolean =
        UrlNormalizer.normalizeInstagramStory(url) != null

    override suspend fun resolve(url: String): ResolverResult = withContext(Dispatchers.IO) {
        val normalized = UrlNormalizer.normalizeInstagramStory(url)
            ?: return@withContext ResolverResult.Failure(ResolverError.InvalidUrl())
        val cookieHeader = runCatching(sessionProvider).getOrNull()
            ?.takeIf {
                !InstagramSessionManager.parseCookies(it)["sessionid"].isNullOrBlank()
            }
            ?: return@withContext ResolverResult.Failure(ResolverError.LoginRequired())

        try {
            val direct = requestStoryByMediaId(normalized, cookieHeader)
            if (direct is ResolverResult.Success) {
                return@withContext enrich(direct)
            }
            if (direct is ResolverResult.Failure &&
                direct.error is ResolverError.LoginRequired
            ) {
                return@withContext direct
            }

            val feed = requestStoryFeed(normalized, cookieHeader)
            if (feed is ResolverResult.Success) {
                enrich(feed)
            } else {
                feed
            }
        } catch (error: IOException) {
            ResolverResult.Failure(ResolverError.Network(error.message))
        } catch (error: RuntimeException) {
            ResolverResult.Failure(
                ResolverError.HtmlStructureChanged("instagram", error.message),
            )
        }
    }

    private suspend fun enrich(result: ResolverResult.Success): ResolverResult.Success {
        val items = client.enrichMediaMetadata(result.manifest.items)
        return ResolverResult.Success(result.manifest.copy(items = items))
    }

    private fun requestStoryByMediaId(
        normalized: InstagramStoryUrl,
        cookieHeader: String,
    ): ResolverResult {
        val request = authenticatedRequest(
            url = "$INSTAGRAM_BASE_URL/api/v1/media/${normalized.storyId}/info/",
            referer = normalized.normalizedUrl,
            cookieHeader = cookieHeader,
        )
        return executeJsonRequest(request) { payload ->
            parseAuthenticatedPayload(
                sourceUrl = normalized.normalizedUrl,
                payload = payload,
                cookieHeader = cookieHeader,
            )
        }
    }

    private fun requestStoryFeed(
        normalized: InstagramStoryUrl,
        cookieHeader: String,
    ): ResolverResult {
        val profileRequest = authenticatedRequest(
            url = "$INSTAGRAM_BASE_URL/api/v1/users/web_profile_info/?username=${normalized.author}",
            referer = "$INSTAGRAM_BASE_URL/${normalized.author}/",
            cookieHeader = cookieHeader,
        )
        val userId = client.newCall(profileRequest).execute().use { response ->
            val payload = response.body?.string().orEmpty()
            if (isLoginResponse(response.request.url.encodedPath, response.code, payload)) {
                return ResolverResult.Failure(ResolverError.LoginRequired())
            }
            if (!response.isSuccessful) {
                return ResolverResult.Failure(
                    ResolverError.fromHttpStatus(response.code, payload, "instagram"),
                )
            }
            runCatching { JSONObject(payload) }.getOrNull()
                ?.optJSONObject("data")
                ?.optJSONObject("user")
                ?.optString("id")
                ?.takeIf { it.matches(NUMERIC_ID_PATTERN) }
                ?: return ResolverResult.Failure(
                    ResolverError.HtmlStructureChanged("instagram", "profile user ID is missing"),
                )
        }

        val feedRequest = authenticatedRequest(
            url = "$INSTAGRAM_BASE_URL/api/v1/feed/reels_media/?reel_ids=$userId",
            referer = normalized.normalizedUrl,
            cookieHeader = cookieHeader,
        )
        return executeJsonRequest(feedRequest) { payload ->
            parseAuthenticatedPayload(
                sourceUrl = normalized.normalizedUrl,
                payload = payload,
                cookieHeader = cookieHeader,
            )
        }
    }

    private fun executeJsonRequest(
        request: Request,
        parser: (String) -> ResolverResult,
    ): ResolverResult = client.newCall(request).execute().use { response ->
        val payload = response.body?.string().orEmpty()
        if (isLoginResponse(response.request.url.encodedPath, response.code, payload)) {
            return ResolverResult.Failure(ResolverError.LoginRequired())
        }
        if (!response.isSuccessful) {
            return ResolverResult.Failure(
                ResolverError.fromHttpStatus(response.code, payload, "instagram"),
            )
        }
        parser(payload)
    }

    private fun authenticatedRequest(
        url: String,
        referer: String,
        cookieHeader: String,
    ): Request {
        val csrfToken = InstagramSessionManager.csrfToken(cookieHeader)
        return Request.Builder()
            .url(url)
            .get()
            .header("User-Agent", ANDROID_CHROME_USER_AGENT)
            .header("Accept", "application/json")
            .header("Accept-Language", "en-US,en;q=0.8")
            .header("Referer", referer)
            .header("Cookie", cookieHeader)
            .header("X-IG-App-ID", INSTAGRAM_WEB_APP_ID)
            .header("X-ASBD-ID", INSTAGRAM_ASBD_ID)
            .header("X-Requested-With", "XMLHttpRequest")
            .apply {
                csrfToken?.let { header("X-CSRFToken", it) }
            }
            .build()
    }

    internal fun parseAuthenticatedPayload(
        sourceUrl: String,
        payload: String,
        cookieHeader: String,
    ): ResolverResult {
        val normalized = UrlNormalizer.normalizeInstagramStory(sourceUrl)
            ?: return ResolverResult.Failure(ResolverError.InvalidUrl())
        val root = runCatching { JSONObject(payload) }.getOrNull()
            ?: return ResolverResult.Failure(
                ResolverError.HtmlStructureChanged("instagram", "invalid authenticated Story response"),
            )
        val message = root.optString("message").lowercase(Locale.US)
        if (
            root.optBoolean("login_required") ||
            "login_required" in message ||
            "login required" in message ||
            "checkpoint_required" in message
        ) {
            return ResolverResult.Failure(ResolverError.LoginRequired())
        }

        val items = findStoryItems(root)
        val media = (0 until items.length())
            .mapNotNull(items::optJSONObject)
            .firstOrNull { candidate ->
                val pk = candidate.optString("pk")
                val id = candidate.optString("id").substringBefore('_')
                pk == normalized.storyId || id == normalized.storyId
            }
            ?: return ResolverResult.Failure(
                ResolverError.ContentNotFound("Story expired, deleted, or unavailable to this account"),
            )

        val image = bestImageCandidate(media)
        val video = bestVideoCandidate(media)
        val type: MediaItemType
        val mediaUrl: String
        val width: Int?
        val height: Int?
        if (video != null) {
            type = MediaItemType.VIDEO
            mediaUrl = video.optString("url")
            width = video.optInt("width").takeIf { it > 0 }
            height = video.optInt("height").takeIf { it > 0 }
        } else if (image != null) {
            type = MediaItemType.IMAGE
            mediaUrl = image.optString("url")
            width = image.optInt("width").takeIf { it > 0 }
            height = image.optInt("height").takeIf { it > 0 }
        } else {
            return ResolverResult.Failure(
                ResolverError.HtmlStructureChanged("instagram", "Story media URL is missing"),
            )
        }
        if (!isTrustedInstagramMediaUrl(mediaUrl)) {
            return ResolverResult.Failure(
                ResolverError.HtmlStructureChanged("instagram", "untrusted Story media URL"),
            )
        }
        val thumbnailUrl = image?.optString("url")
            ?.takeIf(::isTrustedInstagramMediaUrl)
            ?: mediaUrl.takeIf { type == MediaItemType.IMAGE }
        val author = media.optJSONObject("user")
            ?.optString("username")
            ?.takeIf { it.isNotBlank() }
            ?: normalized.author
        val mimeType = inferMimeType(mediaUrl, type)
        val requestHeaders = mapOf(
            "User-Agent" to ANDROID_CHROME_USER_AGENT,
            "Referer" to normalized.normalizedUrl,
        )
        val item = MediaItem(
            id = "instagram_story_${normalized.storyId}",
            type = type,
            downloadUrl = mediaUrl,
            thumbnailUrl = thumbnailUrl,
            width = width,
            height = height,
            durationMs = media.optDouble("video_duration")
                .takeIf { it > 0.0 }
                ?.let { seconds -> (seconds * 1_000.0).toLong() },
            filename = FilenameGenerator.generate(
                platform = MediaPlatform.INSTAGRAM,
                author = author,
                shortcode = "story_${normalized.storyId}",
                index = 0,
                type = type,
                mediaUrl = mediaUrl,
                mimeType = mimeType,
            ),
            contentLength = null,
            mimeType = mimeType,
            requestHeaders = requestHeaders,
        )
        return ResolverResult.Success(
            MediaManifest(
                platform = MediaPlatform.INSTAGRAM,
                type = ManifestType.STORY,
                author = author,
                sourceUrl = normalized.normalizedUrl,
                title = null,
                caption = null,
                thumbnailUrl = thumbnailUrl,
                items = listOf(item),
                isPartial = false,
                warnings = emptyList(),
            ),
        )
    }

    private fun findStoryItems(root: JSONObject): JSONArray {
        root.optJSONArray("items")?.let { return it }
        root.optJSONArray("reels_media")
            ?.optJSONObject(0)
            ?.optJSONArray("items")
            ?.let { return it }
        root.optJSONObject("reels")?.let { reels ->
            reels.keys().forEach { key ->
                reels.optJSONObject(key)?.optJSONArray("items")?.let { return it }
            }
        }
        return JSONArray()
    }

    private fun bestVideoCandidate(media: JSONObject): JSONObject? {
        val versions = media.optJSONArray("video_versions") ?: return null
        return (0 until versions.length())
            .mapNotNull(versions::optJSONObject)
            .filter { isTrustedInstagramMediaUrl(it.optString("url")) }
            .maxByOrNull(::dimensionsArea)
    }

    private fun bestImageCandidate(media: JSONObject): JSONObject? {
        val candidates = media.optJSONObject("image_versions2")
            ?.optJSONArray("candidates")
            ?: return null
        return (0 until candidates.length())
            .mapNotNull(candidates::optJSONObject)
            .filter { isTrustedInstagramMediaUrl(it.optString("url")) }
            .maxByOrNull(::dimensionsArea)
    }

    private fun dimensionsArea(value: JSONObject): Long =
        value.optLong("width").coerceAtLeast(0) *
            value.optLong("height").coerceAtLeast(0)

    private fun isTrustedInstagramMediaUrl(value: String): Boolean {
        val url = value.toHttpUrlOrNull() ?: return false
        if (!url.isHttps) return false
        val host = url.host.lowercase(Locale.US)
        return host.endsWith(".cdninstagram.com") ||
            host == "cdninstagram.com" ||
            host.endsWith(".fbcdn.net") ||
            host == "fbcdn.net"
    }

    private fun isLoginResponse(path: String, statusCode: Int, payload: String): Boolean {
        val lower = payload.lowercase(Locale.US)
        return path.startsWith("/accounts/login") ||
            statusCode == 401 ||
            "\"login_required\":true" in lower ||
            "\"message\":\"login_required\"" in lower ||
            "checkpoint_required" in lower
    }

    companion object {
        private const val INSTAGRAM_BASE_URL = "https://www.instagram.com"
        private const val INSTAGRAM_WEB_APP_ID = "936619743392459"
        private const val INSTAGRAM_ASBD_ID = "129477"
        private val NUMERIC_ID_PATTERN = Regex("[0-9]+")
    }
}
