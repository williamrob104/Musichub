package com.musichub.util

import kotlin.math.max


private val capitalRange = "A".toInt().."Z".toInt()
private val offset = "A".toInt() - "a".toInt()
internal fun sameEnglishLetter(codePoint1: Int, codePoint2: Int): Boolean {
    return (if (codePoint1 in capitalRange) codePoint1 - offset else codePoint1) ==
            (if (codePoint2 in capitalRange) codePoint2 - offset else codePoint2)
}

internal inline fun longestCommonSubsequence(
    str1: String,
    str2: String,
    matchFunc: (Int, Int) -> Boolean = ::sameEnglishLetter
): Int {
    val seq1 = str1.codePointsCompat()
    val seq2 = str2.codePointsCompat()
    val s1 = if (seq1.size < seq2.size) seq2 else seq1
    val s2 = if (seq1.size < seq2.size) seq1 else seq2
    val dyn = arrayOf(IntArray(s2.size + 1), IntArray(s2.size + 1))
    for (i in 1..s1.size) {
        val curr = i % 2
        val prev = (i - 1) % 2
        dyn[curr][0] = 0
        for (j in 1..s2.size) {
            if (matchFunc(s1[i - 1], s2[j - 1]))
                dyn[curr][j] = dyn[prev][j - 1] + 1
            else
                dyn[curr][j] = max(dyn[prev][j], dyn[curr][j - 1])
        }
    }
    return dyn[s1.size % 2][s2.size]
}

internal inline fun longestCommonSubstring(
    str1: String,
    str2: String,
    matchFunc: (Int, Int) -> Boolean = ::sameEnglishLetter
): Int {
    val seq1 = str1.codePointsCompat()
    val seq2 = str2.codePointsCompat()
    val s1 = if (seq1.size < seq2.size) seq2 else seq1
    val s2 = if (seq1.size < seq2.size) seq1 else seq2
    val dyn = arrayOf(IntArray(s2.size + 1), IntArray(s2.size + 1))
    var result = 0
    for (i in 1..s1.size) {
        val curr = i % 2
        val prev = (i - 1) % 2
        dyn[curr][0] = 0
        for (j in 1..s2.size) {
            if (matchFunc(s1[i - 1], s2[j - 1])) {
                dyn[curr][j] = dyn[prev][j - 1] + 1
                result = max(result, dyn[curr][j])
            } else
                dyn[curr][j] = 0
        }
    }
    return result
}

internal inline fun editDistance(
    str1: String, str2: String, matchFunc: (Int, Int) -> Boolean = ::sameEnglishLetter,
    insertOrDeleteCost: Int = 1, substituteCost: Int = 1
): Int {
    val seq1 = str1.codePointsCompat()
    val seq2 = str2.codePointsCompat()
    val s1 = if (seq1.size < seq2.size) seq2 else seq1
    val s2 = if (seq1.size < seq2.size) seq1 else seq2
    val dyn = arrayOf(
        IntArray(s2.size + 1) { j: Int -> j },
        IntArray(s2.size + 1)
    )
    for (i in 1..s1.size) {
        val curr = i % 2
        val prev = (i - 1) % 2
        dyn[curr][0] = i
        for (j in 1..s2.size) {
            if (matchFunc(s1[i - 1], s2[j - 1])) {
                dyn[curr][j] = dyn[prev][j - 1]
            } else {
                dyn[curr][j] = arrayOf(
                    dyn[prev][j - 1] + substituteCost,
                    dyn[prev][j] + insertOrDeleteCost,
                    dyn[curr][j - 1] + insertOrDeleteCost
                ).min()!!
            }
        }
    }
    return dyn[s1.size % 2][s2.size]
}