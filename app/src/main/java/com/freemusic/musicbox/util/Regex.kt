package com.freemusic.musicbox.util


internal fun regexSearch(regex: Regex, input: String, group: Int?=null): String {
    val match = regex.find(input)
    if (match != null) {
        return if (group == null) match.value
        else match.groups[group]?.value ?: throw IllegalArgumentException("regex group error")
    }
    else
        throw IllegalArgumentException("regex match error")
}

internal fun regexSearch(regexCandidates: List<Regex>, input: String, group: Int?=null): String {
    for (regex in regexCandidates) {
        val match = regex.find(input)
        if (match != null)
            return if (group == null) match.value
            else match.groups[group]?.value ?: throw IllegalArgumentException("regex group error")
    }
    throw IllegalArgumentException("regex match error")
}