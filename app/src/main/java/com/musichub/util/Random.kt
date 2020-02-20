package com.musichub.util

import kotlin.random.Random


private val idCharacters = charArrayOf(
    'A','B','C','D','E','F','G','H','I','J','K','L','M','N','O','P','Q','R','S','T','U','V','W','X','Y','Z',
    '0','1','2','3','4','5','6','7','8','9'
)
internal fun Random.nextId(length: Int=10): String {
    val sb = StringBuilder()
    for (i in 0 until length)
        sb.append(idCharacters[this.nextInt(idCharacters.size)])
    return sb.toString()
}