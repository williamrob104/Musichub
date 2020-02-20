package com.musichub.util

import org.json.JSONObject


internal fun JSONObject.getStringOrNull(key: String): String? {
    return if (!this.isNull(key)) this.getString(key)
    else null
}

internal fun JSONObject.getStringOrNull(vararg keyCandidates: String): String? {
    for (key in keyCandidates)
        if (!this.isNull(key))
            return this.getString(key)
    return null
}