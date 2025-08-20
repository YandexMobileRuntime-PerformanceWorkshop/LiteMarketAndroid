package ru.yandex.speed.workshop.android.presentation.productdetail

import ru.yandex.speed.workshop.android.domain.models.ProductDetail

/**
 * DiffCallback для сравнения объектов ProductDetail
 * Используется для определения изменившихся полей при обновлении данных
 */
class ProductDetailDiffCallback(
    private val oldItem: ProductDetail,
    private val newItem: ProductDetail,
) {
    /**
     * Проверяет, изменились ли основные данные продукта
     */
    fun hasBasicInfoChanged(): Boolean {
        // Получаем имя производителя из разных источников
        val oldManufacturerName = oldItem.manufacturer.name.takeIf { it.isNotEmpty() } ?: oldItem.vendor ?: "Unknown"
        val newManufacturerName = newItem.manufacturer.name.takeIf { it.isNotEmpty() } ?: newItem.vendor ?: "Unknown"
        
        return oldItem.title != newItem.title || oldManufacturerName != newManufacturerName
    }

    /**
     * Проверяет, изменились ли данные о цене
     */
    fun hasPriceInfoChanged(): Boolean {
        // Проверяем как поля из API, так и вложенный объект price
        val oldPrice = oldItem.currentPrice ?: oldItem.price.currentPrice
        val newPrice = newItem.currentPrice ?: newItem.price.currentPrice
        
        if (oldPrice != newPrice) {
            return true
        }
        
        // Проверяем старую цену и скидку
        if (oldItem.oldPrice != newItem.oldPrice || 
            oldItem.discountPercent != newItem.discountPercent) {
            return true
        }
        
        // Проверяем вложенный объект price
        return oldItem.price.currentPrice != newItem.price.currentPrice ||
            oldItem.price.oldPrice != newItem.price.oldPrice ||
            oldItem.price.discountPercentage != newItem.price.discountPercentage
    }

    /**
     * Проверяет, изменились ли данные о рейтинге
     */
    fun hasRatingChanged(): Boolean {
        // Форматируем рейтинг до одного знака после запятой для корректного сравнения
        val oldScore = oldItem.rating.score?.let { String.format("%.1f", it) }
        val newScore = newItem.rating.score?.let { String.format("%.1f", it) }
        
        return oldScore != newScore ||
            oldItem.rating.reviewsCount != newItem.rating.reviewsCount
    }

    /**
     * Проверяет, изменились ли данные о продавце
     */
    fun hasSellerChanged(): Boolean {
        // Получаем имя продавца из разных источников
        val oldSellerName = oldItem.seller.name.takeIf { it.isNotEmpty() } ?: oldItem.shopName ?: "Яндекс Фабрика"
        val newSellerName = newItem.seller.name.takeIf { it.isNotEmpty() } ?: newItem.shopName ?: "Яндекс Фабрика"
        
        return oldSellerName != newSellerName ||
            oldItem.seller.rating != newItem.seller.rating ||
            oldItem.seller.reviewsCount != newItem.seller.reviewsCount
    }

    /**
     * Проверяет, изменились ли данные о промокоде
     */
    fun hasPromoCodeChanged(): Boolean {
        // Если оба null или оба не null, проверяем содержимое
        if ((oldItem.promoCode == null && newItem.promoCode == null) ||
            (oldItem.promoCode != null && newItem.promoCode != null)
        ) {
            return oldItem.promoCode != newItem.promoCode
        }
        // Если один null, а другой нет - значит изменились
        return true
    }

    /**
     * Проверяет, изменились ли изображения продукта
     */
    fun hasImagesChanged(): Boolean {
        // Проверяем изображения в обоих полях: images и picture_urls

        // Сначала определяем списки изображений для сравнения
        val oldImages =
            when {
                oldItem.picture_urls.isNotEmpty() -> oldItem.picture_urls
                oldItem.images.isNotEmpty() -> oldItem.images
                else -> emptyList()
            }

        val newImages =
            when {
                newItem.picture_urls.isNotEmpty() -> newItem.picture_urls
                newItem.images.isNotEmpty() -> newItem.images
                else -> emptyList()
            }

        // Сначала проверяем размер списков
        if (oldImages.size != newImages.size) {
            return true
        }

        // Затем проверяем содержимое
        for (i in oldImages.indices) {
            if (oldImages[i] != newImages[i]) {
                return true
            }
        }
        return false
    }

    /**
     * Проверяет, изменились ли данные о доставке
     */
    fun hasDeliveryChanged(): Boolean {
        if (oldItem.delivery.provider != newItem.delivery.provider) {
            return true
        }

        // Проверяем опции доставки
        if (oldItem.delivery.options.size != newItem.delivery.options.size) {
            return true
        }

        // Сравниваем каждую опцию доставки
        for (i in oldItem.delivery.options.indices) {
            val oldOption = oldItem.delivery.options[i]
            val newOption = newItem.delivery.options[i]

            if (oldOption.type != newOption.type ||
                oldOption.date != newOption.date ||
                oldOption.details != newOption.details ||
                oldOption.isSelected != newOption.isSelected
            ) {
                return true
            }
        }
        return false
    }

    /**
     * Проверяет, изменилось ли состояние избранного
     */
    fun hasFavoriteChanged(): Boolean {
        return oldItem.isFavorite != newItem.isFavorite
    }
}
