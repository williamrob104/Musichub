package com.musichub.singleton

import android.content.Context
import android.os.Handler
import androidx.core.os.ConfigurationCompat
import com.android.volley.RequestQueue
import com.android.volley.toolbox.BasicNetwork
import com.android.volley.toolbox.DiskBasedCache
import com.android.volley.toolbox.HurlStack
import com.musichub.concurrent.CallbackExecutor
import com.musichub.networking.BasicHttpRequests
import com.musichub.networking.ImageRequests
import com.musichub.networking.VolleyWrapper
import com.musichub.resource.AppleMusicScraper
import com.musichub.resource.YoutubeScraper
import java.io.File
import java.util.concurrent.Executors


object Singleton {
    private const val NETWORK_CACHE_DIR = "volley"
    private const val NETWORK_CACHE_SIZE = 50 * 1024 * 1024
    private const val IMAGE_CACHE_SIZE = 5 * 1024 * 1024

    private var initialized = false

    private lateinit var volleyWrapper: VolleyWrapper

    private lateinit var callbackExecutor: CallbackExecutor

    internal val basicHttpRequests: BasicHttpRequests
        get() = volleyWrapper

    lateinit var imageRequests: ImageRequests
        private set

    lateinit var appleMusicScraper: AppleMusicScraper
        private set

    lateinit var youtubeScraper: YoutubeScraper
        private set

    fun initialize(context: Context) = synchronized(initialized) {
        if (!initialized) {
            val locale = ConfigurationCompat.getLocales(context.resources.configuration)[0]

            val cacheDir = File(context.cacheDir, NETWORK_CACHE_DIR)
            val cache = DiskBasedCache(cacheDir, NETWORK_CACHE_SIZE)
            val network = BasicNetwork(HurlStack())
            val requestQueue = RequestQueue(cache, network).apply {
                start()
            }

            val executorService = Executors.newFixedThreadPool(4)

            val handler = Handler(context.mainLooper)

            volleyWrapper = VolleyWrapper(requestQueue)
            callbackExecutor = CallbackExecutor(executorService, handler)

            imageRequests = ImageRequests(requestQueue, context, IMAGE_CACHE_SIZE)
            appleMusicScraper = AppleMusicScraper(volleyWrapper, callbackExecutor, locale)
            youtubeScraper = YoutubeScraper(volleyWrapper, callbackExecutor)

            initialized = true
        }
    }
}