package com.jeremysu0818.igthreadsdownloader.accessibility

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLaunchAccessibilityServiceTest {
    private val ownPackage = "com.jeremysu0818.igthreadsdownloader"
    private val inputMethods = setOf(
        "com.google.android.inputmethod.latin",
        "com.samsung.android.honeyboard",
    )

    @Test
    fun `opening or closing a keyboard does not stop the overlay`() {
        inputMethods.forEach { inputMethodPackage ->
            assertEquals(
                ForegroundWindowAction.IGNORE,
                foregroundWindowAction(inputMethodPackage, ownPackage, inputMethods),
            )
        }
    }

    @Test
    fun `target apps start the overlay`() {
        assertEquals(
            ForegroundWindowAction.START,
            foregroundWindowAction("com.instagram.android", ownPackage, inputMethods),
        )
        assertEquals(
            ForegroundWindowAction.START,
            foregroundWindowAction("com.instagram.barcelona", ownPackage, inputMethods),
        )
    }

    @Test
    fun `unrelated apps stop an automatically started overlay`() {
        assertEquals(
            ForegroundWindowAction.STOP,
            foregroundWindowAction("com.example.other", ownPackage, inputMethods),
        )
    }

    @Test
    fun `own and system windows do not change overlay state`() {
        assertEquals(
            ForegroundWindowAction.IGNORE,
            foregroundWindowAction(ownPackage, ownPackage, inputMethods),
        )
        assertEquals(
            ForegroundWindowAction.IGNORE,
            foregroundWindowAction("com.android.systemui", ownPackage, inputMethods),
        )
    }
}
