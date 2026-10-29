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
            if (normalized.isHighlight) {
                val result = requestHighlightFeed(normalized, cookieHeader)
                return@withContext if (result is ResolverResult.Success) enrich(result) else result
            }
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

    private fun requestHighlightFeed(
        normalized: InstagramStoryUrl,
        cookieHeader: String,
    ): ResolverResult {
        val url = "https://i.instagram.com/api/v1/feed/reels_media/".toHttpUrlOrNull()!!
            .newBuilder()
            .addQueryParameter("reel_ids", "highlight:${normalized.storyId}")
            .build()
        val request = authenticatedRequest(url.toString(), normalized.normalizedUrl, cookieHeader)
        val feed = executeJsonRequest(request) { payload ->
            parseAuthenticatedPayload(normalized.normalizedUrl, payload, cookieHeader, hydrateVideos = true)
        }
        if (feed is ResolverResult.Success ||
            feed is ResolverResult.Failure &&
            (feed.error is ResolverError.LoginRequired || feed.error is ResolverError.RateLimited)
        ) return feed
        val variables = JSONObject()
            .put("reel_ids", JSONArray())
            .put("tag_names", JSONArray())
            .put("location_ids", JSONArray())
            .put("highlight_reel_ids", JSONArray().put(normalized.storyId))
            .put("precomposed_overlay", false)
        val graphUrl = "$INSTAGRAM_BASE_URL/graphql/query/".toHttpUrlOrNull()!!
            .newBuilder()
            .addQueryParameter("query_hash", "45246d3fe16ccc6577e0bd297a5db1ab")
            .addQueryParameter("variables", variables.toString())
            .build()
        return executeJsonRequest(authenticatedRequest(
            graphUrl.toString(), normalized.normalizedUrl, cookieHeader,
        )) { payload ->
            parseAuthenticatedPayload(normalized.normalizedUrl, payload, cookieHeader, hydrateVideos = true)
        }
    }

    private fun requestStoryByMediaId(
        normalized: InstagramStoryUrl,
        cookieHeader: String,
    ): ResolverResult {
        val request = authenticatedRequest(
            url = "https://i.instagram.com/api/v1/media/${normalized.storyId}/info/",
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
        hydrateVideos: Boolean = false,
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

        if (normalized.isHighlight) return parseHighlightPayload(normalized, root, cookieHeader, hydrateVideos)

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
        if (isVideo(media) && video == null) {
            return ResolverResult.Failure(
                ResolverError.HtmlStructureChanged("instagram", "Story video URL is missing"),
            )
        }
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

    private fun parseHighlightPayload(
        normalized: InstagramStoryUrl,
        root: JSONObject,
        cookieHeader: String,
        hydrateVideos: Boolean,
    ): ResolverResult {
        val reelKey = "highlight:${normalized.storyId}"
        val reels = root.optJSONArray("reels_media")
            ?: root.optJSONObject("data")?.optJSONArray("reels_media")
        val reel = root.optJSONObject("reels")?.optJSONObject(reelKey)
            ?: reels?.let { array ->
                (0 until array.length()).mapNotNull(array::optJSONObject)
                    .firstOrNull { it.optString("id") in setOf(reelKey, normalized.storyId) }
            }
            ?: return ResolverResult.Failure(ResolverError.ContentNotFound())
        val items = reel.optJSONArray("items")
            ?: return ResolverResult.Failure(ResolverError.ContentNotFound())
        if (items.length() == 0) return ResolverResult.Failure(ResolverError.ContentNotFound())
        val owner = (reel.optJSONObject("user") ?: reel.optJSONObject("owner"))?.optString("username")
            ?.takeIf { it.matches(Regex("[A-Za-z0-9._]+")) && it != "highlights" }
        val resolved = mutableListOf<MediaItem>()
        for (index in 0 until items.length()) {
            val media = items.optJSONObject(index)
                ?: return ResolverResult.Failure(
                    ResolverError.HtmlStructureChanged("instagram", "invalid Story item"),
                )
            val id = media.optString("pk").ifBlank {
                media.optString("id").substringBefore('_')
            }
            if (!id.matches(NUMERIC_ID_PATTERN)) {
                return ResolverResult.Failure(
                    ResolverError.HtmlStructureChanged("instagram", "Story ID is missing"),
                )
            }
            if (isVideo(media) && bestVideoCandidate(media) == null && hydrateVideos) {
                val result = requestStoryByMediaId(
                    InstagramStoryUrl(
                        normalizedUrl = "https://www.instagram.com/stories/${owner ?: "unknown"}/$id/",
                        author = owner ?: "unknown",
                        storyId = id,
                    ),
                    cookieHeader,
                )
                if (result is ResolverResult.Failure) return result
                val item = (result as ResolverResult.Success).manifest.items.single()
                if (item.type != MediaItemType.VIDEO) {
                    return ResolverResult.Failure(
                        ResolverError.HtmlStructureChanged("instagram", "Story video URL is missing"),
                    )
                }
                resolved += item.copy(
                    requestHeaders = item.requestHeaders + ("Referer" to normalized.normalizedUrl),
                )
                continue
            }
            // GraphQL and the mobile API expose different names for the same Story fields.
            if (!media.has("image_versions2")) {
                val resources = media.optJSONArray("display_resources") ?: JSONArray()
                val candidates = JSONArray()
                for (resourceIndex in 0 until resources.length()) {
                    val resource = resources.optJSONObject(resourceIndex) ?: continue
                    candidates.put(JSONObject()
                        .put("url", resource.optString("src"))
                        .put("width", resource.optInt("config_width"))
                        .put("height", resource.optInt("config_height")))
                }
                media.optString("display_url").takeIf { it.isNotBlank() }?.let {
                    candidates.put(JSONObject().put("url", it))
                }
                media.put("image_versions2", JSONObject().put("candidates", candidates))
            }
            // Reuse the exact same media selection and URL validation as ordinary Stories.
            val result = parseAuthenticatedPayload(
                "https://www.instagram.com/stories/${owner ?: "unknown"}/$id/",
                JSONObject().put("items", JSONArray().put(media)).toString(),
                cookieHeader,
            )
            if (result is ResolverResult.Failure) return result
            val item = (result as ResolverResult.Success).manifest.items.single()
            resolved += item.copy(
                requestHeaders = item.requestHeaders + ("Referer" to normalized.normalizedUrl),
            )
        }
        if (resolved.isEmpty()) return ResolverResult.Failure(ResolverError.ContentNotFound())
        val timestamps = (0 until items.length()).mapNotNull { index ->
            val entry = items.optJSONObject(index) ?: return@mapNotNull null
            val id = entry.optString("pk").ifBlank { entry.optString("id").substringBefore('_') }
            val timestamp = entry.optLong("taken_at_timestamp").takeIf { it > 0 }
                ?: entry.optLong("taken_at").takeIf { it > 0 }
            timestamp?.let { "instagram_story_$id" to it }
        }.toMap()
        val unique = resolved.distinctBy { it.id }
        // Responses can arrive newest first. Use publication time for playback order,
        // or the numeric media ID when timestamps are absent; never compare IDs as strings.
        val media = if (unique.all { it.id in timestamps }) {
            unique.sortedBy { timestamps.getValue(it.id) }
        } else {
            unique.sortedBy { java.math.BigInteger(it.id.removePrefix("instagram_story_")) }
        }
        return ResolverResult.Success(
            MediaManifest(
                platform = MediaPlatform.INSTAGRAM,
                type = ManifestType.STORY,
                author = owner ?: items.optJSONObject(0)?.optJSONObject("user")
                    ?.optString("username")?.takeIf { it.isNotBlank() },
                sourceUrl = normalized.normalizedUrl,
                title = reel.optString("title").takeIf { it.isNotBlank() },
                caption = null,
                thumbnailUrl = media.first().thumbnailUrl,
                items = media,
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

    private fun isVideo(media: JSONObject): Boolean =
        media.optBoolean("is_video") || media.optInt("media_type") == 2 ||
            media.optString("__typename").contains("Video", ignoreCase = true) ||
            media.has("video_resources") || media.has("video_versions") ||
            media.optString("video_url").isNotBlank()

    private fun bestVideoCandidate(media: JSONObject): JSONObject? {
        val candidates = mutableListOf<JSONObject>()
        for (key in listOf("video_versions", "video_resources")) {
            val versions = media.optJSONArray(key) ?: continue
            for (index in 0 until versions.length()) {
                val resource = versions.optJSONObject(index) ?: continue
                candidates += JSONObject()
                    .put("url", resource.optString("url").ifBlank { resource.optString("src") })
                    .put("width", resource.optInt("width").takeIf { it > 0 } ?: resource.optInt("config_width"))
                    .put("height", resource.optInt("height").takeIf { it > 0 } ?: resource.optInt("config_height"))
            }
        }
        media.optString("video_url").takeIf { it.isNotBlank() }?.let {
            candidates += JSONObject().put("url", it)
        }
        return candidates.filter { isTrustedInstagramMediaUrl(it.optString("url")) }
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
