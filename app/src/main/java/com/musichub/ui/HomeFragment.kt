package com.musichub.ui

import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.ProgressiveMediaSource
import com.google.android.exoplayer2.source.SingleSampleMediaSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSource
import com.google.android.exoplayer2.upstream.DefaultHttpDataSourceFactory
import com.google.android.exoplayer2.util.MimeTypes
import com.musichub.R
import com.musichub.concurrent.ResponseListener
import com.musichub.playback.YoutubeVideoMediaHolder
import com.musichub.scraper.YoutubeVideoStreams
import com.musichub.singleton.Singleton


class HomeFragment : Fragment() {

    private lateinit var mainActivityAction: MainActivityAction

    private lateinit var editText: EditText
    private lateinit var button: Button

    private lateinit var exoplayer: ExoPlayer

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivityAction = activity as MainActivityAction

        val rendersFactory =
            DefaultRenderersFactory(context!!).setExtensionRendererMode(DefaultRenderersFactory.EXTENSION_RENDERER_MODE_ON)
        exoplayer = SimpleExoPlayer.Builder(context!!, rendersFactory).build()
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editText = view.findViewById(R.id.fragment_home_et)
        editText.setText("T4SimnaiktU")

        button = view.findViewById(R.id.fragment_home_btn)
        button.setOnClickListener {
            Singleton.youtubeScraper.getVideoStreams(editText.text.toString(),
                object: ResponseListener<YoutubeVideoStreams> {
                    override fun onResponse(response: YoutubeVideoStreams) {
                        val stream = response.streamList.maxBy { s -> s.audioFormat?.bitRate ?: 0 }!!

                        val dataSourceFactory = DefaultHttpDataSourceFactory("user-agent-string")
                        val mediaSourceFactory = SingleSampleMediaSource.Factory(dataSourceFactory)
                        val mediaSource = mediaSourceFactory.createMediaSource(
                            Uri.parse(stream.url),
                            Format.createAudioContainerFormat(null, null, MimeTypes.AUDIO_WEBM,
                                MimeTypes.AUDIO_OPUS, "Opus", null, stream.audioFormat!!.bitRate,
                                2, 44100, null,
                                C.SELECTION_FLAG_AUTOSELECT, C.ROLE_FLAG_DESCRIBES_MUSIC_AND_SOUND, "en"),
                            C.TIME_UNSET
                        )
                        /*
                        val mediaSourceFactory = ProgressiveMediaSource.Factory(dataSourceFactory)
                        val mediaSource = mediaSourceFactory.createMediaSource(Uri.parse(stream.url))*/
                        exoplayer.prepare(mediaSource)
                        exoplayer.playWhenReady = true
                    }

                    override fun onError(error: Exception) {
                        Toast.makeText(context, "123", Toast.LENGTH_SHORT).show()
                    }
            })
        }
    }


}
