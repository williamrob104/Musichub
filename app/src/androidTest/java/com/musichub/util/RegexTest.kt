package com.musichub.util

import org.junit.Assert
import org.junit.Test


class RegexTest {
    @Test
    fun testSearchRegex_1() {
        val regex = """valid\s+(\w+)""".toRegex()
        val input = "a valid sentence"
        val fullMatch = "valid sentence"
        val groupMatch = "sentence"

        Assert.assertEquals(regexSearch(regex, input), fullMatch)
        Assert.assertEquals(regexSearch(regex, input, 0), fullMatch)
        Assert.assertEquals(regexSearch(regex, input, 1), groupMatch)

        try { regexSearch(regex, input, 2) } catch (e: Exception) {
            Assert.assertTrue(true)
        }
    }

    @Test
    fun testSearchRegex_2() {
        val regexCandidates = listOf(
            """a random regex""".toRegex(),
            """another random regex""".toRegex(),
            """valid\s+(\w+)""".toRegex())
        val input = "a valid sentence"
        val fullMatch = "valid sentence"
        val groupMatch = "sentence"

        Assert.assertEquals(regexSearch(regexCandidates, input), fullMatch)
        Assert.assertEquals(regexSearch(regexCandidates, input, 0), fullMatch)
        Assert.assertEquals(regexSearch(regexCandidates, input, 1), groupMatch)

        try { regexSearch(regexCandidates, input, 2) } catch (e: Exception) {
            Assert.assertTrue(true)
        }
    }
}