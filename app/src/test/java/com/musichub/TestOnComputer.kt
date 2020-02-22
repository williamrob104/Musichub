package com.musichub

import com.musichub.concurrent.CallbackExecutor
import com.musichub.concurrent.RequestFuture
import com.musichub.networking.BasicHttpRequests
import com.musichub.networking.HttpUrlClient
import com.musichub.scraper.AppleMusicScraper
import com.musichub.scraper.YoutubeScraper
import com.musichub.scraper.YoutubeVideoStreams
import org.junit.Test
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit


private val basicHttpRequests: BasicHttpRequests = HttpUrlClient()
private val callbackExecutor = CallbackExecutor(Executors.newFixedThreadPool(4), null)
private val youtubeDownloader = YoutubeScraper(basicHttpRequests, callbackExecutor)

private fun <T> RequestFuture<T>.defaultGet(): T = this.get(20, TimeUnit.SECONDS)

class TestOnComputer {

    @Test
    fun main() {
        val idNormal = "KYniUCGPGLs"
        val idCopyrighted = "JGwWNGJdvx8"
        val idAgeRestricted = "M-JdRsHr7Bs"

        val future = RequestFuture<YoutubeVideoStreams>()
        youtubeDownloader.getVideoStreams(idCopyrighted, future)
        val streams = future.defaultGet().streamList

        streams.forEach { println(it); println(it.url) }
    }
}