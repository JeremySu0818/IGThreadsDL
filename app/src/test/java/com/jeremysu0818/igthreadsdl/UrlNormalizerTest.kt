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
    fun storyTrayAndHighlightUrlsAreRejected() {
        assertNull(UrlNormalizer.normalizeInstagramStory("https://instagram.com/stories/public.creator/"))
        assertNull(UrlNormalizer.normalizeInstagramStory("https://instagram.com/stories/highlights/123/"))
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
}
