package com.musichub.scraper

import android.content.res.Resources
import androidx.core.content.res.ResourcesCompat
import com.musichub.R
import com.musichub.util.messageFormat
import java.text.SimpleDateFormat
import java.util.*


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


class ApproxDate(
    val year: Int,
    _month: Int?,
    _day: Int?
) : Comparable<ApproxDate> {

    val month: Int? = if (_month != null && _month in 1..12) _month else null
    val day: Int? = if (month != null) _day else null

    override fun compareTo(other: ApproxDate): Int {
        var comp = year - other.year
        if (comp != 0) return comp
        comp = compareNullableInt(month, other.month)
        if (comp != 0) return comp
        return compareNullableInt(day, other.day)
    }

    private fun compareNullableInt(val1: Int?, val2: Int?): Int {
        // null as large
        return if (val1 == null && val2 == null) 0
        else if (val1 == null) 1
        else if (val2 == null) -1
        else val1 - val2

    }

    override fun toString(): String {
        return "Date($year,$month,$day)"
    }

    fun format(locale: Locale, resources: Resources): String {
        val calendar = Calendar.getInstance()
        val style = Calendar.LONG

        return if (month != null && day != null) {
            calendar.set(year, month-1, day)
            val yearStr = calendar.getDisplayName(Calendar.YEAR, style, locale) ?: year.toString()
            val monthStr = calendar.getDisplayName(Calendar.MONTH, style, locale) ?: month.toString()
            val dayStr = calendar.getDisplayName(Calendar.DAY_OF_MONTH, style, locale) ?: day.toString()
            resources.getString(R.string.label_date_day).messageFormat(yearStr, monthStr, dayStr)
        }
        else if (month != null) {
            calendar.set(year, month-1, 1)
            val yearStr = calendar.getDisplayName(Calendar.YEAR, style, locale) ?: year.toString()
            val monthStr = calendar.getDisplayName(Calendar.MONTH, style, locale) ?: month.toString()
            resources.getString(R.string.label_date_month).messageFormat(yearStr, monthStr)
        }
        else {
            calendar.set(year, 0, 1)
            val yearStr = calendar.getDisplayName(Calendar.YEAR, style, locale) ?: year.toString()
            resources.getString(R.string.label_date_year).messageFormat(yearStr)
        }
    }
}