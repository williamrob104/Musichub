package com.musichub.scraper

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
    @Test
    fun testSearchVideos() {
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
        assertTrue(hasConnection(video.thumbnail.sourceLargest().url))
        println(video.descriptionPreview)

        assertTrue(hasConnection(searchResult.itemList.last().thumbnail.sourceLargest().url))
    }

    @Test
    fun testGetVideoRelatedVideos() {
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
            assertTrue(hasConnection(video.thumbnail.sourceLargest().url))
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