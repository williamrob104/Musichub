package com.freemusic.musicbox.networking

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test


class UrlParseTest {
    @Test fun testEncodeAll() {
        val        str = "?=& "
        val encodedStr = "%3F%3D%26%20"
        assertEquals(UrlParse.encodeAll(str), encodedStr)
    }

    @Test fun testEncodeParams() {
        val        params  = mapOf("鍵" to "= 值")
        val encodedParams = "%E9%8D%B5=%3D%20%E5%80%BC"
        assertEquals(UrlParse.encodeParams(params), encodedParams)
    }

    @Test fun testDecodeAll() {
        val suit = mapOf("%20" to " ", "%25" to "%", "%26" to "&", "%3D" to "=")
        for ((raw, decoded) in suit)
            assertEquals(UrlParse.decodeAll(raw), decoded)
    }

    @Test fun testDecodeParams() {
        val params = mapOf("鍵" to "= 值")
        val encoded = UrlParse.encodeParams(params)
        assertEquals(UrlParse.decodeParams(encoded), params)
    }

    @Test fun testJoinUrl_1() {
        val baseUrl = "https://www.youtube.com/"
        val components = arrayOf("/user", "/someone", "/about/")

        val joinedUrl1 = "https://www.youtube.com/user/someone/about"
        val joinedUrl2 = "https://www.youtube.com/user/someone/about/"
        val result = UrlParse.joinUrl(baseUrl, *components)
        assertTrue(result == joinedUrl1 || result == joinedUrl2)
    }

    @Test fun testJoinUrl_2() {
        val baseUrl = "https://www.youtube.com"
        val components = arrayOf("watch/")
        val parameters = "v=bu7nU9Mhpyo"

        val joinedUrl = "https://www.youtube.com/watch?v=bu7nU9Mhpyo"
        val result = UrlParse.joinUrl(baseUrl, *components, params=parameters)
        assertEquals(result, joinedUrl)
    }

    @Test fun testParseBaseUrl() {
        val     url  = "https://www.youtube.com/user/someone/about"
        val baseUrl1 = "https://www.youtube.com/user/someone"
        val baseUrl2 = "https://www.youtube.com/user/someone/"
        val result = UrlParse.parseBaseUrl(url)
        assertTrue(result == baseUrl1 || result == baseUrl2)
    }
}