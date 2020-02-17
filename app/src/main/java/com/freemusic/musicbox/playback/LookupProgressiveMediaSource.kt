package com.freemusic.musicbox.playback

import android.net.Uri
import com.freemusic.musicbox.concurrent.Cancellable
import com.freemusic.musicbox.concurrent.ResponseListener
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.drm.ExoMediaCrypto
import com.google.android.exoplayer2.extractor.DefaultExtractorsFactory
import com.google.android.exoplayer2.extractor.ExtractorsFactory
import com.google.android.exoplayer2.source.*
import com.google.android.exoplayer2.upstream.*
import com.google.android.exoplayer2.util.Assertions
import java.io.IOException


class LookupProgressiveMediaSource private constructor(
    private val lookupCallback: (ResponseListener<Uri>)->Cancellable,
    private val dataSourceFactory: DataSource.Factory,
    private val extractorsFactory: ExtractorsFactory,
    private val drmSessionManager: DrmSessionManager<ExoMediaCrypto>,
    private val loadableLoadErrorHandlingPolicy: LoadErrorHandlingPolicy,
    private val customCacheKey: String?,
    private val continueLoadingCheckIntervalBytes: Int,
    private val _tag: Any?
): BaseMediaSource(), ProgressiveMediaPeriod.Listener {

    class Factory(private val dataSourceFactory: DataSource.Factory,
                  private val extractorsFactory: ExtractorsFactory=DefaultExtractorsFactory()) {
        private var customCacheKey: String? = null
        private var tag: Any? = null
        private var drmSessionManager: DrmSessionManager<ExoMediaCrypto> = DrmSessionManager.getDummyDrmSessionManager()
        private var loadErrorHandlingPolicy: LoadErrorHandlingPolicy = DefaultLoadErrorHandlingPolicy()
        private var continueLoadingCheckIntervalBytes: Int = DEFAULT_LOADING_CHECK_INTERVAL_BYTES
        private var isCreateCalled = false

        fun setCustomCacheKey(customCacheKey: String): Factory {
            Assertions.checkState(!isCreateCalled)
            this.customCacheKey = customCacheKey
            return this
        }

        fun setTag(tag: Any): Factory {
            Assertions.checkState(!isCreateCalled)
            this.tag = tag
            return this
        }

        fun setLoadErrorHandlingPolicy(loadErrorHandlingPolicy: LoadErrorHandlingPolicy): Factory {
            Assertions.checkState(!isCreateCalled)
            this.loadErrorHandlingPolicy = loadErrorHandlingPolicy
            return this
        }

        fun setContinueLoadingCheckIntervalBytes(continueLoadingCheckIntervalBytes: Int): Factory {
            Assertions.checkState(!isCreateCalled)
            this.continueLoadingCheckIntervalBytes = continueLoadingCheckIntervalBytes
            return this
        }

        fun setDrmSessionManager(drmSessionManager: DrmSessionManager<ExoMediaCrypto>): Factory {
            Assertions.checkState(!isCreateCalled)
            this.drmSessionManager = drmSessionManager
            return this
        }

        fun createMediaSource(lookupCallback: (ResponseListener<Uri>)->Cancellable): LookupProgressiveMediaSource {
            isCreateCalled = true
            return LookupProgressiveMediaSource(
                lookupCallback,
                dataSourceFactory,
                extractorsFactory,
                drmSessionManager,
                loadErrorHandlingPolicy,
                customCacheKey,
                continueLoadingCheckIntervalBytes,
                tag
            )
        }
    }

    companion object {
        const val DEFAULT_LOADING_CHECK_INTERVAL_BYTES = 1024 * 1024
    }

    private var timelineDurationUs: Long = C.TIME_UNSET
    private var timelineIsSeekable: Boolean = false
    private var transferListener: TransferListener? = null

    override fun getTag(): Any? = _tag

    override fun prepareSourceInternal(mediaTransferListener: TransferListener?) {
        transferListener = mediaTransferListener
        notifySourceInfoRefreshed(timelineDurationUs, timelineIsSeekable)
    }

    @Throws(IOException::class)
    override fun maybeThrowSourceInfoRefreshError() {
        // Do nothing.
    }

    override fun createPeriod(id: MediaSource.MediaPeriodId, allocator: Allocator, startPositionUs: Long): MediaPeriod {
        val dataSource = dataSourceFactory.createDataSource()
        if (transferListener != null) {
            dataSource.addTransferListener(transferListener)
        }

        return LookupProgressiveMediaPeriod(
            lookupCallback,
            dataSource,
            extractorsFactory.createExtractors(),
            drmSessionManager,
            loadableLoadErrorHandlingPolicy,
            createEventDispatcher(id),
            this,
            allocator,
            customCacheKey,
            continueLoadingCheckIntervalBytes
        )
    }

    override fun releasePeriod(mediaPeriod: MediaPeriod) {
        (mediaPeriod as LookupProgressiveMediaPeriod).release()
    }

    public override fun releaseSourceInternal() {
        // Do nothing.
    }

    override fun onSourceInfoRefreshed(durationUs: Long, isSeekable: Boolean, isLive: Boolean) {
        // If we already have the duration from a previous source info refresh, use it.
        val duration = if (durationUs == C.TIME_UNSET) timelineDurationUs else durationUs
        if (timelineDurationUs == duration && timelineIsSeekable == isSeekable) {
            // Suppress no-op source info changes.
            return
        }
        notifySourceInfoRefreshed(duration, isSeekable)
    }


    private fun notifySourceInfoRefreshed(durationUs: Long, isSeekable: Boolean) {
        timelineDurationUs = durationUs
        timelineIsSeekable = isSeekable
        refreshSourceInfo(
            SinglePeriodTimeline(
                timelineDurationUs, timelineIsSeekable, false, false
            )
        )
    }
}