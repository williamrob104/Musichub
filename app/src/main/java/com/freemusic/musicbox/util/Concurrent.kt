package com.freemusic.musicbox.util

import com.freemusic.musicbox.concurrent.ResponseListener


internal fun <FromT, ToT> pipeResponse(listener: ResponseListener<ToT>, conversionFunc: (FromT)->ToT)
        : ResponseListener<FromT> {
    return object: ResponseListener<FromT> {
        override fun onResponse(response: FromT) {
            var errorOccurred = false
            val convertedResponse = try {
                conversionFunc(response)
            }
            catch (e: Exception) {
                errorOccurred = true
                listener.onError(e)
            }
            if (!errorOccurred) {
                @Suppress("UNCHECKED_CAST")
                listener.onResponse(convertedResponse as ToT)
            }
        }

        override fun onError(error: Exception) {
            listener.onError(error)
        }
    }
}