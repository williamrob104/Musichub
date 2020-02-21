package com.musichub.util

import org.junit.Assert.assertEquals
import org.junit.Test


class StringAlgorithmTest {
    @Test
    fun testLongestCommonSubsequence() {
        val equals = { i1: Int, i2: Int -> i1 == i2 }
        val testCases = listOf(
            Triple("abc", "abc", 3),
            Triple("abc", "abcde", 3),
            Triple("bcd", "abcde", 3),
            Triple("cde", "abcde", 3),
            Triple("ace", "abcde", 3),
            Triple("abc", "def", 0),
            Triple("", "abc", 0)
        )
        for ((str1, str2, lcs) in testCases)
            assertEquals(
                "longestCommonSubsequence($str1, $str2) != $lcs",
                longestCommonSubsequence(str1, str2, equals), lcs
            )
    }

    @Test
    fun testLongestCommonSubstring() {
        val equals = { i1: Int, i2: Int -> i1 == i2 }
        val testCases = listOf(
            Triple("abc", "abc", 3),
            Triple("abc", "abcde", 3),
            Triple("bcd", "abcde", 3),
            Triple("cde", "abcde", 3),
            Triple("ace", "abcde", 1),
            Triple("abc", "def", 0),
            Triple("", "abc", 0)
        )
        for ((str1, str2, lcs) in testCases)
            assertEquals(
                "longestCommonSubstring($str1, $str2) != $lcs",
                longestCommonSubstring(str1, str2, equals), lcs
            )
    }

    @Test
    fun testEditDistance() {
        val equals = { i1: Int, i2: Int -> i1 == i2 }
        val testCases = listOf(
            Triple("abc", "abc", 0),
            Triple("abc", "abcde", 2),
            Triple("bcd", "abcde", 2),
            Triple("cde", "abcde", 2),
            Triple("ace", "abcde", 2),
            Triple("abc", "def", 3),
            Triple("", "abc", 3)
        )
        for ((str1, str2, ed) in testCases)
            assertEquals(
                "editDistance($str1, $str2) != $ed",
                editDistance(str1, str2, equals), ed
            )
    }
}