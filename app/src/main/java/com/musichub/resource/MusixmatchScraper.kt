package com.musichub.resource

import com.musichub.concurrent.ResponseListener
import com.musichub.networking.BasicHttpRequests
import com.musichub.networking.UrlParse.encodeAll
import com.musichub.networking.UrlParse.joinUrl
import com.musichub.util.getElementByClass
import com.musichub.util.getElementByClassOrNull
import com.musichub.util.splitBtIndex
import org.jsoup.nodes.Document


class MusixmatchScraper(private val basicHttpRequests: BasicHttpRequests) {
    private val baseUrl = "https://www.musixmatch.com"

    fun searchTracks(query: String, page: Int=1,
                     listener: ResponseListener<MusixmatchSearch<MusixmatchTrack>>) {
        val url = joinUrl(baseUrl, "search", encodeAll(query), "tracks", page.toString())
        basicHttpRequests.httpGETHtml(url, null, listener) {
            val trackList = extractTrackList(it)
            MusixmatchSearch(trackList, query, page)
        }
    }

    fun searchTracksByLyrics(query: String, page: Int=1,
                             listener: ResponseListener<MusixmatchSearch<MusixmatchTrack>>) {
        val url = joinUrl(baseUrl, "search", encodeAll(query), "lyrics", page.toString())
        basicHttpRequests.httpGETHtml(url, null, listener) {
            val trackList = extractTrackList(it)
            MusixmatchSearch(trackList, query, page)
        }
    }

    fun getTrackLyrics(musixmatchTrackLink: String,
                       listener: ResponseListener<MusixmatchLyrics>) {
        val url = joinUrl(baseUrl, musixmatchTrackLink, "embed")
        basicHttpRequests.httpGETHtml(url, null, listener) {
            val lines = ArrayList<String>()
            val lyricsElm = it.getElementsByClass("track-widget-body").first()
            for (lineElm in lyricsElm.children()) {
                if (lineElm.tagName() == "p")
                    lines.add(lineElm.text())
            }
            MusixmatchLyrics(lines)
        }
    }

    private fun extractTrackList(doc: Document): List<MusixmatchTrack> {
        val trackList = ArrayList<MusixmatchTrack>()
        for (trackElm in doc.getElementsByClass("track-card")) {
            val imageElm = trackElm.getElementByClass("media-card-picture")
            val titleElm = trackElm.getElementByClass("title")
            val artistElms = trackElm.getElementsByClass("artist")
            val addLyricsElm = trackElm.getElementByClassOrNull("add-lyrics-button")

            val imageSrcSet = imageElm.child(0).attr("srcSet")
            val image = generateImage(imageSrcSet)
            val title = titleElm.child(0).text()
            val artists = artistElms.map { it.text() }
            val hasLyrics = addLyricsElm == null
            val musixmatchTrackLink = titleElm.attr("href")
            trackList.add(MusixmatchTrack(title, artists, hasLyrics, musixmatchTrackLink, image))
        }
        return trackList
    }

    private fun generateImage(imageSrcSet: String): Image? {
        if ("nocover" in imageSrcSet)
            return null

        val slash = '/'
        val imageUrl = imageSrcSet.split(',')[0].split(' ')[0]

        val (dir, file) = imageUrl.splitBtIndex(imageUrl.lastIndexOf(slash))
        val dotIndex = file.indexOfFirst { it == '.' }
        val (filename, ext) = file.splitBtIndex(if (dotIndex != -1) dotIndex else file.length)
        val underscoreIndex = filename.indexOfFirst { it == '_' }
        val img = if (underscoreIndex != -1) filename.substring(0, underscoreIndex)
                  else filename

        return MusixmatchImage(joinUrl(dir, "$img{0}.$ext"))
    }
}


data class MusixmatchTrack(
        val title: String,
        val artistsNames: List<String>,
        val hasLyrics: Boolean,
        val musixmatchTrackLink: String,
        val coverart: Image?
)

data class MusixmatchLyrics(
        val lines: List<String>
)

data class MusixmatchSearch<T>(
        val itemList: List<T>,
        val echoQuery: String,
        val echoPage: Int
)

class MusixmatchImage(private val urlTemplate: String): Image {
    private companion object {
        val size2Substitution = listOf(
                100 to "",
                350 to "_350_350",
                500 to "_500_500",
                800 to "_800_800"
        )
    }

    override fun sourceByShortSideEquals(pixels: Int, largerIfNotPresent: Boolean): Image.Source {
        val temp = size2Substitution.binarySearchBy(pixels, selector={ it.first })
        val index =
                if (temp >= 0)
                    temp
                else {
                    val insertionIndex = -(temp + 1)
                    val range = (0 until size2Substitution.size)
                    if (largerIfNotPresent) insertionIndex.coerceIn(range)
                    else (insertionIndex - 1).coerceIn(range)
                }
        val data = size2Substitution[index]
        return Image.Source(
                urlTemplate.replace("{0}", data.second),
                data.first,
                data.first
        )
    }

    override fun sourceByLongSideEquals(pixels: Int, largerIfNotPresent: Boolean): Image.Source {
        return sourceByShortSideEquals(pixels, largerIfNotPresent)
    }

    override fun sourceLargest(): Image.Source {
        val data = size2Substitution.last()
        return Image.Source(
                urlTemplate.replace("{0}", data.second),
                data.first,
                data.first
        )
    }
}
