/*package com.musichub.resource

import com.musichub.concurrent.RequestFuture
import com.musichub.networking.globalClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit


private val musixmatchScraper = MusixmatchScraper(globalClient)

class MusixmatchScraperTest {
    private val myTrackName = "告白氣球"
    private val myArtistName = "周杰倫"
    private val myTrackLink = "/lyrics/artist-12009/%E5%91%8A%E7%99%BD%E6%B0%A3%E7%90%83"

    @Test fun testSearchTracks() {
        val query = "$myArtistName - $myTrackName"
        val future = RequestFuture<MusixmatchSearch<MusixmatchTrack>>()
        musixmatchScraper.searchTracks(query, listener=future)
        val trackSearch = future.get(10, TimeUnit.SECONDS)

        assertEquals(trackSearch.echoQuery, query)
        assertTrue(trackSearch.itemList.size > 3)

        val track = trackSearch.itemList.find { it.title == myTrackName && it.artistsNames[0] == myArtistName }
        assertTrue(track != null)
        assertTrue(track!!.coverart != null)
        for (size in listOf(100, 350, 500, 800))
            assertTrue(isValidUrl(track.coverart!!.sourceByShortSideEquals(size).url))
    }

    @Test fun testSearchTracksByLyrics() {
        val query = "$myArtistName - $myTrackName"
        val future = RequestFuture<MusixmatchSearch<MusixmatchTrack>>()
        musixmatchScraper.searchTracksByLyrics(query, listener=future)
        val trackSearch = future.get(10, TimeUnit.SECONDS)

        assertEquals(trackSearch.echoQuery, query)
        assertTrue(trackSearch.itemList.size > 3)
    }

    @Test fun testGetTrackLyrics() {
        val future = RequestFuture<MusixmatchLyrics>()
        musixmatchScraper.getTrackLyrics(myTrackLink, future)
        val lyrics = future.get(10, TimeUnit.SECONDS)

        assertTrue("塞納河畔" in lyrics.lines[0])
        assertTrue("在說我願意" in lyrics.lines.last())
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

}*/