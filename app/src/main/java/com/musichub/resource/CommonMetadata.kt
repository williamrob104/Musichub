package com.musichub.resource


interface Image {
    fun sourceByShortSideEquals(pixels: Int, largerIfNotPresent: Boolean=true): Source

    fun sourceByLongSideEquals(pixels: Int, largerIfNotPresent: Boolean=true): Source

    fun sourceLargest(): Source

    data class Source(
            val url: String,
            val width: Int,
            val height: Int
    )
}

class SingleSourceImage(url: String, width: Int, height: Int): Image {
    private val source = Image.Source(url, width, height)

    override fun sourceByShortSideEquals(pixels: Int, largerIfNotPresent: Boolean) = source

    override fun sourceByLongSideEquals(pixels: Int, largerIfNotPresent: Boolean) = source

    override fun sourceLargest() = source
}


data class VideoFormat(
        val codec: VideoCodec,
        val height: Int,
        val fps: Int?=null
)

enum class VideoCodec(val standard: String) {
    H263("H263"),
    H264("H264"),
    MP4V("MP4V"),
    VP8("VP8"),
    VP9("VP9")
}

data class AudioFormat(
        val codec: AudioCodec,
        val bitRate: Int
)

enum class AudioCodec(val standard: String) {
    MP3("MP3"),
    AAC("AAC"),
    Vorbis("Vorbis"),
    Opus("Opus")
}

enum class Container(val standard: String, val extension: String) {
    FLV("FLV", "flv"),
    _3GP("3GP", "3gp"),
    MP4("MP4", "mp4"),
    M4A("MP4", "m4a"),
    WebM("WebM", "webm")
}

enum class StreamProtocol {
    Progressive,
    DASH,
    HLS
}

interface Stream {
    val container: Container
    val videoFormat: VideoFormat?
    val audioFormat: AudioFormat?
    val protocol: StreamProtocol
    val url: String
}


interface Track {
    val title: String
    val artistName: String
    val albumTitle: String?
    val length: Int
}


class Date(
        val year: Int,
        _month: Int?,
        _day: Int?
): Comparable<Date> {

    val month: Int? = if (_month != null && _month in 1..12) _month else null
    val day: Int? = if (month != null) _day else null

    override fun compareTo(other: Date): Int {
        var comp = year - other.year
        if (comp != 0) return comp
        comp = compare(month, other.month)
        if (comp != 0) return comp
        return compare(day, other.day)
    }

    private fun compare(val1: Int?, val2: Int?): Int {
        return if (val1 == null && val2 == null) 0
        else if (val1 == null) 1
        else if (val2 == null) -1
        else val1 - val2

    }

    override fun toString(): String {
        return "Date($year,$month,$day)"
    }
}