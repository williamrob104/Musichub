package com.musichub.util

import android.graphics.Bitmap


internal fun Bitmap.squareCropTop(): Bitmap {
    if (width == height)
        return this
    else if (width > height) {
        val size = height
        val x = (width - size) / 2
        return Bitmap.createBitmap(this, x, 0, size, size)
    } else {
        val size = width
        return Bitmap.createBitmap(this, 0, 0, size, size)
    }
}