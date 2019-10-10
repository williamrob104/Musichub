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
import androidx.appcompat.widget.Toolbar
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.freemusic.musicbox.R
import com.freemusic.musicbox.concurrent.Cancellable
import com.freemusic.musicbox.concurrent.ResponseListener
import com.freemusic.musicbox.resource.AppleMusicAlbum3
import com.freemusic.musicbox.resource.AppleMusicSection
import com.freemusic.musicbox.singleton.Singleton
import java.text.MessageFormat
import java.util.concurrent.ConcurrentLinkedQueue


class CatalogArtistAlbumsFragment : Fragment() {
    private var artistName: String? = null
    private val sectionsUrls = ArrayList<Pair<String, Int>>()
    private var sectionIdx = 0
    private var updating = false

    private lateinit var progressBar: ProgressBar
    private lateinit var textViewError: TextView
    private lateinit var toolbar: Toolbar
    private lateinit var recyclerView: RecyclerView

    private lateinit var mainActivityAction: MainActivityAction

    private val requestCancellable = ConcurrentLinkedQueue<Cancellable>()

    companion object {
        private const val ARG_ARTIST_NAME = "param1"
        private const val ARG_SECTION_ALBUM_URL = "param2"
        private const val ARG_SECTION_SINGLE_ALBUM_URL = "param3"
        private const val ARG_SECTION_LIVE_ALBUM_URL = "param4"
        private const val ARG_SECTION_COMPILATION_ALBUM_URL = "param5"

        @JvmStatic
        fun newInstance(artistName: String,
                        sectionAlbumUrl: String?,
                        sectionSingleAlbumUrl: String?,
                        sectionLiveAlbumUrl: String?,
                        sectionCompilationAlbumUrl: String?): Fragment {
            return CatalogArtistAlbumsFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_ARTIST_NAME, artistName)
                    putString(ARG_SECTION_ALBUM_URL, sectionAlbumUrl)
                    putString(ARG_SECTION_SINGLE_ALBUM_URL, sectionSingleAlbumUrl)
                    putString(ARG_SECTION_LIVE_ALBUM_URL, sectionLiveAlbumUrl)
                    putString(ARG_SECTION_COMPILATION_ALBUM_URL, sectionCompilationAlbumUrl)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.apply {
            artistName = getString(ARG_ARTIST_NAME)
            getString(ARG_SECTION_ALBUM_URL)?.let { sectionsUrls.add(it to 0) }
            getString(ARG_SECTION_SINGLE_ALBUM_URL)?.let { sectionsUrls.add(it to 1) }
            getString(ARG_SECTION_LIVE_ALBUM_URL)?.let { sectionsUrls.add(it to 2) }
            getString(ARG_SECTION_COMPILATION_ALBUM_URL)?.let { sectionsUrls.add(it to 3) }
        }
        mainActivityAction = activity as MainActivityAction
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_catalog_artist_albums, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressBar = view.findViewById(R.id.fragment_catalog_artist_albums_pb_loading)
        textViewError = view.findViewById(R.id.fragment_catalog_artist_albums_tv_error)

        toolbar = view.findViewById(R.id.fragment_catalog_artist_albums_tb_title)
        toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }
        if (!artistName.isNullOrBlank()) {
            val template = resources.getString(R.string.label_term_belongs)
            val albumsStr = resources.getString(R.string.label_music_albums)
            toolbar.title = MessageFormat.format(template, artistName, albumsStr)
        }

        recyclerView = view.findViewById(R.id.fragment_catalog_artist_albums_rv_items)
        recyclerView.layoutManager = LinearLayoutManager(context)

        loadContent()
        textViewError.setOnClickListener { loadContent() }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        for (cancellable in requestCancellable)
            cancellable.cancel()
    }

    private fun loadContent() {
        if (sectionsUrls.isEmpty())
            return

        progressBar.visibility = View.VISIBLE
        textViewError.visibility = View.INVISIBLE

        sectionIdx = 0
        updating = true
        Singleton.appleMusicScraper.getSectionAlbums(sectionsUrls[sectionIdx].first, object: ResponseListener<AppleMusicSection<AppleMusicAlbum3>> {
            override fun onResponse(response: AppleMusicSection<AppleMusicAlbum3>) {
                progressBar.visibility = View.INVISIBLE
                val type = sectionsUrls[sectionIdx].second
                val albumList = response.entityList
                if (albumList.isNotEmpty()) {
                    recyclerView.adapter = CatalogArtistAlbumsAdapter(
                        context!!, mainActivityAction,
                        getTypeString(type), albumList.sortedByDescending { it.releaseDate })
                }
                updating = false
            }

            override fun onError(error: Exception) {
                progressBar.visibility = View.INVISIBLE
                textViewError.visibility = View.VISIBLE
                Log.i("MusicBox", "artistAlbums.onError", error)
                updating = false
            }
        }).let { requestCancellable.add(it) }

        recyclerView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager? ?: return
                val adapter = recyclerView.adapter as CatalogArtistAlbumsAdapter?

                if (adapter == null ||
                    layoutManager.findLastVisibleItemPosition() >= adapter.itemCount - 5 &&
                    !updating &&
                    (++sectionIdx) < sectionsUrls.size) {
                    updating = true
                    val type = sectionsUrls[sectionIdx].second
                    Singleton.appleMusicScraper.getSectionAlbums(sectionsUrls[sectionIdx].first, object: ResponseListener<AppleMusicSection<AppleMusicAlbum3>> {
                        override fun onResponse(response: AppleMusicSection<AppleMusicAlbum3>) {
                            val albumList = response.entityList
                            if (albumList.isNotEmpty()) {
                                if (recyclerView.adapter == null)
                                    recyclerView.adapter = CatalogArtistAlbumsAdapter(
                                        context!!, mainActivityAction,
                                        getTypeString(type), albumList.sortedByDescending { it.releaseDate })
                                else
                                    (recyclerView.adapter as CatalogArtistAlbumsAdapter?)?.add(
                                        getTypeString(type), albumList.sortedByDescending { it.releaseDate })
                            }
                            updating = false
                        }

                        override fun onError(error: Exception) {
                            --sectionIdx
                            updating = false
                        }
                    }).let { requestCancellable.add(it) }
                }
            }
        })
    }

    private fun getTypeString(type: Int): String {
        val typeStrId = when(type) {
            0 -> R.string.label_music_album
            1 -> R.string.label_music_album_single
            2 -> R.string.label_music_album_live
            3 -> R.string.label_music_album_compilation
            else -> null
        }
        return if (typeStrId == null) "" else resources.getString(typeStrId)
    }
}


private class CatalogArtistAlbumsAdapter(
    private val context: Context,
    private val mainActivityAction: MainActivityAction,
    typeString: String,
    albumList: List<AppleMusicAlbum3>
): RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    private val displayList = ArrayList<Any>()

    init {
        add(typeString, albumList)
    }

    fun add(typeString: String, albumList: List<AppleMusicAlbum3>) = synchronized(displayList) {
        val oldSize = displayList.size
        displayList.add(typeString)
        displayList.addAll(albumList)
        this.notifyItemRangeInserted(oldSize, albumList.size)
    }

    override fun getItemCount(): Int {
        return displayList.size
    }

    override fun getItemViewType(position: Int): Int {
        return if (displayList[position] is String) 0 else 1
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        if (viewType == 0) {
            return TextViewHolder(TextView(context).apply {
                setTextSize(TypedValue.COMPLEX_UNIT_PX, resources.getDimension(R.dimen.content_medium_text_size))
                setTextColor(ContextCompat.getColor(context, R.color.contentColorPrimary))
                setPadding(padding, padding, 0, 0)
            })
        }
        else {
            val v = LayoutInflater.from(context).inflate(R.layout.item_catalog_album, parent, false)
            return CatalogAlbum(v)
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        if (holder.itemViewType == 0)
            (holder as TextViewHolder).setData(displayList[position] as String)
        else
            (holder as CatalogAlbum).setData(displayList[position] as AppleMusicAlbum3)
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        //if (holder is CatalogAlbum)
            //holder.recycle()
    }


    private val imageSize = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 80f, context.resources.displayMetrics).toInt()

    private val padding = TypedValue.applyDimension(
        TypedValue.COMPLEX_UNIT_DIP, 15f, context.resources.displayMetrics).toInt()

    private inner class CatalogAlbum(view: View): RecyclerView.ViewHolder(view) {
        private val imageViewCoverart: ImageView = view.findViewById(R.id.item_catalog_album_iv_coverart)
        private val textViewTitle: TextView = view.findViewById(R.id.item_catalog_album_tv_title)
        private val textViewLabel: TextView = view.findViewById(R.id.item_catalog_album_tv_label)
        private var cancellable: Cancellable? = null

        init {
            view.setOnClickListener {
                val album = displayList[adapterPosition] as AppleMusicAlbum3
                mainActivityAction.changeFragment(CatalogAlbumFragment.newInstance(album.albumViewUrl))
            }
        }

        fun setData(album: AppleMusicAlbum3) {
            imageViewCoverart.setImageBitmap(null)
            textViewTitle.text = album.title
            textViewLabel.text = album.releaseDate.year.toString()

            val imageUrl = album.coverart?.sourceByShortSideEquals(imageSize)?.url
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

    private inner class TextViewHolder(private val textView: TextView):
        RecyclerView.ViewHolder(textView) {
        fun setData(typeString: String) {
            textView.text = typeString
        }
    }

}
