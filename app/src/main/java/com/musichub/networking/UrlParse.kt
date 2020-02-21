package com.musichub.networking

import java.net.URL
import java.net.URLDecoder
import java.net.URLEncoder


object UrlParse {
    const val DEFAULT_CHARSET = "UTF-8"
    const val DEFAULT_CONTENT_TYPE = "application/x-www-form-urlencoded; charset=$DEFAULT_CHARSET"

    private val percentEncodingReserved = setOf(
        '!'.toInt(),
        '*'.toInt(),
        '\''.toInt(),
        '('.toInt(),
        ')'.toInt(),
        ';'.toInt(),
        ':'.toInt(),
        '@'.toInt(),
        '&'.toInt(),
        '='.toInt(),
        '+'.toInt(),
        '$'.toInt(),
        ','.toInt(),
        '/'.toInt(),
        '?'.toInt(),
        '#'.toInt(),
        '['.toInt(),
        ']'.toInt()
    )

    fun encodeAll(str: String): String {
        return URLEncoder.encode(str, DEFAULT_CHARSET).replace("+", "%20")
    }

    fun encodeParams(parameters: Map<String, String>): String {
        if (parameters.isEmpty())
            return ""

        val sb = StringBuilder()
        for ((key, value) in parameters) {
            sb.append(encodeAll(key))
            sb.append('=')
            sb.append(encodeAll(value))
            sb.append('&')
        }
        return sb.substring(0, sb.length - 1)
    }

    fun decodeAll(str: String): String {
        return URLDecoder.decode(str, DEFAULT_CHARSET)
    }

    fun decodeParams(parametersString: String): Map<String, String> {
        val qs = HashMap<String, String>()
        for (kvStr in parametersString.split('&')) {
            val (key, value) = kvStr.split('=', limit = 2)
            qs[decodeAll(key)] = decodeAll(value)
        }
        return qs
    }

    fun joinUrl(baseUrl: String, vararg slashSeparated: String, params: String? = null): String {
        val slash = '/'
        val sb = StringBuilder()
        sb.append(baseUrl.trimEnd(slash))
        for (component in slashSeparated) {
            sb.append(slash)
            sb.append(component.trim(slash))
        }
        if (!params.isNullOrBlank()) {
            sb.append('?')
            sb.append(params)
        }
        return sb.toString()
    }

    fun parseBaseUrl(_url: String): String {
        val url = URL(_url)
        return "${url.protocol}://${url.host}/"
    }
}