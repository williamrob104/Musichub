package com.musichub.util

import android.content.res.Resources
import com.musichub.R
import com.musichub.resource.Date


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
    val m = rem / 60;   rem %= 60
    val s = rem
    return if (h != 0) {
        "%d:%02d:%02d".format(h,m,s)
    }
    else {
        "%d:%02d".format(m,s)
    }
}

internal fun formatDate(resources: Resources, date: Date): String {
    val monthStrId = when(date.month) {
        1 -> R.string.label_date_jan
        2 -> R.string.label_date_feb
        3 -> R.string.label_date_mar
        4 -> R.string.label_date_apr
        5 -> R.string.label_date_may
        6 -> R.string.label_date_jun
        7 -> R.string.label_date_jul
        8 -> R.string.label_date_aug
        9 -> R.string.label_date_sep
        10 -> R.string.label_date_oct
        11 -> R.string.label_date_nov
        12 -> R.string.label_date_dec
        else -> null
    }
    val monthStr = if (monthStrId == null) null else resources.getString(monthStrId)
    return if (monthStr == null)
        resources.getString(R.string.label_date_1).messageFormat(date.year)
    else if (date.day == null)
        resources.getString(R.string.label_date_2).messageFormat(date.year, monthStr)
    else
        resources.getString(R.string.label_date_3).messageFormat(date.year, monthStr, date.day)
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