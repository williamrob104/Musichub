package com.musichub.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import androidx.fragment.app.Fragment
import com.musichub.R
import com.musichub.playback.YoutubeVideoMediaHolder


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
