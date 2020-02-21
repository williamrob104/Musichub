package com.musichub.scraper


interface Image {
    fun sourceByShortSideEquals(pixels: Int, largerIfNotPresent: Boolean = true): Source

    fun sourceByLongSideEquals(pixels: Int, largerIfNotPresent: Boolean = true): Source

    fun sourceLargest(): Source

    data class Source(
        val url: String,
        val width: Int,
        val height: Int
    )
}

class SingleSourceImage(url: String, width: Int, height: Int) : Image {
    private val source = Image.Source(url, width, height)

    override fun sourceByShortSideEquals(pixels: Int, largerIfNotPresent: Boolean) = source

    override fun sourceByLongSideEquals(pixels: Int, largerIfNotPresent: Boolean) = source

    override fun sourceLargest() = source
}


interface Track {
    val title: String
    val artistName: String
    val albumTitle: String?
    val length: Int
}


class Date(
    val year: Int,
    _month: Int?,
    _day: Int?
) : Comparable<Date> {

    val month: Int? = if (_month != null && _month in 1..12) _month else null
    val day: Int? = if (month != null) _day else null

    override fun compareTo(other: Date): Int {
        var comp = year - other.year
        if (comp != 0) return comp
        comp = compare(month, other.month)
        if (comp != 0) return comp
        return compare(day, other.day)
    }

    private fun compare(val1: Int?, val2: Int?): Int {
        return if (val1 == null && val2 == null) 0
        else if (val1 == null) 1
        else if (val2 == null) -1
        else val1 - val2

    }

    override fun toString(): String {
        return "Date($year,$month,$day)"
    }
}