package com.jeremysu0818.igthreadsdownloader.overlay

import android.view.MotionEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayInteractionTest {
    @Test
    fun `completed preview remains available when clipboard is empty`() {
        assertEquals(
            BubbleContentAction.SHOW_CURRENT,
            bubbleContentAction(
                clipboardUrl = null,
                detectedUrl = "https://www.instagram.com/p/existing",
                hasManifest = true,
            ),
        )
    }

    @Test
    fun `empty clipboard shows guidance when there is no preview`() {
        assertEquals(
            BubbleContentAction.SHOW_CLIPBOARD_HINT,
            bubbleContentAction(
                clipboardUrl = null,
                detectedUrl = null,
                hasManifest = false,
            ),
        )
    }

    @Test
    fun `same clipboard URL reopens completed preview`() {
        val url = "https://www.threads.net/@user/post/existing"
        assertEquals(
            BubbleContentAction.SHOW_CURRENT,
            bubbleContentAction(
                clipboardUrl = url,
                detectedUrl = url,
                hasManifest = true,
            ),
        )
    }

    @Test
    fun `new clipboard URL starts a new resolve`() {
        assertEquals(
            BubbleContentAction.RESOLVE_CLIPBOARD,
            bubbleContentAction(
                clipboardUrl = "https://www.instagram.com/p/new",
                detectedUrl = "https://www.instagram.com/p/existing",
                hasManifest = true,
            ),
        )
    }

    @Test
    fun `dropping over close target closes overlay`() {
        assertTrue(shouldCloseAfterDrag(MotionEvent.ACTION_UP, isOverCloseTarget = true))
    }

    @Test
    fun `canceled drag never closes overlay`() {
        assertFalse(shouldCloseAfterDrag(MotionEvent.ACTION_CANCEL, isOverCloseTarget = true))
    }
}
