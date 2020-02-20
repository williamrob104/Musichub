package com.musichub.networking

import com.musichub.concurrent.RequestFuture
import com.musichub.networking.UrlParse.encodeParams
import org.json.JSONObject
import org.jsoup.nodes.Document
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.concurrent.TimeUnit


private val basicHttpRequests: BasicHttpRequests = HttpUrlClient()

class HttpUrlClientTest {

    @Test fun testHttpGETString() {
        val url = "https://httpbin.org/get"
        val headers = mapOf("User-Agent" to "HttpUrlClient")
        val future = RequestFuture<String>()

        basicHttpRequests.httpGETString(url, headers, future) { it }

        val responseString = future.get(10, TimeUnit.SECONDS)
        val responseJson = JSONObject(responseString)
        assertEquals(responseJson["url"], url)
        for ((key, value) in headers)
            assertEquals(responseJson.getJSONObject("headers")[key], value)
    }

    @Test fun testHttpGETJson() {
        val url = "https://httpbin.org/get?key=value"
        val headers = mapOf("User-Agent" to "HttpUrlClient")
        val future = RequestFuture<JSONObject>()
        basicHttpRequests.httpGETJson(url, headers,  future) { it }

        val responseJson = future.get(10, TimeUnit.SECONDS)

        assertEquals(responseJson.getJSONObject("args")["key"], "value")
        assertEquals(responseJson["url"], url)
        for ((key, value) in headers)
            assertEquals(responseJson.getJSONObject("headers")[key], value)
    }

    @Test fun testHttpGETHtml() {
        val url = "http://help.websiteos.com/websiteos/example_of_a_simple_html_page.htm"
        val header = mapOf("User-Agent" to "HttpUrlClient")
        val future = RequestFuture<Document>()

        basicHttpRequests.httpGETHtml(url, header,  future) { it }

        val responseHtml = future.get(10, TimeUnit.SECONDS)
        assertEquals(responseHtml.title(), "Example of a simple HTML page")
        assertEquals(responseHtml.body().getElementsByTag("h1").text(),
                "Example of a simple HTML page")
    }

    @Test fun testHttpPOSTJson() {
        val url = "https://httpbin.org/post?key=value"
        val headers = mapOf("User-Agent" to "HttpUrlClient", "Content-Type" to UrlParse.DEFAULT_CONTENT_TYPE)
        val form = mapOf("user" to "myUserName", "password" to "123456")
        val future = RequestFuture<JSONObject>()
        basicHttpRequests.httpPOSTJson(url, headers, encodeParams(form).toByteArray(), future) { it }

        val responseJson = future.get(10, TimeUnit.SECONDS)

        assertEquals(responseJson.getJSONObject("args")["key"], "value")
        assertEquals(responseJson["url"], url)
        for ((key, value) in headers)
            assertEquals(responseJson.getJSONObject("headers")[key], value)
        for ((key, value) in form)
            assertEquals(responseJson.getJSONObject("form")[key], value)
    }
}