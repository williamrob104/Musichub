package com.freemusic.musicbox.networking

import com.android.volley.*
import com.android.volley.toolbox.HttpHeaderParser
import com.freemusic.musicbox.concurrent.Cancellable
import com.freemusic.musicbox.concurrent.ResponseListener
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.ByteArrayInputStream
import java.nio.charset.Charset


class VolleyWrapper(private val requestQueue: RequestQueue): BasicHttpRequests {

    private fun myRetryPolicy() =
        DefaultRetryPolicy(2500, 3, 1f)

    override fun <T> httpGETString(url: String, headers: Map<String, String>?,
                                   listener: ResponseListener<T>, converter: (String) -> T): Cancellable {
        return StringConvertRequest(
            Request.Method.GET, url, headers, converter,
            Response.Listener { listener.onResponse(it) },
            Response.ErrorListener { listener.onError(it)}
        ).apply {
            setShouldRetryServerErrors(true)
            setRetryPolicy(myRetryPolicy())
            requestQueue.add(this)
        }.asCancellable()
    }

    override fun <T> httpGETJson(url: String, headers: Map<String, String>?,
                                 listener: ResponseListener<T>, converter: (JSONObject) -> T): Cancellable {
        return JsonConvertRequest(
            Request.Method.GET, url, headers, converter,
            Response.Listener { listener.onResponse(it) },
            Response.ErrorListener { listener.onError(it)}
        ).apply {
            setShouldRetryServerErrors(true)
            setRetryPolicy(myRetryPolicy())
            requestQueue.add(this)
        }.asCancellable()
    }

    override fun <T> httpGETHtml(url: String, headers: Map<String, String>?,
                                 listener: ResponseListener<T>, converter: (Document) -> T): Cancellable {
        return HtmlConvertRequest(
            Request.Method.GET, url, headers, converter,
            Response.Listener { listener.onResponse(it) },
            Response.ErrorListener { listener.onError(it)}
        ).apply {
            setShouldRetryServerErrors(true)
            setRetryPolicy(myRetryPolicy())
            requestQueue.add(this)
        }.asCancellable()
    }

    override fun <T> httpPOSTJson(url: String, headers: Map<String, String>?, data: ByteArray?,
                                  listener: ResponseListener<T>, converter: (JSONObject) -> T): Cancellable {
        return object: JsonConvertRequest<T>(
            Request.Method.POST, url, headers, converter,
            Response.Listener { listener.onResponse(it) },
            Response.ErrorListener { listener.onError(it)}
        ) {
            override fun getBody(): ByteArray {
                return data ?: super.getBody()
            }
        }.apply {
            setShouldRetryServerErrors(true)
            setRetryPolicy(myRetryPolicy())
            requestQueue.add(this)
        }.asCancellable()
    }

    private fun <T> Request<T>.asCancellable(): Cancellable {
        return object: Cancellable {
            override fun cancel() = this@asCancellable.cancel()
            override fun isCancelled() = this@asCancellable.isCanceled
        }
    }
}

private class StringConvertRequest<T>(method: Int, url: String, private val myHeaders: Map<String, String>?,
                              private val converter: (String)->T,
                              private val listener: Response.Listener<T>,
                              errorListener: Response.ErrorListener):
    Request<T>(method, url, errorListener) {
    override fun parseNetworkResponse(response: NetworkResponse?): Response<T> {
        return try {
            requireNotNull(response)
            val charset = Charset.forName(HttpHeaderParser.parseCharset(response.headers))
            val responseString = String(response.data, charset)
            val responseT = converter(responseString)
            Response.success(
                responseT,
                HttpHeaderParser.parseCacheHeaders(response)
            )
        }
        catch (e: Exception) {
            Response.error(VolleyError(e))
        }
    }

    override fun deliverResponse(response: T?) {
        listener.onResponse(response)
    }

    override fun getHeaders(): MutableMap<String, String> {
        return myHeaders?.toMutableMap() ?: super.getHeaders()
    }
}


private open class JsonConvertRequest<T>(method: Int, url: String, private val myHeaders: Map<String, String>?,
                              private val converter: (JSONObject)->T,
                              private val listener: Response.Listener<T>,
                              errorListener: Response.ErrorListener):
    Request<T>(method, url, errorListener) {
    override fun parseNetworkResponse(response: NetworkResponse?): Response<T> {
        return try {
            requireNotNull(response)
            val charset = Charset.forName(HttpHeaderParser.parseCharset(response.headers))
            val responseString = String(response.data, charset)
            val responseJson = JSONObject(responseString)
            val responseT = converter(responseJson)
            Response.success(
                responseT,
                HttpHeaderParser.parseCacheHeaders(response)
            )
        }
        catch (e: Exception) {
            Response.error(VolleyError(e))
        }
    }

    override fun deliverResponse(response: T?) {
        listener.onResponse(response)
    }

    override fun getHeaders(): MutableMap<String, String> {
        return myHeaders?.toMutableMap() ?: super.getHeaders()
    }
}


private class HtmlConvertRequest<T>(method: Int, url: String, private val myHeaders: Map<String, String>?,
                            private val converter: (Document)->T,
                            private val listener: Response.Listener<T>,
                            errorListener: Response.ErrorListener):
    Request<T>(method, url, errorListener) {
    override fun parseNetworkResponse(response: NetworkResponse?): Response<T> {
        return try {
            requireNotNull(response)
            val charset = HttpHeaderParser.parseCharset(response.headers)
            val baseUrl = UrlParse.parseBaseUrl(url)
            val responseStream = ByteArrayInputStream(response.data)
            val responseHtml = Jsoup.parse(responseStream, charset, baseUrl)
            val responseT = converter(responseHtml)
            Response.success(
                responseT,
                HttpHeaderParser.parseCacheHeaders(response)
            )
        }
        catch (e: Exception) {
            Response.error(VolleyError(e))
        }
    }

    override fun deliverResponse(response: T?) {
        listener.onResponse(response)
    }

    override fun getHeaders(): MutableMap<String, String> {
        return myHeaders?.toMutableMap() ?: super.getHeaders()
    }
}