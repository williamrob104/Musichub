package com.freemusic.musicbox.concurrent

interface ResponseListener<T> {
    fun onResponse(response: T): Unit
    fun onError(error: Exception): Unit
}
