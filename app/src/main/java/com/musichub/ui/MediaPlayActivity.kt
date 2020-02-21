package com.musichub.ui

import android.annotation.SuppressLint
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.PorterDuff
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.view.View
import android.widget.ImageView
import android.widget.SeekBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import com.musichub.R
import com.musichub.concurrent.Cancellable
import com.musichub.concurrent.ResponseListener
import com.musichub.playback.*
import com.musichub.singleton.Singleton
import com.musichub.ui.widget.TouchFadeImageView
import com.musichub.util.formatTime
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledExecutorService
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit

class MediaPlayActivity : AppCompatActivity() {

    private lateinit var player: MediaPlayer
    private lateinit var handler: Handler

    private lateinit var toolbar: Toolbar
    private lateinit var imageViewCoverart: ImageView
    private lateinit var textViewTitle: TextView
    private lateinit var textViewSubtitle: TextView
    private lateinit var seekbarTimeline: SeekBar
    private lateinit var textViewPosition: TextView
    private lateinit var textViewDuration: TextView
    private lateinit var imageViewPlay: TouchFadeImageView
    private lateinit var imageViewPrev: TouchFadeImageView
    private lateinit var imageViewNext: TouchFadeImageView
    private lateinit var imageViewMode: TouchFadeImageView
    private lateinit var imageViewFavorite: TouchFadeImageView

    private var imageLoadCancellable: Cancellable? = null

    private val serviceConnection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, service: IBinder?) {
            val binder = service as MediaPlaybackService.PlaybackServiceBinder
            player = binder.getService()
            eventListener.onMediaListChanged()
            eventListener.onMediaChanged(player.currentMedia)
            eventListener.onPlayerStateChanged(player.playWhenReady, player.playbackState)
            eventListener.onRepeatModeChanged(player.repeatMode)
            eventListener.onShuffleEnabledChanged(player.shuffleEnabled)
            player.addListener(eventListener)
            initView()
            timelineUpdateFuture = timelineUpdateExecutor.scheduleAtFixedRate(
                timelineUpdateTask,
                0,
                500,
                TimeUnit.MILLISECONDS
            )
        }

        override fun onServiceDisconnected(name: ComponentName?) {
            player.removeListener(eventListener)
            timelineUpdateFuture?.cancel(true)
        }
    }

    private var timelineDurationIsSet = false
    private var timelineIsSeeking = false

    private val timelineUpdateExecutor: ScheduledExecutorService =
        Executors.newSingleThreadScheduledExecutor()
    private var timelineUpdateFuture: ScheduledFuture<*>? = null
    private val timelineUpdateTask = {
        if (!timelineDurationIsSet)
            handler.post {
                val durationMs = player.duration
                if (durationMs != null) {
                    val durationSec = (durationMs * 1e-3).toInt()
                    seekbarTimeline.max = durationMs.toInt()
                    textViewDuration.text = formatTime(durationSec)
                    textViewDuration.visibility = View.VISIBLE
                    textViewPosition.visibility = View.VISIBLE
                    timelineDurationIsSet = true
                }
            }
        if (timelineDurationIsSet && !timelineIsSeeking)
            handler.post {
                seekbarTimeline.progress = player.currentPosition.toInt()
            }
    }

    private val eventListener = object : MediaPlayer.EventListener {

        override fun onMediaChanged(mediaHolder: MediaHolder?) {
            displayPrevNext()
            timelineDurationIsSet = false
            seekbarTimeline.progress = 0
            textViewDuration.text = resources.getString(R.string.label_media_loading)
            textViewPosition.visibility = View.INVISIBLE
            when (mediaHolder) {
                is AppleMusicTrackMediaHolder -> initMedia(mediaHolder)
                is YoutubeVideoMediaHolder -> initMedia(mediaHolder)
            }
        }

        override fun onMediaListChanged() {
            displayPrevNext()
            player.updateCustomMode()
        }

        private fun displayPrevNext() {
            imageViewPrev.setTouchable(player.hasPrev)
            imageViewNext.setTouchable(player.hasNext)
        }

        override fun onPlayerStateChanged(
            playWhenReady: Boolean,
            playbackState: MediaPlayer.PlaybackState
        ) {
            if (!playWhenReady || playbackState == MediaPlayer.PlaybackState.ENDED)
                imageViewPlay.setImageResource(R.drawable.ic_media_circle_play)
            else
                imageViewPlay.setImageResource(R.drawable.ic_media_circle_pause)
        }

        override fun onRepeatModeChanged(repeatMode: MediaPlayer.RepeatMode) {
            displayCustomMode()
        }

        override fun onShuffleEnabledChanged(shuffleEnabled: Boolean) {
            displayCustomMode()
        }

        private var currentCustomMode: CustomMode? = null

        private fun displayCustomMode() {
            val temp = player.customMode
            if (temp != currentCustomMode) {
                currentCustomMode = temp
                val resId = when (temp) {
                    CustomMode.Linear -> R.drawable.ic_media_no_shuffle
                    CustomMode.Shuffle -> R.drawable.ic_media_shuffle
                    CustomMode.RepeatOne -> R.drawable.ic_media_repeat_one
                }
                imageViewMode.setImageResource(resId)
            }
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    private fun initView() {
        imageViewPlay.setOnClickListener {
            if (!player.playWhenReady || player.playbackState == MediaPlayer.PlaybackState.ENDED) {
                player.playWhenReady = true
                if (player.playbackState == MediaPlayer.PlaybackState.ENDED)
                    player.changeMedia(0)
            } else {
                player.playWhenReady = false
            }
        }

        seekbarTimeline.setOnTouchListener { _, _ -> !timelineDurationIsSet }

        seekbarTimeline.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                textViewPosition.text = formatTime((progress * 1e-3).toInt())
            }

            override fun onStartTrackingTouch(seekBar: SeekBar?) {
                if (seekBar != null)
                    timelineIsSeeking = true
            }

            override fun onStopTrackingTouch(seekBar: SeekBar?) {
                if (seekBar != null) {
                    player.seekTo(seekBar.progress.toLong())
                    timelineIsSeeking = false
                }
            }
        })

        imageViewPrev.setOnClickListener {
            player.prev()
        }

        imageViewNext.setOnClickListener {
            player.next()
        }

        imageViewFavorite.setOnClickListener(object : View.OnClickListener {
            private var checked = false
            override fun onClick(v: View?) {
                if (v !is ImageView) return
                if (checked) {
                    v.setImageResource(R.drawable.ic_favorite_outlined)
                    checked = false
                } else {
                    v.setImageResource(R.drawable.ic_favorite_filled)
                    checked = true
                }
            }
        })

        imageViewMode.setOnClickListener(object : View.OnClickListener {
            private val toast = Toast.makeText(this@MediaPlayActivity, "", Toast.LENGTH_SHORT)
            override fun onClick(v: View?) {
                if (v !is TouchFadeImageView) return
                val nextMode = when (player.customMode) {
                    CustomMode.Linear -> CustomMode.Shuffle
                    CustomMode.Shuffle -> CustomMode.RepeatOne
                    CustomMode.RepeatOne -> CustomMode.Linear
                }
                player.customMode = nextMode
                val strId = when (nextMode) {
                    CustomMode.Linear -> R.string.label_media_mode_linear
                    CustomMode.Shuffle -> R.string.label_media_mode_shuffle
                    CustomMode.RepeatOne -> R.string.label_media_mode_repeat_one
                }
                toast.setText(strId)
                toast.show()
            }
        })
    }

    private fun initMedia(mediaHolder: AppleMusicTrackMediaHolder) {
        imageLoadCancellable?.cancel()
        imageViewCoverart.setImageBitmap(null)

        val track = mediaHolder.appleMusicTrack
        textViewTitle.text = track.title
        textViewSubtitle.text = track.artistName

        val imageUrl = track.coverart?.sourceByShortSideEquals(MediaHolder.LARGE_IMAGE_SIZE)?.url
        if (imageUrl != null)
            imageLoadCancellable =
                Singleton.imageRequests.getImage(imageUrl, object : ResponseListener<Bitmap> {
                    override fun onResponse(response: Bitmap) {
                        imageViewCoverart.setImageBitmap(response)
                    }

                    override fun onError(error: Exception) {}
                })
    }

    private fun initMedia(mediaHolder: YoutubeVideoMediaHolder) {
        imageLoadCancellable?.cancel()
        imageViewCoverart.setImageBitmap(null)
        textViewTitle.text = ""
        textViewSubtitle.text = ""
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_media_play)

        handler = Handler(mainLooper)

        toolbar = findViewById(R.id.activity_play_tb_header)
        imageViewCoverart = findViewById(R.id.activity_play_iv_coverart)
        textViewTitle = findViewById(R.id.activity_play_tv_title)
        textViewSubtitle = findViewById(R.id.activity_play_tv_subtitle)
        seekbarTimeline = findViewById(R.id.activity_play_sb_timeline)
        textViewPosition = findViewById(R.id.activity_play_tv_position)
        textViewDuration = findViewById(R.id.activity_play_tv_duration)
        imageViewPlay = findViewById(R.id.activity_play_iv_play)
        imageViewPrev = findViewById(R.id.activity_play_iv_prev)
        imageViewNext = findViewById(R.id.activity_play_iv_next)
        imageViewMode = findViewById(R.id.activity_play_iv_mode)
        imageViewFavorite = findViewById(R.id.activity_play_iv_favorite)

        toolbar.title = null
        toolbar.setNavigationOnClickListener {
            this.onBackPressed()
        }

        val seekBarColor = ContextCompat.getColor(this, R.color.contentColorPrimary)
        seekbarTimeline.progressDrawable.setColorFilter(seekBarColor, PorterDuff.Mode.MULTIPLY)
        seekbarTimeline.thumb.setColorFilter(seekBarColor, PorterDuff.Mode.SRC_ATOP)
    }

    override fun onStart() {
        super.onStart()
        val intent = Intent(this, MediaPlaybackService::class.java)
        bindService(intent, serviceConnection, Context.BIND_AUTO_CREATE)
    }

    override fun onStop() {
        super.onStop()
        unbindService(serviceConnection)
        player.removeListener(eventListener)
        timelineUpdateFuture?.cancel(true)
    }
}


enum class CustomMode { Linear, Shuffle, RepeatOne }

var MediaPlayer.customMode: CustomMode
    get() {
        return if (this.repeatMode == MediaPlayer.RepeatMode.ONE)
            CustomMode.RepeatOne
        else {
            if (this.shuffleEnabled)
                CustomMode.Shuffle
            else
                CustomMode.Linear
        }
    }
    set(value) {
        when (value) {
            CustomMode.RepeatOne -> this.repeatMode = MediaPlayer.RepeatMode.ONE
            CustomMode.Shuffle -> {
                this.repeatMode =
                    if (this.mediaCount == 1) MediaPlayer.RepeatMode.OFF else MediaPlayer.RepeatMode.ALL
                this.shuffleEnabled = true
            }
            CustomMode.Linear -> {
                this.repeatMode =
                    if (this.mediaCount == 1) MediaPlayer.RepeatMode.OFF else MediaPlayer.RepeatMode.ALL
                this.shuffleEnabled = false
            }
        }
    }

// remember to call this method if media count switches between single or not
fun MediaPlayer.updateCustomMode() {
    this.customMode = this.customMode
}
