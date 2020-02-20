package com.musichub

import com.musichub.concurrent.CallbackExecutor
import com.musichub.concurrent.RequestFuture
import com.musichub.networking.BasicHttpRequests
import com.musichub.networking.HttpUrlClient
import com.musichub.resource.AppleMusicScraper
import com.musichub.resource.YoutubeScraper
import com.musichub.resource.YoutubeVideoStreams
import org.junit.Test
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


private val basicHttpRequests: BasicHttpRequests = HttpUrlClient()
private val executorService = Executors.newFixedThreadPool(4)
private val callbackExecutor = CallbackExecutor(executorService, null)
private val appleMusicScraper = AppleMusicScraper(basicHttpRequests, callbackExecutor, Locale.TAIWAN)
private val youtubeScraper = YoutubeScraper(basicHttpRequests, callbackExecutor)

private fun <T> RequestFuture<T>.defaultGet(): T = this.get(20, TimeUnit.SECONDS)

class LocalTest {

    @Test fun main() {
        val video_id = "tlThdr3O5Qo"

        val future = RequestFuture<YoutubeVideoStreams>()
        youtubeScraper.getVideoStreams(video_id, future)
        val streams = future.defaultGet().streamList

        streams.forEach { println(it.toString() + "\n") }
    }
}