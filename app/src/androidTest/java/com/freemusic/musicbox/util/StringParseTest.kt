package com.freemusic.musicbox.util

import org.junit.Assert.assertEquals
import org.junit.Test


class StringParseTest {
    @Test fun testFormatTime() {
        val suits = listOf(
            3601 to "1:00:01",
            601 to "10:01",
            61 to "1:01",
            11 to "0:11",
            1 to "0:01"
        )
        for ((timeSec, timeStr) in suits)
            assertEquals(formatTime(timeSec), timeStr)
    }
}