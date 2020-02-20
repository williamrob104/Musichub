package com.musichub.ui.widget

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import com.musichub.R


class RoundedTouchFadeTextView: TouchFadeTextView {

    private var radius: Float = 0f
    private var bgColor: Int = Color.TRANSPARENT
    private var strokeColor: Int = Color.TRANSPARENT
    private var strokeWidth: Int = 2

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
            radius = a.getDimension(R.styleable.RoundedView_rounded_radius, radius)
            bgColor = a.getColor(R.styleable.RoundedView_rounded_background_color, bgColor)
            strokeColor = a.getColor(R.styleable.RoundedView_rounded_stroke_color, strokeColor)
            strokeWidth = a.getDimension(R.styleable.RoundedView_rounded_stroke_width, strokeWidth.toFloat()).toInt()
        }
        finally {
            a.recycle()
        }
    }

    fun setRoundedRadius(radius: Float) {
        this.radius = radius
        drawRounded()
    }

    fun setRoundedBackGroundColor(bgColor: Int) {
        this.bgColor = bgColor
        drawRounded()
    }

    fun setRoundedStroke(strokeColor: Int, strokeWidth: Int) {
        this.strokeColor = strokeColor
        this.strokeWidth = strokeWidth
        drawRounded()
    }

    private fun drawRounded() {
        this.background = GradientDrawable().apply {
            cornerRadius = radius
            setColor(bgColor)
            setStroke(strokeWidth, strokeColor)
        }
    }
}