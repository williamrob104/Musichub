package com.freemusic.musicbox.resource

import android.util.Log
import com.freemusic.musicbox.concurrent.Cancellable
import com.freemusic.musicbox.concurrent.RequestFuture
import com.freemusic.musicbox.concurrent.ResponseListener


fun YoutubeScraper.matchingVideoStreams(track: Track, listener: ResponseListener<YoutubeVideoStreams>): Cancellable {
    return callbackExecutor.executeCallback(listener) {
        var cancellable1: Cancellable? = null
        var cancellable2: Cancellable? = null

        val trackTitle = "${track.title}"
        val artistName = "${track.artistName}"
        val albumTitle = track.albumTitle?.let { "${it.removePostDash()}" }
        val query = "$artistName $trackTitle"
        try {
            val searchFuture = RequestFuture<YoutubeSearch<YoutubeVideo>>()
            cancellable1 = this.searchVideos(query, listener=searchFuture)
            val videoList = searchFuture.defaultGet().itemList

            val video = videoList.first()
            Log.i("MusicBox", "${video.title} ${video.duration}")

            val streamsFuture = RequestFuture<YoutubeVideoStreams>()
            cancellable2 = this.getVideoStreams(video.youtubeVideoId, streamsFuture)
            val streams = streamsFuture.defaultGet()

            streams
        }
        catch (e: InterruptedException) {
            cancellable1?.cancel()
            cancellable2?.cancel()
            throw e
        }
        catch (e: Exception) {
            throw e
        }
    }
}

private fun String.removePostDash(): String {
    val idx = this.lastIndexOf(" - ")
    return if (idx == -1) this
    else this.substring(0, idx)
}