package com.freemusic.musicbox.util


internal fun String.codePointsCompat(): IntArray {
    if (android.os.Build.VERSION.SDK_INT >= 24)
        return this.codePoints().toArray()

    val codePoints = ArrayList<Int>()
    val length = this.length
    var offset = 0
    while (offset < length) {
        val codePoint = this.codePointAt(offset)
        offset += Character.charCount(codePoint)
        codePoints.add(codePoint)
    }
    return codePoints.toIntArray()
}

internal fun String.splitBtIndex(index: Int): Pair<String, String> {
    return Pair(
        try { this.substring(0, index) } catch(e: StringIndexOutOfBoundsException) { "" },
        try { this.substring(index+1) } catch(e: StringIndexOutOfBoundsException) { "" })
}