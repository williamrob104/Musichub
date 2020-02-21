package com.musichub.networking

import com.musichub.concurrent.Cancellable
import com.musichub.concurrent.ResponseListener
import org.json.JSONObject
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.Executors


class HttpUrlClient(numThreads: Int = 4) : BasicHttpRequests {
    private val executor = Executors.newFixedThreadPool(numThreads)

    private abstract class HttpRequestTask : Runnable, Cancellable {
        protected var cancelled: Boolean = false
            get() = synchronized(field) { field }
            set(value) = synchronized(field) { field = value }

        override fun cancel() {
            cancelled = true
        }

        override fun isCancelled(): Boolean {
            return cancelled
        }

        protected fun requestGetConnection(
            url: String, headers: Map<String, String>?, method: String,
            checkResponseCode: Boolean = true
        ): HttpURLConnection {
            val con = URL(url).openConnection() as HttpURLConnection
            con.requestMethod = method
            if (headers != null) {
                for ((key, value) in headers)
                    con.setRequestProperty(key, value)
            }
            if (checkResponseCode)
                con.checkResponseCode()
            return con
        }

        protected fun HttpURLConnection.checkResponseCode() {
            if (responseCode / 100 != 2)
                throw RuntimeException("Http response code $responseCode")
        }

        protected fun inputStream2String(inputStream: InputStream, charset: String): String {
            val result = ByteArrayOutputStream()
            val buffer = ByteArray(1024)
            while (true) {
                val length = inputStream.read(buffer)
                if (length == -1)
                    break
                result.write(buffer, 0, length)
            }
            return result.toString(charset)
        }

        protected val HttpURLConnection.contentCharset: String
            get() {
                val contentType = this.contentType
                if (contentType != null) {
                    for (field in contentType.split(';', ',')) {
                        if ("charset" in field) {
                            val tokens = field.split('=')
                            return tokens[1]
                        }
                    }
                }
                return "UTF-8"
            }
    }

    override fun <T> httpGETString(
        url: String, headers: Map<String, String>?,
        listener: ResponseListener<T>, converter: (String) -> T
    ): Cancellable {
        val task = object : HttpRequestTask() {
            override fun run() {
                try {
                    val con = requestGetConnection(url, headers, "GET")
                    val responseStr = inputStream2String(con.inputStream, con.contentCharset)
                    val responseT = converter(responseStr)
                    if (!cancelled)
                        listener.onResponse(responseT)
                } catch (e: Exception) {
                    if (!cancelled)
                        listener.onError(e)
                }
            }
        }
        executor.execute(task)
        return task
    }

    override fun <T> httpGETJson(
        url: String, headers: Map<String, String>?,
        listener: ResponseListener<T>, converter: (JSONObject) -> T
    ): Cancellable {
        val task = object : HttpRequestTask() {
            override fun run() {
                try {
                    val con = requestGetConnection(url, headers, "GET")
                    val responseStr = inputStream2String(con.inputStream, con.contentCharset)
                    val responseJson = JSONObject(responseStr)
                    val responseT = converter(responseJson)
                    if (!cancelled)
                        listener.onResponse(responseT)
                } catch (e: Exception) {
                    if (!cancelled)
                        listener.onError(e)
                }
            }
        }
        executor.execute(task)
        return task
    }

    override fun <T> httpGETHtml(
        url: String, headers: Map<String, String>?,
        listener: ResponseListener<T>, converter: (Document) -> T
    ): Cancellable {
        val task = object : HttpRequestTask() {
            override fun run() {
                try {
                    val con = requestGetConnection(url, headers, "GET")
                    val responseHtml =
                        Jsoup.parse(con.inputStream, con.contentCharset, UrlParse.parseBaseUrl(url))
                    val responseT = converter(responseHtml)
                    if (!cancelled)
                        listener.onResponse(responseT)
                } catch (e: Exception) {
                    if (!cancelled)
                        listener.onError(e)
                }
            }
        }
        executor.execute(task)
        return task
    }

    override fun <T> httpPOSTJson(
        url: String, headers: Map<String, String>?, data: ByteArray?,
        listener: ResponseListener<T>, converter: (JSONObject) -> T
    ): Cancellable {
        val task = object : HttpRequestTask() {
            override fun run() {
                try {
                    val con = requestGetConnection(url, headers, "POST", false)
                    if (data != null && data.isNotEmpty()) {
                        con.doOutput = true
                        con.setChunkedStreamingMode(0)
                        con.outputStream.write(data)
                        con.outputStream.flush()
                    }
                    con.checkResponseCode()
                    val responseStr = inputStream2String(con.inputStream, con.contentCharset)
                    val responseJson = JSONObject(responseStr)
                    val responseT = converter(responseJson)
                    if (!cancelled)
                        listener.onResponse(responseT)
                } catch (e: Exception) {
                    if (!cancelled)
                        listener.onError(e)
                }
            }
        }
        executor.execute(task)
        return task
    }
}
