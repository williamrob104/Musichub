package com.musichub.ui

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.util.Log
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.palette.graphics.Palette
import com.musichub.R
import com.musichub.concurrent.Cancellable
import com.musichub.concurrent.ResponseListener
import com.musichub.playback.AppleMusicTrackMediaHolder
import com.musichub.resource.AppleMusicAlbumBrowse
import com.musichub.resource.AppleMusicTrack
import com.musichub.singleton.Singleton
import com.musichub.ui.widget.RoundedTouchFadeTextView
import com.musichub.util.SpecialCharacters
import com.musichub.util.formatDate


class CatalogAlbumFragment : Fragment() {
    private var albumViewUrl: String? = null
    private var cancellable: Cancellable? = null

    private lateinit var mainActivityAction: MainActivityAction

    private lateinit var progressBar: ProgressBar
    private lateinit var textViewError: TextView
    private lateinit var toolbar: Toolbar
    private lateinit var constraintLayout: ConstraintLayout
    private lateinit var imageViewCoverart: ImageView
    private lateinit var textViewTitle: TextView
    private lateinit var textViewLabel: TextView
    private lateinit var textViewPlay: RoundedTouchFadeTextView
    private lateinit var linearLayout: LinearLayout

    companion object {
        private const val ARG_ALBUM_URL = "param1"

        @JvmStatic
        fun newInstance(albumViewUrl: String) =
            CatalogAlbumFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ALBUM_URL, albumViewUrl)
                }
            }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            albumViewUrl = it.getString(ARG_ALBUM_URL)
        }
        mainActivityAction = activity as MainActivityAction
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_catalog_album, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        progressBar = view.findViewById(R.id.fragment_catalog_album_pb_loading)
        textViewError = view.findViewById(R.id.fragment_catalog_album_tv_error)
        toolbar = view.findViewById(R.id.fragment_catalog_album_tb_title)
        constraintLayout = view.findViewById(R.id.fragment_catalog_album_cl_header)
        imageViewCoverart = view.findViewById(R.id.fragment_catalog_album_iv_coverart)
        textViewTitle = view.findViewById(R.id.fragment_catalog_album_tv_title)
        textViewLabel = view.findViewById(R.id.fragment_catalog_album_tv_label)
        textViewPlay = view.findViewById(R.id.fragment_catalog_album_tv_play)
        linearLayout = view.findViewById(R.id.fragment_catalog_album_ll_content)


        toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }

        loadContent()
        textViewError.setOnClickListener { loadContent() }


    }

    override fun onDestroyView() {
        super.onDestroyView()
        cancellable?.cancel()
    }

    private fun loadContent() {
        val albumViewUrl = this.albumViewUrl ?: return
        progressBar.visibility = View.VISIBLE
        textViewError.visibility = View.INVISIBLE
        textViewPlay.visibility = View.INVISIBLE
        cancellable = Singleton.appleMusicScraper.browseAlbum(albumViewUrl,
            object: ResponseListener<AppleMusicAlbumBrowse> {
                override fun onResponse(response: AppleMusicAlbumBrowse) {
                    progressBar.visibility = View.INVISIBLE
                    toolbar.title = response.title
                    setContent(response)
                }

                override fun onError(error: Exception) {
                    progressBar.visibility = View.INVISIBLE
                    textViewError.visibility = View.VISIBLE
                    Log.i("MusicBox", "browseAlbum.onError", error)
                }
            }
        )
    }

    private fun setContent(album: AppleMusicAlbumBrowse) {
        val imageUrl = album.coverart?.sourceByShortSideEquals(300)?.url
        if (imageUrl != null)
            Singleton.imageRequests.getImage(imageUrl, object: ResponseListener<Bitmap> {
                override fun onResponse(response: Bitmap) {
                    imageViewCoverart.setImageBitmap(response)

                    val defaultColor = ContextCompat.getColor(context!!, R.color.contentBgColorLight)
                    val color = Palette.from(response).generate().getMutedColor(defaultColor)
                    val color2 = Color.argb(0, Color.red(color), Color.green(color), Color.blue(color))
                    val bg = GradientDrawable(GradientDrawable.Orientation.TOP_BOTTOM, intArrayOf(color, color2))
                    constraintLayout.background = bg
                }

                override fun onError(error: Exception) {}
            })
        textViewTitle.text = album.title
        textViewLabel.text = "${album.releaseDate.year} ${SpecialCharacters.smblkcircle} ${album.artistName}"

        val trackList = album.songList
        if (trackList.isEmpty())
            return

        textViewPlay.setOnClickListener {
            val mediaList = trackList.map { AppleMusicTrackMediaHolder(it) }
            mainActivityAction.playMedia(mediaList, null, CustomMode.Shuffle)
        }
        textViewPlay.visibility = View.VISIBLE


        val mediaList = trackList.map { AppleMusicTrackMediaHolder(it) }
        for (i in trackList.indices) {
            val trackView = makeTrackView(trackList[i])
            trackView.setOnClickListener {
                mainActivityAction.playMedia(mediaList, i, null)
            }
            linearLayout.addView(trackView)
        }

        setInfo(album)
    }

    private var imageSize: Int? = null
    private fun makeTrackView(track: AppleMusicTrack): View {
        val view = LayoutInflater.from(context).inflate(R.layout.item_catalog_track, linearLayout, false)
        val imageViewCoverart: ImageView = view.findViewById(R.id.item_catalog_track_iv_coverart)
        val textViewTitle: TextView = view.findViewById(R.id.item_catalog_track_tv_title)
        val textViewLabel: TextView = view.findViewById(R.id.item_catalog_track_tv_label)

        textViewTitle.text = track.title
        textViewLabel.text = track.artistName

        if (imageSize == null)
            imageSize = TypedValue.applyDimension(
                TypedValue.COMPLEX_UNIT_DIP, 60f, context!!.resources.displayMetrics).toInt()
        val imageUrl = track.coverart?.sourceByShortSideEquals(imageSize!!)?.url
        if (imageUrl != null)
            Singleton.imageRequests.getImage(imageUrl, object: ResponseListener<Bitmap> {
                override fun onResponse(response: Bitmap) {
                    imageViewCoverart.setImageBitmap(response)
                }

                override fun onError(error: Exception) {}
            })
        return view
    }

    private fun setInfo(album: AppleMusicAlbumBrowse) {
        val margin = TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, 15f, context!!.resources.displayMetrics).toInt()

        val textViewDate = TextView(context).apply {
            text = formatDate(resources, album.releaseDate)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.content_medium_text_size))
            setTextColor(ContextCompat.getColor(context, R.color.contentColorPrimary))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(margin, 40, margin, 0)
            }
        }
        linearLayout.addView(textViewDate)

        val note = album.noteStandard ?: album.noteShort ?: return
        val textViewNote = TextView(context).apply {
            text = note
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.content_small_text_size))
            setTextColor(ContextCompat.getColor(context, R.color.contentColorPrimary))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                setMargins(margin, 30, margin, 0)
            }
        }
        linearLayout.addView(textViewNote)
    }
}
