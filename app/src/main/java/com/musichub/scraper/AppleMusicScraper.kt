package com.musichub.scraper

import android.util.Log
import com.musichub.concurrent.CallbackExecutor
import com.musichub.concurrent.Cancellable
import com.musichub.concurrent.RequestFuture
import com.musichub.concurrent.ResponseListener
import com.musichub.networking.BasicHttpRequests
import com.musichub.networking.UrlParse
import com.musichub.util.getStringOrNull
import com.musichub.util.regexSearch
import com.musichub.util.splitBtIndex
import com.musichub.util.unescapeHtml
import org.json.JSONArray
import org.json.JSONObject
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap
import kotlin.collections.LinkedHashSet
import kotlin.collections.set
import kotlin.math.round


class AppleMusicScraper(
    private val basicHttpRequests: BasicHttpRequests,
    private val callbackExecutor: CallbackExecutor,
    private val locale: Locale
) {
    private val endpointUrl = "https://itunes.apple.com/search"

    private fun <T> RequestFuture<T>.defaultGet(): T = this.get(8, TimeUnit.SECONDS)

    fun searchAll(
        query: String, limit: Int,
        listener: ResponseListener<AppleMusicSearch<AppleMusicEntity>>
    ): Cancellable {
        return callbackExecutor.executeCallback(listener) {
            val artistsFuture = RequestFuture<AppleMusicSearch<AppleMusicArtist>>()
            this.searchArtists(query, 5, 0, artistsFuture)

            val albumsFuture = RequestFuture<AppleMusicSearch<AppleMusicAlbum>>()
            this.searchAlbums(query, 5, 0, albumsFuture)

            val songsFuture = RequestFuture<AppleMusicSearch<AppleMusicSong>>()
            this.searchSongs(query, limit, 0, songsFuture)

            val artists =
                artistsFuture.defaultGet().itemList.asSequence().map { it.artistId to it }.toMap()
            val albums =
                albumsFuture.defaultGet().itemList.asSequence().map { it.albumId to it }.toMap()
            val songs = songsFuture.defaultGet().itemList

            val entitySet = LinkedHashSet<AppleMusicEntity>()
            for (song in songs) {
                val artist = artists[song.artistId]
                if (artist != null)
                    entitySet.add(artist)
            }
            for (song in songs) {
                val album = albums[song.albumId]
                if (album != null)
                    entitySet.add(album)
            }
            val entityList = entitySet.toMutableList()
            entityList.addAll(songs)
            AppleMusicSearch(entityList, query, limit)
        }
    }

    fun searchArtists(
        query: String, limit: Int, offset: Int,
        listener: ResponseListener<AppleMusicSearch<AppleMusicArtist>>
    ): Cancellable {
        val params = mapOf(
            "term" to query,
            "limit" to limit.toString(),
            "offset" to offset.toString(),
            "media" to "music",
            "entity" to "musicArtist",
            "country" to locale.country
        )
        val url = UrlParse.joinUrl(endpointUrl, params = UrlParse.encodeParams(params))
        return basicHttpRequests.httpGETJson(url, null, listener) {
            val artists = ArrayList<AppleMusicArtist>()
            val artistsJsonArr = it.getJSONArray("results")
            for (i in 0 until artistsJsonArr.length()) {
                val artistJson = artistsJsonArr.getJSONObject(i)
                artists.add(AppleMusicExtractor.extractAppleMusicArtist(artistJson))
            }
            AppleMusicSearch(artists, query, limit)
        }
    }

    fun searchAlbums(
        query: String, limit: Int, offset: Int,
        listener: ResponseListener<AppleMusicSearch<AppleMusicAlbum>>
    ): Cancellable {
        val params = mapOf(
            "term" to query,
            "limit" to limit.toString(),
            "offset" to offset.toString(),
            "media" to "music",
            "entity" to "album",
            "country" to locale.country
        )
        val url = UrlParse.joinUrl(endpointUrl, params = UrlParse.encodeParams(params))
        return basicHttpRequests.httpGETJson(url, null, listener) {
            val albums = ArrayList<AppleMusicAlbum>()
            val albumsJsonArr = it.getJSONArray("results")
            for (i in 0 until albumsJsonArr.length()) {
                val albumJson = albumsJsonArr.getJSONObject(i)
                albums.add(AppleMusicExtractor.extractAppleMusicAlbum(albumJson))
            }
            AppleMusicSearch(albums, query, limit)
        }
    }

    fun searchSongs(
        query: String, limit: Int, offset: Int,
        listener: ResponseListener<AppleMusicSearch<AppleMusicSong>>
    ): Cancellable {
        val params = mapOf(
            "term" to query,
            "limit" to limit.toString(),
            "offset" to offset.toString(),
            "media" to "music",
            "entity" to "song",
            "country" to locale.country
        )
        val url = UrlParse.joinUrl(endpointUrl, params = UrlParse.encodeParams(params))
        return basicHttpRequests.httpGETJson(url, null, listener) {
            val songs = ArrayList<AppleMusicSong>()
            val songsJsonArr = it.getJSONArray("results")
            for (i in 0 until songsJsonArr.length()) {
                val songJson = songsJsonArr.getJSONObject(i)
                songs.add(AppleMusicExtractor.extractAppleMusicSong(songJson))
            }
            AppleMusicSearch(songs, query, limit)
        }
    }

    fun searchMVs(
        query: String, limit: Int,
        listener: ResponseListener<AppleMusicSearch<AppleMusicMV>>
    ): Cancellable {
        val params = mapOf(
            "term" to query,
            "limit" to limit.toString(),
            "media" to "music",
            "entity" to "musicVideo",
            "country" to locale.country
        )
        val url = UrlParse.joinUrl(endpointUrl, params = UrlParse.encodeParams(params))
        return basicHttpRequests.httpGETJson(url, null, listener) {
            val mvs = ArrayList<AppleMusicMV>()
            val mvsJsonArr = it.getJSONArray("results")
            for (i in 0 until mvsJsonArr.length()) {
                val mvJson = mvsJsonArr.getJSONObject(i)
                mvs.add(AppleMusicExtractor.extractAppleMusicMV(mvJson))
            }
            AppleMusicSearch(mvs, query, limit)
        }
    }

    fun browseArtist(
        artistViewUrl: String,
        listener: ResponseListener<AppleMusicArtistBrowse>
    ): Cancellable {
        return basicHttpRequests.httpGETString(artistViewUrl, null, listener) {
            val regex = """>\s*(\{\s*"data".*?\})\s*<""".toRegex()
            val jsonStr = it.regexSearch(regex, 1)
            val json = JSONObject(jsonStr)
            AppleMusicExtractor.extractAppleMusicArtistBrowse(json)
        }
    }

    fun browseAlbum(
        albumViewUrl: String,
        listener: ResponseListener<AppleMusicAlbumBrowse>
    ): Cancellable {
        return basicHttpRequests.httpGETString(albumViewUrl, null, listener) {
            val regex = """>\s*(\{\s*"data".*?\})\s*<""".toRegex()
            val jsonStr = it.regexSearch(regex, 1)
            val json = JSONObject(jsonStr)
            AppleMusicExtractor.extractAppleMusicAlbumBrowse(json)
        }
    }

    fun browsePlaylist(
        playlistViewUrl: String,
        listener: ResponseListener<AppleMusicPlaylistBrowse>
    ): Cancellable {
        return basicHttpRequests.httpGETString(playlistViewUrl, null, listener) {
            val regex = """>\s*(\{\s*"data".*?\})\s*<""".toRegex()
            val jsonStr = it.regexSearch(regex, 1)
            val json = JSONObject(jsonStr)
            AppleMusicExtractor.extractAppleMusicPlaylistBrowse(json)
        }
    }

    fun getSectionSongs(
        sectionViewAllUrl: String,
        listener: ResponseListener<AppleMusicSection<AppleMusicSong3>>
    ): Cancellable {
        return basicHttpRequests.httpGETJson(sectionViewAllUrl, null, listener) {
            val name = it.getJSONObject("pageData").getString("title")
            val results = it.getJSONObject("storePlatformData").getJSONObject("webexp-lockup")
                .getJSONObject("results")
            val songList = ArrayList<AppleMusicSong3>()
            for (key in results.keys()) {
                val songJson = results.getJSONObject(key)
                songList.add(AppleMusicExtractor.extractAppleMusicSong3(songJson))
            }
            AppleMusicSection<AppleMusicSong3>(name, sectionViewAllUrl, listOf()).apply {
                entityList = songList
            }
        }
    }

    fun getSectionMVs(
        sectionViewAllUrl: String,
        listener: ResponseListener<AppleMusicSection<AppleMusicMV3>>
    ): Cancellable {
        return basicHttpRequests.httpGETJson(sectionViewAllUrl, null, listener) {
            val name = it.getJSONObject("pageData").getString("title")
            val results = it.getJSONObject("storePlatformData").getJSONObject("webexp-lockup")
                .getJSONObject("results")
            val mvList = ArrayList<AppleMusicMV3>()
            for (key in results.keys()) {
                val mvJson = results.getJSONObject(key)
                mvList.add(AppleMusicExtractor.extractAppleMusicMV3(mvJson))
            }
            AppleMusicSection<AppleMusicMV3>(name, sectionViewAllUrl, listOf()).apply {
                entityList = mvList
            }
        }
    }

    fun getSectionAlbums(
        sectionViewAllUrl: String,
        listener: ResponseListener<AppleMusicSection<AppleMusicAlbum3>>
    ): Cancellable {
        return basicHttpRequests.httpGETJson(sectionViewAllUrl, null, listener) {
            val name = it.getJSONObject("pageData").getString("title")
            val results = it.getJSONObject("storePlatformData").getJSONObject("webexp-lockup")
                .getJSONObject("results")
            val albumList = ArrayList<AppleMusicAlbum3>()
            for (key in results.keys()) {
                val songJson = results.getJSONObject(key)
                albumList.add(AppleMusicExtractor.extractAppleMusicAlbum3(songJson))
            }
            AppleMusicSection<AppleMusicAlbum3>(name, sectionViewAllUrl, listOf()).apply {
                entityList = albumList
            }
        }
    }

    fun getSectionPlaylists(
        sectionViewAllUrl: String,
        listener: ResponseListener<AppleMusicSection<AppleMusicPlaylist3>>
    ): Cancellable {
        return basicHttpRequests.httpGETJson(sectionViewAllUrl, null, listener) {
            val name = it.getJSONObject("pageData").getString("title")
            val results = it.getJSONObject("storePlatformData").getJSONObject("webexp-lockup")
                .getJSONObject("results")
            val playlistList = ArrayList<AppleMusicPlaylist3>()
            for (key in results.keys()) {
                val songJson = results.getJSONObject(key)
                playlistList.add(AppleMusicExtractor.extractAppleMusicPlaylist3(songJson))
            }
            AppleMusicSection<AppleMusicPlaylist3>(name, sectionViewAllUrl, listOf()).apply {
                entityList = playlistList
            }
        }
    }
}


interface AppleMusicTrack : Track {
    override val title: String
    val id: Long
    override val length: Int
    override val artistName: String
    val artistId: Long
    val artistViewUrl: String?
    override val albumTitle: String?
    val coverart: Image?
}

sealed class AppleMusicEntity

class AppleMusicArtist(
    val name: String,
    val artistViewUrl: String?,
    val artistId: Long,
    val primaryGenre: String?
) : AppleMusicEntity() {
    override fun equals(other: Any?): Boolean {
        return other is AppleMusicArtist && artistId == other.artistId
    }

    override fun hashCode(): Int {
        return artistId.hashCode()
    }
}

class AppleMusicAlbum(
    val title: String,
    val albumId: Long,
    val albumViewUrl: String,
    val coverart: Image?,
    val trackCount: Int,
    val releaseDate: ApproxDate,
    val explicit: Boolean,
    val primaryGenre: String?,
    val artistName: String,
    val artistId: Long,
    val artistViewUrl: String?
) : AppleMusicEntity() {
    override fun equals(other: Any?): Boolean {
        return other is AppleMusicAlbum && albumId == other.albumId
    }

    override fun hashCode(): Int {
        return albumId.hashCode()
    }
}

class AppleMusicSong(
    override val title: String,
    val songId: Long,
    val songNumber: Int,
    override val length: Int,
    val releaseDate: ApproxDate,
    val explicit: Boolean,
    val primaryGenre: String?,
    override val artistName: String,
    override val artistId: Long,
    override val artistViewUrl: String?,
    override val albumTitle: String,
    val albumId: Long,
    val albumViewUrl: String,
    override val coverart: Image?,
    val albumTrackCount: Int,
    val albumExplicit: Boolean
) : AppleMusicEntity(), AppleMusicTrack {
    override fun equals(other: Any?): Boolean {
        return other is AppleMusicSong && songId == other.songId
    }

    override fun hashCode(): Int {
        return songId.hashCode()
    }

    override val id = songId
}

class AppleMusicMV(
    override val title: String,
    val mvId: Long,
    override val length: Int,
    val releaseDate: ApproxDate,
    val explicit: Boolean,
    override val coverart: Image?,
    override val artistName: String,
    override val artistId: Long,
    override val artistViewUrl: String?
) : AppleMusicEntity(), AppleMusicTrack {
    override fun equals(other: Any?): Boolean {
        return other is AppleMusicMV && mvId == other.mvId
    }

    override fun hashCode(): Int {
        return mvId.hashCode()
    }

    override val id = mvId
    override val albumTitle: String? = null
}

class AppleMusicSearch<T>(
    val itemList: List<T>,
    val echoQuery: String,
    val echoLimit: Int
)

class AppleMusicArtistBrowse(
    val name: String,
    val artistViewUrl: String,
    val artistId: Long,
    val bio: String?,
    val bornOrFormedDate: ApproxDate?,
    val origin: String?,
    val avatar: Image?,
    val primaryGenre: String?,
    val sectionTopSongs: AppleMusicSection<AppleMusicSong2>?,
    val sectionLatestRelease: AppleMusicSection<AppleMusicAlbum2>?,
    val sectionFeaturedAlbums: AppleMusicSection<AppleMusicAlbum2>?,
    val sectionFullAlbums: AppleMusicSection<AppleMusicAlbum2>?,
    val sectionRecentVideos: AppleMusicSection<AppleMusicMV2>?,
    val sectionPlaylists: AppleMusicSection<AppleMusicPlaylist2>?,
    val sectionSingleAlbums: AppleMusicSection<AppleMusicAlbum2>?,
    val sectionLiveAlbums: AppleMusicSection<AppleMusicAlbum2>?,
    val sectionCompilationAlbums: AppleMusicSection<AppleMusicAlbum2>?,
    val sectionAppearsOnAlbums: AppleMusicSection<AppleMusicAlbum2>?
)

class AppleMusicPlaylistBrowse(
    val title: String,
    val playlistId: String,
    val playlistViewUrl: String,
    val trackCount: Int,
    val lastModifiedDate: ApproxDate,
    val descriptionStandard: String?,
    val descriptionShort: String?,
    val coverart: Image?,
    val trackList: List<AppleMusicPlaylistItem2>
)

class AppleMusicAlbumBrowse(
    val title: String,
    val albumId: Long,
    val albumViewUrl: String,
    val artistName: String,
    val artistViewUrl: String,
    val songCount: Int,
    val releaseDate: ApproxDate,
    val noteStandard: String?,
    val noteShort: String?,
    val coverart: Image?,
    val trackList: List<AppleMusicTrack>
)

class AppleMusicImage(
    private val templateUrl: String,
    private val maxWidth: Int = 2000,
    private val maxHeight: Int = 2000
) : Image {
    private fun makeSource(width: Int, height: Int): Image.Source {
        val url = templateUrl
            .replace("{w}", width.toString())
            .replace("{h}", height.toString())
        return Image.Source(url, width, height)
    }

    override fun sourceByShortSideEquals(pixels: Int, largerIfNotPresent: Boolean): Image.Source {
        return if (maxWidth < maxHeight)
            sourceByWidthEquals(pixels)
        else
            sourceByHeightEquals(pixels)
    }

    override fun sourceByLongSideEquals(pixels: Int, largerIfNotPresent: Boolean): Image.Source {
        return if (maxWidth > maxHeight)
            sourceByWidthEquals(pixels)
        else
            sourceByHeightEquals(pixels)
    }

    private fun sourceByWidthEquals(pixels: Int): Image.Source {
        return if (maxWidth <= pixels)
            makeSource(maxWidth, maxHeight)
        else {
            val ar = maxWidth.toDouble() / maxHeight.toDouble()
            makeSource(pixels, round(pixels / ar).toInt())
        }
    }

    private fun sourceByHeightEquals(pixels: Int): Image.Source {
        return if (maxHeight <= pixels)
            makeSource(maxWidth, maxHeight)
        else {
            val ar = maxWidth.toDouble() / maxHeight.toDouble()
            makeSource(round(pixels * ar).toInt(), pixels)
        }
    }

    override fun sourceLargest(): Image.Source {
        return makeSource(maxWidth, maxHeight)
    }
}

private interface PostProcess {
    fun connect(resources: Map<String, Any>)
}

class AppleMusicSection<T : Any>(
    val name: String,
    internal val sectionViewAllUrl: String,
    _entityIds: List<String>
) : PostProcess {
    private var entityIds: List<String>? = _entityIds
    lateinit var entityList: List<T>
        internal set

    override fun connect(resources: Map<String, Any>) {
        @Suppress("UNCHECKED_CAST")
        entityList = entityIds?.asSequence()
            ?.filter { it in resources }
            ?.map { resources[it] as T }?.toList() ?: throw RuntimeException("entityIds is null")
        entityIds = null
    }
}

class AppleMusicAlbum2(
    val title: String,
    val albumId: Long,
    val albumViewUrl: String,
    val artistName: String,
    val trackCount: Int,
    val releaseDate: ApproxDate,
    _coverartId: String?
) : PostProcess {
    private var coverartId = _coverartId
    var coverart: Image? = null
        private set

    override fun connect(resources: Map<String, Any>) {
        coverart = if (coverartId == null) null
        else resources[coverartId!!] as Image
        coverartId = null
    }

    override fun equals(other: Any?): Boolean {
        return other is AppleMusicAlbum2 && albumId == other.albumId
    }

    override fun hashCode(): Int {
        return albumId.hashCode()
    }
}

class AppleMusicSong2(
    override val title: String,
    val songId: Long,
    override val artistName: String,
    override val artistId: Long,
    override val artistViewUrl: String?,
    override val albumTitle: String,
    _coverartId: String?,
    _offerIds: List<String>
) : PostProcess, AppleMusicTrack {
    private var _coverartId: String? = _coverartId
    private var _coverart: Image? = null
    override val coverart
        get() = _coverart

    private var _offerIds: List<String>? = _offerIds
    private var _length: Int = 0
    override val length
        get() = _length

    override val id = songId

    override fun connect(resources: Map<String, Any>) {
        _coverart = if (_coverartId == null) null
        else resources[_coverartId!!] as Image
        _coverartId = null

        for (offerId in _offerIds!!) {
            val temp = resources[offerId]
            if (temp != null) {
                _length = temp as Int
                break
            }
        }
        _offerIds = null
    }

    override fun equals(other: Any?): Boolean {
        return other is AppleMusicSong2 && songId == other.songId
    }

    override fun hashCode(): Int {
        return songId.hashCode()
    }
}

class AppleMusicMV2(
    override val title: String,
    val mvId: Long,
    override val artistName: String,
    override val artistId: Long,
    override val artistViewUrl: String?,
    _coverartId: String?,
    _offerIds: List<String>
) : PostProcess, AppleMusicTrack {
    private var _coverartId: String? = _coverartId
    private var _coverart: Image? = null
    override val coverart
        get() = _coverart

    private var _offerIds: List<String>? = _offerIds
    private var _length: Int = 0
    override val length
        get() = _length

    override val id = mvId
    override val albumTitle: String? = null

    override fun connect(resources: Map<String, Any>) {
        _coverart = if (_coverartId == null) null
        else resources[_coverartId!!] as Image
        _coverartId = null

        for (offerId in _offerIds!!) {
            val temp = resources[offerId]
            if (temp != null) {
                _length = temp as Int
                break
            }
        }
        _offerIds = null
    }

    override fun equals(other: Any?): Boolean {
        return other is AppleMusicMV2 && mvId == other.mvId
    }

    override fun hashCode(): Int {
        return mvId.hashCode()
    }
}

class AppleMusicPlaylist2(
    val title: String,
    val playlistViewUrl: String,
    val lastModifiedDate: ApproxDate,
    _coverartId: String?
) : PostProcess {
    private var coverartId = _coverartId
    var coverart: Image? = null
        private set

    override fun connect(resources: Map<String, Any>) {
        coverart = if (coverartId == null) null
        else resources[coverartId!!] as Image
        coverartId = null
    }
}

class AppleMusicPlaylistItem2(
    override val title: String,
    val trackId: Long,
    override val artistName: String,
    override val artistId: Long,
    override val artistViewUrl: String,
    val isMusicVideo: Boolean,
    _coverartId: String?,
    _offerIds: List<String>
) : PostProcess, AppleMusicTrack {
    private var _coverartId: String? = _coverartId
    private var _coverart: Image? = null
    override val coverart
        get() = _coverart

    private var _offerIds: List<String>? = _offerIds
    private var _length: Int = 0
    override val length
        get() = _length

    override val id = trackId
    override val albumTitle: String? = null

    override fun connect(resources: Map<String, Any>) {
        _coverart = if (_coverartId == null) null
        else resources[_coverartId!!] as Image
        _coverartId = null

        for (offerId in _offerIds!!) {
            val temp = resources[offerId]
            if (temp != null) {
                _length = temp as Int
                break
            }
        }
        _offerIds = null
    }
}


class AppleMusicSong3(
    override val title: String,
    val songId: Long,
    override val artistName: String,
    override val artistId: Long,
    override val artistViewUrl: String,
    override val albumTitle: String,
    val releaseDate: ApproxDate,
    override val coverart: Image?,
    override val length: Int,
    val songNumber: Int,
    val popularity: Double
) : AppleMusicTrack {
    override val id = songId

    override fun equals(other: Any?): Boolean {
        return other is AppleMusicSong3 && songId == other.songId
    }

    override fun hashCode(): Int {
        return songId.hashCode()
    }
}

class AppleMusicMV3(
    override val title: String,
    val mvId: Long,
    override val artistName: String,
    override val artistId: Long,
    override val artistViewUrl: String,
    val releaseDate: ApproxDate,
    override val coverart: Image?,
    override val length: Int,
    val popularity: Double
) : AppleMusicTrack {
    override val id = mvId
    override val albumTitle: String? = null

    override fun equals(other: Any?): Boolean {
        return other is AppleMusicMV3 && mvId == other.mvId
    }

    override fun hashCode(): Int {
        return mvId.hashCode()
    }
}

class AppleMusicAlbum3(
    val title: String,
    val albumId: Long,
    val albumViewUrl: String,
    val artistName: String,
    val artistId: Long,
    val artistViewUrl: String,
    val trackCount: Int,
    val releaseDate: ApproxDate,
    val coverart: Image?,
    val popularity: Double
) {
    override fun equals(other: Any?): Boolean {
        return other is AppleMusicAlbum3 && albumId == other.albumId
    }

    override fun hashCode(): Int {
        return albumId.hashCode()
    }
}

class AppleMusicPlaylist3(
    val title: String,
    val playlistId: String,
    val playlistViewUrl: String,
    val lastModifiedDate: ApproxDate,
    val descriptionStandard: String?,
    val descriptionShort: String?
)


private object AppleMusicExtractor {
    fun extractAppleMusicEntity(json: JSONObject): AppleMusicEntity? {
        return when {
            json.getString("wrapperType") == "artist" ->
                extractAppleMusicArtist(json)
            json.getString("wrapperType") == "collection" ->
                extractAppleMusicAlbum(json)
            json.getString("wrapperType") == "track" && json.getString("kind") == "song" ->
                extractAppleMusicSong(json)
            json.getString("wrapperType") == "track" && json.getString("kind") == "music-video" ->
                extractAppleMusicMV(json)
            else -> null
        }
    }

    fun extractAppleMusicArtist(json: JSONObject): AppleMusicArtist {
        return AppleMusicArtist(
            json.getString("artistName"),
            json.getStringOrNull("artistViewUrl", "artistLinkUrl"),
            json.getLong("artistId"),
            json.getStringOrNull("primaryGenreName")
        )
    }

    fun extractAppleMusicAlbum(json: JSONObject): AppleMusicAlbum {
        val artist = extractAppleMusicArtist(json)
        return AppleMusicAlbum(
            json.getString("collectionName"),
            json.getLong("collectionId"),
            json.getString("collectionViewUrl"),
            parseImageUrl(json.getStringOrNull("artworkUrl100", "artworkUrl60")),
            json.getInt("trackCount"),
            parseDate(json.getString("releaseDate")),
            json.getString("collectionExplicitness").toLowerCase() == "explicit",
            json.getStringOrNull("primaryGenreName"),
            artist.name,
            artist.artistId,
            artist.artistViewUrl
        )
    }

    fun extractAppleMusicSong(json: JSONObject): AppleMusicSong {
        val album = extractAppleMusicAlbum(json)
        return AppleMusicSong(
            json.getString("trackName"),
            json.getLong("trackId"),
            round(json.getLong("trackTimeMillis") * 1e-3).toInt(),
            json.getInt("trackNumber"),
            parseDate(json.getString("releaseDate")),
            json.getString("trackExplicitness").toLowerCase() == "explicit",
            json.getStringOrNull("primaryGenreName"),
            album.artistName,
            album.artistId,
            album.artistViewUrl,
            album.title,
            album.albumId,
            album.albumViewUrl,
            album.coverart,
            album.trackCount,
            album.explicit
        )
    }

    fun extractAppleMusicMV(json: JSONObject): AppleMusicMV {
        val artist = extractAppleMusicArtist(json)
        return AppleMusicMV(
            json.getString("trackName"),
            json.getLong("trackId"),
            round(json.getLong("trackTimeMillis") * 1e-3).toInt(),
            parseDate(json.getString("releaseDate")),
            json.getString("trackExplicitness").toLowerCase() == "explicit",
            parseImageUrl(json.getStringOrNull("artworkUrl100", "artworkUrl60")),
            artist.name,
            artist.artistId,
            artist.artistViewUrl
        )
    }


    fun extractAppleMusicArtistBrowse(json: JSONObject): AppleMusicArtistBrowse {
        val data = json.getJSONObject("data")
        val attr = data.getJSONObject("attributes")

        val artistViewUrl = attr.getString("url")
        val resources = extractAppleMusicResourceMap(json.getJSONArray("included"), artistViewUrl)

        val sectionsJsonArr = data.getJSONObject("relationships")
            .optJSONObject("content")?.getJSONArray("data")
        val sectionIds = HashMap<String, String>()
        if (sectionsJsonArr != null)
            for (i in 0 until sectionsJsonArr.length()) {
                val sectionJson = sectionsJsonArr.getJSONObject(i)
                if (sectionJson.getString("type") == "lockup/section") {
                    val id = sectionJson.getString("id")
                    val type = id.substring(id.indexOf('/') + 1)
                    sectionIds[type] = id
                }
            }

        val bio = attr.getStringOrNull("artistBio")
        val date = attr.getStringOrNull("bornOrFormedDate")
        val origin = attr.getStringOrNull("origin")
        val avatarId = data.getJSONObject("relationships").optJSONObject("artwork")
            ?.optJSONObject("data")?.getStringOrNull("id")
        val genreId = data.getJSONObject("relationships").optJSONObject("genres")
            ?.optJSONArray("data")?.optJSONObject(0)?.getStringOrNull("id")

        @Suppress("UNCHECKED_CAST")
        return AppleMusicArtistBrowse(
            attr.getString("name"),
            attr.getString("url"),
            data.optLong("id"),
            if (bio.isNullOrBlank()) null else bio,
            if (date.isNullOrBlank()) null else parseDate(date),
            if (origin.isNullOrBlank()) null else origin,
            if (avatarId == null) null else (resources[avatarId] as Image),
            if (genreId == null) null else (resources[genreId] as String),

            resources[sectionIds["topSongs"]] as AppleMusicSection<AppleMusicSong2>?,
            resources[sectionIds["latestRelease"]] as AppleMusicSection<AppleMusicAlbum2>?,
            resources[sectionIds["featuredAlbums"]] as AppleMusicSection<AppleMusicAlbum2>?,
            resources[sectionIds["fullAlbums"]] as AppleMusicSection<AppleMusicAlbum2>?,
            resources[sectionIds["recentVideos"]] as AppleMusicSection<AppleMusicMV2>?,
            resources[sectionIds["playlists"]] as AppleMusicSection<AppleMusicPlaylist2>?,
            resources[sectionIds["singleAlbums"]] as AppleMusicSection<AppleMusicAlbum2>?,
            resources[sectionIds["liveAlbums"]] as AppleMusicSection<AppleMusicAlbum2>?,
            resources[sectionIds["compilationAlbums"]] as AppleMusicSection<AppleMusicAlbum2>?,
            resources[sectionIds["appearsOnAlbums"]] as AppleMusicSection<AppleMusicAlbum2>?

        )
    }

    fun extractAppleMusicPlaylistBrowse(json: JSONObject): AppleMusicPlaylistBrowse {
        val resources = extractAppleMusicResourceMap(json.getJSONArray("included"), "")

        val data = json.getJSONObject("data")
        val tracksJsonArr = data.getJSONObject("relationships")
            .getJSONObject("songs").getJSONArray("data")
        val trackList = ArrayList<AppleMusicPlaylistItem2>()
        for (i in 0 until tracksJsonArr.length()) {
            val trackJson = tracksJsonArr.getJSONObject(i)
            val id = trackJson.getString("id")
            trackList.add(resources[id] as AppleMusicPlaylistItem2)
        }

        val attr = data.getJSONObject("attributes")
        val coverartId = data.getJSONObject("relationships").optJSONObject("artwork")
            ?.optJSONObject("data")?.getStringOrNull("id")

        return AppleMusicPlaylistBrowse(
            attr.getString("name"),
            data.getString("id"),
            attr.getString("url"),
            attr.getInt("trackCount"),
            parseDate(attr.getString("lastModifiedDate")),
            attr.optJSONObject("description")?.getStringOrNull("standard")?.let { unescapeHtml(it) },
            attr.optJSONObject("description")?.getStringOrNull("short")?.let { unescapeHtml(it) },
            if (coverartId == null) null else (resources[coverartId] as Image),
            trackList
        )
    }

    fun extractAppleMusicAlbumBrowse(json: JSONObject): AppleMusicAlbumBrowse {
        val data = json.getJSONObject("data")
        val attr = data.getJSONObject("attributes")

        val albumTitle = attr.getString("name")
        val artistViewUrl = attr.getString("artistUrl")
        val resources =
            extractAppleMusicResourceMap(json.getJSONArray("included"), artistViewUrl, albumTitle)

        val tracksJsonArr = data.getJSONObject("relationships")
            .getJSONObject("songs").getJSONArray("data")
        val trackList = ArrayList<AppleMusicTrack>()
        for (i in 0 until tracksJsonArr.length()) {
            val trackJson = tracksJsonArr.getJSONObject(i)
            val id = trackJson.getString("id")
            trackList.add(resources[id] as AppleMusicTrack)
        }

        val coverartId = data.getJSONObject("relationships").optJSONObject("artwork")
            ?.optJSONObject("data")?.getStringOrNull("id")

        return AppleMusicAlbumBrowse(
            attr.getString("name"),
            data.optLong("id"),
            attr.getString("url"),
            attr.getString("artistName"),
            attr.getString("artistUrl"),
            attr.getInt("trackCount"),
            parseDate(attr.getString("releaseDate")),
            attr.optJSONObject("itunesNotes")?.getStringOrNull("standard")?.let { unescapeHtml(it) },
            attr.optJSONObject("itunesNotes")?.getStringOrNull("short")?.let { unescapeHtml(it) },
            if (coverartId == null) null else (resources[coverartId] as Image),
            trackList
        )
    }

    private fun extractResource(
        json: JSONObject,
        defaultArtistViewUrl: String,
        defaultAlbumTitle: String?
    ): Any? {
        try {
            return when (json.getString("type")) {
                "image" -> extractAppleMusicImage(json)
                "genre" -> json.getJSONObject("attributes").getString("name")
                "offer" -> extractAppleMusicOffer(json)
                "lockup/section" -> extractAppleMusicSection(json)
                "lockup/album" -> extractAppleMusicAlbum2(json)
                "lockup/song" -> extractAppleMusicSong2(json, null)
                "lockup/music-video" -> extractAppleMusicMV2(json, defaultArtistViewUrl)
                "lockup/playlist" -> extractAppleMusicPlaylist2(json)
                "product/album/song" -> extractAppleMusicSong2(json, defaultAlbumTitle)
                "product/playlist/song" -> extractAppleMusicPlaylistItem2(json)
                else -> null
            }
        } catch (e: Exception) {
            Log.i("MusicBox", json.toString(4))
            throw e
        }
    }

    private fun extractAppleMusicResourceMap(
        jsonArr: JSONArray,
        defaultArtistViewUrl: String,
        defaultAlbumTitle: String? = null
    ): Map<String, Any> {
        val resources = HashMap<String, Any>()
        for (i in 0 until jsonArr.length()) {
            val resourceJson = jsonArr.getJSONObject(i)
            val id = resourceJson.getString("id")
            val resource = extractResource(resourceJson, defaultArtistViewUrl, defaultAlbumTitle)
            if (resource != null)
                resources[id] = resource
        }
        for (resource in resources.values) {
            if (resource is PostProcess)
                resource.connect(resources)
        }
        return resources
    }

    private fun extractAppleMusicSection(json: JSONObject): AppleMusicSection<out Any>? {
        val attr = json.getJSONObject("attributes")
        val name = attr.getString("name")
        val url = attr.getString("seeAllUrl")

        val dataJsonArr =
            json.getJSONObject("relationships").getJSONObject("content").getJSONArray("data")
        val entityIds = ArrayList<String>()
        for (i in 0 until dataJsonArr.length()) {
            val dataJson = dataJsonArr.getJSONObject(i)
            entityIds.add(dataJson.getString("id"))
        }

        return when (attr.getString("type")) {
            "topSongs" -> AppleMusicSection<AppleMusicSong2>(name, url, entityIds)
            "latestRelease" -> AppleMusicSection<AppleMusicAlbum2>(name, url, entityIds)
            "featuredAlbums" -> AppleMusicSection<AppleMusicAlbum2>(name, url, entityIds)
            "fullAlbums" -> AppleMusicSection<AppleMusicAlbum2>(name, url, entityIds)
            "recentVideos" -> AppleMusicSection<AppleMusicMV2>(name, url, entityIds)
            "playlists" -> AppleMusicSection<AppleMusicPlaylist2>(name, url, entityIds)
            "singleAlbums" -> AppleMusicSection<AppleMusicAlbum2>(name, url, entityIds)
            "liveAlbums" -> AppleMusicSection<AppleMusicAlbum2>(name, url, entityIds)
            "compilationAlbums" -> AppleMusicSection<AppleMusicAlbum2>(name, url, entityIds)
            "appearsOnAlbums" -> AppleMusicSection<AppleMusicAlbum2>(name, url, entityIds)
            else -> null
        }
    }

    private fun extractAppleMusicOffer(json: JSONObject): Int? {
        val assetsJsonArr = json.optJSONObject("attributes")?.optJSONArray("assets") ?: return null
        for (i in 0 until assetsJsonArr.length()) {
            val assetJson = assetsJsonArr.getJSONObject(i)
            if (!assetJson.isNull("duration")) {
                return assetJson.getInt("duration")
            }
        }
        return null
    }

    private fun extractAppleMusicAlbum2(json: JSONObject): AppleMusicAlbum2 {
        val attr = json.getJSONObject("attributes")
        return AppleMusicAlbum2(
            attr.getString("name"),
            json.optLong("id"),
            attr.getString("url"),
            attr.getString("artistName"),
            attr.getInt("trackCount"),
            parseDate(attr.getString("releaseDate")),
            json.getJSONObject("relationships").optJSONObject("artwork")
                ?.optJSONObject("data")?.getString("id")
        )
    }

    private fun extractAppleMusicSong2(
        json: JSONObject,
        defaultAlbumTitle: String?
    ): AppleMusicSong2 {
        val attr = json.getJSONObject("attributes")
        val rela = json.getJSONObject("relationships")

        val artistViewUrl = attr.getString("artistUrl")
        val artistId = if (attr.isNull("artistId")) parseId(attr.getString("artistUrl"))
        else attr.optLong("artistId")

        val dataJsonArr = rela.getJSONObject("offers").getJSONArray("data")
        val offerIds = ArrayList<String>()
        for (i in 0 until dataJsonArr.length()) {
            val dataJson = dataJsonArr.getJSONObject(i)
            offerIds.add(dataJson.getString("id"))
        }
        return AppleMusicSong2(
            attr.getString("name"),
            json.optLong("id"),
            attr.getString("artistName"),
            artistId,
            artistViewUrl,
            defaultAlbumTitle ?: attr.getString("collectionName"),
            rela.optJSONObject("artwork")?.optJSONObject("data")?.getString("id"),
            offerIds
        )
    }

    private fun extractAppleMusicMV2(json: JSONObject, artistViewUrl: String): AppleMusicMV2 {
        val attr = json.getJSONObject("attributes")
        val rela = json.getJSONObject("relationships")

        val dataJsonArr = rela.getJSONObject("offers").getJSONArray("data")
        val offerIds = ArrayList<String>()
        for (i in 0 until dataJsonArr.length()) {
            val dataJson = dataJsonArr.getJSONObject(i)
            offerIds.add(dataJson.getString("id"))
        }
        return AppleMusicMV2(
            attr.getString("name"),
            json.optLong("id"),
            attr.getString("artistName"),
            parseId(artistViewUrl),
            ensureViewUrl(artistViewUrl),
            rela.optJSONObject("artwork")?.optJSONObject("data")?.getString("id"),
            offerIds
        )
    }

    private fun extractAppleMusicPlaylist2(json: JSONObject): AppleMusicPlaylist2 {
        val attr = json.getJSONObject("attributes")
        return AppleMusicPlaylist2(
            attr.getString("name"),
            attr.getString("url"),
            parseDate(attr.getString("lastModifiedDate")),
            json.getJSONObject("relationships").optJSONObject("artwork")
                ?.optJSONObject("data")?.getString("id")
        )
    }

    private fun extractAppleMusicPlaylistItem2(json: JSONObject): AppleMusicPlaylistItem2 {
        val attr = json.getJSONObject("attributes")
        val rela = json.getJSONObject("relationships")

        val dataJsonArr = rela.getJSONObject("offers").getJSONArray("data")
        val offerIds = ArrayList<String>()
        for (i in 0 until dataJsonArr.length()) {
            val dataJson = dataJsonArr.getJSONObject(i)
            offerIds.add(dataJson.getString("id"))
        }
        return AppleMusicPlaylistItem2(
            attr.getString("name"),
            json.optLong("id"),
            attr.getString("artistName"),
            attr.optLong("artistId"),
            attr.getString("artistUrl"),
            attr.getString("kind") == "musicVideo",
            rela.optJSONObject("artwork")?.optJSONObject("data")?.getString("id"),
            offerIds
        )
    }


    fun extractAppleMusicSong3(json: JSONObject): AppleMusicSong3 {
        var length: Int = 0
        val offersJsonArr = json.getJSONArray("offers")
        all@ for (i in 0 until offersJsonArr.length()) {
            val offerJson = offersJsonArr.getJSONObject(i)
            val assetsJsonArr = offerJson.getJSONArray("assets")
            for (j in 0 until assetsJsonArr.length()) {
                val assetJson = assetsJsonArr.getJSONObject(j)
                if (!assetJson.isNull("duration")) {
                    length = assetJson.getInt("duration")
                    break@all
                }
            }
        }
        return AppleMusicSong3(
            json.getString("name"),
            json.optLong("id"),
            json.getString("artistName"),
            json.optLong("artistId"),
            json.getString("artistUrl"),
            json.getString("collectionName"),
            parseDate(json.getString("releaseDate")),
            if (json.isNull("artwork")) null else extractAppleMusicImage(json.getJSONObject("artwork")),
            length,
            json.getInt("trackNumber"),
            json.getDouble("popularity")
        )
    }

    fun extractAppleMusicMV3(json: JSONObject): AppleMusicMV3 {
        var length: Int = 0
        val offersJsonArr = json.getJSONArray("offers")
        all@ for (i in 0 until offersJsonArr.length()) {
            val offerJson = offersJsonArr.getJSONObject(i)
            val assetsJsonArr = offerJson.getJSONArray("assets")
            for (j in 0 until assetsJsonArr.length()) {
                val assetJson = assetsJsonArr.getJSONObject(j)
                if (!assetJson.isNull("duration")) {
                    length = assetJson.getInt("duration")
                    break@all
                }
            }
        }
        return AppleMusicMV3(
            json.getString("name"),
            json.optLong("id"),
            json.getString("artistName"),
            json.optLong("artistId"),
            json.getString("artistUrl"),
            parseDate(json.getString("releaseDate")),
            if (json.isNull("artwork")) null else extractAppleMusicImage(json.getJSONObject("artwork")),
            length,
            json.getDouble("popularity")
        )
    }

    fun extractAppleMusicAlbum3(json: JSONObject): AppleMusicAlbum3 {
        return AppleMusicAlbum3(
            json.getString("name"),
            json.optLong("id"),
            json.getString(("url")),
            json.getString("artistName"),
            json.optLong("artistId"),
            json.getString("artistUrl"),
            json.getInt("trackCount"),
            parseDate(json.getString("releaseDate")),
            if (json.isNull("artwork")) null else extractAppleMusicImage(json.getJSONObject("artwork")),
            json.getDouble("popularity")
        )
    }

    fun extractAppleMusicPlaylist3(json: JSONObject): AppleMusicPlaylist3 {
        return AppleMusicPlaylist3(
            json.getString("name"),
            json.getString("id"),
            json.getString("url"),
            parseDate(json.getString("lastModifiedDate")),
            json.optJSONObject("description")?.getStringOrNull("standard"),
            json.optJSONObject("description")?.getStringOrNull("short")
        )
    }


    private fun extractAppleMusicImage(json: JSONObject): AppleMusicImage {
        val attr = if (!json.isNull("url")) json
        else json.getJSONObject("attributes")
        val url = attr.getString("url")
        val templateUrl = url.replace("{f}", "jpg")
        val maxWidth = attr.getInt("width")
        val maxHeight = attr.getInt("height")
        return AppleMusicImage(templateUrl, maxWidth, maxHeight)
    }

    private fun parseImageUrl(imageUrl: String?): Image? {
        if (imageUrl.isNullOrEmpty())
            return null

        val slash = '/'
        val (dir, file) = imageUrl.splitBtIndex(imageUrl.lastIndexOf(slash))
        var first = true
        val fileTemplate = """\d+""".toRegex().replace(file) {
            if (first) {
                first = false; "{w}"
            } else "{h}"
        }
        val urlTemplate = "$dir$slash$fileTemplate"
        return AppleMusicImage(urlTemplate)
    }

    private fun parseDate(_dateStr: String): ApproxDate {
        // 2015-11-05T12:00:00Z
        // 2015
        val index = _dateStr.indexOf('T')
        val dateStr = if (index == -1) _dateStr
        else _dateStr.substring(0, index)
        val tokens = dateStr.split('-', limit = 3)
        return ApproxDate(
            tokens[0].toInt(),
            if (tokens.size >= 2) tokens[1].toInt() else null,
            if (tokens.size >= 3) tokens[2].toInt() else null
        )
    }

    private fun parseId(url: String): Long {
        if (ensureViewUrl(url) != null) {
            val paramsIdx = url.lastIndexOf('?')
            val baseUrl = if (paramsIdx == -1) url
            else url.substring(0, paramsIdx)
            return baseUrl.substring(baseUrl.lastIndexOf('/') + 1).toLong()
        } else {
            val start = url.indexOf("ids=") + 4
            val idx = url.indexOf('?', start)
            val end = if (idx == -1) url.length else idx
            return url.substring(start, end).split('-')[0].toLong()
        }
    }

    private fun ensureViewUrl(url: String): String? {
        return if ("WebObjects" in url) null else url
    }
}

