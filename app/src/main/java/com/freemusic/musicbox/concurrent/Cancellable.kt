package com.freemusic.musicbox.concurrent

interface Cancellable {
    fun cancel()

    fun isCancelled(): Boolean
}