package com.freemusic.musicbox.concurrent

import android.os.Handler
import android.os.Looper
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.Executors


class CallbackExecutorTest {

    private val context = InstrumentationRegistry.getInstrumentation().targetContext
    private val executorService = Executors.newFixedThreadPool(4)
    private val callbackExecutor = CallbackExecutor(executorService, Handler(context.mainLooper))

    @Test fun testExecuteCallback() {
        var finished = false
        var succeed = false

        callbackExecutor.executeCallback(object: ResponseListener<Int> {
            override fun onResponse(response: Int) {
                succeed = Looper.myLooper() == context.mainLooper
                finished = true
            }

            override fun onError(error: Exception) {
                finished = true
            }
        }) { 1927 }

        while (!finished) {}

        assertTrue(succeed)
    }
}