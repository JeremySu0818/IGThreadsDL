package com.jeremysu0818.igthreadsdl

import com.jeremysu0818.igthreadsdl.data.session.InstagramSessionManager
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InstagramSessionManagerTest {
    @Test
    fun cookieParserPreservesValuesContainingEqualsCharacters() {
        val cookies = InstagramSessionManager.parseCookies(
            "csrftoken=csrf123; sessionid=abc==; ds_user_id=42",
        )

        assertEquals("csrf123", cookies["csrftoken"])
        assertEquals("abc==", cookies["sessionid"])
        assertEquals("42", cookies["ds_user_id"])
    }

    @Test
    fun malformedCookiePartsAreIgnored() {
        val cookies = InstagramSessionManager.parseCookies("broken; =empty; valid=yes")

        assertFalse(cookies.containsKey("broken"))
        assertFalse(cookies.containsKey(""))
        assertTrue(cookies["valid"] == "yes")
    }
}
