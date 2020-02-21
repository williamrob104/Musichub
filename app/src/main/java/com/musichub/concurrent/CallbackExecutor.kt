package com.musichub.concurrent

import android.os.Handler
import java.util.concurrent.ExecutorService


class CallbackExecutor(
    private val executorService: ExecutorService,
    private val handler: Handler?
) {

    fun <T> executeCallback(listener: ResponseListener<T>, callable: () -> T): Cancellable {
        val task = CallbackExecutorTask(listener, callable)
        val future = executorService.submit(task)
        return object : Cancellable {
            override fun cancel() {
                task.cancel()
                future.cancel(true)
            }

            override fun isCancelled(): Boolean {
                return future.isCancelled
            }
        }
    }

    private inner class CallbackExecutorTask<T>(
        private val listener: ResponseListener<T>,
        private val callable: () -> T
    ) : Runnable {

        private var cancelled = false

        override fun run() {
            if (cancelled)
                return
            val result = try {
                callable()
            } catch (e: InterruptedException) {
                cancelled = true
                return
            } catch (e: Exception) {
                synchronized(cancelled) {
                    if (!cancelled) {
                        if (handler != null)
                            handler.post { listener.onError(e) }
                        else
                            listener.onError(e)
                    }
                }
                return
            }
            synchronized(cancelled) {
                if (!cancelled) {
                    if (handler != null)
                        handler.post { listener.onResponse(result) }
                    else
                        listener.onResponse(result)
                }

            }
        }

        fun cancel() = synchronized(cancelled) {
            cancelled = true
        }
    }
}