package com.freemusic.musicbox.ui

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import com.freemusic.musicbox.R
import com.freemusic.musicbox.concurrent.ResponseListener
import com.freemusic.musicbox.playback.YoutubeVideoMediaHolder
import com.freemusic.musicbox.resource.Stream
import com.freemusic.musicbox.resource.StreamProtocol
import com.freemusic.musicbox.resource.YoutubeStream
import com.freemusic.musicbox.resource.YoutubeVideoStreams
import com.freemusic.musicbox.singleton.Singleton
import com.google.android.exoplayer2.*
import com.google.android.exoplayer2.source.dash.DashChunkSource
import com.google.android.exoplayer2.source.dash.DashMediaSource
import com.google.android.exoplayer2.source.dash.manifest.*
import com.google.android.exoplayer2.upstream.DefaultDataSourceFactory
import com.google.android.material.snackbar.Snackbar


class HomeFragment : Fragment() {

    private lateinit var mainActivityAction: MainActivityAction

    private lateinit var editText: EditText
    private lateinit var button: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivityAction = activity as MainActivityAction
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_home, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        editText = view.findViewById(R.id.fragment_home_et)

        button = view.findViewById(R.id.fragment_home_btn)
        button.setOnClickListener {
            mainActivityAction.playMedia(YoutubeVideoMediaHolder(editText.text.toString()), null)
        }
    }


}
