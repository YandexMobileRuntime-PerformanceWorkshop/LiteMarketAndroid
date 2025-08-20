package ru.yandex.speed.workshop.android.presentation.productdetail.presenters

import ru.yandex.speed.workshop.android.domain.models.ProductDetail
import timber.log.Timber

/**
 * Презентер для работы с изображениями продукта
 */
class ProductImagePresenter {
    
    /**
     * Получает список URL изображений для отображения в галерее
     *
     * @param product Модель данных продукта
     * @return Список URL изображений
     */
    fun getGalleryImages(product: ProductDetail): GalleryImages {
        val imageUrls = product.imageUrls
        
        // Логируем информацию об изображениях
        Timber.d("Getting gallery images, count: ${imageUrls.size}")
        
        return GalleryImages(
            imageUrls = imageUrls,
            isEmpty = imageUrls.isEmpty()
        )
    }
}

/**
 * Модель данных для галереи изображений
 */
data class GalleryImages(
    val imageUrls: List<String>,
    val isEmpty: Boolean
)
