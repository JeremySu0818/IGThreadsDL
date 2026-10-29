package com.jeremysu0818.igthreadsdl

import com.jeremysu0818.igthreadsdl.data.resolver.InstagramPublicResolver
import com.jeremysu0818.igthreadsdl.domain.model.ManifestType
import com.jeremysu0818.igthreadsdl.domain.model.MediaItemType
import com.jeremysu0818.igthreadsdl.domain.resolver.ResolverError
import com.jeremysu0818.igthreadsdl.domain.resolver.ResolverResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import okhttp3.ResponseBody.Companion.toResponseBody

class InstagramPublicResolverTest {
    private val resolver = InstagramPublicResolver()
    private val sourceUrl = "https://www.instagram.com/p/Carousel123/"

    @Test
    fun explicitAuthenticationFailureIsNotMisclassifiedAsStructureChange() {
        val payload = """{"message":"challenge_required","status":"fail"}"""
        assertTrue((resolver.parseGraphQlPayload(sourceUrl, payload) as ResolverResult.Failure)
            .error is ResolverError.LoginRequired)
        assertTrue((resolver.parseHtml(sourceUrl, payload) as ResolverResult.Failure)
            .error is ResolverError.LoginRequired)
    }

    @Test
    fun scriptTranslationAndFooterLoginDoNotMisclassifyUnknownResponse() {
        val result = resolver.parseHtml(sourceUrl, """
            <html><body><main>No media exposed</main><footer>Log in</footer>
            <script>{"translation":"login_required","require_login":true}</script></body></html>
        """)
        assertTrue((result as ResolverResult.Failure).error is ResolverError.HtmlStructureChanged)
    }

    @Test
    fun privatePostFallbackUsesCurrentSessionAndUpdatedCsrfWithoutLeakingToCdn() =
        kotlinx.coroutines.runBlocking {
            var graphqlRequested = false
            val client = okhttp3.OkHttpClient.Builder().addInterceptor { chain ->
                val request = chain.request()
                val body = when {
                    request.url.host.endsWith("cdninstagram.com") -> {
                        assertEquals(null, request.header("Cookie"))
                        ""
                    }
                    request.url.encodedPath == "/graphql/query" -> {
                        graphqlRequested = true
                        assertEquals("sessionid=logged-in; csrftoken=fresh", request.header("Cookie"))
                        assertEquals("fresh", request.header("X-CSRFToken"))
                        """{"data":{"xdt_api__v1__media__shortcode__web_info":{"items":[{
                            "image_versions2":{"candidates":[{
                                "url":"https://scontent.cdninstagram.com/v/private.jpg",
                                "width":1080,"height":1080
                            }]}
                        }]}}}"""
                    }
                    else -> {
                        assertEquals("sessionid=logged-in; csrftoken=old", request.header("Cookie"))
                        """<html><body>This account is private
                            <script>["LSD",[],{"token":"test-lsd"}]</script>
                        </body></html>"""
                    }
                }
                okhttp3.Response.Builder().request(request)
                    .protocol(okhttp3.Protocol.HTTP_1_1).code(200).message("OK")
                    .apply {
                        if (request.url.encodedPath.startsWith("/p/")) {
                            addHeader("Set-Cookie", "csrftoken=fresh; Path=/; Secure")
                        }
                    }
                    .body(body.toResponseBody()).build()
            }.build()
            val authenticatedResolver = InstagramPublicResolver(client) {
                "sessionid=logged-in; csrftoken=old"
            }
            val result = authenticatedResolver.resolve(sourceUrl)
            assertTrue(result is ResolverResult.Success)
            assertTrue(graphqlRequested)
            assertFalse((result as ResolverResult.Success).manifest.items.single()
                .requestHeaders.containsKey("Cookie"))
        }

    @Test
    fun verifiedSidecarCreatesOneItemPerNodeAndDoesNotDownloadVideoCover() {
        val html = checkNotNull(
            javaClass.classLoader?.getResource("fixtures/instagram_carousel.html"),
        ).readText()

        val result = resolver.parseHtml(sourceUrl, html)

        assertTrue(result is ResolverResult.Success)
        val manifest = (result as ResolverResult.Success).manifest
        assertEquals(ManifestType.CAROUSEL, manifest.type)
        assertEquals("jere.my0818", manifest.author)
        assertEquals(2, manifest.items.size)
        assertEquals(1, manifest.items.count { it.type == MediaItemType.IMAGE })
        assertEquals(1, manifest.items.count { it.type == MediaItemType.VIDEO })
        assertFalse(manifest.isPartial)
        assertTrue(manifest.items.none { "video-cover" in it.downloadUrl })
        assertTrue(manifest.items.none { "/preview/" in it.downloadUrl })
    }

    @Test
    fun visiblePrivatePageReturnsPrivateContent() {
        val result = resolver.parseHtml(
            sourceUrl,
            "<html><body><main>This account is private</main></body></html>",
        )

        assertTrue(result is ResolverResult.Failure)
        assertTrue((result as ResolverResult.Failure).error is ResolverError.PrivateContent)
    }

    @Test
    fun translationStringInsideScriptDoesNotMisclassifyPublicPostAsPrivate() {
        val result = resolver.parseHtml(
            sourceUrl,
            """
            <html><head>
              <meta property="og:image"
                    content="https://scontent.cdninstagram.com/v/media/public.jpg?x=1">
            </head><body><main>Public media</main>
              <script>{"translation":"This account is private"}</script>
            </body></html>
            """.trimIndent(),
        )

        assertTrue(result is ResolverResult.Success)
    }

    @Test
    fun graphQlReelUsesVideoVersionAndKeepsImageAsThumbnailOnly() {
        val result = resolver.parseGraphQlPayload(
            sourceUrl = "https://www.instagram.com/reel/Reel123/",
            payload = """
                {
                  "data": {
                    "xdt_api__v1__media__shortcode__web_info": {
                      "items": [{
                        "media_type": 2,
                        "video_versions": [
                          {"width": 480, "height": 854,
                           "url": "https://instagram.example.fbcdn.net/v/reel-small.mp4?token=1"},
                          {"width": 1080, "height": 1920,
                           "url": "https://instagram.example.fbcdn.net/v/reel-hd.mp4?token=2"}
                        ],
                        "image_versions2": {"candidates": [
                          {"width": 1080, "height": 1920,
                           "url": "https://instagram.example.fbcdn.net/v/reel-cover.jpg?token=3"}
                        ]},
                        "user": {"username": "public.creator"},
                        "caption": {"text": "Public reel"}
                      }]
                    }
                  }
                }
            """.trimIndent(),
            cookieHeader = "csrftoken=test",
        )

        assertTrue(result is ResolverResult.Success)
        val manifest = (result as ResolverResult.Success).manifest
        assertEquals(ManifestType.REEL, manifest.type)
        assertEquals("public.creator", manifest.author)
        assertEquals(1, manifest.items.size)
        assertEquals(MediaItemType.VIDEO, manifest.items.single().type)
        assertTrue("reel-hd.mp4" in manifest.items.single().downloadUrl)
        assertTrue("reel-cover.jpg" in checkNotNull(manifest.items.single().thumbnailUrl))
        assertTrue(manifest.items.none { "reel-cover.jpg" in it.downloadUrl })
        assertFalse(manifest.items.single().requestHeaders.containsKey("Cookie"))
    }

    @Test
    fun graphQlCarouselCreatesOneDownloadForEachPhotoAndVideoNode() {
        val result = resolver.parseGraphQlPayload(
            sourceUrl = sourceUrl,
            payload = """
                {
                  "data": {
                    "xdt_api__v1__media__shortcode__web_info": {
                      "items": [{
                        "user": {"username": "carousel.owner"},
                        "carousel_media": [
                          {
                            "media_type": 1,
                            "image_versions2": {"candidates": [
                              {"width": 1440, "height": 1080,
                               "url": "https://instagram.example.fbcdn.net/v/photo.jpg?token=1"}
                            ]}
                          },
                          {
                            "media_type": 2,
                            "video_versions": [
                              {"width": 1080, "height": 1920,
                               "url": "https://instagram.example.fbcdn.net/v/video.mp4?token=2"}
                            ],
                            "image_versions2": {"candidates": [
                              {"width": 1080, "height": 1920,
                               "url": "https://instagram.example.fbcdn.net/v/video-cover.jpg?token=3"}
                            ]}
                          }
                        ]
                      }]
                    }
                  }
                }
            """.trimIndent(),
        )

        assertTrue(result is ResolverResult.Success)
        val manifest = (result as ResolverResult.Success).manifest
        assertEquals(ManifestType.CAROUSEL, manifest.type)
        assertEquals(2, manifest.items.size)
        assertEquals(1, manifest.items.count { it.type == MediaItemType.IMAGE })
        assertEquals(1, manifest.items.count { it.type == MediaItemType.VIDEO })
        assertTrue(manifest.items.none { "video-cover.jpg" in it.downloadUrl })
        assertFalse(manifest.isPartial)
    }
}
