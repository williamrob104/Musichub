package com.musichub.resource

import androidx.test.platform.app.InstrumentationRegistry
import com.musichub.concurrent.RequestFuture
import com.musichub.singleton.Singleton
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit


private val youtubeScraper = Singleton
    .apply { initialize(InstrumentationRegistry.getInstrumentation().targetContext) }.youtubeScraper

class YoutubeScraperTest {
    @Test fun testSearchVideos() {
        val myQuery = "周杰倫 - 告白氣球"
        val myYoutubeVideoId = "bu7nU9Mhpyo"
        val myNumViews = 193_964_975

        val query = myQuery
        val page = 1
        val future = RequestFuture<YoutubeSearch<YoutubeVideo>>()
        youtubeScraper.searchVideos(query, page, future)

        val searchResult = future.get(5, TimeUnit.SECONDS)
        assertEquals(searchResult.echoQuery, query)
        assertEquals(searchResult.echoPage, page)

        assertTrue(searchResult.itemList.isNotEmpty())

        val video = searchResult.itemList[0]
        assertEquals(video.youtubeVideoId, myYoutubeVideoId)
        assertTrue(video.viewCount > myNumViews)
        assertTrue(isValidUrl(video.thumbnail.sourceLargest().url))
        println(video.descriptionPreview)

        assertTrue(isValidUrl(searchResult.itemList.last().thumbnail.sourceLargest().url))
    }

    @Test fun testGetVideoRelatedVideos() {
        val youtubeVideoId = "bu7nU9Mhpyo"
        val future = RequestFuture<YoutubeVideoRelatedVideos>()
        youtubeScraper.getVideoRelatedVideos(youtubeVideoId, future)

        val relatedVideos = future.get(5, TimeUnit.SECONDS)
        assertEquals(relatedVideos.echoYoutubeVideoId, youtubeVideoId)
        assertTrue(relatedVideos.videoList.isNotEmpty())

        val idSet = HashSet<String>()
        for (video in relatedVideos.videoList) {
            assert(video.youtubeVideoId !in idSet)
            idSet.add(video.youtubeVideoId)
            assertTrue(isValidUrl(video.thumbnail.sourceLargest().url))
        }
    }

    @Test fun testGetVideoStreams_1() {
        // normal
        checkYoutubeStreams("KYniUCGPGLs")
    }

    @Test fun testGetVideoStreams_2() {
        // copyrighted
        checkYoutubeStreams("JGwWNGJdvx8")
    }

    @Test fun testGetVideoStreams_3() {
        // copyrighted
        checkYoutubeStreams("RgKAFK5djSk")
    }

    @Test fun testGetVideoStreams_4() {
        // age restricted
        checkYoutubeStreams("EzFIWyXuMmU")
    }

    @Test fun testGetVideoStreams_5() {
        // not available
        try {
            checkYoutubeStreams("aXBL6j0XS1s")
            assertTrue(false)
        }
        catch (e: Exception) {
            assertTrue("available" in e.message!!)
        }
    }

    @Test fun testGetVideoStreams_6() {
        // live stream (not supported)
        try {
            checkYoutubeStreams("eMRuyO9NZfY")
            assertTrue(false)
        }
        catch (e: Exception) {
            assertTrue("live" in e.message!!)
        }
    }

    private fun checkYoutubeStreams(youtubeVideoId: String): YoutubeVideoStreams {
        val future = RequestFuture<YoutubeVideoStreams>()
        youtubeScraper.getVideoStreams(youtubeVideoId, future)
        val videoStreams = future.get(30, TimeUnit.SECONDS)

        assertEquals(videoStreams.echoYoutubeVideoId, youtubeVideoId)

        val itagSet = HashSet<Int>()
        for (stream in videoStreams.streamList) {
            assertTrue(isValidUrl(stream.url))
            assertTrue(stream.itag !in itagSet)
            itagSet.add(stream.itag)
            assertTrue(stream.audioFormat != null || stream.videoFormat != null)
        }
        println()
        return videoStreams
    }

    private fun isValidUrl(_url: String): Boolean {
        return try {
            val url = URL(_url)
            val con = url.openConnection() as HttpURLConnection
            val responseCode = con.responseCode
            responseCode / 100 == 2
        }
        catch (e: Exception) {
            false
        }
    }
}