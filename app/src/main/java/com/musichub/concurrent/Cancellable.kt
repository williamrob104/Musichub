package com.musichub.concurrent

interface Cancellable {
    fun cancel()

    fun isCancelled(): Boolean
}