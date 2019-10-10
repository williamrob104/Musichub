/*package com.freemusic.musicbox.resource

import com.freemusic.musicbox.concurrent.RequestFuture
import com.freemusic.musicbox.networking.globalClient
import java.util.concurrent.TimeUnit

import kotlin.test.Test


val auddApi = AuddApi(globalClient)

class AuddApiTest {
    @Test fun testRecognizeMusic() {
        val musicFile = this::class.java.classLoader.getResourceAsStream("example.mp3").readAllBytes()
        val future = RequestFuture<AuddRecognition>()
        auddApi.recognizeMusic(musicFile, future)

        val rec = future.get(10, TimeUnit.SECONDS)
        println(rec)
    }
}*/