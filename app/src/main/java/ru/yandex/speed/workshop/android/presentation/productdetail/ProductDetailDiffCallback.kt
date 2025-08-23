package ru.yandex.speed.workshop.android.presentation.productdetail

import ru.yandex.speed.workshop.android.domain.models.ProductDetail
import ru.yandex.speed.workshop.android.utils.NumberUtils

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
        return oldItem.title != newItem.title || oldItem.manufacturer != newItem.manufacturer
    }

    /**
     * Проверяет, изменились ли данные о цене
     */
    fun hasPriceInfoChanged(): Boolean {
        // Проверяем цену, старую цену и скидку
        return oldItem.currentPrice != newItem.currentPrice ||
            oldItem.oldPrice != newItem.oldPrice ||
            oldItem.discountPercent != newItem.discountPercent ||
            oldItem.discountPercentage != newItem.discountPercentage
    }

    /**
     * Проверяет, изменились ли данные о рейтинге
     */
    fun hasRatingChanged(): Boolean {
        // Форматируем рейтинг до одного знака после запятой для корректного сравнения
        // Используем NumberUtils для форматирования с локалью US, чтобы всегда использовать точку как разделитель
        val oldScore = NumberUtils.formatDouble(oldItem.rating.score, 1)
        val newScore = NumberUtils.formatDouble(newItem.rating.score, 1)

        // Сравниваем форматированные строки и количество отзывов
        return oldScore != newScore ||
            oldItem.rating.reviewsCount != newItem.rating.reviewsCount
    }

    /**
     * Проверяет, изменились ли данные о продавце
     */
    fun hasSellerChanged(): Boolean {
        return oldItem.seller != newItem.seller ||
            oldItem.sellerRating != newItem.sellerRating ||
            oldItem.sellerReviewsCount != newItem.sellerReviewsCount
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
        // Сначала проверяем размер списков
        if (oldItem.imageUrls.size != newItem.imageUrls.size) {
            return true
        }

        // Затем проверяем содержимое
        for (i in oldItem.imageUrls.indices) {
            if (oldItem.imageUrls[i] != newItem.imageUrls[i]) {
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
