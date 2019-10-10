package com.freemusic.musicbox.cache

import java.util.*


interface Cache {
    /**
     * Performs any potentially long-running actions needed to initialize the cache; will be called
     * from a worker thread.
     */
    fun intialize()

    operator fun get(key: String): Entry?

    operator fun set(key: String, entry: Entry)

    fun remove(key: String)

    fun clear()

    class Entry(
        val data: ByteArray,
        val lastModified: Date
    )

}