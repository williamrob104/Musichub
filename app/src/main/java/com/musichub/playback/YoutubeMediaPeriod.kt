package com.musichub.playback

import com.google.android.exoplayer2.SeekParameters
import com.google.android.exoplayer2.source.MediaPeriod
import com.google.android.exoplayer2.source.MediaSourceEventListener.EventDispatcher
import com.google.android.exoplayer2.source.SampleStream
import com.google.android.exoplayer2.source.TrackGroupArray
import com.google.android.exoplayer2.trackselection.TrackSelection
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.LoadErrorHandlingPolicy
import com.google.android.exoplayer2.upstream.TransferListener
import com.musichub.concurrent.RequestFuture
import com.musichub.scraper.YoutubeVideoStreams
import com.musichub.singleton.Singleton
import java.util.concurrent.ExecutorService
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit


internal class YoutubeMediaPeriod: MediaPeriod {

    interface Listener {
        fun onSourceInfoRefreshed(durationUs: Long, isLive: Boolean)
    }


    private var youtubeVideoId: String? = null
    private var youtubeVideoIdCallable: (()->String)? = null
    private var dataSource: DataSource
    private var executorService: ExecutorService
    private var transferListener: TransferListener?
    private var loadErrorHandlingPolicy: LoadErrorHandlingPolicy
    private var eventDispatcher: EventDispatcher
    private var listener: Listener

    private var executorServiceTaskFuture: Future<*>? = null
    private var videoStreams: YoutubeVideoStreams? = null
    private var prepareError: Exception? = null

    constructor(
        youtubeVideoId: String,
        dataSource: DataSource,
        executorService: ExecutorService,
        transferListener: TransferListener?,
        loadErrorHandlingPolicy: LoadErrorHandlingPolicy,
        eventDispatcher: EventDispatcher,
        listener: Listener) {
        this.youtubeVideoId = youtubeVideoId
        this.dataSource = dataSource
        this.executorService = executorService
        this.transferListener = transferListener
        this.loadErrorHandlingPolicy = loadErrorHandlingPolicy
        this.eventDispatcher = eventDispatcher
        this.listener = listener
    }

    constructor(
        youtubeVideoIdCallable: ()->String,
        dataSource: DataSource,
        executorService: ExecutorService,
        transferListener: TransferListener?,
        loadErrorHandlingPolicy: LoadErrorHandlingPolicy,
        eventDispatcher: EventDispatcher,
        listener: Listener) {
        this.youtubeVideoIdCallable = youtubeVideoIdCallable
        this.dataSource = dataSource
        this.executorService = executorService
        this.transferListener = transferListener
        this.loadErrorHandlingPolicy = loadErrorHandlingPolicy
        this.eventDispatcher = eventDispatcher
        this.listener = listener
    }

    override fun prepare(callback: MediaPeriod.Callback, positionUs: Long) {
        executorServiceTaskFuture = executorService.submit {
            try {
                val youtubeVideoId = this.youtubeVideoId
                val youtubeVideoIdCallable = this.youtubeVideoIdCallable
                val videoId: String = when {
                    youtubeVideoId != null -> youtubeVideoId
                    youtubeVideoIdCallable != null -> youtubeVideoIdCallable()
                    else -> throw Exception("youtubeVideoId or youtubeVideoIdCallable must be initialized")
                }

                val future = RequestFuture<YoutubeVideoStreams>()
                Singleton.youtubeScraper.getVideoStreams(videoId, future)
                videoStreams = future.get(20, TimeUnit.SECONDS)

                callback.onPrepared(this)
            }
            catch (e: Exception) {
                prepareError = e
            }
        }
    }

    fun release() {
        executorServiceTaskFuture?.cancel(true)
        eventDispatcher.mediaPeriodReleased()
    }

    override fun maybeThrowPrepareError() {
        val prepareError = this.prepareError
        if (prepareError != null)
            throw prepareError
    }

    override fun getTrackGroups(): TrackGroupArray {
        TODO("not implemented")
    }

    override fun selectTracks(
        selections: Array<out TrackSelection>?,
        mayRetainStreamFlags: BooleanArray?,
        streams: Array<out SampleStream>?,
        streamResetFlags: BooleanArray?,
        positionUs: Long
    ): Long {
        TODO("not implemented")
    }

    override fun discardBuffer(positionUs: Long, toKeyframe: Boolean) {
        TODO("not implemented")
    }

    override fun readDiscontinuity(): Long {
        TODO("not implemented")
    }

    override fun seekToUs(positionUs: Long): Long {
        TODO("not implemented")
    }

    override fun getAdjustedSeekPositionUs(
        positionUs: Long,
        seekParameters: SeekParameters?
    ): Long {
        TODO("not implemented")
    }

    override fun getBufferedPositionUs(): Long {
        TODO("not implemented")
    }

    override fun getNextLoadPositionUs(): Long {
        TODO("not implemented")
    }

    override fun continueLoading(positionUs: Long): Boolean {
        TODO("not implemented")
    }

    override fun isLoading(): Boolean {
        TODO("not implemented")
    }

    override fun reevaluateBuffer(positionUs: Long) {
        TODO("not implemented")
    }

}


object YoutubeDownloader {

}