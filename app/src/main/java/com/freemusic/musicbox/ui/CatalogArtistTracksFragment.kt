package com.freemusic.musicbox.ui

import android.content.Context
import android.graphics.Bitmap
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.freemusic.musicbox.R
import com.freemusic.musicbox.concurrent.Cancellable
import com.freemusic.musicbox.concurrent.ResponseListener
import com.freemusic.musicbox.playback.AppleMusicTrackMediaHolder
import com.freemusic.musicbox.resource.AppleMusicSection
import com.freemusic.musicbox.resource.AppleMusicSong3
import com.freemusic.musicbox.resource.AppleMusicTrack
import com.freemusic.musicbox.singleton.Singleton
import java.text.MessageFormat


class CatalogArtistTracksFragment : Fragment() {

    private var artistName: String? = null
    private var sectionTopSongsUrl: String? = null

    private var requestCancellable: Cancellable? = null

    private lateinit var mainActivityAction: MainActivityAction

    private lateinit var progressBar: ProgressBar
    private lateinit var textViewError: TextView
    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView

    companion object {
        private const val ARG_ARTIST_NAME = "param1"
        private const val ARG_SECTION_TOP_SONGS_URL = "param2"

        @JvmStatic
        fun newInstance(artistName: String, appleMusicSectionTopSongsUrl: String): Fragment {
            return CatalogArtistTracksFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ARTIST_NAME, artistName)
                    putString(ARG_SECTION_TOP_SONGS_URL, appleMusicSectionTopSongsUrl)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            artistName = it.getString(ARG_ARTIST_NAME)
            sectionTopSongsUrl = it.getString(ARG_SECTION_TOP_SONGS_URL)
        }
        mainActivityAction = activity as MainActivityAction
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_catalog_artist_tracks, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressBar = view.findViewById(R.id.fragment_catalog_artist_tracks_pb_loading)
        textViewError = view.findViewById(R.id.fragment_catalog_artist_tracks_tv_error)

        toolbar = view.findViewById(R.id.fragment_catalog_artist_tracks_tb_title)
        toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }
        if (!artistName.isNullOrBlank()) {
            val template = resources.getString(R.string.label_term_belongs)
            val albumsStr = resources.getString(R.string.label_music_tracks)
            toolbar.title = MessageFormat.format(template, artistName, albumsStr)
        }

        recyclerView = view.findViewById(R.id.fragment_catalog_artist_tracks_rv_items)
        recyclerView.layoutManager = LinearLayoutManager(context)

        loadContent()
        textViewError.setOnClickListener { loadContent() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requestCancellable?.cancel()
    }

    private fun loadContent() {
        val sectionTopSongsUrl = this.sectionTopSongsUrl ?: return

        progressBar.visibility = View.VISIBLE
        textViewError.visibility = View.INVISIBLE

        requestCancellable = Singleton.appleMusicScraper.getSectionSongs(
            sectionTopSongsUrl,
            object: ResponseListener<AppleMusicSection<AppleMusicSong3>> {
                override fun onResponse(response: AppleMusicSection<AppleMusicSong3>) {
                    progressBar.visibility = View.INVISIBLE
                    val trackList = response.entityList.toMutableList()
                    if (trackList.isNotEmpty()) {
                        trackList.sortByDescending { it.popularity }
                        recyclerView.adapter = CatalogArtistTracksAdapter(context!!, mainActivityAction, trackList)
                    }
                }
                override fun onError(error: Exception) {
                    progressBar.visibility = View.INVISIBLE
                    textViewError.visibility = View.VISIBLE
                    Log.i("MusicBox", "artistTracks.onError", error)
                }
            }
        )
    }
}


private class CatalogArtistTracksAdapter(
    private val context: Context,
    private val mainActivityAction: MainActivityAction,
    private val trackList: List<AppleMusicTrack>
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val mediaList = trackList.map { AppleMusicTrackMediaHolder(it) }

    override fun getItemCount(): Int {
        return trackList.size
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val v = LayoutInflater.from(context).inflate(R.layout.item_catalog_track, parent, false)
        return CatalogTrack(v)
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        (holder as CatalogTrack).setData(trackList[position])
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        //(holder as CatalogTrack).recycle()
    }


    private val imageSize = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 60f, context.resources.displayMetrics).toInt()

    private inner class CatalogTrack(view: View): RecyclerView.ViewHolder(view) {
        private val imageViewCoverart: ImageView = view.findViewById(R.id.item_catalog_track_iv_coverart)
        private val textViewTitle: TextView = view.findViewById(R.id.item_catalog_track_tv_title)
        private val textViewLabel: TextView = view.findViewById(R.id.item_catalog_track_tv_label)
        private var cancellable: Cancellable? = null

        init {
            view.setOnClickListener {
                mainActivityAction.playMedia(mediaList, this.adapterPosition, null)
            }
        }

        fun setData(track: AppleMusicTrack) {
            imageViewCoverart.setImageBitmap(null)
            textViewTitle.text = track.title
            textViewLabel.text = track.artistName
            val imageUrl = track.coverart?.sourceByShortSideEquals(imageSize)?.url
            if (imageUrl != null) {
                cancellable = Singleton.imageRequests.getImage(imageUrl, object: ResponseListener<Bitmap> {
                    override fun onResponse(response: Bitmap) {
                        imageViewCoverart.setImageBitmap(response)
                    }

                    override fun onError(error: Exception) {}
                })
            }
        }

        fun recycle() {
            imageViewCoverart.setImageBitmap(null)
            cancellable?.cancel()
        }
    }

}
