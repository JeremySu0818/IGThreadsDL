package com.jeremysu0818.igthreadsdl

import com.jeremysu0818.igthreadsdl.data.resolver.UrlNormalizer
import com.jeremysu0818.igthreadsdl.domain.model.ManifestType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class UrlNormalizerTest {
    @Test
    fun instagramTrackingParametersAreRemovedAndReelsRouteIsNormalized() {
        val result = UrlNormalizer.normalizeInstagram(
            "https://www.instagram.com/reels/ABC_def-12/?igsh=tracking&utm_source=copy",
        )

        assertNotNull(result)
        assertEquals("ABC_def-12", result?.shortcode)
        assertEquals(ManifestType.REEL, result?.type)
        assertEquals("https://www.instagram.com/reel/ABC_def-12/", result?.normalizedUrl)
    }

    @Test
    fun supportedInstagramRoutesExtractShortcode() {
        listOf("p", "reel", "reels", "tv").forEach { route ->
            assertEquals(
                "C0de_123",
                UrlNormalizer.normalizeInstagram("https://instagram.com/$route/C0de_123/")?.shortcode,
            )
        }
    }

    @Test
    fun unsupportedInstagramProfileUrlIsRejected() {
        assertNull(UrlNormalizer.normalizeInstagram("https://instagram.com/example/"))
    }

    @Test
    fun storyShareUrlExtractsAuthorAndNumericStoryId() {
        val result = UrlNormalizer.normalizeInstagramStory(
            "https://instagram.com/stories/public.creator/3950932965561026193/?utm_source=ig_story_item_share",
        )

        assertEquals("public.creator", result?.author)
        assertEquals("3950932965561026193", result?.storyId)
        assertEquals(
            "https://www.instagram.com/stories/public.creator/3950932965561026193/",
            result?.normalizedUrl,
        )
    }

    @Test
    fun storyTrayIsRejectedButHighlightShareUrlIsSupported() {
        assertNull(UrlNormalizer.normalizeInstagramStory("https://instagram.com/stories/public.creator/"))
        val url = "https://instagram.com/stories/highlights/123/?igsh=tracking"
        val normalized = UrlNormalizer.normalizeInstagramStory(url)
        assertEquals(true, normalized?.isHighlight)
        assertEquals("123", normalized?.storyId)
        assertEquals("https://www.instagram.com/stories/highlights/123/", normalized?.normalizedUrl)
        assertEquals(true, UrlNormalizer.isSupported(url))
        assertEquals(url, UrlNormalizer.extractSupportedUrl("Share $url"))
        assertNull(UrlNormalizer.normalizeInstagramStory("https://instagram.com/stories/highlights/abc/"))
        assertNull(UrlNormalizer.normalizeInstagramStory("https://example.com/stories/highlights/123/"))
    }

    @Test
    fun encodedHighlightSharePreservesSelectedStoryAndRemovesTracking() {
        val url = "https://www.instagram.com/s/aGlnaGxpZ2h0OjE4MDA2NDI1OTY2ODAyMzQw" +
            "?story_media_id=3735235249904446946_37098377413&psln=bzQ4OTFnMWh5cDVo"
        val normalized = UrlNormalizer.normalizeInstagramStory(url)
        assertEquals("18006425966802340", normalized?.storyId)
        assertEquals(true, normalized?.isHighlight)
        assertEquals("3735235249904446946", normalized?.selectedStoryId)
        assertEquals(
            "https://www.instagram.com/stories/highlights/18006425966802340/" +
                "?story_media_id=3735235249904446946",
            normalized?.normalizedUrl,
        )
        assertEquals(normalized, normalized?.normalizedUrl?.let(UrlNormalizer::normalizeInstagramStory))
        assertEquals(true, UrlNormalizer.isSupported(url))
        assertEquals(url, UrlNormalizer.extractSupportedUrl("Share $url"))
    }

    @Test
    fun malformedHighlightSharesAreRejected() {
        assertNull(UrlNormalizer.normalizeInstagramStory("https://instagram.com/s/invalid!/"))
        assertNull(UrlNormalizer.normalizeInstagramStory("https://instagram.com/s/cG9zdDoxMjM/"))
        assertNull(UrlNormalizer.normalizeInstagramStory(
            "https://instagram.com/stories/highlights/123/?story_media_id=invalid",
        ))
        assertNull(UrlNormalizer.normalizeInstagramStory(
            "https://example.com/s/aGlnaGxpZ2h0OjEyMw",
        ))
        val highlight = UrlNormalizer.normalizeInstagramStory(
            "https://instagram.com/s/aGlnaGxpZ2h0OjEyMw",
        )
        assertEquals("123", highlight?.storyId)
        assertNull(highlight?.selectedStoryId)
    }

    @Test
    fun threadsNetIsNormalizedWithoutLosingRequiredQuery() {
        val result = UrlNormalizer.normalizeThreads(
            "https://www.threads.net/@author/post/Code_123?xmt=required#fragment",
        )

        assertEquals("Code_123", result?.shortcode)
        assertEquals("author", result?.author)
        assertEquals(
            "https://www.threads.com/@author/post/Code_123?xmt=required",
            result?.normalizedUrl,
        )
    }

    @Test
    fun threadsShareLinkIsAcceptedAndTrackingParametersAreRemoved() {
        val result = UrlNormalizer.normalizeThreads(
            "https://www.threads.com/share/Bk6vY7HQu-/?utm_source=share",
        )

        assertEquals("Bk6vY7HQu-", result?.shortcode)
        assertEquals(true, result?.isShareLink)
        assertEquals("https://www.threads.com/share/Bk6vY7HQu-/", result?.normalizedUrl)
    }
}
