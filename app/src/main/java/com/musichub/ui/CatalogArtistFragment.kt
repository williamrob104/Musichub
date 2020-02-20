package com.musichub.ui

import android.graphics.Bitmap
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableString
import android.text.SpannableStringBuilder
import android.text.style.StyleSpan
import android.util.Log
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.musichub.R
import com.musichub.concurrent.Cancellable
import com.musichub.concurrent.ResponseListener
import com.musichub.playback.AppleMusicTrackMediaHolder
import com.musichub.resource.AppleMusicAlbum2
import com.musichub.resource.AppleMusicArtistBrowse
import com.musichub.singleton.Singleton
import com.musichub.ui.widget.RoundedTouchFadeTextView
import com.musichub.util.SpecialCharacters
import com.musichub.util.messageFormat
import com.musichub.util.setColor
import com.google.android.material.appbar.CollapsingToolbarLayout


class CatalogArtistFragment : Fragment() {

    private var artistViewUrl: String? = null

    private var requestCancellable: Cancellable? = null

    private lateinit var mainActivityAction: MainActivityAction

    private lateinit var progressBar: ProgressBar
    private lateinit var textViewError: TextView
    private lateinit var collapsingToolbar: CollapsingToolbarLayout
    private lateinit var toolbar: Toolbar
    private lateinit var imageViewAvatar: ImageView
    private lateinit var textViewPlay: TextView
    private lateinit var linearLayout: LinearLayout
    private lateinit var textViewTopSongs: TextView

    companion object {
        private const val ARG_ARTIST_VIEW_URL = "param1"

        @JvmStatic
        fun newInstance(appleMusicArtistViewUrl: String): Fragment {
            return CatalogArtistFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ARTIST_VIEW_URL, appleMusicArtistViewUrl)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            artistViewUrl = it.getString(ARG_ARTIST_VIEW_URL)
        }
        mainActivityAction = activity as MainActivityAction
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_catalog_artist, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        progressBar = view.findViewById(R.id.fragment_catalog_artist_pb_loading)
        textViewError = view.findViewById(R.id.fragment_catalog_artist_tv_error)
        collapsingToolbar = view.findViewById(R.id.fragment_catalog_artist_ctb)
        toolbar = view.findViewById(R.id.fragment_catalog_artist_tb)
        imageViewAvatar = view.findViewById(R.id.fragment_catalog_artist_iv_avatar)
        textViewPlay = view.findViewById(R.id.fragment_catalog_album_tv_play)
        linearLayout = view.findViewById(R.id.fragment_catalog_artist_ll_content)
        textViewTopSongs = view.findViewById(R.id.fragment_catalog_artist_tv_topsongs)

        toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }

        loadContent()
        textViewError.setOnClickListener { loadContent() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        requestCancellable?.cancel()
    }

    private fun loadContent() {
        val artistViewUrl = this.artistViewUrl ?: return

        progressBar.visibility = View.VISIBLE
        textViewError.visibility = View.INVISIBLE

        textViewPlay.bringToFront()
        textViewPlay.visibility = View.INVISIBLE

        requestCancellable = Singleton.appleMusicScraper.browseArtist(
            artistViewUrl,
            object: ResponseListener<AppleMusicArtistBrowse> {
                override fun onResponse(response: AppleMusicArtistBrowse) {
                    progressBar.visibility = View.INVISIBLE
                    setContent(response)
                }

                override fun onError(error: Exception) {
                    progressBar.visibility = View.INVISIBLE
                    textViewError.visibility = View.VISIBLE
                    Log.i("MusicBox", "browseArtist.onError", error)
                }
            }
        )
    }

    private fun setContent(artist: AppleMusicArtistBrowse) {
        val imageUrl = artist.avatar?.sourceByShortSideEquals(640)?.url
        if (imageUrl != null)
            Singleton.imageRequests.getImage(imageUrl, object: ResponseListener<Bitmap> {
                override fun onResponse(response: Bitmap) {
                    imageViewAvatar.setImageBitmap(response)
                }

                override fun onError(error: Exception) {}
            })

        collapsingToolbar.title = artist.name
        setTopSongs(artist)
        setAlbums(artist)
    }

    private fun setTopSongs(artist: AppleMusicArtistBrowse) {
        val songs = artist.sectionTopSongs?.entityList
        if (songs.isNullOrEmpty()) return

        val maxLines = 4
        val colorPrimary = ContextCompat.getColor(context!!, R.color.contentColorPrimary)
        val colorSecondary = ContextCompat.getColor(context!!, R.color.contentColorSecondary)

        val postfix = SpannableString(resources.getString(R.string.label_term_andmore)).setColor(colorPrimary)
        val text = SpannableStringBuilder(postfix)

        var i = 0
        for (song in songs) {
            i += 1
            text
                .insert(text.length - postfix.length,
                    SpannableString(song.artistName).setColor(colorPrimary))
                .insert(text.length - postfix.length,
                    SpannableString(" ${song.title} ${SpecialCharacters.smblkcircle} ").setColor(colorSecondary))

            textViewTopSongs.setText(text, TextView.BufferType.SPANNABLE)
            if (textViewTopSongs.lineCount >= maxLines)
                break
        }
        if (i < songs.size) {
            val start = text.length - postfix.length
            text
                .insert(text.length - postfix.length,
                    SpannableString(songs[i].artistName).setColor(colorPrimary))
                .insert(text.length - postfix.length,
                    SpannableString(" ${songs[i].title} ${SpecialCharacters.smblkcircle} ").setColor(colorSecondary))

            textViewTopSongs.setText(text, TextView.BufferType.SPANNABLE)

            if (textViewTopSongs.lineCount > maxLines)
                text.replace(start, text.length - postfix.length, "")
        }
        text.setSpan(StyleSpan(Typeface.BOLD), text.length - postfix.length, text.length, SpannableString.SPAN_INCLUSIVE_EXCLUSIVE)
        textViewTopSongs.setText(text, TextView.BufferType.SPANNABLE)

        textViewPlay.setOnClickListener {
            mainActivityAction.playMedia(songs.map { AppleMusicTrackMediaHolder(it) }, null, CustomMode.Shuffle)
        }
        textViewPlay.visibility = View.VISIBLE

        textViewTopSongs.setOnClickListener {
            mainActivityAction.changeFragment(
                CatalogArtistTracksFragment.newInstance(artist.name, artist.sectionTopSongs.sectionViewAllUrl) )
        }
    }

    private fun setAlbums(artist: AppleMusicArtistBrowse) {
        val maxCount = 5

        val albums = LinkedHashMap<AppleMusicAlbum2, Int>()
        artist.sectionFullAlbums?.entityList?.asSequence()?.map { it to 0 }?.toMap(albums)
        artist.sectionSingleAlbums?.entityList?.asSequence()?.map { it to 1 }?.toMap(albums)
        artist.sectionLiveAlbums?.entityList?.asSequence()?.map { it to 2 }?.toMap(albums)
        artist.sectionCompilationAlbums?.entityList?.asSequence()?.map { it to 3 }?.toMap(albums)
        if (albums.isEmpty()) return
        val latestAlbum = artist.sectionLatestRelease?.entityList?.firstOrNull()

        val textViewTitle = TextView(context).apply {
            text = resources.getString(R.string.label_music_albums)
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.content_medium_text_size))
            setTextColor(ContextCompat.getColor(context, R.color.contentColorPrimary))
            gravity = Gravity.CENTER_HORIZONTAL
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                this.setMargins(0, resources.getDimension(R.dimen.content_large_text_size).toInt(), 0, 0)
            }
        }
        linearLayout.addView(textViewTitle)

        if (latestAlbum != null) {
            val type = albums[latestAlbum]
            if (type != null) albums.remove(latestAlbum)
            addAlbum(latestAlbum, type, true)
        }
        for ((album, type) in albums.asSequence().take(if (latestAlbum == null) maxCount else maxCount-1)) {
            addAlbum(album, type, false)
        }

        val textViewAllAlbums = RoundedTouchFadeTextView(context!!).apply {
            text = resources.getString(R.string.label_term_viewall).messageFormat(
                resources.getString(R.string.label_music_albums)
            )
            setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.content_small_text_size))
            setTextColor(ContextCompat.getColor(context, R.color.contentColorPrimary))
            gravity = Gravity.CENTER
            setRoundedRadius(30f)
            setRoundedStroke(ContextCompat.getColor(context, R.color.contentColorSecondary), 2)
            sizeFactor = 0.95f
            alphaFactor = 0.6f
            duration = 50L
            setPadding(40, 0, 40, 0)
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, 60).apply {
                gravity = Gravity.CENTER_HORIZONTAL
            }
        }
        textViewAllAlbums.setOnClickListener {
            mainActivityAction.changeFragment(CatalogArtistAlbumsFragment.newInstance(
                artist.name,
                artist.sectionFullAlbums?.sectionViewAllUrl,
                artist.sectionSingleAlbums?.sectionViewAllUrl,
                artist.sectionLiveAlbums?.sectionViewAllUrl,
                artist.sectionCompilationAlbums?.sectionViewAllUrl
            ))
        }
        linearLayout.addView(textViewAllAlbums)
    }

    private fun addAlbum(album: AppleMusicAlbum2, type: Int?, isLatest: Boolean) {
        val view = layoutInflater.inflate(R.layout.item_catalog_album, linearLayout, false)
        val imageView: ImageView = view.findViewById(R.id.item_catalog_album_iv_coverart)
        val textViewTitle: TextView = view.findViewById(R.id.item_catalog_album_tv_title)
        val textViewLabel: TextView = view.findViewById(R.id.item_catalog_album_tv_label)

        val imageUrl = album.coverart?.sourceByShortSideEquals(300)?.url
        if (imageUrl != null)
            Singleton.imageRequests.getImage(imageUrl, object: ResponseListener<Bitmap> {
                override fun onResponse(response: Bitmap) {
                    imageView.setImageBitmap(response)
                }

                override fun onError(error: Exception) {}
            })
        textViewTitle.text = album.title

        val release = if (isLatest) resources.getString(R.string.label_music_latest_release)
                      else album.releaseDate.year.toString()
        val typeStrId = when(type) {
            0 -> R.string.label_music_album
            1 -> R.string.label_music_album_single
            2 -> R.string.label_music_album_live
            3 -> R.string.label_music_album_compilation
            else -> null
        }
        val postfix = if (typeStrId == null) ""
        else " ${SpecialCharacters.smblkcircle} ${resources.getString(typeStrId)}"
        textViewLabel.text = release + postfix

        view.setOnClickListener {
            mainActivityAction.changeFragment(CatalogAlbumFragment.newInstance(album.albumViewUrl))
        }

        linearLayout.addView(view)
    }
}
