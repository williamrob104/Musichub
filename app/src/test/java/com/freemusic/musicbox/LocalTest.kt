package com.freemusic.musicbox

import com.freemusic.musicbox.concurrent.CallbackExecutor
import com.freemusic.musicbox.concurrent.RequestFuture
import com.freemusic.musicbox.networking.BasicHttpRequests
import com.freemusic.musicbox.networking.HttpUrlClient
import com.freemusic.musicbox.resource.AppleMusicScraper
import com.freemusic.musicbox.resource.YoutubeScraper
import com.freemusic.musicbox.resource.YoutubeVideoStreams
import org.junit.Test
import java.util.*
import java.util.concurrent.TimeUnit


private val basicHttpRequests: BasicHttpRequests = HttpUrlClient()
private val callbackExecutor = CallbackExecutor(null)
private val appleMusicScraper = AppleMusicScraper(basicHttpRequests, callbackExecutor, Locale.TAIWAN)
private val youtubeScraper = YoutubeScraper(basicHttpRequests, callbackExecutor)

//private val gson = GsonBuilder().setPrettyPrinting().create()

private fun <T> RequestFuture<T>.defaultGet(): T = this.get(20, TimeUnit.SECONDS)

class LocalTest {

    @Test fun main() {
        val id_removed = "aXBL6j0XS1s"
        val id_live = "eMRuyO9NZfY"
        //"liveStreamability"

        val future = RequestFuture<YoutubeVideoStreams>()
        youtubeScraper.getVideoStreams(id_live, future)
        val streams = future.defaultGet().streamList

        streams.forEach { println(it.toString() + "\n") }
    }
}