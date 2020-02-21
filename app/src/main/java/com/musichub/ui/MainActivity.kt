package com.musichub.ui

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.IBinder
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.musichub.R
import com.musichub.playback.MediaHolder
import com.musichub.playback.MediaPlaybackService
import com.musichub.playback.MediaPlayer
import com.musichub.playback.YoutubeVideoMediaHolder
import com.musichub.scraper.YoutubeScraper
import com.musichub.singleton.Flags
import com.musichub.singleton.Singleton


private const val ARG_NAV_STACK = "param1"

class MainActivity : AppCompatActivity(), MainActivityAction {

    init {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true)
    }

    private lateinit var mediaPlayer: MediaPlayer
    private var playbackServiceBound = false

    private lateinit var mediaPlayView: MediaPlayView

    private val fragmentManager = supportFragmentManager
    private lateinit var navView: BottomNavigationView
    private lateinit var navStack: ArrayList<Int>

    private val onNavigationItemSelectedListener =
        BottomNavigationView.OnNavigationItemSelectedListener { item ->
            val currentId = navView.selectedItemId
            when (item.itemId) {
                R.id.activity_main_bnv_navigate_home -> {
                    if (currentId != R.id.activity_main_bnv_navigate_home)
                        changeFragment(HomeFragment())
                    return@OnNavigationItemSelectedListener true
                }
                R.id.activity_main_bnv_navigate_search -> {
                    if (currentId != R.id.activity_main_bnv_navigate_search)
                        changeFragment(SearchFragment())
                    return@OnNavigationItemSelectedListener true
                }
                R.id.activity_main_bnv_navigate_library -> {
                    if (currentId != R.id.activity_main_bnv_navigate_library)
                        changeFragment(LibraryFragment())
                    return@OnNavigationItemSelectedListener true
                }
            }
            false
        }

    private val onBackStackChangedListener = FragmentManager.OnBackStackChangedListener {
        val currentFragment = fragmentManager.findFragmentById(R.id.activity_main_fl_fragment)
        if (currentFragment != null)
            when (currentFragment) {
                is HomeFragment -> navView.menu.getItem(0).isChecked = true
                is SearchFragment -> navView.menu.getItem(1).isChecked = true
                is LibraryFragment -> navView.menu.getItem(2).isChecked = true
                else -> {
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        Singleton.initialize(applicationContext)

        mediaPlayView = findViewById(R.id.activity_main_media_play_view)
        mediaPlayView.visibility = View.GONE
        mediaPlayView.setOnClickListener {
            val intent = Intent(this@MainActivity, MediaPlayActivity::class.java)
            startActivity(intent)
        }

        navView = findViewById(R.id.activity_main_bnv_navigate)
        navView.setOnNavigationItemSelectedListener(onNavigationItemSelectedListener)

        navStack = savedInstanceState?.getIntegerArrayList(ARG_NAV_STACK) ?: ArrayList()
        if (navStack.isEmpty()) {
            fragmentManager.beginTransaction().replace(
                R.id.activity_main_fl_fragment,
                HomeFragment()
            ).commit()
            navStack.add(0)
        }

        fragmentManager.addOnBackStackChangedListener(onBackStackChangedListener)

        handelIntent(intent)
    }

    override fun onStart() {
        super.onStart()
        if (Flags.playbackServiceStarted) {
            if (playbackServiceBound) {
                mediaPlayView.setPlayer(mediaPlayer)
                mediaPlayView.visibility = View.VISIBLE
            } else {
                val intent = Intent(this, MediaPlaybackService::class.java)
                bindService(intent, object : ServiceConnection {
                    override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                        mediaPlayer =
                            (binder as MediaPlaybackService.PlaybackServiceBinder).getService()
                        playbackServiceBound = true
                        mediaPlayView.setPlayer(mediaPlayer)
                        mediaPlayView.visibility = View.VISIBLE
                    }

                    override fun onServiceDisconnected(name: ComponentName?) {
                        mediaPlayView.setPlayer(null)
                        mediaPlayView.visibility = View.GONE
                    }
                }, Context.BIND_AUTO_CREATE)
            }
        }
    }

    override fun onNewIntent(intent: Intent?) {
        super.onNewIntent(intent)
        handelIntent(intent)
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putIntegerArrayList(ARG_NAV_STACK, navStack)
    }

    override fun onDestroy() {
        super.onDestroy()
        fragmentManager.removeOnBackStackChangedListener(onBackStackChangedListener)
        mediaPlayView.setPlayer(null)
    }

    override fun onBackPressed() {
        val currentFragment = fragmentManager.findFragmentById(R.id.activity_main_fl_fragment)
        if (currentFragment is FragmentActions && currentFragment.onBackPressed())
            return

        if (navStack.size <= 1)
            finish()
        else {
            fragmentManager.popBackStack()
            navStack.removeAt(navStack.size - 1)
            navView.menu.getItem(navStack.last()).isChecked = true
        }
    }

    private fun handelIntent(intent: Intent?) {
        when (intent?.action) {
            Intent.ACTION_SEND -> {
                val text = intent.getStringExtra(Intent.EXTRA_TEXT) ?: return
                val videoId = YoutubeScraper.parseYoutubeUrl(text)
                if (videoId == null) {
                    Toast.makeText(
                        this,
                        resources.getText(R.string.msg_unsupported_intent),
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                } else
                    playMedia(YoutubeVideoMediaHolder(videoId), null)
            }
        }
    }


    /* implements MainActivityAction */

    override fun changeFragment(fragment: Fragment) {
        fragmentManager.beginTransaction().replace(R.id.activity_main_fl_fragment, fragment)
            .addToBackStack(null).commit()
        val navPos = when (fragment) {
            is HomeFragment -> 0
            is SearchFragment -> 1
            is LibraryFragment -> 2
            else -> navStack.last()
        }
        navStack.add(navPos)
    }

    private fun runPlaybackService(playbackServiceRun: (MediaPlayer) -> Unit) {
        val intent = Intent(this, MediaPlaybackService::class.java)

        if (!Flags.playbackServiceStarted)
            startService(intent)

        if (!playbackServiceBound) {
            bindService(intent, object : ServiceConnection {
                override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
                    mediaPlayer =
                        (binder as MediaPlaybackService.PlaybackServiceBinder).getService()
                    playbackServiceBound = true
                    playbackServiceRun(mediaPlayer)
                }

                override fun onServiceDisconnected(name: ComponentName?) {
                    playbackServiceBound = false
                }
            }, Context.BIND_AUTO_CREATE)
        } else {
            playbackServiceRun(mediaPlayer)
        }
    }

    override fun playMedia(media: MediaHolder, mode: CustomMode?) =
        runPlaybackService { mediaPlayer ->
            val intent = Intent(this, MediaPlayActivity::class.java)
            startActivity(intent)

            mediaPlayer.setMediaList(listOf(media))
            if (mode != null)
                mediaPlayer.customMode = mode
            mediaPlayer.playWhenReady = true
        }

    override fun playMedia(mediaList: List<MediaHolder>, startIndex: Int?, mode: CustomMode?) =
        runPlaybackService { mediaPlayer ->
            val intent = Intent(this, MediaPlayActivity::class.java)
            startActivity(intent)

            mediaPlayer.setMediaList(mediaList)
            if (startIndex != null)
                mediaPlayer.changeMedia(startIndex)
            if (mode != null)
                mediaPlayer.customMode = mode
            mediaPlayer.playWhenReady = true
        }
}


interface MainActivityAction {
    fun changeFragment(fragment: Fragment)

    fun playMedia(media: MediaHolder, mode: CustomMode?)

    fun playMedia(mediaList: List<MediaHolder>, startIndex: Int?, mode: CustomMode?)
}


interface FragmentActions {
    fun onBackPressed(): Boolean {
        return false
    }
}
