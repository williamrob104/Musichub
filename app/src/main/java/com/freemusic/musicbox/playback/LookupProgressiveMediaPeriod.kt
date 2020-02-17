package com.freemusic.musicbox.playback

import android.net.Uri
import android.util.Log
import com.freemusic.musicbox.concurrent.Cancellable
import com.freemusic.musicbox.concurrent.ResponseListener
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.SeekParameters
import com.google.android.exoplayer2.drm.DrmSessionManager
import com.google.android.exoplayer2.drm.ExoMediaCrypto
import com.google.android.exoplayer2.extractor.Extractor
import com.google.android.exoplayer2.source.*
import com.google.android.exoplayer2.trackselection.TrackSelection
import com.google.android.exoplayer2.upstream.Allocator
import com.google.android.exoplayer2.upstream.DataSource
import com.google.android.exoplayer2.upstream.LoadErrorHandlingPolicy


class LookupProgressiveMediaPeriod(
    private val lookupCallback: (ResponseListener<Uri>)->Cancellable,
    private val dataSource: DataSource,
    private val extractors: Array<Extractor>,
    private val drmSessionManager: DrmSessionManager<ExoMediaCrypto>,
    private val loadErrorHandlingPolicy: LoadErrorHandlingPolicy,
    private val eventDispatcher: MediaSourceEventListener.EventDispatcher,
    private val listener: ProgressiveMediaPeriod.Listener,
    private val allocator: Allocator,
    private val customCacheKey: String?,
    private val continueLoadingCheckIntervalBytes: Int
): MediaPeriod {

    private var progressiveMediaPeriod: ProgressiveMediaPeriod? = null
    private var cancellable: Cancellable? = null

    override fun prepare(callback: MediaPeriod.Callback?, positionUs: Long) {
        cancellable = lookupCallback(object: ResponseListener<Uri> {
            override fun onResponse(response: Uri) {
                progressiveMediaPeriod = ProgressiveMediaPeriod(
                    response,
                    dataSource,
                    extractors,
                    drmSessionManager,
                    loadErrorHandlingPolicy,
                    eventDispatcher,
                    listener,
                    allocator,
                    customCacheKey,
                    continueLoadingCheckIntervalBytes
                )
                progressiveMediaPeriod!!.prepare(object: MediaPeriod.Callback {
                    override fun onPrepared(mediaPeriod: MediaPeriod?) {
                        callback?.onPrepared(this@LookupProgressiveMediaPeriod)
                    }

                    override fun onContinueLoadingRequested(source: MediaPeriod?) {
                        callback?.onContinueLoadingRequested(this@LookupProgressiveMediaPeriod)
                    }
                }, positionUs)
            }

            override fun onError(error: Exception) {
                Log.i("MusicBox", "get uri error", error)
                progressiveMediaPeriod = null
                listener.onSourceInfoRefreshed(0, false, false)
                callback?.onPrepared(this@LookupProgressiveMediaPeriod)
            }
        })
    }

    override fun maybeThrowPrepareError() {
        progressiveMediaPeriod?.maybeThrowPrepareError()
    }

    override fun getTrackGroups(): TrackGroupArray {
        return progressiveMediaPeriod?.trackGroups ?: TrackGroupArray.EMPTY
    }

    override fun selectTracks(
        selections: Array<out TrackSelection>?,
        mayRetainStreamFlags: BooleanArray?,
        streams: Array<out SampleStream>?,
        streamResetFlags: BooleanArray?,
        positionUs: Long
    ): Long {
        return progressiveMediaPeriod?.selectTracks(
            selections, mayRetainStreamFlags, streams, streamResetFlags, positionUs) ?: 0L
    }

    override fun discardBuffer(positionUs: Long, toKeyframe: Boolean) {
        progressiveMediaPeriod?.discardBuffer(positionUs, toKeyframe)
    }

    override fun readDiscontinuity(): Long {
        return progressiveMediaPeriod?.readDiscontinuity() ?: C.TIME_UNSET
    }

    override fun seekToUs(positionUs: Long): Long {
        return progressiveMediaPeriod?.seekToUs(positionUs) ?: 0L
    }

    override fun getAdjustedSeekPositionUs(positionUs: Long, seekParameters: SeekParameters?): Long {
        return progressiveMediaPeriod?.getAdjustedSeekPositionUs(positionUs, seekParameters) ?: 0L
    }

    override fun getBufferedPositionUs(): Long {
        return progressiveMediaPeriod?.bufferedPositionUs ?: C.TIME_END_OF_SOURCE
    }

    override fun getNextLoadPositionUs(): Long {
        return progressiveMediaPeriod?.nextLoadPositionUs ?: C.TIME_END_OF_SOURCE
    }

    override fun continueLoading(positionUs: Long): Boolean {
        return progressiveMediaPeriod?.continueLoading(positionUs) ?: false
    }

    override fun isLoading(): Boolean {
        return progressiveMediaPeriod?.isLoading ?: false
    }

    override fun reevaluateBuffer(positionUs: Long) {
        progressiveMediaPeriod?.reevaluateBuffer(positionUs)
    }

    fun release() {
        cancellable?.cancel()
        progressiveMediaPeriod?.release()
    }
}