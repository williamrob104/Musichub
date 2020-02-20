package com.musichub.networking

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import com.musichub.concurrent.RequestFuture
import com.musichub.singleton.Singleton
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit


private val imageRequests = Singleton
    .apply { initialize(InstrumentationRegistry.getInstrumentation().targetContext) }.imageRequests

class ImageRequestsTest {

    private fun getBitmap(url:String): Bitmap {
        val future = RequestFuture<Bitmap>()
        imageRequests.getImage(url, future)
        return future.get(10, TimeUnit.SECONDS)
    }

    @Test fun testGetImage() {
        val url = "https://www.google.com/images/branding/googlelogo/1x/googlelogo_color_272x92dp.png"

        val bitmap1: Any = getBitmap(url)
        val bitmap2: Any = getBitmap(url)

        assertTrue("request should be cached", bitmap1 == bitmap2)
    }
}