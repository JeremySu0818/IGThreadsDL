package com.jeremysu0818.igthreadsdl

import com.jeremysu0818.igthreadsdl.data.resolver.ThreadsHtmlResolver
import com.jeremysu0818.igthreadsdl.domain.resolver.ResolverResult
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertTrue
import org.junit.Assume.assumeTrue
import org.junit.Test

/**
 * Opt-in integration check for the confirmed production endpoint.
 *
 * Run with:
 * RUN_LIVE_RESOLVER_TESTS=true ./gradlew testDebugUnitTest
 */
class ThreadsLiveValidationTest {
    @Test
    fun confirmedThreadsUrlReturnsRealMedia() = runBlocking {
        assumeTrue(System.getenv("RUN_LIVE_RESOLVER_TESTS") == "true")
        val sourceUrl =
            "https://www.threads.com/@jiangmingyuan9/post/Da9u2HzlAaT"

        val result = ThreadsHtmlResolver().resolve(sourceUrl)

        assertTrue("Expected success but got $result", result is ResolverResult.Success)
        val items = (result as ResolverResult.Success).manifest.items
        assertTrue(items.isNotEmpty())
        assertTrue(items.all { it.downloadUrl.startsWith("https://") })
        assertTrue(items.none { "thumbnailsite" in it.downloadUrl || "favicon" in it.downloadUrl })
    }

    @Test
    fun threadsShareUrlResolvesPermalinkAndReturnsRealMedia() = runBlocking {
        assumeTrue(System.getenv("RUN_LIVE_RESOLVER_TESTS") == "true")

        val result = ThreadsHtmlResolver().resolve(
            "https://www.threads.com/share/Bk6vY7HQu-/",
        )

        assertTrue("Expected success but got $result", result is ResolverResult.Success)
        val manifest = (result as ResolverResult.Success).manifest
        assertTrue("Share URL should resolve to a post URL", "/post/" in manifest.sourceUrl)
        assertTrue(manifest.items.isNotEmpty())
        assertTrue(manifest.items.all { it.downloadUrl.startsWith("https://") })
    }
}
