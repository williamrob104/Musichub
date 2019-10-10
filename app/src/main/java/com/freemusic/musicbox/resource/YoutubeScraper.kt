package com.freemusic.musicbox.resource

import android.util.Log
import com.freemusic.musicbox.concurrent.CallbackExecutor
import com.freemusic.musicbox.concurrent.Cancellable
import com.freemusic.musicbox.concurrent.RequestFuture
import com.freemusic.musicbox.concurrent.ResponseListener
import com.freemusic.musicbox.networking.BasicHttpRequests
import com.freemusic.musicbox.networking.UrlParse
import com.freemusic.musicbox.networking.UrlParse.decodeParams
import com.freemusic.musicbox.networking.UrlParse.encodeParams
import com.freemusic.musicbox.networking.UrlParse.joinUrl
import com.freemusic.musicbox.resource.AudioCodec.*
import com.freemusic.musicbox.resource.Container.*
import com.freemusic.musicbox.resource.StreamProtocol.*
import com.freemusic.musicbox.resource.VideoCodec.*
import com.freemusic.musicbox.util.*
import org.json.JSONObject
import org.jsoup.nodes.Element
import java.io.File
import java.net.MalformedURLException
import java.net.URL
import java.util.*
import java.util.concurrent.TimeUnit
import kotlin.collections.ArrayList
import kotlin.collections.HashMap


class YoutubeScraper(private val basicHttpRequests: BasicHttpRequests,
                     internal val callbackExecutor: CallbackExecutor) {
    private val baseUrl = "https://www.youtube.com/"
    private val headers = null //hashMapOf("User-Agent" to "curl/7.37.1")
    private val desktopParam = "app" to "desktop"

    fun searchVideos(query: String, page: Int=1,
                    listener: ResponseListener<YoutubeSearch<YoutubeVideo>>): Cancellable {
        val params = mapOf("search_query" to query, "sp" to "EgIQAQ==", "page" to page.toString(), desktopParam)
        val url = joinUrl(baseUrl, "results", params=encodeParams(params))
        val timeLogger = TimeLogger("MusicBox searchVideos")
        return basicHttpRequests.httpGETHtml(url, headers, listener) {
            timeLogger.log("get data finished")
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
                }
                catch(e : Exception) {
                    val str = videoElm.toString()
                    for (line in str.lines())
                        Log.i("MusicBox", line)
                    throw e
                }
            }
            timeLogger.log("parse data finished")
            YoutubeSearch(videoList, query, page)
        }
    }

    fun getVideoRelatedVideos(youtubeVideoId: String,
                              listener: ResponseListener<YoutubeVideoRelatedVideos>): Cancellable {
        val params = mapOf("v" to youtubeVideoId, desktopParam)
        val url = joinUrl(baseUrl, "watch", params=encodeParams(params))
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
                videoList.add(YoutubeVideo(title, videoId, uploaderName,
                        image, duration, viewCount, null))
            }
            YoutubeVideoRelatedVideos(videoList, youtubeVideoId)
        }
    }

    fun getVideoStreams(youtubeVideoId: String,
                        listener: ResponseListener<YoutubeVideoStreams>): Cancellable {
        return callbackExecutor.executeCallback(listener) {
            val timeLogger = TimeLogger("MusicBox getVideoStreams")
            val params = mapOf("v" to youtubeVideoId, desktopParam)
            val watchUrl = joinUrl(baseUrl, "watch", params=encodeParams(params))
            val watchHtml = basicHttpRequests.get(watchUrl)
            timeLogger.log("get html finished")

            val streamList = extractStreams(youtubeVideoId, watchHtml)
            timeLogger.log("extract streams finished")
            YoutubeVideoStreams(streamList, youtubeVideoId)
        }
    }

    companion object {
        fun parseYoutubeUrl(url: String): String? {
            return try {
                parseVideoId(url)
            }
            catch (e: IllegalArgumentException) {
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
                        "watch" -> decodeParams(url.query)["v"] ?: throw IllegalArgumentException(videoLink)
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

    private fun extractStreams(youtubeVideoId: String, watchHtml: String): List<YoutubeStream> {
        if ("og:title" !in watchHtml)
            throw RuntimeException("video not available")
        //if ("liveStreamability" in watchHtml)
            //throw RuntimeException("live stream not supported")
        val ageRestricted = "og:restrictions:age" in watchHtml
        var jsUrl: String? = null

        val streamJointList = ArrayList<String>()
        val fmts = arrayOf("url_encoded_fmt_stream_map", "adaptive_fmts")
        if (!ageRestricted) {
            val configJson = regexSearch(""";ytplayer\.config\s*=\s*(\{.*?\});""".toRegex(), watchHtml, group=1)
            val config = JSONObject(configJson)
            for (fmt in fmts) {
                val streamJoint = config.getJSONObject("args").getStringOrNull(fmt)
                if (streamJoint != null)
                    streamJointList.add(streamJoint)
            }
            jsUrl = config.getJSONObject("assets").getString("js")
        }
        else {
            val infoUrl = "https://www.youtube.com/get_video_info?el=embedded&eurl=&ps=default&video_id=$youtubeVideoId"
            val infoQs = basicHttpRequests.get(infoUrl)
            val info = decodeParams(infoQs)
            for (fmt in fmts) {
                info[fmt]?.apply { streamJointList.add(this) }
            }
        }
        if (streamJointList.isEmpty())
            throw RuntimeException("cannot find streams")

        val streams = ArrayList<MutableMap<String, String>>()
        for (streamJoint in streamJointList)
            for (stream in streamJoint.split(','))
                streams.add(decodeParams(stream).toMutableMap())

        if (streams.all { !it.containsKey("s") }) {
            //println("Pre-signed")
            return convertStreams(streams)
        }

        if (jsUrl == null) {
            val embedUrl = "https://www.youtube.com/embed/$youtubeVideoId"
            val embedHtml = basicHttpRequests.get(embedUrl)
            val jsUrlJson = regexSearch(""""assets":\s*(\{.*?"js":\s*"[^"]+".*?\})""".toRegex(), embedHtml, group=1)
            jsUrl = JSONObject(jsUrlJson).getString("js")
        }
        jsUrl = joinUrl(baseUrl, jsUrl!!)
        //println("JS url: $jsUrl")

        val decryptFunc = synchronized(decryptFuncCache) {
            decryptFuncCache[jsUrl] ?: run {
                val jsCode = basicHttpRequests.get(jsUrl)
                val myDecryptFunc = extractDecryptFunc(jsCode)
                decryptFuncCache[jsCode] = myDecryptFunc
                myDecryptFunc
            }
        }
        for (stream in streams)
            stream["url"] = stream["url"] + "&sig=${decryptFunc(stream["s"]!!)}"
        return convertStreams(streams)
    }

    private fun extractDecryptFunc(jsCode: String): (String)->String {
        val funcCode = try {
            val funcName = regexSearch(listOf(
                    """\b[cs]\s*&&\s*[adf]\.set\([^,]+\s*,\s*encodeURIComponent\s*\(\s*(?[a-zA-Z0-9${'$'}]+)\(""".toRegex(),
                    """\b[a-zA-Z0-9]+\s*&&\s*[a-zA-Z0-9]+\.set\([^,]+\s*,\s*encodeURIComponent\s*\(\s*(?[a-zA-Z0-9${'$'}]+)\(""".toRegex(),
                    """(?[a-zA-Z0-9${'$'}]+)\s*=\s*function\(\s*a\s*\)\s*\{\s*a\s*=\s*a\.split\(\s*""\s*\)""".toRegex()
            ), jsCode, group=1)
            regexSearch("""${Regex.escape(funcName)}=function\(\w\)\{[a-z=\.\(\"\)]*;(.*);(?:.+)\}""".toRegex(), jsCode, group=1)
        }
        catch (e: Exception) {
            regexSearch("""split\(""\);(.*?);return""".toRegex(), jsCode, group=1)
        }
        val funcParts = funcCode.split(';')

        val objName = funcParts[0].split('.')[0].trim()
        val objCode = regexSearch("""var ${Regex.escape(objName)}=\{(.*?)\};""".toRegex(RegexOption.DOT_MATCHES_ALL), jsCode, group=1)
        val objParts = objCode.split("},")

        val mapping = HashMap<String, Int>()
        for (objPart in objParts) {
            val (partName, partCode) = objPart.split(':',limit=2)
            mapping[partName.trim()] = when {
                "reverse" in partCode -> 0
                "splice" in partCode -> 1
                else -> 2
            }
        }

        return fun (encryptedSig: String): String {
            val err = RuntimeException("decrypt function error")
            val sb = StringBuilder(encryptedSig)
            val regex = """${Regex.escape(objName)}\.(\w+)\(\w+,(\d+)\)""".toRegex()
            for (funPart in funcParts) {
                val match = regex.matchEntire(funPart) ?: throw err
                val key = match.groups[1]?.value ?: throw err
                val num = match.groups[2]?.value?.toInt() ?: throw err
                when (mapping[key] ?: throw err) {
                    0 -> sb.reverse()
                    1 -> sb.delete(0, num)
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

    private fun convertStreams(streams: List<Map<String, String>>): List<YoutubeStream> {
        val ytStreams = ArrayList<YoutubeStream>()
        for (stream in streams) {
            if (!stream.containsKey("itag") || !stream.containsKey("url"))
                continue
            val itag = stream["itag"]!!.toInt()
            if (!formats.containsKey(itag))
                continue
            val ytStream = formats[itag]!!.copy()
            ytStream.url = stream["url"]!!
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

    internal fun <T> RequestFuture<T>.defaultGet() = this.get(20, TimeUnit.SECONDS)

    private val formats = hashMapOf(
            // Progressive stream
            5 to YoutubeStream(FLV, VideoFormat(H263, 240), AudioFormat(MP3, 64), Progressive),
            6 to YoutubeStream(FLV, VideoFormat(H263, 270), AudioFormat(MP3, 64), Progressive),
            13 to YoutubeStream(_3GP, VideoFormat(MP4V, 144), AudioFormat(AAC, 24), Progressive),
            17 to YoutubeStream(_3GP, VideoFormat(MP4V, 144), AudioFormat(AAC, 24), Progressive),
            18 to YoutubeStream(MP4, VideoFormat(H264, 360), AudioFormat(AAC, 96), Progressive),
            22 to YoutubeStream(MP4, VideoFormat(H264, 720), AudioFormat(AAC, 192), Progressive),
            34 to YoutubeStream(FLV, VideoFormat(H264, 360), AudioFormat(AAC, 128), Progressive),
            35 to YoutubeStream(FLV, VideoFormat(H264, 480), AudioFormat(AAC, 128), Progressive),
            36 to YoutubeStream(_3GP, VideoFormat(MP4V, 240), AudioFormat(AAC, 24), Progressive),
            37 to YoutubeStream(MP4, VideoFormat(H264, 1080), AudioFormat(AAC, 192), Progressive),
            38 to YoutubeStream(MP4, VideoFormat(H264, 3072), AudioFormat(AAC, 192), Progressive),
            43 to YoutubeStream(WebM, VideoFormat(VP8, 360), AudioFormat(Vorbis, 128), Progressive),
            44 to YoutubeStream(WebM, VideoFormat(VP8, 480), AudioFormat(Vorbis, 128), Progressive),
            45 to YoutubeStream(WebM, VideoFormat(VP8, 720), AudioFormat(Vorbis, 192), Progressive),
            46 to YoutubeStream(WebM, VideoFormat(VP8, 1080), AudioFormat(Vorbis, 192), Progressive),
            59 to YoutubeStream(MP4, VideoFormat(H264, 480), AudioFormat(AAC, 128), Progressive),
            78 to YoutubeStream(MP4, VideoFormat(H264, 480), AudioFormat(AAC, 128), Progressive),

            // 3D videos
            82 to YoutubeStream(MP4, VideoFormat(H264, 360), AudioFormat(AAC, 128), Progressive),
            83 to YoutubeStream(MP4, VideoFormat(H264, 480), AudioFormat(AAC, 128), Progressive),
            84 to YoutubeStream(MP4, VideoFormat(H264, 720), AudioFormat(AAC, 192), Progressive),
            85 to YoutubeStream(MP4, VideoFormat(H264, 1080), AudioFormat(AAC, 192), Progressive),
            100 to YoutubeStream(WebM, VideoFormat(VP8, 360), AudioFormat(Vorbis, 128), Progressive),
            101 to YoutubeStream(WebM, VideoFormat(VP8, 480), AudioFormat(Vorbis, 192), Progressive),
            102 to YoutubeStream(WebM, VideoFormat(VP8, 720), AudioFormat(Vorbis, 192), Progressive),

            // HLS
            91 to YoutubeStream(MP4, VideoFormat(H264, 144), AudioFormat(AAC, 48), HLS),
            92 to YoutubeStream(MP4, VideoFormat(H264, 240), AudioFormat(AAC, 48), HLS),
            93 to YoutubeStream(MP4, VideoFormat(H264, 360), AudioFormat(AAC, 128), HLS),
            94 to YoutubeStream(MP4, VideoFormat(H264, 480), AudioFormat(AAC, 128), HLS),
            95 to YoutubeStream(MP4, VideoFormat(H264, 720), AudioFormat(AAC, 256), HLS),
            96 to YoutubeStream(MP4, VideoFormat(H264, 1080), AudioFormat(AAC, 256), HLS),
            132 to YoutubeStream(MP4, VideoFormat(H264, 240), AudioFormat(AAC, 48), HLS),
            151 to YoutubeStream(MP4, VideoFormat(H264, 72), AudioFormat(AAC, 24), HLS),

            // DASH MP4 video
            133 to YoutubeStream(MP4, VideoFormat(H264, 240), null, DASH),
            134 to YoutubeStream(MP4, VideoFormat(H264, 360), null, DASH),
            135 to YoutubeStream(MP4, VideoFormat(H264, 480), null, DASH),
            136 to YoutubeStream(MP4, VideoFormat(H264, 720), null, DASH),
            137 to YoutubeStream(MP4, VideoFormat(H264, 1080), null, DASH),
            138 to YoutubeStream(MP4, VideoFormat(H264, 2160), null, DASH),
            160 to YoutubeStream(MP4, VideoFormat(H264, 144), null, DASH),
            212 to YoutubeStream(MP4, VideoFormat(H264, 480), null, DASH),
            298 to YoutubeStream(MP4, VideoFormat(H264, 720, 60), null, DASH),
            299 to YoutubeStream(MP4, VideoFormat(H264, 1080, 60), null, DASH),
            264 to YoutubeStream(MP4, VideoFormat(H264, 1440), null, DASH),
            266 to YoutubeStream(MP4, VideoFormat(H264, 2160), null, DASH),

            // DASH MP4 audio
            139 to YoutubeStream(M4A, null, AudioFormat(AAC, 48), DASH),
            140 to YoutubeStream(M4A, null, AudioFormat(AAC, 128), DASH),
            141 to YoutubeStream(M4A, null, AudioFormat(AAC, 256), DASH),

            // DASH WebM video
            167 to YoutubeStream(WebM, VideoFormat(VP8, 360), null, DASH),
            168 to YoutubeStream(WebM, VideoFormat(VP8, 480), null, DASH),
            169 to YoutubeStream(WebM, VideoFormat(VP8, 720), null, DASH),
            170 to YoutubeStream(WebM, VideoFormat(VP8, 1080), null, DASH),
            218 to YoutubeStream(WebM, VideoFormat(VP8, 480), null, DASH),
            219 to YoutubeStream(WebM, VideoFormat(VP8, 480), null, DASH),
            278 to YoutubeStream(WebM, VideoFormat(VP9, 144), null, DASH),
            242 to YoutubeStream(WebM, VideoFormat(VP9, 240), null, DASH),
            243 to YoutubeStream(WebM, VideoFormat(VP9, 360), null, DASH),
            244 to YoutubeStream(WebM, VideoFormat(VP9, 480), null, DASH),
            245 to YoutubeStream(WebM, VideoFormat(VP9, 480), null, DASH),
            246 to YoutubeStream(WebM, VideoFormat(VP9, 480), null, DASH),
            247 to YoutubeStream(WebM, VideoFormat(VP9, 720), null, DASH),
            248 to YoutubeStream(WebM, VideoFormat(VP9, 1080), null, DASH),
            271 to YoutubeStream(WebM, VideoFormat(VP9, 1440), null, DASH),
            272 to YoutubeStream(WebM, VideoFormat(VP9, 2160), null, DASH),
            302 to YoutubeStream(WebM, VideoFormat(VP9, 720, 60), null, DASH),
            303 to YoutubeStream(WebM, VideoFormat(VP9, 1080, 60), null, DASH),
            308 to YoutubeStream(WebM, VideoFormat(VP9, 1440, 60), null, DASH),
            313 to YoutubeStream(WebM, VideoFormat(VP9, 2160), null, DASH),
            315 to YoutubeStream(WebM, VideoFormat(VP9, 2160, 60), null, DASH),

            // DASH WebM audio
            171 to YoutubeStream(WebM, null, AudioFormat(Vorbis, 128), DASH),
            172 to YoutubeStream(WebM, null, AudioFormat(Vorbis, 256), DASH),
            249 to YoutubeStream(WebM, null, AudioFormat(Opus, 50), DASH),
            250 to YoutubeStream(WebM, null, AudioFormat(Opus, 70), DASH),
            251 to YoutubeStream(WebM, null, AudioFormat(Opus, 160), DASH)
    )

    private val decryptFuncCache = TreeMap<String, (String)->String>()
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

data class YoutubeStream(
        override val container: Container,
        override val videoFormat: VideoFormat?,
        override val audioFormat: AudioFormat?,
        override val protocol: StreamProtocol
): Stream {
    init { assert(videoFormat != null || audioFormat != null)}

    var itag: Int = 0
        internal set

    override var url = ""
        internal set
}


data class YoutubeSearch<T>(
        val itemList: List<T>,
        val echoQuery: String,
        val echoPage: Int
)

data class YoutubeVideoRelatedVideos(
        val videoList: List<YoutubeVideo>,
        val echoYoutubeVideoId: String
)

data class YoutubeVideoStreams(
        val streamList: List<YoutubeStream>,
        val echoYoutubeVideoId: String
)