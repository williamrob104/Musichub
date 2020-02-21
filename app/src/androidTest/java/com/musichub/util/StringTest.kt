package com.musichub.util

import org.junit.Assert
import org.junit.Test


class StringTest {
    @Test
    fun testSearchRegex() {
        val regex = """valid\s+(\w+)""".toRegex()
        val input = "a valid sentence"
        val fullMatch = "valid sentence"
        val groupMatch = "sentence"

        Assert.assertEquals(input.regexSearch(regex), fullMatch)
        Assert.assertEquals(input.regexSearch(regex, 0), fullMatch)
        Assert.assertEquals(input.regexSearch(regex, 1), groupMatch)

        try {
            input.regexSearch(regex, 2)
        } catch (e: Exception) {
            Assert.assertTrue(true)
        }
    }
}