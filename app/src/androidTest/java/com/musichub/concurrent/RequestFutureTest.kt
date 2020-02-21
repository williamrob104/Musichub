package com.musichub.concurrent

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.*
import java.util.concurrent.TimeUnit


class RequestFutureTest {
    private fun <T> delayedResponse(delayMs: Long, response: T, listener: ResponseListener<T>) {
        Timer().schedule(object : TimerTask() {
            override fun run() {
                listener.onResponse(response)
            }
        }, delayMs)
    }

    @Test
    fun testGet() {
        val message = "message"
        val future = RequestFuture<String>()
        delayedResponse(500, message, future)
        val response = future.get(3000, TimeUnit.MILLISECONDS)
        assertEquals(response, message)
    }

    @Test
    fun testIsDone() {
        val message = "message"
        val future = RequestFuture<String>()
        delayedResponse(500, message, future)
        future.get(3000, TimeUnit.MILLISECONDS)
        assertTrue(future.isDone)
    }
}