package com.musichub.util

import android.text.SpannableString
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan


private fun SpannableString.setSpan(what: Any): SpannableString {
    this.setSpan(what, 0, this.length, SpannableString.SPAN_INCLUSIVE_EXCLUSIVE)
    return this
}

internal fun SpannableString.setColor(color: Int) = this.setSpan(ForegroundColorSpan(color))

internal fun SpannableString.setTypeface(typeface: Int) = this.setSpan(StyleSpan(typeface))