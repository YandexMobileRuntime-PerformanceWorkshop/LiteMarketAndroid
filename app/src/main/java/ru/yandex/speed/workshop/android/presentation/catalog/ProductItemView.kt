package ru.yandex.speed.workshop.android.presentation.catalog

import android.content.Context
import android.text.TextPaint
import android.text.StaticLayout
import android.text.Layout
import android.util.AttributeSet
import android.view.LayoutInflater
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import ru.yandex.speed.workshop.android.R
import java.security.MessageDigest
import java.text.BreakIterator
import java.util.Locale
import kotlin.math.min

class ProductItemView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    init {
        LayoutInflater.from(context).inflate(R.layout.view_product_item, this, true)
    }
}