package com.example.cravings.utils

import android.content.Context
import android.util.AttributeSet
import androidx.appcompat.widget.AppCompatImageView

class BottomCropImageView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyle: Int = 0
) : AppCompatImageView(context, attrs, defStyle) {

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)

        drawable?.let { drawable ->
            val imageWidth = drawable.intrinsicWidth
            val imageHeight = drawable.intrinsicHeight

            val viewWidth = w.toFloat()
            val viewHeight = h.toFloat()

            val scale = viewWidth / imageWidth
            val scaledHeight = imageHeight * scale

            val dy = viewHeight - scaledHeight

            imageMatrix.setScale(scale, scale)
            imageMatrix.postTranslate(0f, dy)
        }
    }
}
