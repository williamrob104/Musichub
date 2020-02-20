package com.musichub.ui.widget

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import com.musichub.R


class RoundedTouchFadeFrameLayout: TouchFadeFrameLayout {

    private var _radius: Float = 0f
    private var _bgColor: Int = Color.TRANSPARENT
    private var _strokeColor: Int = Color.TRANSPARENT
    private var _strokeWidth: Int = 2

    constructor(context: Context): super(context) {
        drawRounded()
    }

    constructor(context: Context, attrs: AttributeSet?): super(context, attrs) {
        extractAttrs(attrs, 0, 0)
        drawRounded()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int):
            super(context, attrs, defStyleAttr) {
        extractAttrs(attrs, defStyleAttr, 0)
        drawRounded()
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int):
            super(context, attrs, defStyleAttr, defStyleRes) {
        extractAttrs(attrs, defStyleAttr, defStyleRes)
        drawRounded()
    }

    private fun extractAttrs(attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        val a = context.theme.obtainStyledAttributes(attrs, R.styleable.RoundedView, defStyleAttr, defStyleRes)
        try {
            _radius = a.getDimension(R.styleable.RoundedView_rounded_radius, _radius)
            _bgColor = a.getColor(R.styleable.RoundedView_rounded_background_color, _bgColor)
            _strokeColor = a.getColor(R.styleable.RoundedView_rounded_stroke_color, _strokeColor)
            _strokeWidth = a.getDimension(R.styleable.RoundedView_rounded_stroke_width, _strokeWidth.toFloat()).toInt()
        }
        finally {
            a.recycle()
        }
    }

    var roundedRadius: Float
        get() = _radius
        set(value) {
            this._radius = value
            drawRounded()
        }

    var roundedBackgroundColor: Int
        get() = _bgColor
        set(value) {
            this._bgColor = value
            drawRounded()
        }

    var roundedStrokeColor: Int
        get() = _strokeColor
        set(value) {
            this._strokeColor = value
            drawRounded()
        }

    var roundedStrokeWidth: Int
        get() = _strokeWidth
        set(value) {
            this._strokeWidth = value
            drawRounded()
        }

    private fun drawRounded() {
        this.background = GradientDrawable().apply {
            cornerRadius = _radius
            setColor(_bgColor)
            setStroke(_strokeWidth, _strokeColor)
        }
    }
}