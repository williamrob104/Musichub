package com.musichub.scraper

import androidx.test.platform.app.InstrumentationRegistry
import com.musichub.concurrent.RequestFuture
import com.musichub.singleton.Singleton
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit


private val youtubeScraper = Singleton
    .apply { initialize(InstrumentationRegistry.getInstrumentation().targetContext) }.youtubeScraper

class YoutubeScraperTest {
    @Test
    fun testSearchVideos() {
        val myQuery = "周杰倫 - 告白氣球"
        val myYoutubeVideoId = "bu7nU9Mhpyo"
        val myNumViews = 193_964_975

        val query = myQuery
        val page = 1
        val future = RequestFuture<YoutubeSearch<YoutubeVideo>>()
        youtubeScraper.searchVideos(query, page, future)

        val searchResult = future.get(20, TimeUnit.SECONDS)
        assertEquals(searchResult.echoQuery, query)
        assertEquals(searchResult.echoPage, page)

        assertTrue(searchResult.itemList.isNotEmpty())

        val video = searchResult.itemList[0]
        assertEquals(video.youtubeVideoId, myYoutubeVideoId)
        assertTrue(video.viewCount ?: 0 > myNumViews)
        assertTrue(hasConnection(video.thumbnail.sourceLargest().url))
        println(video.descriptionPreview)

        assertTrue(hasConnection(searchResult.itemList.last().thumbnail.sourceLargest().url))
    }

    @Test
    fun testGetVideoRelatedVideos() {
        val youtubeVideoId = "bu7nU9Mhpyo"
        val future = RequestFuture<YoutubeVideoRelatedVideos>()
        youtubeScraper.getVideoRelatedVideos(youtubeVideoId, future)

        val relatedVideos = future.get(20, TimeUnit.SECONDS)
        assertEquals(relatedVideos.echoYoutubeVideoId, youtubeVideoId)
        assertTrue(relatedVideos.videoList.isNotEmpty())

        val idSet = HashSet<String>()
        for (video in relatedVideos.videoList) {
            assert(video.youtubeVideoId !in idSet)
            idSet.add(video.youtubeVideoId)
            assertTrue(hasConnection(video.thumbnail.sourceLargest().url))
        }
    }

    @Test
    fun testGetVideoStreams_normal() {
        checkYoutubeStreams("KYniUCGPGLs")
    }

    @Test
    fun testGetVideoStreams_copyrighted() {
        checkYoutubeStreams("JGwWNGJdvx8")
        checkYoutubeStreams("RgKAFK5djSk")
    }

    @Test
    fun testGetVideoStreams_ageRestricted() {
        checkYoutubeStreams("M-JdRsHr7Bs")
    }

    @Test
    fun testGetVideoStreams_notAvailable() {
        try {
            checkYoutubeStreams("aXBL6j0XS1s")
            checkYoutubeStreams("EzFIWyXuMmU")
            checkYoutubeStreams("EzFIWyXuMm")
            Assert.assertTrue(false)
        } catch (e: Exception) {
            Assert.assertTrue("available" in e.message!!)
        }
    }

    /*
    @Test
    fun testGetVideoStreams_liveNotSupported() {
        try {
            checkYoutubeStreams("RaIJ767Bj_M")
            Assert.assertTrue(false)
        } catch (e: Exception) {
            Assert.assertTrue("live" in e.message!!)
        }
    }*/

    private fun checkYoutubeStreams(youtubeVideoId: String) {
        val future = RequestFuture<YoutubeVideoStreams>()
        youtubeScraper.getVideoStreams(youtubeVideoId, future)
        val videoStreams = future.get(30, TimeUnit.SECONDS)

        Assert.assertTrue(videoStreams.streamList.isNotEmpty())
        Assert.assertEquals(videoStreams.echoYoutubeVideoId, youtubeVideoId)

        val itagSet = HashSet<Int>()
        for (stream in videoStreams.streamList) {
            Assert.assertTrue(hasConnection(stream.url))
            Assert.assertTrue(stream.itag !in itagSet)
            itagSet.add(stream.itag)
            Assert.assertTrue(stream.audioFormat != null || stream.videoFormat != null)
        }
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