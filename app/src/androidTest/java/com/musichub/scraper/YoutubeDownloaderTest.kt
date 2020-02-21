package com.musichub.scraper

import androidx.test.platform.app.InstrumentationRegistry
import com.musichub.concurrent.RequestFuture
import com.musichub.singleton.Singleton
import org.junit.Assert
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit


private val youtubeDownloader = Singleton
    .apply { initialize(InstrumentationRegistry.getInstrumentation().targetContext) }
    .youtubeDownloader

class YoutubeDownloaderTest {
    @Test
    fun testGetVideoStreams_1() {
        // normal
        checkYoutubeStreams("KYniUCGPGLs")
    }

    @Test
    fun testGetVideoStreams_2() {
        // copyrighted
        checkYoutubeStreams("JGwWNGJdvx8")
    }

    @Test
    fun testGetVideoStreams_3() {
        // copyrighted
        checkYoutubeStreams("RgKAFK5djSk")
    }

    @Test
    fun testGetVideoStreams_4() {
        // age restricted
        checkYoutubeStreams("EzFIWyXuMmU")
    }

    @Test
    fun testGetVideoStreams_5() {
        // not available
        try {
            checkYoutubeStreams("aXBL6j0XS1s")
            Assert.assertTrue(false)
        } catch (e: Exception) {
            Assert.assertTrue("available" in e.message!!)
        }
    }

    @Test
    fun testGetVideoStreams_6() {
        // live stream (not supported)
        try {
            checkYoutubeStreams("eMRuyO9NZfY")
            Assert.assertTrue(false)
        } catch (e: Exception) {
            Assert.assertTrue("live" in e.message!!)
        }
    }

    private fun checkYoutubeStreams(youtubeVideoId: String): YoutubeVideoStreams {
        val future = RequestFuture<YoutubeVideoStreams>()
        youtubeDownloader.getVideoStreams(youtubeVideoId, future)
        val videoStreams = future.get(30, TimeUnit.SECONDS)

        Assert.assertEquals(videoStreams.echoYoutubeVideoId, youtubeVideoId)

        val itagSet = HashSet<Int>()
        for (stream in videoStreams.streamList) {
            Assert.assertTrue(hasConnection(stream.url))
            Assert.assertTrue(stream.itag !in itagSet)
            itagSet.add(stream.itag)
            Assert.assertTrue(stream.audioFormat != null || stream.videoFormat != null)
        }
        println()
        return videoStreams
    }

    private fun hasConnection(_url: String): Boolean {
        return try {
            val url = URL(_url)
            val con = url.openConnection() as HttpURLConnection
            val responseCode = con.responseCode
            responseCode / 100 == 2
        } catch (e: Exception) {
            false
        }
    }
}