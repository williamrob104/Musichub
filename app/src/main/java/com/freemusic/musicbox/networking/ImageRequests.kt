package com.freemusic.musicbox.networking

import android.content.Context
import android.graphics.Bitmap
import android.os.Handler
import androidx.collection.LruCache
import com.android.volley.RequestQueue
import com.android.volley.Response
import com.android.volley.toolbox.ImageRequest
import com.freemusic.musicbox.concurrent.Cancellable
import com.freemusic.musicbox.concurrent.ResponseListener


class ImageRequests(private val requestQueue: RequestQueue, context: Context, cacheSizeBytes: Int) {

    private val handler = Handler(context.mainLooper)

    private val cache = object: LruCache<String, Bitmap>(cacheSizeBytes) {
        override fun sizeOf(key: String, value: Bitmap): Int {
            return value.rowBytes * value.height
        }
    }

    fun getImage(url: String,
                 listener: ResponseListener<Bitmap>): Cancellable {
        val cachedBitmap = cache[url]
        if (cachedBitmap != null) {
            handler.post { listener.onResponse(cachedBitmap) }
            return object: Cancellable {
                override fun cancel() {}
                override fun isCancelled(): Boolean { return false }
            }
        }
        else {
            val request = ImageRequest(
                url,
                Response.Listener<Bitmap> { cache.put(url, it); listener.onResponse(it) },
                0, 0, null, null,
                Response.ErrorListener { listener.onError(it) }
            ).apply {
                setShouldRetryServerErrors(true)
                requestQueue.add(this)
            }
            return object: Cancellable {
                override fun cancel() = request.cancel()
                override fun isCancelled() = request.isCanceled
            }
        }
    }
}