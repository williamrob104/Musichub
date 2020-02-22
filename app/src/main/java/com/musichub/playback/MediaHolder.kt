package com.musichub.playback

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import android.util.Log
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.musichub.concurrent.Cancellable
import com.musichub.concurrent.RequestFuture
import com.musichub.concurrent.ResponseListener
import com.musichub.scraper.*
import com.musichub.singleton.Singleton
import com.musichub.util.pipeResponse
import java.util.concurrent.TimeUnit


sealed class MediaHolder {
    abstract val title: String
    abstract val subtitle: String
    abstract val image: Image?

    abstract fun getMediaSource(): MediaSource


    private var imageBitmap: Bitmap? = null
    private var imageBitmapSet = false

    private val tag = "MusicBox"

    fun getMediaDescription(refresh: () -> Unit): MediaDescriptionCompat {
        val extras = Bundle().apply {
            this.putParcelable(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, imageBitmap)
        }
        val mediaDescription = MediaDescriptionCompat.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setExtras(extras)
            .build()
        if (!imageBitmapSet) {
            val imageUrl = image?.sourceByShortSideEquals(LARGE_IMAGE_SIZE)?.url
            if (imageUrl == null) {
                imageBitmap = null
                imageBitmapSet = true
            } else {
                Singleton.imageRequests.getImage(imageUrl, object : ResponseListener<Bitmap> {
                    override fun onResponse(response: Bitmap) {
                        imageBitmap = response
                        imageBitmapSet = true
                        refresh()
                    }

                    override fun onError(error: Exception) {
                        imageBitmap = null
                        imageBitmapSet = true
                    }
                })
            }
        }
        return mediaDescription
    }

    companion object {
        private var initialized = false

        private lateinit var dataSourceFactory: DataSource.Factory

        lateinit var lookupProgressiveMediaSourceFactory: LookupProgressiveMediaSource.Factory

        fun initialize(context: Context) = synchronized(initialized) {
            if (!initialized) {
                dataSourceFactory = DefaultDataSourceFactory(context, "MusicBox")
                lookupProgressiveMediaSourceFactory =
                    LookupProgressiveMediaSource.Factory(dataSourceFactory)

                initialized = true
            }
        }

        const val LARGE_IMAGE_SIZE = 640
    }
}

class AppleMusicTrackMediaHolder(val appleMusicTrack: AppleMusicTrack) : MediaHolder() {
    override val title: String = appleMusicTrack.title
    override val subtitle: String = appleMusicTrack.artistName
    override val image: Image? = appleMusicTrack.coverart

    override fun getMediaSource(): MediaSource {
        return lookupProgressiveMediaSourceFactory.createMediaSource {
            return@createMediaSource trackMatchingVideoStreams(
                appleMusicTrack,
                pipeResponse(it) { response ->
                    val stream = response.streamList.maxBy { s -> s.audioFormat?.bitRate ?: 0 }
                    if (stream?.audioFormat != null)
                        Uri.parse(stream.url)
                    else
                        throw RuntimeException("no audio stream")
                })
        }
    }
}

class YoutubeVideoMediaHolder(val youtubeVideoId: String) : MediaHolder() {
    override val title: String = ""
    override val subtitle: String = ""
    override val image: Image? = null

    override fun getMediaSource(): MediaSource {
        return lookupProgressiveMediaSourceFactory.createMediaSource {
            return@createMediaSource Singleton.youtubeScraper.getVideoStreams(
                youtubeVideoId,
                pipeResponse(it) { response ->
                    val stream = response.streamList.maxBy { s -> s.audioFormat?.bitRate ?: 0 }
                    if (stream?.audioFormat != null)
                        Uri.parse(stream.url)
                    else
                        throw RuntimeException("no audio stream")
                })
        }
    }
}


private fun trackMatchingVideoStreams(
    track: Track,
    listener: ResponseListener<YoutubeVideoStreams>
): Cancellable {
    return Singleton.callbackExecutor.executeCallback(listener) {
        var cancellable1: Cancellable? = null
        var cancellable2: Cancellable? = null

        val query = "${track.title} ${track.artistName}"
        try {
            val searchFuture = RequestFuture<YoutubeSearch<YoutubeVideo>>()
            cancellable1 = Singleton.youtubeScraper.searchVideos(query, listener = searchFuture)
            val videoList = searchFuture.get(20, TimeUnit.SECONDS).itemList

            val video = videoList.first()
            Log.i("MusicBox", "${video.title} ${video.duration}")

            val streamsFuture = RequestFuture<YoutubeVideoStreams>()
            cancellable2 =
                Singleton.youtubeScraper.getVideoStreams(video.youtubeVideoId, streamsFuture)
            val streams = streamsFuture.get(20, TimeUnit.SECONDS)

            streams
        } catch (e: InterruptedException) {
            cancellable1?.cancel()
            cancellable2?.cancel()
            throw e
        } catch (e: Exception) {
            throw e
        }
    }
}