package com.jeremysu0818.igthreadsdl.data.resolver

import com.jeremysu0818.igthreadsdl.domain.model.ManifestType
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.util.Base64

data class InstagramUrl(
    val normalizedUrl: String,
    val shortcode: String,
    val type: ManifestType,
)

data class InstagramStoryUrl(
    val normalizedUrl: String,
    val author: String,
    val storyId: String,
    val isHighlight: Boolean = false,
    val selectedStoryId: String? = null,
)

data class ThreadsUrl(
    val normalizedUrl: String,
    val shortcode: String,
    val author: String?,
    val isShareLink: Boolean = false,
)

object UrlNormalizer {
    private val urlRegex = Regex("""https?://[^\s<>"']+""", RegexOption.IGNORE_CASE)
    private val instagramHosts = setOf("instagram.com", "www.instagram.com", "m.instagram.com")
    private val threadsHosts = setOf(
        "threads.com",
        "www.threads.com",
        "threads.net",
        "www.threads.net",
    )
    private val trailingPunctuation = charArrayOf('.', ',', ';', ':', '!', '?', ')', ']', '}', '，', '。')

    fun extractFirstUrl(text: String): String? =
        urlRegex.findAll(text)
            .map { it.value.trimEnd(*trailingPunctuation) }
            .firstOrNull()

    fun extractSupportedUrl(text: String): String? =
        urlRegex.findAll(text)
            .map { it.value.trimEnd(*trailingPunctuation) }
            .firstOrNull {
                normalizeInstagramStory(it) != null ||
                    normalizeInstagram(it) != null ||
                    normalizeThreads(it) != null
            }

    fun normalizeInstagramStory(input: String): InstagramStoryUrl? {
        val raw = extractFirstUrl(input) ?: input.trim()
        val parsed = raw.toHttpUrlOrNull() ?: return null
        if (parsed.host.lowercase() !in instagramHosts) return null

        val segments = parsed.pathSegments.filter { it.isNotBlank() }
        if (segments.size == 2 && segments[0].equals("s", ignoreCase = true)) {
            val decoded = runCatching {
                String(Base64.getUrlDecoder().decode(segments[1]), Charsets.UTF_8)
            }.getOrNull() ?: return null
            val highlightId = Regex("highlight:([0-9]+)").matchEntire(decoded)
                ?.groupValues?.get(1) ?: return null
            val canonical = parsed.newBuilder()
                .encodedPath("/stories/highlights/$highlightId/")
                .build()
            return normalizeInstagramStory(canonical.toString())
        }
        if (segments.size < 3 || !segments[0].equals("stories", ignoreCase = true)) return null
        val author = segments[1]
            .removePrefix("@")
            .takeIf { it.matches(Regex("[A-Za-z0-9._]+")) }
            ?: return null
        val isHighlight = author.equals("highlights", ignoreCase = true)
        val storyId = segments[2].takeIf { it.matches(Regex("[0-9]+")) } ?: return null

        val selectedStoryId = if (isHighlight) {
            parsed.queryParameter("story_media_id")?.let { value ->
                if (!value.matches(Regex("[0-9]+(?:_[0-9]+)?"))) return null
                value.substringBefore('_')
            }
        } else null
        val canonicalUrl = "https://www.instagram.com/stories/${if (isHighlight) "highlights" else author}/$storyId/"
        return InstagramStoryUrl(
            normalizedUrl = canonicalUrl + (selectedStoryId?.let { "?story_media_id=$it" } ?: ""),
            author = author,
            storyId = storyId,
            isHighlight = isHighlight,
            selectedStoryId = selectedStoryId,
        )
    }

    fun normalizeInstagram(input: String): InstagramUrl? {
        val raw = extractFirstUrl(input) ?: input.trim()
        val parsed = raw.toHttpUrlOrNull() ?: return null
        if (parsed.host.lowercase() !in instagramHosts) return null

        val segments = parsed.pathSegments.filter { it.isNotBlank() }
        if (segments.size < 2) return null
        val route = segments[0].lowercase()
        if (route !in setOf("p", "reel", "reels", "tv")) return null
        val shortcode = segments[1].takeIf { it.matches(Regex("[A-Za-z0-9_-]+")) } ?: return null
        val normalizedRoute = if (route == "reels") "reel" else route
        val type = when (normalizedRoute) {
            "reel", "tv" -> ManifestType.REEL
            else -> ManifestType.POST
        }

        return InstagramUrl(
            normalizedUrl = "https://www.instagram.com/$normalizedRoute/$shortcode/",
            shortcode = shortcode,
            type = type,
        )
    }

    fun normalizeThreads(input: String): ThreadsUrl? {
        val raw = extractFirstUrl(input) ?: input.trim()
        val parsed = raw.toHttpUrlOrNull() ?: return null
        if (parsed.host.lowercase() !in threadsHosts) return null
        val segments = parsed.pathSegments.filter { it.isNotBlank() }
        if (segments.size == 2 && segments[0].equals("share", ignoreCase = true)) {
            val shareCode = segments[1]
                .takeIf { it.matches(Regex("[A-Za-z0-9_-]+")) }
                ?: return null
            return ThreadsUrl(
                normalizedUrl = "https://www.threads.com/share/$shareCode/",
                shortcode = shareCode,
                author = null,
                isShareLink = true,
            )
        }
        val postIndex = segments.indexOfFirst { it.equals("post", ignoreCase = true) }
        if (postIndex < 1 || postIndex + 1 >= segments.size) return null
        val shortcode = segments[postIndex + 1]
            .takeIf { it.matches(Regex("[A-Za-z0-9_-]+")) }
            ?: return null
        val author = segments.getOrNull(postIndex - 1)?.removePrefix("@")?.takeIf { it.isNotBlank() }
        val normalized = parsed.newBuilder()
            .scheme("https")
            .host("www.threads.com")
            .fragment(null)
            .build()
            .toString()

        return ThreadsUrl(
            normalizedUrl = normalized,
            shortcode = shortcode,
            author = author,
        )
    }

    fun isSupported(input: String): Boolean =
        normalizeInstagramStory(input) != null ||
            normalizeInstagram(input) != null ||
            normalizeThreads(input) != null
}
