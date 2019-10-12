package com.freemusic.musicbox.playback

import android.app.Notification
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.Bitmap
import android.media.AudioManager
import android.os.Binder
import android.os.IBinder
import android.support.v4.media.MediaDescriptionCompat
import android.support.v4.media.session.MediaSessionCompat
import android.util.Log
import androidx.core.app.TaskStackBuilder
import androidx.media.session.MediaButtonReceiver
import com.freemusic.musicbox.R
import com.freemusic.musicbox.concurrent.ResponseListener
import com.freemusic.musicbox.singleton.Flags
import com.freemusic.musicbox.singleton.Singleton
import com.freemusic.musicbox.ui.MediaPlayActivity
import com.freemusic.musicbox.util.squareCropTop
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON
import com.google.android.exoplayer2.audio.AudioAttributes
import com.google.android.exoplayer2.ext.mediasession.MediaSessionConnector
import com.google.android.exoplayer2.ext.mediasession.TimelineQueueNavigator
import com.google.android.exoplayer2.source.ConcatenatingMediaSource
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.ui.PlayerNotificationManager
import java.util.concurrent.CopyOnWriteArraySet


class MediaPlaybackService: Service(), MediaPlayer {

    private val PLAYBACK_CHANNEL_ID = "PlaybackServiceChannel"
    private val PLAYBACK_NOTIFICATION_ID = 1927
    private val MEDIA_SESSION_TAG = "MusicBoxMediaSession"

    private lateinit var player: SimpleExoPlayer
    private lateinit var playerNotificationManager: PlayerNotificationManager
    private lateinit var mediaSession: MediaSessionCompat
    private lateinit var mediaSessionConnector: MediaSessionConnector

    private val listeners = CopyOnWriteArraySet<MediaPlayer.EventListener>()
    private val mediaHolderList = ArrayList<MediaHolder>()

    private var cachePlaybackState: MediaPlayer.PlaybackState = MediaPlayer.PlaybackState.IDLE
    private var cachePlayWhenReady: Boolean = false
    private var cacheRepeatMode: MediaPlayer.RepeatMode = MediaPlayer.RepeatMode.OFF
    private var cacheShuffleEnabled: Boolean = false

    private val eventListener = object: Player.EventListener {
        override fun onPlayerError(error: ExoPlaybackException?) {
            val tag = "MusicBox"
            if (error != null) {
                Log.i("MusicBox", "ExoPlayer.onPlayerError")
                when (error.type) {
                    ExoPlaybackException.TYPE_OUT_OF_MEMORY -> Log.i(
                        tag,
                        "out_of_memory",
                        error.outOfMemoryError
                    )
                    ExoPlaybackException.TYPE_REMOTE -> Log.i(tag, "remote", error.cause)
                    ExoPlaybackException.TYPE_RENDERER -> Log.i(
                        tag,
                        "render",
                        error.rendererException
                    )
                    ExoPlaybackException.TYPE_SOURCE -> Log.i(tag, "source", error.sourceException)
                    ExoPlaybackException.TYPE_UNEXPECTED -> Log.i(
                        tag,
                        "unexpected",
                        error.unexpectedException
                    )
                }
            }
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: Int) {
            cachePlayWhenReady = playWhenReady
            cachePlaybackState = mapPlaybackState(playbackState)
            for (listener in listeners)
                listener.onPlayerStateChanged(cachePlayWhenReady, cachePlaybackState)
        }

        override fun onRepeatModeChanged(repeatMode: Int) {
            cacheRepeatMode = mapRepeatMode(repeatMode)
            for (listener in listeners)
                listener.onRepeatModeChanged(cacheRepeatMode)
        }

        override fun onShuffleModeEnabledChanged(shuffleModeEnabled: Boolean) {
            cacheShuffleEnabled = shuffleModeEnabled
            for (listener in listeners)
                listener.onShuffleEnabledChanged(cacheShuffleEnabled)
        }


        override fun onTimelineChanged(timeline: Timeline?, manifest: Any?, reason: Int) {
            for (listener in listeners)
                listener.onMediaListChanged()
            checkOnMediaChanged()
        }

        override fun onPositionDiscontinuity(reason: Int) {
            checkOnMediaChanged()
        }


        private var currentMediaHolder: MediaHolder? = null

        private fun checkOnMediaChanged() {
            val index = player.currentWindowIndex
            val temp = if (index in 0 until mediaHolderList.size) mediaHolderList[index]
                       else null
            if (temp != currentMediaHolder) {
                currentMediaHolder = temp
                for (listener in listeners)
                    listener.onMediaChanged(temp)
            }
        }
    }

    private val noisyReceiver = object: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            player.playWhenReady = false
        }
    }


    override fun onCreate() {
        super.onCreate()
        val context: Context = this

        Singleton.initialize(context)
        MediaHolder.initialize(context)

        val rendersFactory = DefaultRenderersFactory(context).setExtensionRendererMode(EXTENSION_RENDERER_MODE_ON)
        val trackSelector = DefaultTrackSelector()
        player = ExoPlayerFactory.newSimpleInstance(context, rendersFactory, trackSelector)
        player.addListener(eventListener)
        cachePlaybackState = mapPlaybackState(player.playbackState)
        cachePlayWhenReady = player.playWhenReady
        cacheRepeatMode = mapRepeatMode(player.repeatMode)
        cacheShuffleEnabled = player.shuffleModeEnabled

        playerNotificationManager = PlayerNotificationManager.createWithNotificationChannel(
            context, PLAYBACK_CHANNEL_ID, R.string.app_name, R.string.app_name, PLAYBACK_NOTIFICATION_ID,
            object: PlayerNotificationManager.MediaDescriptionAdapter {
                override fun getCurrentContentTitle(player: Player?): String? {
                    return if (player == null) null
                           else mediaHolderList[player.currentWindowIndex].title
                }

                override fun getCurrentContentText(player: Player?): String? {
                    return if (player == null) null
                           else mediaHolderList[player.currentWindowIndex].subtitle
                }

                override fun getCurrentLargeIcon(player: Player?, callback: PlayerNotificationManager.BitmapCallback?): Bitmap? {
                    val imageUrl = if (player == null) null
                        else mediaHolderList[player.currentWindowIndex].image?.sourceByShortSideEquals(MediaHolder.LARGE_IMAGE_SIZE)?.url
                    if (imageUrl != null && callback != null)
                        Singleton.imageRequests.getImage(imageUrl, object: ResponseListener<Bitmap> {
                            override fun onResponse(response: Bitmap) {
                                callback.onBitmap(response.squareCropTop())
                            }

                            override fun onError(error: Exception) {}
                        })
                    return null
                }

                override fun createCurrentContentIntent(player: Player?): PendingIntent? {
                    val intent = Intent(context, MediaPlayActivity::class.java)
                    return TaskStackBuilder.create(this@MediaPlaybackService).run {
                        addNextIntentWithParentStack(intent)
                        getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT)
                    }
                }
            },
            object: PlayerNotificationManager.NotificationListener {
                override fun onNotificationPosted(notificationId: Int, notification: Notification?, ongoing: Boolean) {
                    startForeground(notificationId, notification)
                }

                override fun onNotificationCancelled(notificationId: Int, dismissedByUser: Boolean) {
                    if (dismissedByUser) {
                        player.playWhenReady = false
                        stopSelf()
                        Flags.playbackServiceStarted = false
                    }
                }
            }
        )
        playerNotificationManager.apply {
            setSmallIcon(R.drawable.ic_stat_app)
            setFastForwardIncrementMs(0)
            setRewindIncrementMs(0)
            setPlayer(player)
        }

        val audioAttributes = AudioAttributes.Builder()
            .setContentType(C.CONTENT_TYPE_MUSIC)
            .setUsage(C.USAGE_MEDIA)
            .build()
        player.setAudioAttributes(audioAttributes, true)

        registerReceiver(noisyReceiver, IntentFilter(AudioManager.ACTION_AUDIO_BECOMING_NOISY))

        mediaSession = MediaSessionCompat(context, MEDIA_SESSION_TAG)
        mediaSession.isActive = true
        playerNotificationManager.setMediaSessionToken(mediaSession.sessionToken)

        mediaSessionConnector = MediaSessionConnector(mediaSession)
        mediaSessionConnector.setQueueNavigator(object: TimelineQueueNavigator(mediaSession) {
            override fun getMediaDescription(player: Player?, windowIndex: Int): MediaDescriptionCompat {
                return mediaHolderList[windowIndex].getMediaDescription(mediaSessionConnector::invalidateMediaSessionQueue)
            }
        })
        mediaSessionConnector.setPlayer(player)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Flags.playbackServiceStarted = true
        MediaButtonReceiver.handleIntent(mediaSession, intent)
        return START_STICKY
    }

    override fun onDestroy() {
        Flags.playbackServiceStarted = false
        player.removeListener(eventListener)
        player.release()
        playerNotificationManager.setPlayer(null)
        unregisterReceiver(noisyReceiver)
        mediaSession.release()
        mediaSessionConnector.setPlayer(null)
    }

    override fun onBind(intent: Intent?): IBinder? {
        return PlaybackServiceBinder()
    }

    inner class PlaybackServiceBinder: Binder() {
        fun getService() = this@MediaPlaybackService
    }


    /* implements MediaPlayer */

    override fun addListener(eventListener: MediaPlayer.EventListener) {
        listeners.add(eventListener)
    }

    override fun removeListener(eventListener: MediaPlayer.EventListener) {
        listeners.remove(eventListener)
    }

    override fun setMediaList(mediaHolderList: List<MediaHolder>) {
        val concatMediaSource = ConcatenatingMediaSource()
        for (mediaHolder in mediaHolderList)
            concatMediaSource.addMediaSource(mediaHolder.getMediaSource())

        player.stop()
        this.mediaHolderList.clear()
        this.mediaHolderList.addAll(mediaHolderList)
        player.prepare(concatMediaSource)
    }

    override val mediaCount: Int
        get() = mediaHolderList.size

    override fun seekTo(position: Long) {
        player.seekTo(position)
    }

    override fun changeMedia(index: Int) {
        player.seekToDefaultPosition(index)
    }

    override val currentMedia: MediaHolder?
        get() {
            val index = player.currentWindowIndex
            return if (index in 0 until mediaHolderList.size) mediaHolderList[index]
                   else null
        }

    override val duration: Long?
        get() {
            val dur = player.duration
            return if (dur == C.TIME_UNSET) null else dur
        }

    override val currentPosition: Long
        get() = player.currentPosition

    override val playbackState: MediaPlayer.PlaybackState
        get() = cachePlaybackState

    override var playWhenReady: Boolean
        get() = cachePlayWhenReady
        set(value) { player.playWhenReady = value }

    override var repeatMode: MediaPlayer.RepeatMode
        get() = cacheRepeatMode
        set(value) {
            player.repeatMode = when(value) {
                MediaPlayer.RepeatMode.OFF -> Player.REPEAT_MODE_OFF
                MediaPlayer.RepeatMode.ONE -> Player.REPEAT_MODE_ONE
                MediaPlayer.RepeatMode.ALL -> Player.REPEAT_MODE_ALL
            }
        }

    override var shuffleEnabled: Boolean
        get() = cacheShuffleEnabled
        set(value) { player.shuffleModeEnabled = value }

    override val hasNext: Boolean
        get() = player.hasNext()

    override fun next() {
        player.next()
    }

    override val hasPrev: Boolean
        get() = player.hasPrevious()

    override fun prev() {
        player.previous()
    }

    private fun mapPlaybackState(playbackState: Int): MediaPlayer.PlaybackState {
        return when (playbackState) {
            Player.STATE_BUFFERING -> MediaPlayer.PlaybackState.BUFFERING
            Player.STATE_ENDED -> MediaPlayer.PlaybackState.ENDED
            Player.STATE_IDLE -> MediaPlayer.PlaybackState.IDLE
            Player.STATE_READY -> MediaPlayer.PlaybackState.READY
            else -> MediaPlayer.PlaybackState.IDLE
        }
    }

    private fun mapRepeatMode(repeatMode:Int): MediaPlayer.RepeatMode {
        return when (repeatMode) {
            Player.REPEAT_MODE_OFF -> MediaPlayer.RepeatMode.OFF
            Player.REPEAT_MODE_ONE -> MediaPlayer.RepeatMode.ONE
            Player.REPEAT_MODE_ALL -> MediaPlayer.RepeatMode.ALL
            else -> MediaPlayer.RepeatMode.OFF
        }
    }

}