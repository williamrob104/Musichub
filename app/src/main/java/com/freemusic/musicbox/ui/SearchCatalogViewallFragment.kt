package com.freemusic.musicbox.ui

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.freemusic.musicbox.R
import com.freemusic.musicbox.concurrent.Cancellable
import com.freemusic.musicbox.concurrent.ResponseListener
import com.freemusic.musicbox.resource.AppleMusicEntity
import com.freemusic.musicbox.resource.AppleMusicSearch
import com.freemusic.musicbox.singleton.Singleton
import com.freemusic.musicbox.util.messageFormat
import java.util.*
import java.util.concurrent.ConcurrentLinkedQueue


class SearchCatalogViewallFragment: Fragment() {
    private var entityType = 0
    private lateinit var query: String
    private val LIMIT = 30

    private val requestCancellable = ConcurrentLinkedQueue<Cancellable>()

    private val entitySet = Collections.synchronizedSet(HashSet<AppleMusicEntity>())
    private var updating = false
    private var endTouched = false

    private lateinit var progressBar: ProgressBar
    private lateinit var textViewMsg: TextView
    private lateinit var recyclerView: RecyclerView
    private lateinit var toolbar: Toolbar

    companion object {
        private const val ARG_ENTITY_TYPE = "param1"
        private const val ARG_QUERY = "param2"

        @JvmStatic
        fun newInstance(entityType: Int, query: String): SearchCatalogViewallFragment {
            return SearchCatalogViewallFragment().apply {
                arguments = Bundle().apply {
                    putInt(ARG_ENTITY_TYPE, entityType)
                    putString(ARG_QUERY, query)
                }
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            entityType = it.getInt(ARG_ENTITY_TYPE)
            query = it.getString(ARG_QUERY)!!
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_search_catalog_viewall, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        entitySet.clear()
        endTouched = false

        progressBar = view.findViewById(R.id.fragment_search_catalog_viewall_pb_loading)
        textViewMsg = view.findViewById(R.id.fragment_search_catalog_viewall_tv_msg)

        recyclerView = view.findViewById(R.id.fragment_search_catalog_viewall_rv_items)
        recyclerView.layoutManager = LinearLayoutManager(context)


        toolbar = view.findViewById(R.id.fragment_search_catalog_viewall_tb_title)
        val entityTypeString = context!!.resources.getString(when(entityType) {
            0 -> R.string.label_music_artist
            1 -> R.string.label_music_album
            2 -> R.string.label_music_track
            else -> throw IllegalArgumentException()
        })
        toolbar.title = context!!.resources.getString(R.string.label_term_containing).messageFormat(
            entityTypeString, query
        )
        toolbar.setNavigationOnClickListener {
            activity?.onBackPressed()
        }

        loadContent()
        textViewMsg.setOnClickListener {
            if (textViewMsg.text == resources.getString(R.string.msg_load_error_press_retry))
                loadContent()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        for (cancellable in requestCancellable)
            cancellable.cancel()
    }

    private fun loadContent() {
        progressBar.visibility = View.VISIBLE
        textViewMsg.visibility = View.INVISIBLE

        when(entityType) {
            0 -> Singleton.appleMusicScraper.searchArtists(query, LIMIT, 0, initializeCallback())
            1 -> Singleton.appleMusicScraper.searchAlbums(query, LIMIT, 0, initializeCallback())
            2 -> Singleton.appleMusicScraper.searchSongs(query, LIMIT, 0, initializeCallback())
            else -> null
        }.let { if (it != null) requestCancellable.add(it) }

        recyclerView.addOnScrollListener(object: RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)

                val layoutManager = recyclerView.layoutManager as LinearLayoutManager? ?: return
                val adapter = recyclerView.adapter as SearchCatalogRecyclerViewAdapter? ?: return
                val currentSize = adapter.entityList.size

                if (layoutManager.findLastVisibleItemPosition() >= currentSize - 10 && !updating && !endTouched) {
                    updating = true
                    when(entityType) {
                        0 -> Singleton.appleMusicScraper.searchArtists(query, LIMIT, currentSize, updateCallback())
                        1 -> Singleton.appleMusicScraper.searchAlbums(query, LIMIT, currentSize, updateCallback())
                        2 -> Singleton.appleMusicScraper.searchSongs(query, LIMIT, currentSize, updateCallback())
                        else -> null
                    }.let { if (it != null) requestCancellable.add(it) }
                }
            }
        })
    }

    private fun <T: AppleMusicEntity> initializeCallback(): ResponseListener<AppleMusicSearch<T>> {
        return object: ResponseListener<AppleMusicSearch<T>> {
            override fun onResponse(response: AppleMusicSearch<T>) {
                val entityList = response.itemList
                if (entityList.isNotEmpty()) {
                    progressBar.visibility = View.INVISIBLE
                    textViewMsg.visibility = View.INVISIBLE
                    if (entityList.size < LIMIT)
                        endTouched = true
                    entitySet.addAll(entityList)
                    recyclerView.adapter = SearchCatalogRecyclerViewAdapter(
                        context!!, activity as MainActivityAction, entityList, null, false
                    )
                }
                else {
                    progressBar.visibility = View.INVISIBLE
                    textViewMsg.visibility = View.VISIBLE
                    textViewMsg.text = resources.getString(R.string.msg_no_matching_result)
                    endTouched = true
                }
                updating = false
            }

            override fun onError(error: Exception) {
                progressBar.visibility = View.INVISIBLE
                textViewMsg.visibility = View.VISIBLE
                textViewMsg.text = resources.getString(R.string.msg_load_error_press_retry)
                updating = false
            }
        }
    }

    private fun <T: AppleMusicEntity> updateCallback(): ResponseListener<AppleMusicSearch<T>> {
        return object: ResponseListener<AppleMusicSearch<T>> {
            override fun onResponse(response: AppleMusicSearch<T>) {
                val entityList = response.itemList
                if (entityList.isNotEmpty()) {
                    if (entityList.size < LIMIT)
                        endTouched = true
                    val adapter = recyclerView.adapter as SearchCatalogRecyclerViewAdapter? ?: return
                    val oldSize = adapter.entityList.size
                    for (entity in entityList) {
                        if (entitySet.add(entity))
                            adapter.entityList.add(entity)
                    }
                    val newSize = adapter.entityList.size
                    adapter.notifyItemRangeInserted(oldSize, newSize - oldSize)
                }
                else {
                    endTouched = true
                }
                updating = false
            }

            override fun onError(error: Exception) {
                updating = false
            }
        }
    }
}
