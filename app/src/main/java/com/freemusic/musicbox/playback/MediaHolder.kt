package com.freemusic.musicbox.playback

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.os.Bundle
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.MediaMetadataCompat
import com.freemusic.musicbox.concurrent.Cancellable
import com.freemusic.musicbox.concurrent.ResponseListener
import com.freemusic.musicbox.resource.AppleMusicTrack
import com.freemusic.musicbox.resource.Image
import com.freemusic.musicbox.resource.YoutubeVideoStreams
import com.freemusic.musicbox.resource.matchingVideoStreams
import com.freemusic.musicbox.singleton.Singleton
import com.freemusic.musicbox.util.pipeResponse
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import kotlin.random.Random


sealed class MediaHolder {
    abstract val title: String
    abstract val subtitle: String
    abstract val image: Image?

    abstract fun getMediaSource(): MediaSource


    private var imageBitmap: Bitmap? = null
    private var imageBitmapSet = false

    private val tag = "MusicBox"

    fun getMediaDescription(refresh: ()->Unit): MediaDescriptionCompat {
        val extras = Bundle().apply {
            this.putParcelable(MediaMetadataCompat.METADATA_KEY_ALBUM_ART, imageBitmap)
        }
        val mediaDescription =  MediaDescriptionCompat.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setExtras(extras)
            .build()
        if (!imageBitmapSet) {
            val imageUrl = image?.sourceByShortSideEquals(LARGE_IMAGE_SIZE)?.url
            if (imageUrl == null) {
                imageBitmap = null
                imageBitmapSet = true
            }
            else {
                Singleton.imageRequests.getImage(imageUrl, object: ResponseListener<Bitmap> {
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
                lookupProgressiveMediaSourceFactory = LookupProgressiveMediaSource.Factory(dataSourceFactory)

                initialized = true
            }
        }

        const val LARGE_IMAGE_SIZE = 640
    }
}

class AppleMusicTrackMediaHolder(val appleMusicTrack: AppleMusicTrack): MediaHolder() {
    override val title: String = appleMusicTrack.title
    override val subtitle: String = appleMusicTrack.artistName
    override val image: Image? = appleMusicTrack.coverart

    override fun getMediaSource(): MediaSource {
        return lookupProgressiveMediaSourceFactory.createMediaSource {
            return@createMediaSource Singleton.youtubeScraper.matchingVideoStreams(appleMusicTrack, pipeResponse(it) { response ->
                val stream = response.streamList.maxBy { s -> s.audioFormat?.bitRate ?: 0 }
                if (stream?.audioFormat != null)
                    Uri.parse(stream.url)
                else
                    throw RuntimeException("no audio stream")
            })
        }
    }
}

class YoutubeVideoMediaHolder(val youtubeVideoId: String): MediaHolder() {
    override val title: String = ""
    override val subtitle: String = ""
    override val image: Image? = null

    override fun getMediaSource(): MediaSource {
        return lookupProgressiveMediaSourceFactory.createMediaSource {
            return@createMediaSource Singleton.youtubeScraper.getVideoStreams(youtubeVideoId, pipeResponse(it) { response ->
                val stream = response.streamList.maxBy { s -> s.audioFormat?.bitRate ?: 0 }
                if (stream?.audioFormat != null)
                    Uri.parse(stream.url)
                else
                    throw RuntimeException("no audio stream")
            })
        }
    }
}