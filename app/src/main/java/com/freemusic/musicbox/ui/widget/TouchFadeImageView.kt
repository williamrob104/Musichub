package com.freemusic.musicbox.ui.widget

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.widget.AppCompatImageView
import com.freemusic.musicbox.R


class TouchFadeImageView: AppCompatImageView {

    private var sizeFactor = 1.0f
    private var alphaFactor = 1.0f
    private var duration = 200L
    private var delay = 0L
    private var touchable = true

    private val rect = Rect()
    private var released = true


    constructor(context: Context): super(context)

    constructor(context: Context, attrs: AttributeSet?): super(context, attrs)  {
        extractAttrs(attrs, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int):
            super(context, attrs, defStyleAttr) {
        extractAttrs(attrs, defStyleAttr, 0)
    }

    private fun extractAttrs(attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.TouchFadeView, defStyleAttr, defStyleRes)
        try {
            sizeFactor = a.getFloat(R.styleable.TouchFadeView_fade_size_factor, sizeFactor)
            alphaFactor = a.getFloat(R.styleable.TouchFadeView_fade_alpha_factor, sizeFactor)
            duration = a.getString(R.styleable.TouchFadeView_fade_duration)?.toLong() ?: duration
            delay = a.getString(R.styleable.TouchFadeView_fade_delay)?.toLong() ?: delay
        }
        finally {
            a.recycle()
        }
    }


    fun setTouchable(touchable: Boolean) {
        if (this.touchable == touchable)
            return

        this.touchable = touchable
        if (!touchable) {
            this.scaleX = sizeFactor
            this.scaleY = sizeFactor
            this.alpha = alphaFactor
        }
        else {
            this.scaleX = 1.0f
            this.scaleY = 1.0f
            this.alpha = 1.0f
        }
        invalidate()
    }

    private fun View.animateRelease() {
        this.animate().cancel()
        this.animate()
            .scaleX(1.0f)
            .scaleY(1.0f)
            .alpha(1.0f)
            .setDuration(duration)
            .setStartDelay(0)
            .start()
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (!touchable)
            return false
            
        when (event?.action) {
            MotionEvent.ACTION_DOWN -> {
                rect.set(left, top, right, bottom)
                released = false
                this.animate()
                    .scaleX(sizeFactor)
                    .scaleY(sizeFactor)
                    .alpha(alphaFactor)
                    .setDuration(duration)
                    .setStartDelay(delay)
                    .start()
                return true
            }
            MotionEvent.ACTION_UP -> {
                if (!released) {
                    released = true
                    this.animateRelease()
                    this.performClick()
                    return true
                }
            }
            MotionEvent.ACTION_CANCEL -> {
                if (!released) {
                    released = true
                    this.animateRelease()
                    return true
                }
            }
            MotionEvent.ACTION_MOVE -> {
                if (!released && !rect.contains((left + event.x).toInt(), (top + event.y).toInt())) {
                    released = true
                    this.animateRelease()
                    return true
                }
            }
        }
        return false
    }
}