package com.musichub.scraper

import android.util.Log
import com.musichub.concurrent.CallbackExecutor
import com.musichub.concurrent.Cancellable
import com.musichub.concurrent.RequestFuture
import com.musichub.concurrent.ResponseListener
import com.musichub.networking.BasicHttpRequests
import com.musichub.networking.UrlParse
import com.musichub.util.*
import org.json.JSONObject
import org.jsoup.nodes.Element
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList


class YoutubeScraper(
    private val basicHttpRequests: BasicHttpRequests,
    private val callbackExecutor: CallbackExecutor
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
        val url = UrlParse.joinUrl(baseUrl, "results", params = UrlParse.encodeParams(params))
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
        val url = UrlParse.joinUrl(baseUrl, "watch", params = UrlParse.encodeParams(params))
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

    fun parseYoutubeUrl(url: String): String? {
        return try {
            parseVideoId(url)
        } catch (e: IllegalArgumentException) {
            null
        }
    }

    private fun checkVideoId(youtubeVideoId: String): Boolean {
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
                    "watch" -> UrlParse.decodeParams(url.query)["v"] ?: throw IllegalArgumentException(
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

    private fun parseViewCount(numViewsString: String): Long? {
        val str = numViewsString.replace(",", "")
        return try {
            parseFirstLong(str)
        }
        catch (e: Exception) {
            null
        }
    }

    private fun extractImage(imageElm: Element): Image {
        val imageUrl = if (imageElm.attr("data-thumb").isNotEmpty()) imageElm.attr("data-thumb")
        else imageElm.attr("src")
        val width = imageElm.attr("width").toInt()
        val height = imageElm.attr("height").toInt()
        return SingleSourceImage(imageUrl, width, height)
    }




    fun getVideoStreams(
        youtubeVideoId: String,
        listener: ResponseListener<YoutubeVideoStreams>
    ): Cancellable {
        return callbackExecutor.executeCallback(listener) {
            val params = mapOf("v" to youtubeVideoId, desktopParam)
            val watchUrl =
                UrlParse.joinUrl(baseUrl, "watch", params = UrlParse.encodeParams(params))
            val watchHtml = basicHttpRequests.get(watchUrl)

            val streamList = extractStreams(youtubeVideoId, watchHtml)
            YoutubeVideoStreams(streamList, youtubeVideoId)
        }
    }

    private fun extractStreams(youtubeVideoId: String, watchHtml: String): List<YoutubeStream> {
        if ("og:title" !in watchHtml)
            throw RuntimeException("video not available")
        val ageRestricted = "og:restrictions:age" in watchHtml
        var jsUrl: String? = null

        val playerResponseJson = if (!ageRestricted) {
            val configJson =
                watchHtml.regexSearch(""";ytplayer\.config\s*=\s*(\{.*?\});""".toRegex(), 1)
            val config = JSONObject(configJson)

            jsUrl = config.getJSONObject("assets").getString("js")
            config.getJSONObject("args").getString("player_response")
        } else {
            val infoUrl = "https://www.youtube.com/get_video_info?video_id=$youtubeVideoId"
            val infoQs = basicHttpRequests.get(infoUrl)
            val info = UrlParse.decodeParams(infoQs)
            info["player_response"] ?: throw RuntimeException("cannot find player_response")
        }
        val playerResponse = JSONObject(playerResponseJson)
        val streamingData = playerResponse.getJSONObject("streamingData")

        val streams = ArrayList<JSONObject>()
        for (arg in listOf("formats", "adaptiveFormats")) {
            if (streamingData.has(arg)) {
                val streamingDataFormats = streamingData.getJSONArray(arg)
                for (i in 0 until streamingDataFormats.length())
                    streams.add(streamingDataFormats.getJSONObject(i))
            }
        }

        if (streams.all { !it.has("cipher") })
            return convertStreams(streams)

        if (jsUrl == null) {
            val embedUrl = "https://www.youtube.com/embed/$youtubeVideoId"
            val embedHtml = basicHttpRequests.get(embedUrl)
            val jsUrlJson = embedHtml.regexSearch(""""assets":\s*\{.*?"js":\s*"([^"]+)".*?\}""".toRegex(), 1)
            jsUrl = JSONObject(jsUrlJson).getString("js")
        }
        jsUrl = UrlParse.joinUrl(baseUrl, jsUrl!!)

        val decryptFunc = synchronized(decryptFuncCache) {
            decryptFuncCache[jsUrl] ?: run {
                val jsCode = basicHttpRequests.get(jsUrl)
                val newDecryptFunc = extractDecryptFunc(jsCode)
                decryptFuncCache[jsCode] = newDecryptFunc
                newDecryptFunc
            }
        }

        for (stream in streams) {
            val cipher = UrlParse.decodeParams(stream.getString("cipher"))
            val url = cipher["url"] + "&sig=${decryptFunc(cipher["s"]!!)}"
            stream.put("url", url)
        }
        return convertStreams(streams)
    }

    private fun extractDecryptFunc(jsCode: String): (String) -> String {
        val funcCode = jsCode.regexSearch("""split\(""\);(.*?);return""".toRegex(), 1)
        // e.g.  "Ks.ki(a,62);Ks.ki(a,33);Ks.ki(a,34);Ks.ki(a,67);Ks.M6(a,9)"

        val funcParts = funcCode.split(';')
        // e.g.  ["Ks.ki(a,62)", "Ks.ki(a,33)", "Ks.ki(a,34)", "Ks.ki(a,67)", "Ks.M6(a,9)"]

        val objName = funcParts[0].split('.')[0].trim()
        // e.g.  "Ks"

        val objCode = jsCode.regexSearch(
            """var ${Regex.escape(objName)}=\{(.*?)\};""".toRegex(RegexOption.DOT_MATCHES_ALL),
            1
        )
        // e.g.  "D3:function(a,b){a.splice(0,b)},
        //        M6:function(a){a.reverse()},
        //        ki:function(a,b){var c=a[0];a[0]=a[b%a.length];a[b%a.length]=c}"

        val objParts = objCode.split("},")
        // e.g.  ["D3:function(a,b){a.splice(0,b)",
        //        "M6:function(a){a.reverse()",
        //        "ki:function(a,b){var c=a[0];a[0]=a[b%a.length];a[b%a.length]=c}"]

        val objMapping = TreeMap<String, Int>()
        for (objPart in objParts) {
            val (partName, partCode) = objPart.split(':', limit = 2)
            objMapping[partName.trim()] = when {
                "splice" in partCode -> 0
                "reverse" in partCode -> 1
                else -> 2
            }
        }

        return fun(encryptedSig: String): String {
            val sb = StringBuilder(encryptedSig)
            val regex = """${Regex.escape(objName)}\.(\w+)\(\w+,(\d+)\)""".toRegex()
            for (funPart in funcParts) {
                val match = regex.matchEntire(funPart)

                val key =
                    match?.groups?.get(1)?.value ?: throw RuntimeException("decrypt function error")
                // e.g.  "ki"

                val num = match.groups[2]?.value?.toInt()
                    ?: throw RuntimeException("decrypt function error")
                // e.g.  62

                when (objMapping[key] ?: throw RuntimeException("decrypt function error")) {
                    0 -> sb.delete(0, num)
                    1 -> sb.reverse()
                    2 -> {
                        val r = num % sb.length
                        val temp = sb[0]
                        sb.setCharAt(0, sb[r])
                        sb.setCharAt(r, temp)
                    }
                }
            }
            return sb.toString()
        }
    }

    private fun convertStreams(streams: List<JSONObject>): List<YoutubeStream> {
        val ytStreams = ArrayList<YoutubeStream>()
        for (stream in streams) {
            if (!stream.has("itag") || !stream.has("url"))
                continue
            val itag = stream.getInt("itag")
            if (!formats.containsKey(itag))
                continue
            val ytStream = formats[itag]!!.copy()
            ytStream.url = stream.getString("url")
            ytStream.itag = itag
            ytStreams.add(ytStream)
        }
        return ytStreams
    }

    private fun BasicHttpRequests.get(url: String): String {
        val future = RequestFuture<String>()
        this.httpGETString(url, headers, future) { it }
        return future.defaultGet()
    }

    private fun <T> RequestFuture<T>.defaultGet() = this.get(20, TimeUnit.SECONDS)

    private val formats = hashMapOf(
        // Progressive stream
        5 to YoutubeStream(
            Container.FLV, VideoFormat(VideoCodec.H263, 240), AudioFormat(
                AudioCodec.MP3, 64
            ), StreamProtocol.Progressive
        ),
        6 to YoutubeStream(
            Container.FLV, VideoFormat(VideoCodec.H263, 270), AudioFormat(
                AudioCodec.MP3, 64
            ), StreamProtocol.Progressive
        ),
        13 to YoutubeStream(
            Container._3GP, VideoFormat(VideoCodec.MP4V, 144), AudioFormat(
                AudioCodec.AAC, 24
            ), StreamProtocol.Progressive
        ),
        17 to YoutubeStream(
            Container._3GP, VideoFormat(VideoCodec.MP4V, 144), AudioFormat(
                AudioCodec.AAC, 24
            ), StreamProtocol.Progressive
        ),
        18 to YoutubeStream(
            Container.MP4, VideoFormat(VideoCodec.H264, 360), AudioFormat(
                AudioCodec.AAC, 96
            ), StreamProtocol.Progressive
        ),
        22 to YoutubeStream(
            Container.MP4, VideoFormat(VideoCodec.H264, 720), AudioFormat(
                AudioCodec.AAC, 192
            ), StreamProtocol.Progressive
        ),
        34 to YoutubeStream(
            Container.FLV, VideoFormat(VideoCodec.H264, 360), AudioFormat(
                AudioCodec.AAC, 128
            ), StreamProtocol.Progressive
        ),
        35 to YoutubeStream(
            Container.FLV, VideoFormat(VideoCodec.H264, 480), AudioFormat(
                AudioCodec.AAC, 128
            ), StreamProtocol.Progressive
        ),
        36 to YoutubeStream(
            Container._3GP, VideoFormat(VideoCodec.MP4V, 240), AudioFormat(
                AudioCodec.AAC, 24
            ), StreamProtocol.Progressive
        ),
        37 to YoutubeStream(
            Container.MP4, VideoFormat(VideoCodec.H264, 1080), AudioFormat(
                AudioCodec.AAC, 192
            ), StreamProtocol.Progressive
        ),
        38 to YoutubeStream(
            Container.MP4, VideoFormat(VideoCodec.H264, 3072), AudioFormat(
                AudioCodec.AAC, 192
            ), StreamProtocol.Progressive
        ),
        43 to YoutubeStream(
            Container.WebM, VideoFormat(VideoCodec.VP8, 360), AudioFormat(
                AudioCodec.Vorbis, 128
            ), StreamProtocol.Progressive
        ),
        44 to YoutubeStream(
            Container.WebM, VideoFormat(VideoCodec.VP8, 480), AudioFormat(
                AudioCodec.Vorbis, 128
            ), StreamProtocol.Progressive
        ),
        45 to YoutubeStream(
            Container.WebM, VideoFormat(VideoCodec.VP8, 720), AudioFormat(
                AudioCodec.Vorbis, 192
            ), StreamProtocol.Progressive
        ),
        46 to YoutubeStream(
            Container.WebM, VideoFormat(VideoCodec.VP8, 1080), AudioFormat(
                AudioCodec.Vorbis, 192
            ), StreamProtocol.Progressive
        ),
        59 to YoutubeStream(
            Container.MP4, VideoFormat(VideoCodec.H264, 480), AudioFormat(
                AudioCodec.AAC, 128
            ), StreamProtocol.Progressive
        ),
        78 to YoutubeStream(
            Container.MP4, VideoFormat(VideoCodec.H264, 480), AudioFormat(
                AudioCodec.AAC, 128
            ), StreamProtocol.Progressive
        ),

        // 3D videos
        82 to YoutubeStream(
            Container.MP4, VideoFormat(VideoCodec.H264, 360), AudioFormat(
                AudioCodec.AAC, 128
            ), StreamProtocol.Progressive
        ),
        83 to YoutubeStream(
            Container.MP4, VideoFormat(VideoCodec.H264, 480), AudioFormat(
                AudioCodec.AAC, 128
            ), StreamProtocol.Progressive
        ),
        84 to YoutubeStream(
            Container.MP4, VideoFormat(VideoCodec.H264, 720), AudioFormat(
                AudioCodec.AAC, 192
            ), StreamProtocol.Progressive
        ),
        85 to YoutubeStream(
            Container.MP4, VideoFormat(VideoCodec.H264, 1080), AudioFormat(
                AudioCodec.AAC, 192
            ), StreamProtocol.Progressive
        ),
        100 to YoutubeStream(
            Container.WebM, VideoFormat(VideoCodec.VP8, 360), AudioFormat(
                AudioCodec.Vorbis, 128
            ), StreamProtocol.Progressive
        ),
        101 to YoutubeStream(
            Container.WebM, VideoFormat(VideoCodec.VP8, 480), AudioFormat(
                AudioCodec.Vorbis, 192
            ), StreamProtocol.Progressive
        ),
        102 to YoutubeStream(
            Container.WebM, VideoFormat(VideoCodec.VP8, 720), AudioFormat(
                AudioCodec.Vorbis, 192
            ), StreamProtocol.Progressive
        ),

        // HLS
        91 to YoutubeStream(
            Container.MP4, VideoFormat(VideoCodec.H264, 144), AudioFormat(
                AudioCodec.AAC, 48
            ), StreamProtocol.HLS
        ),
        92 to YoutubeStream(
            Container.MP4, VideoFormat(VideoCodec.H264, 240), AudioFormat(
                AudioCodec.AAC, 48
            ), StreamProtocol.HLS
        ),
        93 to YoutubeStream(
            Container.MP4, VideoFormat(VideoCodec.H264, 360), AudioFormat(
                AudioCodec.AAC, 128
            ), StreamProtocol.HLS
        ),
        94 to YoutubeStream(
            Container.MP4, VideoFormat(VideoCodec.H264, 480), AudioFormat(
                AudioCodec.AAC, 128
            ), StreamProtocol.HLS
        ),
        95 to YoutubeStream(
            Container.MP4, VideoFormat(VideoCodec.H264, 720), AudioFormat(
                AudioCodec.AAC, 256
            ), StreamProtocol.HLS
        ),
        96 to YoutubeStream(
            Container.MP4, VideoFormat(VideoCodec.H264, 1080), AudioFormat(
                AudioCodec.AAC, 256
            ), StreamProtocol.HLS
        ),
        132 to YoutubeStream(
            Container.MP4, VideoFormat(VideoCodec.H264, 240), AudioFormat(
                AudioCodec.AAC, 48
            ), StreamProtocol.HLS
        ),
        151 to YoutubeStream(
            Container.MP4, VideoFormat(VideoCodec.H264, 72), AudioFormat(
                AudioCodec.AAC, 24
            ), StreamProtocol.HLS
        ),

        // DASH MP4 video
        133 to YoutubeStream(
            Container.MP4, VideoFormat(VideoCodec.H264, 240), null,
            StreamProtocol.DASH
        ),
        134 to YoutubeStream(
            Container.MP4, VideoFormat(VideoCodec.H264, 360), null,
            StreamProtocol.DASH
        ),
        135 to YoutubeStream(
            Container.MP4, VideoFormat(VideoCodec.H264, 480), null,
            StreamProtocol.DASH
        ),
        136 to YoutubeStream(
            Container.MP4, VideoFormat(VideoCodec.H264, 720), null,
            StreamProtocol.DASH
        ),
        137 to YoutubeStream(
            Container.MP4, VideoFormat(VideoCodec.H264, 1080), null,
            StreamProtocol.DASH
        ),
        138 to YoutubeStream(
            Container.MP4, VideoFormat(VideoCodec.H264, 2160), null,
            StreamProtocol.DASH
        ),
        160 to YoutubeStream(
            Container.MP4, VideoFormat(VideoCodec.H264, 144), null,
            StreamProtocol.DASH
        ),
        212 to YoutubeStream(
            Container.MP4, VideoFormat(VideoCodec.H264, 480), null,
            StreamProtocol.DASH
        ),
        298 to YoutubeStream(
            Container.MP4, VideoFormat(VideoCodec.H264, 720, 60), null,
            StreamProtocol.DASH
        ),
        299 to YoutubeStream(
            Container.MP4, VideoFormat(VideoCodec.H264, 1080, 60), null,
            StreamProtocol.DASH
        ),
        264 to YoutubeStream(
            Container.MP4, VideoFormat(VideoCodec.H264, 1440), null,
            StreamProtocol.DASH
        ),
        266 to YoutubeStream(
            Container.MP4, VideoFormat(VideoCodec.H264, 2160), null,
            StreamProtocol.DASH
        ),

        // DASH MP4 audio
        139 to YoutubeStream(
            Container.M4A, null, AudioFormat(AudioCodec.AAC, 48),
            StreamProtocol.DASH
        ),
        140 to YoutubeStream(
            Container.M4A, null, AudioFormat(AudioCodec.AAC, 128),
            StreamProtocol.DASH
        ),
        141 to YoutubeStream(
            Container.M4A, null, AudioFormat(AudioCodec.AAC, 256),
            StreamProtocol.DASH
        ),

        // DASH WebM video
        167 to YoutubeStream(
            Container.WebM, VideoFormat(VideoCodec.VP8, 360), null,
            StreamProtocol.DASH
        ),
        168 to YoutubeStream(
            Container.WebM, VideoFormat(VideoCodec.VP8, 480), null,
            StreamProtocol.DASH
        ),
        169 to YoutubeStream(
            Container.WebM, VideoFormat(VideoCodec.VP8, 720), null,
            StreamProtocol.DASH
        ),
        170 to YoutubeStream(
            Container.WebM, VideoFormat(VideoCodec.VP8, 1080), null,
            StreamProtocol.DASH
        ),
        218 to YoutubeStream(
            Container.WebM, VideoFormat(VideoCodec.VP8, 480), null,
            StreamProtocol.DASH
        ),
        219 to YoutubeStream(
            Container.WebM, VideoFormat(VideoCodec.VP8, 480), null,
            StreamProtocol.DASH
        ),
        278 to YoutubeStream(
            Container.WebM, VideoFormat(VideoCodec.VP9, 144), null,
            StreamProtocol.DASH
        ),
        242 to YoutubeStream(
            Container.WebM, VideoFormat(VideoCodec.VP9, 240), null,
            StreamProtocol.DASH
        ),
        243 to YoutubeStream(
            Container.WebM, VideoFormat(VideoCodec.VP9, 360), null,
            StreamProtocol.DASH
        ),
        244 to YoutubeStream(
            Container.WebM, VideoFormat(VideoCodec.VP9, 480), null,
            StreamProtocol.DASH
        ),
        245 to YoutubeStream(
            Container.WebM, VideoFormat(VideoCodec.VP9, 480), null,
            StreamProtocol.DASH
        ),
        246 to YoutubeStream(
            Container.WebM, VideoFormat(VideoCodec.VP9, 480), null,
            StreamProtocol.DASH
        ),
        247 to YoutubeStream(
            Container.WebM, VideoFormat(VideoCodec.VP9, 720), null,
            StreamProtocol.DASH
        ),
        248 to YoutubeStream(
            Container.WebM, VideoFormat(VideoCodec.VP9, 1080), null,
            StreamProtocol.DASH
        ),
        271 to YoutubeStream(
            Container.WebM, VideoFormat(VideoCodec.VP9, 1440), null,
            StreamProtocol.DASH
        ),
        272 to YoutubeStream(
            Container.WebM, VideoFormat(VideoCodec.VP9, 2160), null,
            StreamProtocol.DASH
        ),
        302 to YoutubeStream(
            Container.WebM, VideoFormat(VideoCodec.VP9, 720, 60), null,
            StreamProtocol.DASH
        ),
        303 to YoutubeStream(
            Container.WebM, VideoFormat(VideoCodec.VP9, 1080, 60), null,
            StreamProtocol.DASH
        ),
        308 to YoutubeStream(
            Container.WebM, VideoFormat(VideoCodec.VP9, 1440, 60), null,
            StreamProtocol.DASH
        ),
        313 to YoutubeStream(
            Container.WebM, VideoFormat(VideoCodec.VP9, 2160), null,
            StreamProtocol.DASH
        ),
        315 to YoutubeStream(
            Container.WebM, VideoFormat(VideoCodec.VP9, 2160, 60), null,
            StreamProtocol.DASH
        ),

        // DASH WebM audio
        171 to YoutubeStream(
            Container.WebM, null, AudioFormat(AudioCodec.Vorbis, 128),
            StreamProtocol.DASH
        ),
        172 to YoutubeStream(
            Container.WebM, null, AudioFormat(AudioCodec.Vorbis, 256),
            StreamProtocol.DASH
        ),
        249 to YoutubeStream(
            Container.WebM, null, AudioFormat(AudioCodec.Opus, 50),
            StreamProtocol.DASH
        ),
        250 to YoutubeStream(
            Container.WebM, null, AudioFormat(AudioCodec.Opus, 70),
            StreamProtocol.DASH
        ),
        251 to YoutubeStream(
            Container.WebM, null, AudioFormat(AudioCodec.Opus, 160),
            StreamProtocol.DASH
        )
    )

    private val decryptFuncCache = TreeMap<String, (String) -> String>()
}



data class YoutubeVideo(
    val title: String,
    val youtubeVideoId: String,
    val uploaderName: String,
    val thumbnail: Image,
    val duration: Int,
    val viewCount: Long?,
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



data class YoutubeStream(
    val container: Container,
    val videoFormat: VideoFormat?,
    val audioFormat: AudioFormat?,
    val streamProtocol: StreamProtocol
) {
    init {
        assert(videoFormat != null || audioFormat != null)
    }

    var itag: Int = 0
        internal set

    var url = ""
        internal set
}

data class YoutubeVideoStreams(
    val streamList: List<YoutubeStream>,
    val echoYoutubeVideoId: String
)


data class VideoFormat(
    val codec: VideoCodec,
    val height: Int,
    val fps: Int? = null
)

enum class VideoCodec(val standard: String) {
    H263("H263"),
    H264("H264"),
    MP4V("MP4V"),
    VP8("VP8"),
    VP9("VP9")
}

data class AudioFormat(
    val codec: AudioCodec,
    val bitRate: Int
)

enum class AudioCodec(val standard: String) {
    MP3("MP3"),
    AAC("AAC"),
    Vorbis("Vorbis"),
    Opus("Opus")
}

enum class Container(val standard: String, val extension: String) {
    FLV("FLV", "flv"),
    _3GP("3GP", "3gp"),
    MP4("MP4", "mp4"),
    M4A("MP4", "m4a"),
    WebM("WebM", "webm")
}

enum class StreamProtocol {
    Progressive,
    DASH,
    HLS
}
