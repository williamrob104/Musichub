package com.musichub

import com.musichub.concurrent.CallbackExecutor
import com.musichub.concurrent.RequestFuture
import com.musichub.networking.BasicHttpRequests
import com.musichub.networking.HttpUrlClient
import com.musichub.scraper.YoutubeScraper
import com.musichub.scraper.YoutubeVideoStreams
import org.junit.Test
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


private val basicHttpRequests: BasicHttpRequests = HttpUrlClient()
private val callbackExecutor = CallbackExecutor(Executors.newFixedThreadPool(4), null)
private val youtubeScraper = YoutubeScraper(basicHttpRequests, callbackExecutor)

class TestOnComputer {

    @Test
    fun main() {
        val idLive = "RaIJ767Bj_M"

        val future = RequestFuture<YoutubeVideoStreams>()
        youtubeScraper.getVideoStreams(idLive, future)

        val streams = future.get().streamList

        streams.forEach { println(it.url) }
    }
}