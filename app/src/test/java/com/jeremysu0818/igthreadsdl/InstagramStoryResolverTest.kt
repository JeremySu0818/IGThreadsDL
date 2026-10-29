package com.jeremysu0818.igthreadsdl

import com.jeremysu0818.igthreadsdl.data.resolver.InstagramStoryResolver
import com.jeremysu0818.igthreadsdl.domain.model.ManifestType
import com.jeremysu0818.igthreadsdl.domain.model.MediaItemType
import com.jeremysu0818.igthreadsdl.domain.resolver.ResolverError
import com.jeremysu0818.igthreadsdl.domain.resolver.ResolverResult
import kotlinx.coroutines.runBlocking
import okhttp3.ResponseBody.Companion.toResponseBody
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

    private val highlightUrl = "https://www.instagram.com/stories/highlights/123/"
    private val highlightPayload = """
        {"reels":{"highlight:123":{"title":"Travel","user":{"username":"creator"},
        "items":[
            {"pk":"101","image_versions2":{"candidates":[
                {"url":"https://scontent.cdninstagram.com/v/photo.jpg","width":1080,"height":1920}]}},
            {"pk":"102","video_versions":[
                {"url":"https://scontent.cdninstagram.com/v/video.mp4","width":1080,"height":1920}],
             "image_versions2":{"candidates":[
                {"url":"https://scontent.cdninstagram.com/v/cover.jpg","width":1080,"height":1920}]}}
        ]}}}
    """.trimIndent()

    @Test
    fun highlightReturnsAllItemsInOrderAsStories() {
        assertTrue(resolver.supports(highlightUrl))
        val result = resolver.parseAuthenticatedPayload(highlightUrl, highlightPayload, cookieHeader)
        assertTrue(result is ResolverResult.Success)
        val manifest = (result as ResolverResult.Success).manifest
        assertEquals(ManifestType.STORY, manifest.type)
        assertEquals(highlightUrl, manifest.sourceUrl)
        assertEquals("creator", manifest.author)
        assertEquals("Travel", manifest.title)
        assertEquals(listOf(MediaItemType.IMAGE, MediaItemType.VIDEO), manifest.items.map { it.type })
        assertEquals(2, manifest.items.map { it.filename }.toSet().size)
        assertTrue(manifest.items.none { "Cookie" in it.requestHeaders })
        assertTrue(manifest.items.all { it.requestHeaders["Referer"] == highlightUrl })
    }

    @Test
    fun highlightNewestFirstResponsesAreSortedByPublicationTime() {
        for (timestampKey in listOf("taken_at", "taken_at_timestamp")) {
            val root = org.json.JSONObject(highlightPayload)
            val reel = root.getJSONObject("reels").getJSONObject("highlight:123")
            val original = reel.getJSONArray("items")
            val older = original.getJSONObject(0).put(timestampKey, 1000)
            val newer = original.getJSONObject(1).put(timestampKey, 2000)
            reel.put("items", org.json.JSONArray().put(newer).put(older))
            val result = resolver.parseAuthenticatedPayload(highlightUrl, root.toString(), cookieHeader)
            val manifest = (result as ResolverResult.Success).manifest
            assertEquals(listOf("instagram_story_101", "instagram_story_102"), manifest.items.map { it.id })
            assertTrue(manifest.thumbnailUrl!!.endsWith("/photo.jpg"))
            assertEquals(listOf(MediaItemType.IMAGE, MediaItemType.VIDEO), manifest.items.map { it.type })
        }
    }

    @Test
    fun highlightMissingTimestampsUseNumericIdsWithoutOverflow() {
        val root = org.json.JSONObject(highlightPayload)
        val reel = root.getJSONObject("reels").getJSONObject("highlight:123")
        val original = reel.getJSONArray("items")
        val older = original.getJSONObject(0).put("pk", "9999999999999999999")
        val newer = original.getJSONObject(1).put("pk", "10000000000000000000")
        reel.put("items", org.json.JSONArray().put(newer).put(older))
        val result = resolver.parseAuthenticatedPayload(highlightUrl, root.toString(), cookieHeader)
        assertEquals(
            listOf("instagram_story_9999999999999999999", "instagram_story_10000000000000000000"),
            (result as ResolverResult.Success).manifest.items.map { it.id },
        )
    }

    @Test
    fun highlightTimestampsTakePriorityOverIds() {
        val root = org.json.JSONObject(highlightPayload)
        val items = root.getJSONObject("reels").getJSONObject("highlight:123").getJSONArray("items")
        items.getJSONObject(0).put("taken_at_timestamp", 2000)
        items.getJSONObject(1).put("taken_at_timestamp", 1000)
        val result = resolver.parseAuthenticatedPayload(highlightUrl, root.toString(), cookieHeader)
        assertEquals(
            listOf("instagram_story_102", "instagram_story_101"),
            (result as ResolverResult.Success).manifest.items.map { it.id },
        )
    }

    @Test
    fun encodedHighlightShareReturnsWholeCollectionRegardlessOfStartingStory() {
        for (start in listOf("102", "999")) {
            val url = "https://instagram.com/s/aGlnaGxpZ2h0OjEyMw?story_media_id=${start}_42&psln=tracking"
            assertTrue(resolver.supports(url))
            val result = resolver.parseAuthenticatedPayload(url, highlightPayload, cookieHeader)
            val manifest = (result as ResolverResult.Success).manifest
            assertEquals(listOf("instagram_story_101", "instagram_story_102"), manifest.items.map { it.id })
            assertTrue(manifest.sourceUrl.endsWith("?story_media_id=$start"))
        }
    }

    @Test
    fun highlightDoesNotSelectAnotherReelOrAcceptUntrustedMedia() {
        val wrongReel = resolver.parseAuthenticatedPayload(
            highlightUrl, highlightPayload.replace("highlight:123", "highlight:999"), cookieHeader,
        )
        assertTrue((wrongReel as ResolverResult.Failure).error is ResolverError.ContentNotFound)
        val untrusted = resolver.parseAuthenticatedPayload(
            highlightUrl, highlightPayload.replace("scontent.cdninstagram.com", "example.com"), cookieHeader,
        )
        assertTrue((untrusted as ResolverResult.Failure).error is ResolverError.HtmlStructureChanged)
    }

    @Test
    fun highlightFeedFailureFallsBackToGraphQlForWholeCollection() = runBlocking {
        val paths = mutableListOf<String>()
        val graphPayload = """
            {"data":{"reels_media":[{"id":"123","owner":{"username":"creator"},
            "items":[
              {"id":"101","display_resources":[
                {"src":"https://scontent.cdninstagram.com/v/photo.jpg","config_width":1080,"config_height":1920}]},
              {"id":"102","is_video":true,"video_url":"https://scontent.cdninstagram.com/v/video.mp4",
                "display_url":"https://scontent.cdninstagram.com/v/cover.jpg"}
            ]}]}}
        """.trimIndent()
        val client = okhttp3.OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            val isApi = request.method == "GET"
            val isGraph = request.url.encodedPath == "/graphql/query/"
            if (isApi) {
                paths += request.url.encodedPath
                assertEquals(cookieHeader, request.header("Cookie"))
                assertEquals("csrf123", request.header("X-CSRFToken"))
                if (isGraph) {
                    val variables = org.json.JSONObject(request.url.queryParameter("variables")!!)
                    assertEquals("123", variables.getJSONArray("highlight_reel_ids").getString(0))
                    assertEquals(0, variables.getJSONArray("reel_ids").length())
                }
            } else {
                assertEquals(null, request.header("Cookie"))
            }
            okhttp3.Response.Builder().request(request).protocol(okhttp3.Protocol.HTTP_1_1)
                .code(if (isApi && !isGraph) 400 else 200).message("response")
                .body((if (isGraph) graphPayload else "{}").toResponseBody()).build()
        }.build()
        val source = "https://instagram.com/s/aGlnaGxpZ2h0OjEyMw?story_media_id=101_42"
        val result = InstagramStoryResolver(client) { cookieHeader }.resolve(source)
        val manifest = (result as ResolverResult.Success).manifest
        assertEquals(listOf(MediaItemType.IMAGE, MediaItemType.VIDEO), manifest.items.map { it.type })
        assertTrue(manifest.items.none { "Cookie" in it.requestHeaders })
        assertEquals(listOf("/api/v1/feed/reels_media/", "/graphql/query/"), paths)
    }

    @Test
    fun highlightRequestUsesSavedSessionAndHighlightFeed() = runBlocking {
        val client = okhttp3.OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            val isFeed = request.method == "GET"
            if (isFeed) {
                assertEquals("/api/v1/feed/reels_media/", request.url.encodedPath)
                assertEquals("highlight:123", request.url.queryParameter("reel_ids"))
                assertEquals(cookieHeader, request.header("Cookie"))
                assertEquals("csrf123", request.header("X-CSRFToken"))
            } else {
                assertEquals(null, request.header("Cookie"))
            }
            okhttp3.Response.Builder().request(request).protocol(okhttp3.Protocol.HTTP_1_1)
                .code(200).message("OK")
                .body((if (isFeed) highlightPayload else "").toResponseBody()).build()
        }.build()
        val result = InstagramStoryResolver(client) { cookieHeader }.resolve(highlightUrl)
        assertTrue(result is ResolverResult.Success)
        assertEquals(2, (result as ResolverResult.Success).manifest.items.size)
        val loggedOut = resolver.resolve(highlightUrl)
        assertTrue((loggedOut as ResolverResult.Failure).error is ResolverError.LoginRequired)
    }

    @Test
    fun highlightVideoResourcesDownloadVideoAndKeepImageOnlyAsCover() {
        val payload = """
            {"data":{"reels_media":[{"id":"123","owner":{"username":"creator"},"items":[
              {"id":"102","is_video":true,
               "video_resources":[
                 {"src":"https://scontent.cdninstagram.com/v/small.mp4","config_width":480,"config_height":854},
                 {"src":"https://scontent.cdninstagram.com/v/hd.mp4","config_width":1080,"config_height":1920}],
               "display_url":"https://scontent.cdninstagram.com/v/cover.jpg"}
            ]}]}}
        """
        val result = resolver.parseAuthenticatedPayload(highlightUrl, payload, cookieHeader)
        val item = (result as ResolverResult.Success).manifest.items.single()
        assertEquals(MediaItemType.VIDEO, item.type)
        assertTrue(item.downloadUrl.endsWith("/hd.mp4"))
        assertTrue(item.filename.endsWith(".mp4"))
        assertTrue(item.thumbnailUrl!!.endsWith("/cover.jpg"))
    }

    @Test
    fun missingVideoNeverFallsBackToCoverImage() {
        for (marker in listOf("\"is_video\":true", "\"media_type\":2")) {
            val payload = """{"items":[{"pk":"3950932965561026193",$marker,
                "image_versions2":{"candidates":[{"url":"https://scontent.cdninstagram.com/v/cover.jpg"}]}}]}"""
            val result = resolver.parseAuthenticatedPayload(sourceUrl, payload, cookieHeader)
            assertTrue((result as ResolverResult.Failure).error is ResolverError.HtmlStructureChanged)
        }
    }

    @Test
    fun highlightMissingVideoIsFetchedWithSessionWithoutLosingOtherItems() = runBlocking {
        val paths = mutableListOf<String>()
        val client = okhttp3.OkHttpClient.Builder().addInterceptor { chain ->
            val request = chain.request()
            val body = if (request.method == "HEAD") "" else {
                paths += request.url.encodedPath
                assertEquals(cookieHeader, request.header("Cookie"))
                when (request.url.encodedPath) {
                    "/api/v1/feed/reels_media/" -> org.json.JSONObject(highlightPayload).apply {
                        getJSONObject("reels").getJSONObject("highlight:123")
                            .getJSONArray("items").getJSONObject(1).apply {
                                remove("video_versions")
                                put("is_video", true)
                            }
                    }.toString()
                    "/api/v1/media/102/info/" -> """
                        {"items":[{"pk":"102","media_type":2,"video_versions":[
                          {"url":"https://scontent.cdninstagram.com/v/recovered.mp4","width":1080,"height":1920}
                        ]}]}
                    """
                    else -> error("Unexpected request: ${request.url.encodedPath}")
                }
            }
            okhttp3.Response.Builder().request(request).protocol(okhttp3.Protocol.HTTP_1_1)
                .code(200).message("OK").body(body.toResponseBody()).build()
        }.build()
        val result = InstagramStoryResolver(client) { cookieHeader }.resolve(highlightUrl)
        val items = (result as ResolverResult.Success).manifest.items
        assertEquals(listOf(MediaItemType.IMAGE, MediaItemType.VIDEO), items.map { it.type })
        assertTrue(items.last().downloadUrl.endsWith("/recovered.mp4"))
        assertEquals(listOf("/api/v1/feed/reels_media/", "/api/v1/media/102/info/"), paths)
    }

    @Test
    fun resolvingWithoutSavedSessionStopsBeforeNetwork() = runBlocking {
        val result = resolver.resolve(sourceUrl)

        assertTrue(result is ResolverResult.Failure)
        assertTrue((result as ResolverResult.Failure).error is ResolverError.LoginRequired)
    }
}
