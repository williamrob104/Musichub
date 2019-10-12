package com.freemusic.musicbox.ui


import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.webkit.WebView
import android.webkit.WebViewClient
import com.freemusic.musicbox.R
import com.freemusic.musicbox.networking.UrlParse
import com.freemusic.musicbox.playback.YoutubeVideoMediaHolder
import com.freemusic.musicbox.resource.YoutubeScraper


class SearchYoutubeFragment : SearchTargetFragment(), FragmentActions {

    private lateinit var mainActivityAction: MainActivityAction

    private lateinit var webView: WebView
    private var webViewClearHistory = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mainActivityAction = activity as MainActivityAction
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        return inflater.inflate(R.layout.fragment_search_youtube, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        webView = view.findViewById(R.id.fragment_search_youtube_wv)
        webView.settings.javaScriptEnabled = true
        webView.settings.domStorageEnabled = true
        webView.webViewClient = object: WebViewClient() {
            override fun onPageFinished(view: WebView?, url: String?) {
                //val tag = "ytm-pivot-bar-renderer"
                if (url != null) {
                    val videoId = YoutubeScraper.parseYoutubeUrl(url)
                    if (videoId != null) {
                        mainActivityAction.playMedia(YoutubeVideoMediaHolder(videoId), null)
                        view?.goBack()
                        return
                    }
                }
                super.onPageFinished(view, url)
                if (webViewClearHistory) {
                    view?.clearHistory()
                    webViewClearHistory = false
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        webView.onResume()
    }

    override fun onPause() {
        super.onPause()
        webView.onPause()
    }

    override fun onDestroyView() {
        webView.destroy()
        super.onDestroyView()
    }


    /* implements FragmentActions */

    override fun onBackPressed(): Boolean {
        return if (webView.canGoBack()) {
            webView.goBack()
            true
        }
        else false
    }


    /* extends SearchTargetFragment */

    override fun onQueryTextSubmit(query: String) { performSearch(query) }

    override fun onPageChange(query: String) { performSearch(query) }

    override fun onQueryTextChange(query: String) {}

    private fun performSearch(query: String) {
        if (query.isBlank()) {
            val url = "https://www.youtube.com/channel/UC-9-kyTW8ZkZNDHQJ6FgpwQ"
            webViewClearHistory = true
            webView.loadUrl(url)
        }
        else {
            val urlTemplate = "https://www.youtube.com/results?search_query=%s"
            val url = urlTemplate.format(UrlParse.encodeAll(query))
            webViewClearHistory = true
            webView.loadUrl(url)
        }
    }


}
