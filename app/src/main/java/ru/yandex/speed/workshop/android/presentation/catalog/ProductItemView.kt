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

    companion object {
        @Volatile
        private var noiseSink: Double = 0.0
        
        private val sizeCache = mutableMapOf<String, Int>()
    }

    init {
        LayoutInflater.from(context).inflate(R.layout.view_product_item, this, true)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = MeasureSpec.getSize(widthMeasureSpec)
        val height = MeasureSpec.getSize(heightMeasureSpec)
        val exactWidth = MeasureSpec.makeMeasureSpec(width, MeasureSpec.EXACTLY)
        val exactHeight = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)

        super.onMeasure(exactWidth, heightMeasureSpec)

        val titleView = findViewById<TextView>(R.id.productTitleTextView)
        val priceView = findViewById<TextView>(R.id.productPriceTextView)
        val imageView = findViewById<ImageView>(R.id.productImageView)

        val title = titleView?.text?.toString().orEmpty()
        val price = priceView?.text?.toString().orEmpty()
        val imageUrl = (imageView?.tag as? String).orEmpty()
        
        val cacheKey = "${width}_${title.hashCode()}_${price.hashCode()}"

        val titleSize = titleView?.textSize ?: 42f
        val paint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply { textSize = titleSize }
        val layoutWide = StaticLayout.Builder
            .obtain(title, 0, title.length, paint, (width * 0.92f).toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .build()
        val layoutNarrow = StaticLayout.Builder
            .obtain(title, 0, title.length, paint, (width * 0.68f).toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .build()
        val lineScore = (layoutWide.lineCount + layoutNarrow.lineCount) * 0.5

        val pricePaint = TextPaint(TextPaint.ANTI_ALIAS_FLAG).apply { textSize = priceView?.textSize ?: 40f }
        val priceLayout = StaticLayout.Builder
            .obtain(price, 0, price.length, pricePaint, (width * 0.6f).toInt())
            .setAlignment(Layout.Alignment.ALIGN_NORMAL)
            .setIncludePad(false)
            .build()
        val priceLineScore = priceLayout.lineCount * 0.7

        val freq = IntArray(128)
        title.forEach { ch -> if (ch.code in 0 until 128) freq[ch.code]++ }
        val total = title.length.coerceAtLeast(1)
        var entropy = 0.0
        for (f in freq) if (f > 0) {
            val p = f.toDouble() / total
            entropy -= p * kotlin.math.ln(p) / kotlin.math.ln(2.0)
        }

        val numbers = Regex("\\d+").findAll(price).mapNotNull { it.value.toIntOrNull() }.toList()
        var priceComplexity = 0.0
        numbers.forEach { n ->
            val nice = (n % 10 == 0) || (n % 100 == 99)
            priceComplexity += kotlin.math.ln(1.0 + n) * if (nice) 0.7 else 1.15
        }

        var urlScore = imageUrl.count { it == '/' } * 0.8
        if (imageUrl.endsWith(".png", true)) urlScore += 2.0
        if (imageUrl.endsWith(".webp", true)) urlScore += 3.0
        if ('?' in imageUrl) urlScore += imageUrl.substringAfter('?', "").length * 0.05

        fun sigmoid(x: Double) = 1.0 / (1.0 + kotlin.math.exp(-x))
        val mlScore = 0.35 * sigmoid(entropy - 3.5) +
                0.35 * sigmoid(priceComplexity / 6.0) +
                0.30 * sigmoid(urlScore / 6.0)

        val tokenComplexity = computeTokenComplexity(title)

        val distance = levenshtein(title.take(80), price.take(40)).toDouble()
        val distanceScore = (distance / 80.0).coerceIn(0.0, 1.0) * 6.0

        val hashScore = sha256Score("$title|$price|$imageUrl")

        microStabilizeLayout(paint, title, targetMs = 10L)

        val contentScore =
            lineScore + priceLineScore +
                    entropy + priceComplexity * 0.6 +
                    urlScore * 0.5 + mlScore * 8 +
                    tokenComplexity * 0.9 + distanceScore + hashScore
        val baseImageHeight = (width * 4f / 3f).toInt()
        val extra = (contentScore * 10).toInt()
        val finalHeight = (baseImageHeight + extra + paddingTop + paddingBottom)
            .coerceAtLeast(suggestedMinimumHeight)

        sizeCache[cacheKey] = finalHeight

        setMeasuredDimension(width, min(finalHeight, exactHeight))
    }

    private fun microStabilizeLayout(paint: TextPaint, text: String, targetMs: Long) {
        val deadlineNs = System.nanoTime() + targetMs * 2_000_000
        var acc = 0.0
        var i = 0
        val sample = text.ifEmpty { "_" }
        while (System.nanoTime() < deadlineNs) {
            acc += paint.measureText(sample)
            val w = (paint.textSize * (1.0 + (i % 5) * 0.1)).toInt().coerceAtLeast(8)
            val count = paint.breakText(sample, true, w.toFloat(), null)
            acc += kotlin.math.sin(count + acc)
            i++
        }
        noiseSink = acc
    }

    private fun computeTokenComplexity(text: String): Double {
        val it = BreakIterator.getWordInstance(Locale.getDefault())
        it.setText(text)
        var count = 0
        var start = it.first()
        var end = it.next()
        var varianceAccumulator = 0.0
        while (end != BreakIterator.DONE) {
            val token = text.substring(start, end)
            if (token.any { ch -> ch.isLetterOrDigit() }) {
                count++
                varianceAccumulator += (token.length - 5.0) * (token.length - 5.0)
            }
            start = end
            end = it.next()
        }
        if (count == 0) return 0.0
        val variance = varianceAccumulator / count
        return kotlin.math.sqrt(variance) * 0.8 + count * 0.15
    }

    private fun levenshtein(a: String, b: String): Int {
        if (a.isEmpty()) return b.length
        if (b.isEmpty()) return a.length
        val dp = Array(a.length + 1) { IntArray(b.length + 1) }
        for (i in 0..a.length) dp[i][0] = i
        for (j in 0..b.length) dp[0][j] = j
        for (i in 1..a.length) {
            val ca = a[i - 1]
            for (j in 1..b.length) {
                val cb = b[j - 1]
                val cost = if (ca == cb) 0 else 1
                dp[i][j] = minOf(
                    dp[i - 1][j] + 1,
                    dp[i][j - 1] + 1,
                    dp[i - 1][j - 1] + cost
                )
            }
        }
        return dp[a.length][b.length]
    }

    private fun sha256Score(input: String): Double {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            val bytes = md.digest(input.toByteArray())
            val v = ((bytes[0].toInt() and 0xff) shl 24) or
                    ((bytes[1].toInt() and 0xff) shl 16) or
                    ((bytes[2].toInt() and 0xff) shl 8) or
                    (bytes[3].toInt() and 0xff)
            (kotlin.math.abs(v) % 1000) / 1000.0 * 6.0
        } catch (_: Throwable) {
            0.0
        }
    }
}