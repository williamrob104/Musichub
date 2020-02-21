package com.musichub.scraper

import android.util.Log
import com.musichub.concurrent.CallbackExecutor
import com.musichub.concurrent.Cancellable
import com.musichub.concurrent.ResponseListener
import com.musichub.networking.BasicHttpRequests
import com.musichub.networking.UrlParse.decodeParams
import com.musichub.networking.UrlParse.encodeParams
import com.musichub.networking.UrlParse.joinUrl
import com.musichub.util.*
import org.jsoup.nodes.Element
import java.net.MalformedURLException
import java.net.URL


class YoutubeScraper(
    private val basicHttpRequests: BasicHttpRequests,
    internal val callbackExecutor: CallbackExecutor
) {
    private val baseUrl = "https://www.youtube.com/"
    private val headers = null
    private val desktopParam = "app" to "desktop"

    fun searchVideos(
        query: String, page: Int = 1,
        listener: ResponseListener<YoutubeSearch<YoutubeVideo>>
    ): Cancellable {
        val params = mapOf(
            "search_query" to query,
            "sp" to "EgIQAQ==",
            "page" to page.toString(),
            desktopParam
        )
        val url = joinUrl(baseUrl, "results", params = encodeParams(params))
        return basicHttpRequests.httpGETHtml(url, headers, listener) {
            val videoList = ArrayList<YoutubeVideo>()
            for (videoElm in it.getElementsByClass("yt-lockup-tile")) {
                try {
                    val imageElm = videoElm.getElementByClass("video-thumb").getElementByTag("img")
                    val durationElm =
                        videoElm.getElementByClassOrNull("video-time") ?: /* live */ continue
                    val titleElm =
                        videoElm.getElementByClass("yt-lockup-title").getElementByTag("a")
                    if (titleElm.hasAttr("data-url")) /* ad */ continue
                    val uploaderElm =
                        videoElm.getElementByClass("yt-lockup-byline").getElementByTag("a")
                    val viewsElm =
                        videoElm.getElementByClass("yt-lockup-meta-info").children().last()
                    val descriptionElm = videoElm.getElementByClassOrNull("yt-lockup-description")

                    val image = extractImage(imageElm)
                    val duration = parseTime(durationElm.text())
                    val title = titleElm.text()
                    val videoId = parseVideoId(titleElm.attr("href"))
                    val uploaderName = uploaderElm.text()
                    val viewCount = parseViewCount(viewsElm.text())
                    val description = descriptionElm?.text()
                    videoList.add(
                        YoutubeVideo(
                            title, videoId, uploaderName,
                            image, duration, viewCount, description
                        )
                    )
                } catch (e: Exception) {
                    val str = videoElm.toString()
                    for (line in str.lines())
                        Log.i("MusicBox", line)
                    throw e
                }
            }
            YoutubeSearch(videoList, query, page)
        }
    }

    fun getVideoRelatedVideos(
        youtubeVideoId: String,
        listener: ResponseListener<YoutubeVideoRelatedVideos>
    ): Cancellable {
        val params = mapOf("v" to youtubeVideoId, desktopParam)
        val url = joinUrl(baseUrl, "watch", params = encodeParams(params))
        return basicHttpRequests.httpGETHtml(url, headers, listener) {
            val videoList = ArrayList<YoutubeVideo>()
            for (videoElm in it.getElementsByClass("related-list-item")) {
                if (!videoElm.child(0).hasClass("content-wrapper"))
                    continue

                val linkElm = videoElm.getElementByClass("content-link")
                val titleElm = videoElm.getElementByClass("title")
                val uploaderElm = videoElm.getElementByClass("attribution")
                val viewsElm = videoElm.getElementByClass("view-count")
                val imageElm = videoElm.getElementByTag("img")
                val durationElm = videoElm.getElementByClass("video-time")

                val videoId = parseVideoId(linkElm.attr("href"))
                val title = titleElm.text()
                val uploaderName = uploaderElm.text()
                val viewCount = parseViewCount(viewsElm.text())
                val image = extractImage(imageElm)
                val duration = parseTime(durationElm.text())
                videoList.add(
                    YoutubeVideo(
                        title, videoId, uploaderName,
                        image, duration, viewCount, null
                    )
                )
            }
            YoutubeVideoRelatedVideos(videoList, youtubeVideoId)
        }
    }

    companion object {
        fun parseYoutubeUrl(url: String): String? {
            return try {
                parseVideoId(url)
            } catch (e: IllegalArgumentException) {
                null
            }
        }

        fun checkVideoId(youtubeVideoId: String): Boolean {
            return youtubeVideoId.length == 11
        }

        private fun parseVideoId(videoLink: String): String {
            val slash = '/'
            val url = try {
                URL(videoLink)
            } catch (_: MalformedURLException) {
                val base = URL("https://www.youtube.com/")
                URL(base.protocol, base.host, videoLink)
            }
            val videoId = when {
                url.host.endsWith("youtube.com") -> {
                    val dirs = url.path.trimStart(slash).split('/')
                    when (dirs[0]) {
                        "watch" -> decodeParams(url.query)["v"] ?: throw IllegalArgumentException(
                            videoLink
                        )
                        "embed" -> dirs[1]
                        else -> throw IllegalArgumentException(videoLink)
                    }
                }
                url.host == "youtu.be" -> url.file.trimStart(slash)
                else -> throw IllegalArgumentException(videoLink)
            }
            if (checkVideoId(videoId))
                return videoId
            else
                throw IllegalArgumentException(videoLink)
        }
    }

    private fun parseViewCount(numViewsString: String): Long {
        val str = numViewsString.replace(",", "")
        return parseFirstLong(str)
    }

    private fun extractImage(imageElm: Element): Image {
        val imageUrl = if (imageElm.attr("data-thumb").isNotEmpty()) imageElm.attr("data-thumb")
        else imageElm.attr("src")
        val width = imageElm.attr("width").toInt()
        val height = imageElm.attr("height").toInt()
        return SingleSourceImage(imageUrl, width, height)
    }
}


data class YoutubeVideo(
    val title: String,
    val youtubeVideoId: String,
    val uploaderName: String,
    val thumbnail: Image,
    val duration: Int,
    val viewCount: Long,
    val descriptionPreview: String?
)


data class YoutubeSearch<T>(
    val itemList: List<T>,
    val echoQuery: String,
    val echoPage: Int
)

data class YoutubeVideoRelatedVideos(
    val videoList: List<YoutubeVideo>,
    val echoYoutubeVideoId: String
)
