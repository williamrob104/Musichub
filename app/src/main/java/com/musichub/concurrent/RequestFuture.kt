package com.musichub.concurrent

import java.util.concurrent.Future
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException


class RequestFuture<T> : ResponseListener<T>, Future<T> {
    private var mResponse: T? = null
    private var mError: Exception? = null
    private var doneLock = Object()

    override fun onResponse(response: T) = synchronized(doneLock) {
        mResponse = response
        doneLock.notifyAll()
    }

    override fun onError(error: Exception) = synchronized(doneLock) {
        mError = error
        doneLock.notifyAll()
    }

    override fun isDone(): Boolean = synchronized(doneLock) {
        return mResponse != null || mError != null
    }

    @Deprecated("This method has no effect.", ReplaceWith(""), DeprecationLevel.ERROR)
    override fun cancel(mayInterruptIfRunning: Boolean): Boolean {
        return false
    }

    @Deprecated("This method has no effect.", ReplaceWith(""), DeprecationLevel.ERROR)
    override fun isCancelled(): Boolean {
        return false
    }

    @Deprecated("This method may block the thread forever!")
    override fun get(): T {
        return doGet(null)
    }

    override fun get(timeout: Long, unit: TimeUnit): T = synchronized(doneLock) {
        return doGet(TimeUnit.MILLISECONDS.convert(timeout, unit))
    }

    private fun doGet(timeoutMs: Long?): T = synchronized(doneLock) {
        val error1 = mError
        if (error1 != null)
            throw error1

        val response = mResponse
        if (response != null)
            return response

        if (timeoutMs == null) {
            while (!isDone)
                doneLock.wait(0)
        } else if (timeoutMs > 0) {
            var nowMs = System.currentTimeMillis()
            val deadlineMs = nowMs + timeoutMs
            while (!isDone && nowMs < deadlineMs) {
                doneLock.wait(deadlineMs - nowMs)
                nowMs = System.currentTimeMillis()
            }
        }

        val error2 = mError
        if (error2 != null)
            throw error2

        return mResponse ?: throw TimeoutException()
    }
}