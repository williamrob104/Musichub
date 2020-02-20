package com.musichub.ui


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.musichub.R
import com.musichub.concurrent.Cancellable
import com.musichub.concurrent.ResponseListener
import com.musichub.resource.AppleMusicEntity
import com.musichub.resource.AppleMusicMV
import com.musichub.resource.AppleMusicSearch
import com.musichub.singleton.Singleton


class SearchCatalogFragment : SearchTargetFragment() {

    private lateinit var progressBar: ProgressBar
    private lateinit var textViewMsg: TextView
    private lateinit var recyclerView: RecyclerView
    private var searchCancellable: Cancellable? = null

    private var query: String = ""

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_search_catalog, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        progressBar = view.findViewById(R.id.fragment_search_catalog_pb_loading)
        textViewMsg = view.findViewById(R.id.fragment_search_catalog_tv_msg)
        recyclerView = view.findViewById(R.id.fragment_search_catalog_rv_items)

        progressBar.visibility = View.INVISIBLE
        textViewMsg.visibility = View.INVISIBLE
        recyclerView.visibility = View.INVISIBLE

        recyclerView.layoutManager = LinearLayoutManager(context)

        textViewMsg.setOnClickListener {
            if (textViewMsg.text == resources.getString(R.string.msg_load_error_press_retry))
                loadContent(query)
        }
    }

    override fun onStop() {
        super.onStop()
        searchCancellable?.cancel()
    }


    override fun onQueryTextSubmit(query: String) { loadContent(query) }

    override fun onQueryTextChange(query: String) { }

    override fun onPageChange(query: String) { loadContent(query) }

    private fun loadContent(query: String) {
        this.query = query
        if (query.isBlank()) return

        performSearch(query)
    }

    private fun performSearch(query: String) {
        progressBar.visibility = View.VISIBLE
        textViewMsg.visibility = View.INVISIBLE
        recyclerView.visibility = View.INVISIBLE

        searchCancellable?.cancel()
        searchCancellable = Singleton.appleMusicScraper.searchAll(query, 10,
            object: ResponseListener<AppleMusicSearch<AppleMusicEntity>> {
            override fun onResponse(response: AppleMusicSearch<AppleMusicEntity>) {
                progressBar.visibility = View.INVISIBLE
                val itemList = response.itemList.filter { it !is AppleMusicMV }
                if (itemList.isNotEmpty()) {
                    progressBar.visibility = View.INVISIBLE
                    textViewMsg.visibility = View.INVISIBLE
                    recyclerView.visibility = View.VISIBLE
                    recyclerView.adapter = SearchCatalogRecyclerViewAdapter(
                        context!!, activity as MainActivityAction, itemList, query, true
                    )
                }
                else {
                    progressBar.visibility = View.INVISIBLE
                    textViewMsg.visibility = View.VISIBLE
                    recyclerView.visibility = View.INVISIBLE
                    textViewMsg.text = resources.getString(R.string.msg_no_matching_result)
                }
            }

            override fun onError(error: Exception) {
                progressBar.visibility = View.INVISIBLE
                textViewMsg.visibility = View.VISIBLE
                recyclerView.visibility = View.INVISIBLE
                textViewMsg.text = resources.getString(R.string.msg_load_error_press_retry)
            }
        })
    }

}
