package com.musichub.networking

import com.musichub.concurrent.Cancellable
import com.musichub.concurrent.ResponseListener
import org.json.JSONObject
import org.jsoup.nodes.Document


interface BasicHttpRequests {
    fun <T> httpGETString(url: String, headers: Map<String, String>?,
                          listener: ResponseListener<T>, converter: (String)->T): Cancellable

    fun <T> httpGETJson(url: String, headers: Map<String, String>?,
                        listener: ResponseListener<T>, converter: (JSONObject)->T): Cancellable

    fun <T> httpGETHtml(url: String, headers: Map<String, String>?,
                        listener: ResponseListener<T>, converter: (Document)->T): Cancellable

    fun <T> httpPOSTJson(url: String, headers: Map<String, String>?, data: ByteArray?,
                        listener: ResponseListener<T>, converter: (JSONObject)->T): Cancellable
}