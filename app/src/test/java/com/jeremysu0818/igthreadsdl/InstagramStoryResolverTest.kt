package com.jeremysu0818.igthreadsdl

import com.jeremysu0818.igthreadsdl.data.resolver.InstagramStoryResolver
import com.jeremysu0818.igthreadsdl.domain.model.ManifestType
import com.jeremysu0818.igthreadsdl.domain.model.MediaItemType
import com.jeremysu0818.igthreadsdl.domain.resolver.ResolverError
import com.jeremysu0818.igthreadsdl.domain.resolver.ResolverResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class InstagramStoryResolverTest {
    private val resolver = InstagramStoryResolver(sessionProvider = { null })
    private val sourceUrl =
        "https://www.instagram.com/stories/public.creator/3950932965561026193/"
    private val cookieHeader = "csrftoken=csrf123; sessionid=session123"

    @Test
    fun authenticatedVideoResponseUsesHighestResolutionAndKeepsCoverAsThumbnail() {
        val result = resolver.parseAuthenticatedPayload(
            sourceUrl = sourceUrl,
            cookieHeader = cookieHeader,
            payload = """
                {
                  "items": [{
                    "pk": "3950932965561026193",
                    "id": "3950932965561026193_42",
                    "user": {"username": "public.creator"},
                    "video_duration": 5,
                    "video_versions": [
                      {
                        "width": 480,
                        "height": 854,
                        "url": "https://scontent.cdninstagram.com/v/story-small.mp4"
                      },
                      {
                        "width": 1080,
                        "height": 1920,
                        "url": "https://instagram.example.fbcdn.net/v/story-hd.mp4"
                      }
                    ],
                    "image_versions2": {"candidates": [{
                      "width": 1080,
                      "height": 1920,
                      "url": "https://scontent.cdninstagram.com/v/story-cover.jpg"
                    }]}
                  }]
                }
            """.trimIndent(),
        )

        assertTrue(result is ResolverResult.Success)
        val manifest = (result as ResolverResult.Success).manifest
        assertEquals(ManifestType.STORY, manifest.type)
        assertEquals("public.creator", manifest.author)
        assertEquals(1, manifest.items.size)
        assertEquals(MediaItemType.VIDEO, manifest.items.single().type)
        assertTrue("story-hd.mp4" in manifest.items.single().downloadUrl)
        assertTrue("story-cover.jpg" in checkNotNull(manifest.items.single().thumbnailUrl))
        assertTrue("Cookie" !in manifest.items.single().requestHeaders)
        assertEquals(5_000L, manifest.items.single().durationMs)
    }

    @Test
    fun authenticatedFeedSelectsOnlyExactSharedStoryId() {
        val result = resolver.parseAuthenticatedPayload(
            sourceUrl = sourceUrl,
            cookieHeader = cookieHeader,
            payload = """
                {
                  "reels": {
                    "42": {
                      "items": [
                        {
                          "pk": "3950000000000000000",
                          "image_versions2": {"candidates": [{
                            "url": "https://scontent.cdninstagram.com/v/wrong.jpg",
                            "width": 1080,
                            "height": 1920
                          }]}
                        },
                        {
                          "pk": "3950932965561026193",
                          "image_versions2": {"candidates": [{
                            "url": "https://scontent.cdninstagram.com/v/target.jpg",
                            "width": 1080,
                            "height": 1920
                          }]}
                        }
                      ]
                    }
                  }
                }
            """.trimIndent(),
        )

        assertTrue(result is ResolverResult.Success)
        val item = (result as ResolverResult.Success).manifest.items.single()
        assertEquals(MediaItemType.IMAGE, item.type)
        assertTrue("target.jpg" in item.downloadUrl)
        assertTrue("wrong.jpg" !in item.downloadUrl)
    }

    @Test
    fun expiredStoryDoesNotFallBackToAnotherFeedItem() {
        val result = resolver.parseAuthenticatedPayload(
            sourceUrl = sourceUrl,
            cookieHeader = cookieHeader,
            payload = """
                {
                  "items": [{
                    "pk": "3950000000000000000",
                    "image_versions2": {"candidates": [{
                      "url": "https://scontent.cdninstagram.com/v/other.jpg"
                    }]}
                  }]
                }
            """.trimIndent(),
        )

        assertTrue(result is ResolverResult.Failure)
        assertTrue((result as ResolverResult.Failure).error is ResolverError.ContentNotFound)
    }

    @Test
    fun loginRequiredPayloadIsReportedAsLoginRequired() {
        val result = resolver.parseAuthenticatedPayload(
            sourceUrl = sourceUrl,
            cookieHeader = cookieHeader,
            payload = """{"message":"login_required","login_required":true,"status":"fail"}""",
        )

        assertTrue(result is ResolverResult.Failure)
        assertTrue((result as ResolverResult.Failure).error is ResolverError.LoginRequired)
    }

    @Test
    fun thirdPartyProxyUrlIsRejected() {
        val result = resolver.parseAuthenticatedPayload(
            sourceUrl = sourceUrl,
            cookieHeader = cookieHeader,
            payload = """
                {
                  "items": [{
                    "pk": "3950932965561026193",
                    "image_versions2": {"candidates": [{
                      "url": "https://project.supabase.co/functions/v1/story"
                    }]}
                  }]
                }
            """.trimIndent(),
        )

        assertTrue(result is ResolverResult.Failure)
        assertTrue((result as ResolverResult.Failure).error is ResolverError.HtmlStructureChanged)
    }

    @Test
    fun resolvingWithoutSavedSessionStopsBeforeNetwork() = runBlocking {
        val result = resolver.resolve(sourceUrl)

        assertTrue(result is ResolverResult.Failure)
        assertTrue((result as ResolverResult.Failure).error is ResolverError.LoginRequired)
    }
}
