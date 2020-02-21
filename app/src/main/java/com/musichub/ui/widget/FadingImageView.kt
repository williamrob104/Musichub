package com.musichub.ui.widget

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView
import com.musichub.R


class FadingImageView : AppCompatImageView {

    var fadeLeft: Boolean = false
    var fadeRight: Boolean = false
    var fadeTop: Boolean = false
    var fadeBottom: Boolean = false

    private val defautFadingWidth = 30

    constructor(context: Context) : super(context) {
        this.setFadingEdgeLength(defautFadingWidth)
    }

    constructor(context: Context, attrs: AttributeSet?) : super(context, attrs) {
        extractAttrs(attrs, 0, 0)
    }

    constructor(context: Context, attrs: AttributeSet?, defStyleAttr: Int) :
            super(context, attrs, defStyleAttr) {
        extractAttrs(attrs, defStyleAttr, 0)
    }

    private fun extractAttrs(attrs: AttributeSet?, defStyleAttr: Int, defStyleRes: Int) {
        val a = context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.FadingImageView,
            defStyleAttr,
            defStyleRes
        )
        try {
            val sidesInt = a.getInt(R.styleable.FadingImageView_fadingSides, 0)
            fadeLeft = (sidesInt and (1 shl 0)) != 0
            fadeRight = (sidesInt and (1 shl 1)) != 0
            fadeTop = (sidesInt and (1 shl 2)) != 0
            fadeBottom = (sidesInt and (1 shl 3)) != 0

            val fadingWidth = a.getDimensionPixelSize(
                R.styleable.FadingImageView_fadingEdgeWidth,
                defautFadingWidth
            )
            this.setFadingEdgeLength(fadingWidth)
        } finally {
            a.recycle()
        }
    }

    init {
        this.isHorizontalFadingEdgeEnabled = true
        this.isVerticalFadingEdgeEnabled = true
    }

    override fun getLeftFadingEdgeStrength(): Float {
        return if (fadeLeft) 1f else 0f
    }

    override fun getRightFadingEdgeStrength(): Float {
        return if (fadeRight) 1f else 0f
    }

    override fun getTopFadingEdgeStrength(): Float {
        return if (fadeTop) 1f else 0f
    }

    override fun getBottomFadingEdgeStrength(): Float {
        return if (fadeBottom) 1f else 0f
    }

    override fun hasOverlappingRendering(): Boolean {
        return true
    }

    override fun onSetAlpha(alpha: Int): Boolean {
        return false
    }

}