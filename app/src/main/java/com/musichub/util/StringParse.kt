package com.musichub.util


internal fun parseTime(timeStr: String): Int {
    val tokens = timeStr.split(':')
    return when (tokens.size) {
        1 -> tokens[0].toInt()
        2 -> tokens[0].toInt() * 60 + tokens[1].toInt()
        3 -> tokens[0].toInt() * 3600 + tokens[1].toInt() * 60 + tokens[2].toInt()
        else -> throw IllegalArgumentException()
    }
}

internal fun formatTime(timeSeconds: Int): String {
    var rem = timeSeconds
    val h = rem / 3600; rem %= 3600
    val m = rem / 60; rem %= 60
    val s = rem
    return if (h != 0) {
        "%d:%02d:%02d".format(h, m, s)
    } else {
        "%d:%02d".format(m, s)
    }
}


private fun parseFirstNumString(str: String): String {
    val regex = """-?[0-9]+""".toRegex()
    val match = regex.find(str) ?: throw IllegalArgumentException("cannot find integer in '$str'")
    return match.value
}

internal fun parseFirstInt(str: String): Int {
    return parseFirstNumString(str).toInt()
}

internal fun parseFirstLong(str: String): Long {
    return parseFirstNumString(str).toLong()
}