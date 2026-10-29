package com.jeremysu0818.igthreadsdl

import com.jeremysu0818.igthreadsdl.data.resolver.ThreadsHtmlResolver
import com.jeremysu0818.igthreadsdl.domain.model.ManifestType
import com.jeremysu0818.igthreadsdl.domain.model.MediaItemType
import com.jeremysu0818.igthreadsdl.domain.resolver.ResolverError
import com.jeremysu0818.igthreadsdl.domain.resolver.ResolverResult
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import okhttp3.ResponseBody.Companion.toResponseBody

class ThreadsHtmlResolverTest {
    private val resolver = ThreadsHtmlResolver()
    private val sourceUrl =
        "https://www.threads.com/@jere.my0818/post/Da-qUPhgayG" +
            "?xmt=AQG0z-e4gEeGf__WPzUsgnz4MxUrverhOKD5A5WpmlY4jQ"

    @Test
    fun endpointUsesOfficialEmbedPage() {
        val endpoint = resolver.buildEndpoint(sourceUrl)

        assertEquals("www.threads.com", endpoint.host)
        assertEquals("/t/Da-qUPhgayG/embed", endpoint.encodedPath)
        assertFalse(endpoint.toString().contains("threadsphotodownloader"))
    }

    @Test
    fun shareEndpointIsFetchedDirectlyBeforeResolvingItsPermalink() {
        val endpoint = resolver.buildEndpoint("https://www.threads.com/share/Bk6vY7HQu-/")

        assertEquals("/share/Bk6vY7HQu-/", endpoint.encodedPath)
    }

    @Test
    fun postUrlIsExtractedFromSharePageCanonicalMetadata() {
        val html = """
            <html><head>
              <meta property="al:android:url"
                    content="https://www.threads.com/&#064;fallback/post/Fallback123" />
              <link rel="canonical"
                    href="https://www.threads.com/&#064;bing_sunzhi/post/DczreZyAXtC" />
            </head></html>
        """.trimIndent()

        assertEquals(
            "https://www.threads.com/@bing_sunzhi/post/DczreZyAXtC",
            resolver.extractPostUrl(html),
        )
    }

    @Test
    fun fixtureExtractsAllImageVideoAndDownloadLinksAndFiltersUiAssets() {
        val html = checkNotNull(
            javaClass.classLoader?.getResource("fixtures/threads_download.html"),
        ).readText()

        val result = resolver.parseHtml(
            sourceUrl = sourceUrl,
            html = html,
            baseUrl = "https://cdn.threadsphotodownloader.com/result/",
            cookieHeader = "NEXT_LOCALE=en",
        )

        assertTrue(result is ResolverResult.Success)
        val manifest = (result as ResolverResult.Success).manifest
        assertEquals(ManifestType.CAROUSEL, manifest.type)
        assertEquals(4, manifest.items.size)
        assertEquals(3, manifest.items.count { it.type == MediaItemType.IMAGE })
        assertEquals(1, manifest.items.count { it.type == MediaItemType.VIDEO })
        assertTrue(manifest.items.all { it.requestHeaders["Cookie"] == "NEXT_LOCALE=en" })
        assertTrue(manifest.items.none { "logo" in it.downloadUrl || "thumbnailsite" in it.downloadUrl })
        assertTrue(manifest.items.any { "photo-one.webp" in it.downloadUrl })
        assertTrue(manifest.items.any { "photo-three.avif" in it.downloadUrl && "&y=2" in it.downloadUrl })
    }

    @Test
    fun jsonLoginResponseKeepsThreadsPlatform() {
        val result = resolver.parseHtml(sourceUrl, """{"login_required":true,"status":"fail"}""")
        val error = (result as ResolverResult.Failure).error as ResolverError.LoginRequired
        assertEquals("threads", error.platform)
    }

    @Test
    fun explicitLoginWallIsReportedForThreads() {
        val result = resolver.parseHtml(sourceUrl, "<main>Log in to view this content</main>")
        val error = (result as ResolverResult.Failure).error as ResolverError.LoginRequired
        assertEquals("threads", error.platform)
    }

    @Test
    fun footerLoginAndScriptTranslationsDoNotImplyLoginIsRequired() {
        val result = resolver.parseHtml(sourceUrl, """
            <main>No media exposed</main><footer><a href="/login">Log in</a></footer>
            <script>{"translation":"Log in to view","login_required":false}</script>
        """)
        assertTrue((result as ResolverResult.Failure).error is ResolverError.HtmlStructureChanged)
    }

    @Test
    fun http200LoginRedirectStopsBeforeSharePermalinkExtraction() = kotlinx.coroutines.runBlocking {
        val client = okhttp3.OkHttpClient.Builder().addInterceptor { chain ->
            okhttp3.Response.Builder()
                .request(chain.request().newBuilder().url("https://www.threads.com/login").build())
                .protocol(okhttp3.Protocol.HTTP_1_1).code(200).message("OK")
                .body("<html>Log in</html>".toResponseBody()).build()
        }.build()
        val result = ThreadsHtmlResolver(client).resolve("https://www.threads.com/share/abc/")
        val error = (result as ResolverResult.Failure).error as ResolverError.LoginRequired
        assertEquals("threads", error.platform)
    }

    @Test
    fun unresolvedShareLinkHasActionableError() = kotlinx.coroutines.runBlocking {
        val client = okhttp3.OkHttpClient.Builder().addInterceptor { chain ->
            okhttp3.Response.Builder().request(chain.request())
                .protocol(okhttp3.Protocol.HTTP_1_1).code(200).message("OK")
                .body("<html>No permalink</html>".toResponseBody()).build()
        }.build()
        val result = ThreadsHtmlResolver(client).resolve("https://www.threads.com/share/abc/")
        assertTrue((result as ResolverResult.Failure).error is ResolverError.ShareLinkUnresolved)
    }

    @Test
    fun changedHtmlReturnsTypedErrorInsteadOfCrashing() {
        val result = resolver.parseHtml(
            sourceUrl = sourceUrl,
            html = "<html><body><p>No result component</p></body></html>",
        )

        assertTrue(result is ResolverResult.Failure)
        assertTrue(
            (result as ResolverResult.Failure).error is ResolverError.HtmlStructureChanged,
        )
    }
}
