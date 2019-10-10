package com.freemusic.musicbox.util

import android.util.Log

class TimeLogger(private val tag: String) {
    var start = System.nanoTime()

    fun log(msg: String) {
        val end = System.nanoTime()
        Log.i(tag, "$msg (%.2f ms)".format((end-start) * 1e-6))
        start = end
    }
}