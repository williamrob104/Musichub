package com.musichub.scraper

import com.musichub.concurrent.Cancellable
import com.musichub.concurrent.ResponseListener
import com.musichub.networking.BasicHttpRequests
import com.musichub.networking.UrlParse
import com.musichub.util.nextId
import com.musichub.util.parseTime
import org.json.JSONException
import java.io.ByteArrayOutputStream
import java.io.DataOutputStream
import kotlin.random.Random


class AuddApi(private val basicHttpRequests: BasicHttpRequests) {
    private val baseUrl = "https://api.audd.io/"

    fun recognizeMusic(
        musicFile: ByteArray,
        listener: ResponseListener<AuddRecognition>
    ): Cancellable {
        val params = mapOf("method" to "recognize", "return" to "timecode,itunes")
        val url = UrlParse.joinUrl(baseUrl, params = UrlParse.encodeParams(params))

        val attachmentName = "file"
        val attachmentFileName = "file"
        val boundary = Random.nextId(20)
        val twoHyphens = "--"
        val crlf = "\r\n"

        val headers = mapOf("Content-Type" to "multipart/form-data; boundary=$boundary")

        val data = ByteArrayOutputStream()
        DataOutputStream(data).apply {
            writeBytes(twoHyphens + boundary + crlf)
            writeBytes("Content-Disposition: form-data; name=\"$attachmentName\"; filename=\"$attachmentFileName\"$crlf")
            writeBytes(crlf)
            write(musicFile)
            writeBytes(crlf)
            writeBytes(twoHyphens + boundary + twoHyphens + crlf)
            flush()
        }

        return basicHttpRequests.httpPOSTJson(url, headers, data.toByteArray(), listener) {
            if (it.has("error")) {
                val errMsg = it.optJSONObject("error")?.optString("error_message")
                val errCode = it.optJSONObject("error")?.optInt("error_code")
                throw AuddError("Error $errCode: $errMsg")
            }
            if (it.isNull("result")) {
                throw AuddError("cannot recognize")
            }

            val result = it.getJSONObject("result")
            val trackTitle = result.getString("title")
            val artistName = result.getString("artist")
            val albumTitle = try {
                result.getJSONObject("itunes").getString("collectionName")
            } catch (e: JSONException) {
                result.getString("album")
            }
            val timeCode = parseTime(result.getString("timecode"))

            AuddRecognition(trackTitle, artistName, albumTitle, timeCode)
        }
    }
}


data class AuddRecognition(
    val trackTitle: String,
    val artistName: String,
    val albumTitle: String,
    val timeCode: Int
)

class AuddError(errMsg: String) : Exception(errMsg)