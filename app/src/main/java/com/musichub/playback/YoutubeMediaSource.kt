package com.musichub.playback

import com.google.android.exoplayer2.Timeline
import com.google.android.exoplayer2.source.BaseMediaSource
import com.google.android.exoplayer2.source.MediaPeriod
import com.google.android.exoplayer2.source.MediaSource
import com.google.android.exoplayer2.source.SinglePeriodTimeline
import com.google.android.exoplayer2.upstream.*
import java.util.concurrent.ExecutorService


class YoutubeMediaSource: BaseMediaSource, YoutubeMediaPeriod.Listener {

    class Factory(
        private val dataSourceFactory: DataSource.Factory,
        private val executorService: ExecutorService) {
        private var loadErrorHandlingPolicy: LoadErrorHandlingPolicy = DefaultLoadErrorHandlingPolicy()
        private var tag: Any? = null
        private var isCreateCalled = false

        fun setLoadErrorHandlingPolicy(loadErrorHandlingPolicy: LoadErrorHandlingPolicy): Factory {
            assert(!isCreateCalled)
            this.loadErrorHandlingPolicy = loadErrorHandlingPolicy
            return this
        }

        fun setTag(tag: Any?): Factory {
            assert(!isCreateCalled)
            this.tag = tag
            return this
        }

        fun createMediaSource(youtubeVideoId: String): YoutubeMediaSource {
            isCreateCalled = true
            return YoutubeMediaSource(
                youtubeVideoId, dataSourceFactory, executorService, loadErrorHandlingPolicy, tag)
        }

        fun createMediaSource(youtubeVideoIdCallable: ()->String): YoutubeMediaSource {
            isCreateCalled = true
            return YoutubeMediaSource(
                youtubeVideoIdCallable, dataSourceFactory, executorService, loadErrorHandlingPolicy, tag)

        }
    }


    private var youtubeVideoId: String? = null
    private var youtubeVideoIdCallable: (()->String)? = null
    private var dataSourceFactory: DataSource.Factory
    private var executorService: ExecutorService
    private var loadErrorHandlingPolicy: LoadErrorHandlingPolicy
    private var _tag: Any?

    private var transferListener: TransferListener? = null

    private constructor(
        youtubeVideoId: String,
        dataSourceFactory: DataSource.Factory,
        executorService: ExecutorService,
        loadErrorHandlingPolicy: LoadErrorHandlingPolicy,
        tag: Any?) {
        this.youtubeVideoId = youtubeVideoId
        this.dataSourceFactory = dataSourceFactory
        this.executorService = executorService
        this.loadErrorHandlingPolicy = loadErrorHandlingPolicy
        this._tag = tag
    }

    private constructor(
        youtubeVideoIdCallable: ()->String,
        dataSourceFactory: DataSource.Factory,
        executorService: ExecutorService,
        loadErrorHandlingPolicy: LoadErrorHandlingPolicy,
        tag: Any?) {
        this.youtubeVideoIdCallable = youtubeVideoIdCallable
        this.dataSourceFactory = dataSourceFactory
        this.executorService = executorService
        this.loadErrorHandlingPolicy = loadErrorHandlingPolicy
        this._tag = tag
    }

    override fun getTag(): Any? {
        return _tag
    }

    override fun prepareSourceInternal(mediaTransferListener: TransferListener?) {
        this.transferListener = mediaTransferListener
        refreshSourceInfo(Timeline.EMPTY)
    }

    override fun maybeThrowSourceInfoRefreshError() {
        // Do nothing.
    }

    override fun createPeriod(
        id: MediaSource.MediaPeriodId?,
        allocator: Allocator?,
        startPositionUs: Long
    ): MediaPeriod {
        val dataSource = dataSourceFactory.createDataSource()
        dataSource.addTransferListener(transferListener)

        val youtubeVideoId = this.youtubeVideoId
        val youtubeVideoIdCallable = this.youtubeVideoIdCallable
        return when {
            youtubeVideoId != null -> YoutubeMediaPeriod(
                youtubeVideoId,
                dataSource,
                executorService,
                transferListener,
                loadErrorHandlingPolicy,
                createEventDispatcher(id),
                this
            )
            youtubeVideoIdCallable != null -> YoutubeMediaPeriod(
                youtubeVideoIdCallable,
                dataSource,
                executorService,
                transferListener,
                loadErrorHandlingPolicy,
                createEventDispatcher(id),
                this
            )
            else -> throw Exception("youtubeVideoId or youtubeVideoIdCallable must be initialized")
        }
    }

    override fun releasePeriod(mediaPeriod: MediaPeriod) {
        (mediaPeriod as YoutubeMediaPeriod).release()
    }

    override fun releaseSourceInternal() {
        // Do nothing.
    }


    override fun onSourceInfoRefreshed(durationUs: Long, isLive: Boolean) {
        refreshSourceInfo(SinglePeriodTimeline(durationUs, true, false, isLive))
    }

}