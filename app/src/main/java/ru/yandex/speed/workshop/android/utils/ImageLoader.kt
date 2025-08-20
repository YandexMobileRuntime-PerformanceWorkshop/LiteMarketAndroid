package ru.yandex.speed.workshop.android.utils

import android.app.Application
import android.content.Context
import android.widget.ImageView
import com.bumptech.glide.Glide
import com.bumptech.glide.MemoryCategory
import com.bumptech.glide.load.engine.DiskCacheStrategy
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.yandex.speed.workshop.android.R
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Централизованный загрузчик изображений с оптимизациями
 */
@Singleton
class ImageLoader
    @Inject
    constructor(private val context: Application) {
        init {
            Glide.get(context).apply {
                setMemoryCategory(MemoryCategory.HIGH)
            }
            Timber.d("ImageLoader initialized with optimized settings")
        }

        /**
         * Загрузка изображения продукта в каталоге
         */
        fun loadCatalogImage(
            imageView: ImageView,
            url: String,
            width: Int,
            height: Int,
        ) {
            Glide.with(imageView)
                .load(url)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder)
                .override(width, height)
                .centerCrop()
                .thumbnail(0.25f)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageView)
        }

        /**
         * Загрузка первого изображения в детальной карточке (без placeholder для избежания мигания)
         */
        fun loadDetailFirstImage(
            imageView: ImageView,
            url: String,
        ) {
            Glide.with(imageView)
                .load(url)
                .error(R.drawable.ic_placeholder_large)
                .fitCenter()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .into(imageView)
        }

        /**
         * Загрузка остальных изображений в детальной карточке
         */
        fun loadDetailImage(
            imageView: ImageView,
            url: String,
        ) {
            Glide.with(imageView)
                .load(url)
                .placeholder(R.drawable.ic_placeholder_large)
                .error(R.drawable.ic_placeholder_large)
                .fitCenter()
                .diskCacheStrategy(DiskCacheStrategy.ALL)
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
        ) {
            Glide.with(context)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
                .preload()
        }

        /**
         * Предзагрузка изображения (используя контекст приложения)
         */
        fun preloadImage(url: String) {
            Glide.with(context)
                .load(url)
                .diskCacheStrategy(DiskCacheStrategy.ALL)
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
    }
