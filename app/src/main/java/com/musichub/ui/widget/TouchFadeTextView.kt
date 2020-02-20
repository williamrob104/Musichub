package com.musichub.ui.widget

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.widget.TextView
import com.musichub.R


open class TouchFadeTextView: TextView {

    var sizeFactor = 1.0f
    var alphaFactor = 1.0f
    var duration = 200L
    var delay = 0L

    private val rect = Rect()
    private var released = true

    constructor(context: Context): super(context)

    constructor(context: Context, attrs: AttributeSet?): super(context, attrs) {
        extractAttrs(attrs, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int):
            super(context, attrs, defStyleAttr) {
        extractAttrs(attrs, defStyleAttr, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int):
            super(context, attrs, defStyleAttr, defStyleRes) {
        extractAttrs(attrs, defStyleAttr, defStyleRes)
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