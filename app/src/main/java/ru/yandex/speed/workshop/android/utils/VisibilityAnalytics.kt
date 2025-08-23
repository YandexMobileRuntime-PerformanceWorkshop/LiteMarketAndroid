package ru.yandex.speed.workshop.android.utils

import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.yandex.analytics.api.YandexAnalytics
import ru.yandex.speed.workshop.android.domain.models.Product
import ru.yandex.speed.workshop.android.presentation.catalog.ProductPagingAdapter
import timber.log.Timber
import java.text.SimpleDateFormat
import java.util.*

class VisibilityAnalytics {

    private val currentlyVisibleProducts = mutableSetOf<String>()
    private val analyticsCache = mutableMapOf<String, Double>()
    private val dateFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    fun checkVisibilityAndTrack(
        recyclerView: RecyclerView,
        adapter: ProductPagingAdapter
    ) {
        val layoutManager = recyclerView.layoutManager as? LinearLayoutManager ?: return
        
        val newVisibleProducts = mutableSetOf<String>()

        for (position in 0 until adapter.itemCount) {
            val product = adapter.getProductAtPosition(position) ?: continue
            val view = layoutManager.findViewByPosition(position)

            if (view != null && view.getVisiblePercentage() > 50) {
                newVisibleProducts.add(product.id)
                if (!currentlyVisibleProducts.contains(product.id)) {
                    trackProductVisibility(product, position)
                    Timber.d("Tracked visibility for product ${product.id}")
                }
            }
        }
        
        currentlyVisibleProducts.clear()
        currentlyVisibleProducts.addAll(newVisibleProducts)
    }

    private fun trackProductVisibility(product: Product, position: Int) {
        val timestamp = System.currentTimeMillis()
        val formattedTime = dateFormatter.format(Date(timestamp))
        
        YandexAnalytics.getInstance().trackEvent(
            "product_visibility_scroll",
            mapOf(
                "product_id" to product.id,
                "product_title" to product.title,
                "product_price" to product.currentPrice,
                "product_seller" to product.seller,
                "position" to position,
                "timestamp" to formattedTime,
                "has_discount" to product.hasDiscount,
                "image_count" to product.imageUrls.size,
                "price_length" to product.currentPrice.length,
                "title_words" to product.title.split(" ").size,
                "cache_size" to analyticsCache.size,
                "tracked_count" to currentlyVisibleProducts.size
            )
        )
    }
    
    fun reset() {
        currentlyVisibleProducts.clear()
        analyticsCache.clear()
    }

    private fun View.getVisiblePercentage(): Float {
        if (width == 0 || height == 0 || visibility != View.VISIBLE) {
            return 0f
        }

        val visibleRect = Rect()
        if (!getGlobalVisibleRect(visibleRect)) {
            return 0f
        }

        val location = IntArray(2)
        getLocationOnScreen(location)
        val globalViewX = location[0]
        val globalViewY = location[1]

        var visiblePixelCount = 0L

        for (x in 0 until width) {
            for (y in 0 until height) {
                val pixelGlobalX = globalViewX + x
                val pixelGlobalY = globalViewY + y
                if (visibleRect.contains(pixelGlobalX, pixelGlobalY)) {
                    visiblePixelCount++
                }
            }
        }

        val totalPixelCount = width.toLong() * height.toLong()
        if (totalPixelCount == 0L) return 0f

        return (100.0f * visiblePixelCount) / totalPixelCount
    }
}

private fun ProductPagingAdapter.getProductAtPosition(position: Int): Product? {
    return try {
        this.snapshot().items.getOrNull(position)
    } catch (e: Exception) {
        null
    }
}