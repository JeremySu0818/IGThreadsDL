package com.jeremysu0818.igthreadsdl

import com.jeremysu0818.igthreadsdl.data.resolver.InstagramStoryResolver
import com.jeremysu0818.igthreadsdl.domain.model.ManifestType
import com.jeremysu0818.igthreadsdl.domain.resolver.ResolverResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Opt-in integration check for a currently active public Instagram Story.
 *
 * Run with:
 * RUN_LIVE_RESOLVER_TESTS=true \
 * INSTAGRAM_STORY_LIVE_TEST_URL=https://www.instagram.com/stories/user/id/ \
 * INSTAGRAM_SESSION_COOKIE='sessionid=…; csrftoken=…' \
 * ./gradlew testDebugUnitTest --tests '*InstagramStoryLiveValidationTest'
 */
class InstagramStoryLiveValidationTest {
    @Test
    fun activePublicStoryReturnsDownloadableMedia() = runBlocking {
        val sourceUrl = System.getenv("INSTAGRAM_STORY_LIVE_TEST_URL")
        val cookieHeader = System.getenv("INSTAGRAM_SESSION_COOKIE")
        assumeTrue(
            System.getenv("RUN_LIVE_RESOLVER_TESTS") == "true" &&
                !sourceUrl.isNullOrBlank() &&
                !cookieHeader.isNullOrBlank(),
        )

        val result = InstagramStoryResolver(
            sessionProvider = { checkNotNull(cookieHeader) },
        ).resolve(checkNotNull(sourceUrl))

        assertTrue(result is ResolverResult.Success)
        val manifest = (result as ResolverResult.Success).manifest
        assertEquals(ManifestType.STORY, manifest.type)
        assertEquals(1, manifest.items.size)
        val item = manifest.items.single()
        assertEquals(com.jeremysu0818.igthreadsdl.domain.model.MediaItemType.VIDEO, item.type)
        assertEquals("video/mp4", item.mimeType)
        assertTrue(item.downloadUrl.startsWith("https://"))

        // Physically download the resolved URL to disk
        val client = okhttp3.OkHttpClient()
        val request = okhttp3.Request.Builder()
            .url(item.downloadUrl)
            .get()
            .apply {
                item.requestHeaders.forEach { (k, v) -> header(k, v) }
            }
            .build()

        val saveFile = java.io.File("/tmp/actual_story_video.mp4")
        client.newCall(request).execute().use { response ->
            assertTrue("HTTP request for video must succeed", response.isSuccessful)
            val bytes = response.body?.bytes()
            assertNotNull(bytes)
            saveFile.writeBytes(checkNotNull(bytes))
        }

        println("=== PHYSICAL FILE DOWNLOAD COMPLETED ===")
        println("SAVED PATH: ${saveFile.absolutePath}")
        println("FILE SIZE: ${saveFile.length()} bytes")
        println("EXISTS: ${saveFile.exists()}")
        println("=========================================")

        assertTrue(saveFile.exists())
        assertTrue(saveFile.length() > 10000)
    }
}
