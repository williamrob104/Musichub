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

private val appleMusicScraper = Singleton
    .apply { initialize(InstrumentationRegistry.getInstrumentation().targetContext) }
    .appleMusicScraper

private var myArtistBrowse: AppleMusicArtistBrowse? = null

class AppleMusicScraperTest {
    private val myArtistName = "周杰倫"
    private val myTrackName = "告白氣球"
    private val myAlbumName = "周杰倫的床邊故事"

    private val myArtistId = 300117743
    private val myAlbumId = 1119071949

    private val myArtistViewUrl =
        "https://music.apple.com/tw/artist/%E5%91%A8%E6%9D%B0%E5%80%AB/300117743?uo=4"
    private val myAlbumViewUrl =
        "https://music.apple.com/tw/album/%E5%91%8A%E7%99%BD%E6%B0%A3%E7%90%83/1118757859?i=1118757877&uo=4"
    private val myPlaylistViewUrl1 =
        "https://music.apple.com/tw/playlist/%E5%91%A8%E6%9D%B0%E5%80%AB%E7%9A%84%E5%89%B5%E4%BD%9C/pl.5a3e20295f804b298e507226f335e567"
    private val myPlaylistViewUrl2 =
        "https://music.apple.com/tw/playlist/%E5%91%A8%E6%9D%B0%E5%80%AB%E5%BD%B1%E7%89%87%E7%B2%BE%E9%81%B8/pl.02c3e2ca3c5240868bca7d72ff2258e2"

    private fun <T> RequestFuture<T>.defaultGet(): T {
        return this.get(15, TimeUnit.SECONDS)
    }

    init {
        if (myArtistBrowse == null) {
            val future = RequestFuture<AppleMusicArtistBrowse>()
            appleMusicScraper.browseArtist(myArtistViewUrl, future)
            myArtistBrowse = future.defaultGet()
        }
    }

    @Test
    fun testSearchAll() {
        val future = RequestFuture<AppleMusicSearch<AppleMusicEntity>>()
        appleMusicScraper.searchAll(myArtistName, 10, future)
        val entities = future.defaultGet().itemList

        assertTrue(entities.any { it is AppleMusicArtist && it.name == myArtistName })
    }

    @Test
    fun testSearchArtists() {
        val future = RequestFuture<AppleMusicSearch<AppleMusicArtist>>()
        appleMusicScraper.searchArtists(myArtistName, 10, 0, future)
        val artists = future.defaultGet().itemList

        assertTrue(artists.any { it.name == myArtistName })
    }

    @Test
    fun testSearchAlbums() {
        val future = RequestFuture<AppleMusicSearch<AppleMusicAlbum>>()
        appleMusicScraper.searchAlbums(myAlbumName, 10, 0, future)
        val albums = future.defaultGet().itemList

        val album = albums.find { it.title == myAlbumName }
        assertTrue(album != null)

        assertEquals(album!!.artistName, myArtistName)
        assertTrue(album.coverart != null)
        assertTrue(isValidUrl(album.coverart!!.sourceLargest().url))
    }

    @Test
    fun testSearchSongs() {
        val future = RequestFuture<AppleMusicSearch<AppleMusicSong>>()
        appleMusicScraper.searchSongs(myTrackName, 10, 0, future)
        val tracks = future.defaultGet().itemList

        val track = tracks.find { it.title == myTrackName }
        assertTrue(track != null)

        assertEquals(track!!.artistName, myArtistName)
        assertEquals(track.albumTitle, myAlbumName)

        val coverart = track.coverart
        assertTrue(coverart != null)
        assertTrue(isValidUrl(coverart!!.sourceLargest().url))
    }

    @Test
    fun testSearchMVs() {
        val future = RequestFuture<AppleMusicSearch<AppleMusicMV>>()
        appleMusicScraper.searchMVs(myTrackName, 10, future)
        val tracks = future.defaultGet().itemList

        val track = tracks.find { it.title == myTrackName }
        assertTrue(track != null)

        assertEquals(track!!.artistName, myArtistName)

        val coverart = track.coverart
        assertTrue(coverart != null)
        assertTrue(isValidUrl(coverart!!.sourceLargest().url))
    }

    @Test
    fun testBrowseArtist() {
        val future = RequestFuture<AppleMusicArtistBrowse>()
        appleMusicScraper.browseArtist(myArtistViewUrl, future)
        val artist = future.defaultGet()

        assertEquals(artist.name, myArtistName)

        assertTrue(artist.avatar != null)
        assertTrue(isValidUrl(artist.avatar!!.sourceLargest().url))

        //val gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()
        //println(gson.toJson(artist))
    }

    @Test
    fun testBrowseAlbum() {
        val future = RequestFuture<AppleMusicAlbumBrowse>()
        appleMusicScraper.browseAlbum(myAlbumViewUrl, future)
        val album = future.defaultGet()

        assertEquals(album.songList.size, album.songCount)

        //val gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()
        //println(gson.toJson(album))
    }

    @Test
    fun testBrowsePlaylist_1() {
        val future = RequestFuture<AppleMusicPlaylistBrowse>()
        appleMusicScraper.browsePlaylist(myPlaylistViewUrl1, future)
        val playlist = future.defaultGet()

        assertEquals(playlist.trackList.size, playlist.trackCount)

        //val gson = GsonBuilder().serializeNulls().serializeNulls().setPrettyPrinting().create()
        //println(gson.toJson(playlist))
    }

    @Test
    fun testBrowsePlaylist_2() {
        val future = RequestFuture<AppleMusicPlaylistBrowse>()
        appleMusicScraper.browsePlaylist(myPlaylistViewUrl2, future)
        val playlist = future.defaultGet()

        assertEquals(playlist.trackList.size, playlist.trackCount)

        //val gson = GsonBuilder().serializeNulls().setPrettyPrinting().create()
        //println(gson.toJson(playlist))
    }

    @Test
    fun testGetSectionSongs() {
        val sectionTopSongs = myArtistBrowse!!.sectionTopSongs!!

        val future = RequestFuture<AppleMusicSection<AppleMusicSong3>>()
        appleMusicScraper.getSectionSongs(sectionTopSongs.sectionViewAllUrl, future)
        future.defaultGet().entityList
    }

    @Test
    fun testGetSectionMVs() {
        val sectionRecentVideos = myArtistBrowse!!.sectionRecentVideos!!

        val future = RequestFuture<AppleMusicSection<AppleMusicMV3>>()
        appleMusicScraper.getSectionMVs(sectionRecentVideos.sectionViewAllUrl, future)
        val recentVideos = future.defaultGet().entityList

        assertTrue(recentVideos.isNotEmpty())
    }

    @Test
    fun testGetSectionAlbums_1() {
        val sectionFullAlbums = myArtistBrowse!!.sectionFullAlbums!!

        val future = RequestFuture<AppleMusicSection<AppleMusicAlbum3>>()
        appleMusicScraper.getSectionAlbums(sectionFullAlbums.sectionViewAllUrl, future)
        val fullAlbums = future.defaultGet().entityList

        assertTrue(fullAlbums.isNotEmpty())
    }

    @Test
    fun testGetSectionAlbums_2() {
        val sectionSingleAlbums = myArtistBrowse!!.sectionSingleAlbums!!

        val future = RequestFuture<AppleMusicSection<AppleMusicAlbum3>>()
        appleMusicScraper.getSectionAlbums(sectionSingleAlbums.sectionViewAllUrl, future)
        val singleAlbums = future.defaultGet().entityList

        assertTrue(singleAlbums.isNotEmpty())
    }

    @Test
    fun testGetSectionAlbums_3() {
        val sectionLiveAlbums = myArtistBrowse!!.sectionLiveAlbums!!

        val future = RequestFuture<AppleMusicSection<AppleMusicAlbum3>>()
        appleMusicScraper.getSectionAlbums(sectionLiveAlbums.sectionViewAllUrl, future)
        val liveAlbums = future.defaultGet().entityList

        assertTrue(liveAlbums.isNotEmpty())
    }

    @Test
    fun testGetSectionAlbums_4() {
        val sectionCompilationAlbums = myArtistBrowse!!.sectionCompilationAlbums!!

        val future = RequestFuture<AppleMusicSection<AppleMusicAlbum3>>()
        appleMusicScraper.getSectionAlbums(sectionCompilationAlbums.sectionViewAllUrl, future)
        val compilationAlbums = future.defaultGet().entityList

        assertTrue(compilationAlbums.isNotEmpty())
    }

    @Test
    fun testGetSectionAlbums_5() {
        val sectionFeaturedAlbums = myArtistBrowse!!.sectionFeaturedAlbums!!

        val future = RequestFuture<AppleMusicSection<AppleMusicAlbum3>>()
        appleMusicScraper.getSectionAlbums(sectionFeaturedAlbums.sectionViewAllUrl, future)
        val featuredAlbums = future.defaultGet().entityList

        assertTrue(featuredAlbums.isNotEmpty())
    }

    @Test
    fun testGetSectionAlbums_6() {
        val sectionLatestRelease = myArtistBrowse!!.sectionLatestRelease!!

        val future = RequestFuture<AppleMusicSection<AppleMusicAlbum3>>()
        appleMusicScraper.getSectionAlbums(sectionLatestRelease.sectionViewAllUrl, future)
        val latestRelease = future.defaultGet().entityList

        assertTrue(latestRelease.isNotEmpty())
    }

    @Test
    fun testGetSectionPlaylists() {
        val sectionPlaylists = myArtistBrowse!!.sectionPlaylists!!

        val future = RequestFuture<AppleMusicSection<AppleMusicPlaylist3>>()
        appleMusicScraper.getSectionPlaylists(sectionPlaylists.sectionViewAllUrl, future)
        val playlists = future.defaultGet().entityList

        assertTrue(playlists.isNotEmpty())
    }


    private fun isValidUrl(_url: String): Boolean {
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