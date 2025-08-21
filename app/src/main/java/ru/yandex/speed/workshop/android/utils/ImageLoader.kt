package ru.yandex.speed.workshop.android.utils

import android.app.Application
import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.MemoryCategory
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.yandex.speed.workshop.android.R
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Централизованный загрузчик изображений с оптимизациями
 */
/**
 * Enum для типов изображений - для категоризации и анализа метрик
 */
enum class ImageType {
    CATALOG_THUMBNAIL,     // Миниатюры в каталоге
    DETAIL_FIRST,         // Первое изображение в детальной карточке
    DETAIL_OTHER,         // Остальные изображения в галерее
    PRELOADED             // Предзагруженные изображения
}

@Singleton
class ImageLoader
    @Inject
    constructor(
        private val context: Application,
        private val performanceMetricManager: PerformanceMetricManager
    ) {
        init {
            Glide.get(context).apply {
                setMemoryCategory(MemoryCategory.HIGH)
            }
            Timber.d("ImageLoader initialized with optimized settings")
        }
        
        // Хранит время начала загрузки для каждого URL
        private val imageLoadStartTimes = HashMap<String, Long>()
        
        // Коллбек для измерения времени загрузки изображений
        private fun createImageLoadListener(
            startTimeNanos: Long,
            url: String,
            imageType: ImageType,
            trackingId: String? = null
        ): RequestListener<Drawable> {
            // Сохраняем время начала для этого конкретного URL
            val uniqueKey = "$url-$trackingId"
            imageLoadStartTimes[uniqueKey] = startTimeNanos
            
            return object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable>,
                    isFirstResource: Boolean
                ): Boolean {
                    // Получаем сохраненное время начала для этого конкретного URL
                    val actualStartTime = imageLoadStartTimes.remove(uniqueKey) ?: startTimeNanos
                    val loadTimeMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - actualStartTime)
                    
                    // Записываем метрику с ошибкой
                    performanceMetricManager.recordMetric(
                        name = "ImageLoadFailed",
                        valueMs = loadTimeMs,
                        context = mapOf(
                            "url" to url,
                            "type" to imageType.name,
                            "error" to (e?.message ?: "unknown error"),
                            "tracking_id" to (trackingId ?: "none")
                        )
                    )
                    
                    return false // Позволяем Glide обработать ошибку дальше
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable>,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    // Получаем сохраненное время начала для этого конкретного URL
                    val actualStartTime = imageLoadStartTimes.remove(uniqueKey) ?: startTimeNanos
                    val loadTimeMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - actualStartTime)
                    
                    // Записываем метрику успешной загрузки
                    performanceMetricManager.recordMetric(
                        name = "ImageLoadTime",
                        valueMs = loadTimeMs,
                        context = mapOf(
                            "url" to url,
                            "type" to imageType.name,
                            "data_source" to dataSource.name,
                            "is_first_resource" to isFirstResource.toString(),
                            "tracking_id" to (trackingId ?: "none")
                        )
                    )
                    
                    // Для первого изображения в детальной карточке также логируем как отдельную метрику
                    if (imageType == ImageType.DETAIL_FIRST) {
                        Timber.tag("Performance").i("ProductDetail: FirstImageLoadTime = ${loadTimeMs}ms")
                    }
                    
                    return false // Позволяем Glide продолжить обработку
                }
            }
        }

        /**
         * Загрузка изображения продукта в каталоге
         */
        fun loadCatalogImage(
            imageView: ImageView,
            url: String,
            width: Int,
            height: Int,
            trackingId: String? = null
        ) {
            val startTimeNanos = System.nanoTime()
            
            Glide.with(imageView)
                .load(url)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder)
                .override(width, height)
                .centerCrop()
                .thumbnail(0.25f)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .listener(createImageLoadListener(startTimeNanos, url, ImageType.CATALOG_THUMBNAIL, trackingId))
                .into(imageView)
        }

        /**
         * Загрузка первого изображения в детальной карточке (без placeholder для избежания мигания)
         */
        fun loadDetailFirstImage(
            imageView: ImageView,
            url: String,
            trackingId: String? = null
        ) {
            val startTimeNanos = System.nanoTime()
            
            Glide.with(imageView)
                .load(url)
                .error(R.drawable.ic_placeholder_large)
                .fitCenter()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .listener(createImageLoadListener(startTimeNanos, url, ImageType.DETAIL_FIRST, trackingId))
                .into(imageView)
        }

        /**
         * Загрузка остальных изображений в детальной карточке
         */
        fun loadDetailImage(
            imageView: ImageView,
            url: String,
            trackingId: String? = null
        ) {
            val startTimeNanos = System.nanoTime()
            
            Glide.with(imageView)
                .load(url)
                .placeholder(R.drawable.ic_placeholder_large)
                .error(R.drawable.ic_placeholder_large)
                .fitCenter()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .listener(createImageLoadListener(startTimeNanos, url, ImageType.DETAIL_OTHER, trackingId))
                .into(imageView)
        }

        /**
         * Параллельная загрузка изображений в детальной карточке
         */
        suspend fun loadDetailImagesParallel(
            imageViews: List<ImageView>,
            urls: List<String>,
        ) {
            withContext(Dispatchers.IO) {
                urls.zip(imageViews).map { (url, imageView) ->
                    withContext(Dispatchers.Main) {
                        loadDetailImage(imageView, url)
                    }
                }
            }
        }

        /**
         * Предзагрузка изображения
         */
        fun preloadImage(
            context: Context,
            url: String,
            trackingId: String? = null
        ) {
            val startTimeNanos = System.nanoTime()
            
            Glide.with(context)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .listener(createImageLoadListener(startTimeNanos, url, ImageType.PRELOADED, trackingId))
                .preload()
        }

        /**
         * Предзагрузка изображения (используя контекст приложения)
         */
        fun preloadImage(
            url: String,
            trackingId: String? = null
        ) {
            val startTimeNanos = System.nanoTime()
            
            Glide.with(context)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .listener(createImageLoadListener(startTimeNanos, url, ImageType.PRELOADED, trackingId))
                .preload()
        }

        /**
         * Предзагрузка списка изображений
         */
        fun preloadImages(urls: List<String>) {
            urls.forEach { url ->
                preloadImage(url)
            }
        }

        /**
         * Параллельная предзагрузка списка изображений
         */
        suspend fun preloadImagesParallel(urls: List<String>) {
            withContext(Dispatchers.IO) {
                urls.map { url ->
                    withContext(Dispatchers.IO) {
                        preloadImage(url)
                    }
                }
            }
        }

        /**
         * Очистка ресурсов при необходимости
         */
        fun clearMemory() {
            Glide.get(context).clearMemory()
        }
        
        /**
         * Анализ метрик загрузки изображений
         * Выводит статистику по разным типам изображений
         */
        fun analyzeImageLoadingMetrics() {
            val metrics = performanceMetricManager.getAllMetrics()
            val imageLoadMetrics = metrics.filter { it.name == "ImageLoadTime" }
            
            if (imageLoadMetrics.isEmpty()) {
                Timber.tag("Performance").i("No image loading metrics recorded yet")
                return
            }
            
            Timber.tag("Performance").i("=== IMAGE LOADING METRICS SUMMARY ===")
            Timber.tag("Performance").i("Total images loaded: ${imageLoadMetrics.size}")
            
            // Группировка по типу изображений
            val groupedByType = imageLoadMetrics.groupBy { 
                it.context["type"]?.toString() ?: "unknown" 
            }
            
            // Анализ для каждого типа
            for ((type, typeMetrics) in groupedByType) {
                val values = typeMetrics.map { it.value }
                val avg = values.average()
                val min = values.minOrNull() ?: 0
                val max = values.maxOrNull() ?: 0
                
                Timber.tag("Performance").i(
                    "$type: ${typeMetrics.size} images, " +
                    "avg=${String.format("%.2f", avg)}ms, " +
                    "min=${min}ms, " +
                    "max=${max}ms"
                )
                
                // Анализ по источнику данных
                val groupedBySource = typeMetrics.groupBy {
                    it.context["data_source"]?.toString() ?: "unknown"
                }
                
                for ((source, sourceMetrics) in groupedBySource) {
                    val sourceValues = sourceMetrics.map { it.value }
                    val sourceAvg = sourceValues.average()
                    
                    Timber.tag("Performance").i(
                        "  - $source: ${sourceMetrics.size} images, " +
                        "avg=${String.format("%.2f", sourceAvg)}ms, " +
                        "${sourceMetrics.size * 100 / typeMetrics.size}% of $type"
                    )
                }
            }
            
            Timber.tag("Performance").i("===============================")
        }
    }
