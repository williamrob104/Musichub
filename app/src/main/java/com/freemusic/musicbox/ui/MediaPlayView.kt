package com.freemusic.musicbox.ui

import android.content.Context
import android.graphics.Bitmap
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.util.AttributeSet
import android.util.TypedValue
import android.view.View
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import com.freemusic.musicbox.R
import com.freemusic.musicbox.concurrent.ResponseListener
import com.freemusic.musicbox.playback.AppleMusicTrackMediaHolder
import com.freemusic.musicbox.playback.MediaHolder
import com.freemusic.musicbox.playback.MediaPlayer
import com.freemusic.musicbox.playback.YoutubeVideoMediaHolder
import com.freemusic.musicbox.singleton.Singleton
import com.freemusic.musicbox.ui.widget.TouchFadeImageView
import com.freemusic.musicbox.util.SpecialCharacters
import com.freemusic.musicbox.util.setColor
import com.freemusic.musicbox.util.squareCropTop


class MediaPlayView: ConstraintLayout {

    constructor(context: Context): super(context)

    constructor(context: Context, attrs: AttributeSet): super(context, attrs)

    constructor(context: Context, attrs: AttributeSet, defStyleAttr: Int): super(context, attrs, defStyleAttr)


    private var mediaPlayer: MediaPlayer? = null

    fun setPlayer(mediaPlayer: MediaPlayer?) {
        this.mediaPlayer?.removeListener(eventListener)
        this.mediaPlayer = mediaPlayer
        if (mediaPlayer != null) {
            eventListener.onMediaChanged(mediaPlayer.currentMedia)
            eventListener.onPlayerStateChanged(mediaPlayer.playWhenReady, mediaPlayer.playbackState)
        }
        this.mediaPlayer?.addListener(eventListener)
    }


    private var imageViewCoverart: ImageView
    private var textViewTitle: TextView
    private var imageViewPlay: TouchFadeImageView

    init {
        val rootView = View.inflate(context, R.layout.view_media_play, this)
        imageViewCoverart = rootView.findViewById(R.id.view_media_play_iv_coverart)
        textViewTitle = rootView.findViewById(R.id.view_media_play_tv_title)
        imageViewPlay = rootView.findViewById(R.id.view_media_play_iv_play)

        textViewTitle.isSelected = true

        imageViewPlay.setOnClickListener {
            val player = mediaPlayer ?: return@setOnClickListener
            if (!player.playWhenReady || player.playbackState == MediaPlayer.PlaybackState.ENDED) {
                player.playWhenReady = true
                if (player.playbackState == MediaPlayer.PlaybackState.ENDED)
                    player.changeMedia(0)
            }
            else {
                player.playWhenReady = false
            }
        }
    }

    private val eventListener = object: MediaPlayer.EventListener {
        override fun onMediaChanged(mediaHolder: MediaHolder?) {
            when (mediaHolder) {
                is AppleMusicTrackMediaHolder -> initMedia(mediaHolder)
                is YoutubeVideoMediaHolder -> initMedia(mediaHolder)
            }
        }

        override fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: MediaPlayer.PlaybackState) {
            if (!playWhenReady || playbackState == MediaPlayer.PlaybackState.ENDED)
                imageViewPlay.setImageResource(R.drawable.ic_media_play)
            else
                imageViewPlay.setImageResource(R.drawable.ic_media_pause)
        }
    }

    private val imageSize = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 55f, context.resources.displayMetrics).toInt()

    private fun initMedia(appleMusicTrackMediaHolder: AppleMusicTrackMediaHolder) {
        val track = appleMusicTrackMediaHolder.appleMusicTrack

        val colorPrimary = ContextCompat.getColor(context, R.color.contentColorPrimary)
        val colorSecondary = ContextCompat.getColor(context, R.color.contentColorSecondary)
        val text = SpannableStringBuilder()
            .append(SpannableString(track.title).setColor(colorPrimary))
            .append(SpannableString(" ${SpecialCharacters.smblkcircle} ${track.artistName}").setColor(colorSecondary))
        textViewTitle.setText(text, TextView.BufferType.SPANNABLE)

        val imageUrl = track.coverart?.sourceByShortSideEquals(imageSize)?.url
        if (imageUrl != null)
            Singleton.imageRequests.getImage(imageUrl, object: ResponseListener<Bitmap> {
                override fun onResponse(response: Bitmap) {
                    imageViewCoverart.setImageBitmap(response.squareCropTop())
                }

                override fun onError(error: Exception) {}
            })
    }

    private fun initMedia(mediaHolder: YoutubeVideoMediaHolder) {
        imageViewCoverart.setImageBitmap(null)
        textViewTitle.text = ""
    }
}
