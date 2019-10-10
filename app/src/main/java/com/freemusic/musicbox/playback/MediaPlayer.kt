package com.freemusic.musicbox.playback


interface MediaPlayer {
    enum class PlaybackState { BUFFERING, ENDED, IDLE, READY }
    enum class RepeatMode { OFF, ONE, ALL }

    interface EventListener {
        fun onMediaListChanged() {}

        fun onMediaChanged(mediaHolder: MediaHolder?) {}

        fun onPlayerStateChanged(playWhenReady: Boolean, playbackState: PlaybackState) {}

        fun onRepeatModeChanged(repeatMode: RepeatMode) {}

        fun onShuffleEnabledChanged(shuffleEnabled: Boolean) {}
    }

    fun addListener(eventListener: EventListener)

    fun removeListener(eventListener: EventListener)

    fun setMediaList(mediaHolderList: List<MediaHolder>)

    val mediaCount: Int

    fun seekTo(position: Long)

    fun changeMedia(index: Int)

    val currentMedia: MediaHolder?

    val duration: Long?

    val currentPosition: Long

    val playbackState: PlaybackState

    var playWhenReady: Boolean

    var repeatMode: RepeatMode

    var shuffleEnabled: Boolean

    val hasNext: Boolean

    fun next()

    val hasPrev: Boolean

    fun prev()
}