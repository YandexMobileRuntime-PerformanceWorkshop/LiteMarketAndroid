package ru.yandex.speed.workshop.android.domain.models

/**
 * Чистая доменная модель детальной информации о товаре
 */
data class ProductDetail(
    val id: String,
    val title: String,
    val currentPrice: String,
    val oldPrice: String? = null,
    val discountPercent: Int? = null,
    val imageUrls: List<String> = emptyList(),
    val manufacturer: String = "Unknown",
    val seller: String = "Unknown",
    val rating: ProductRating = ProductRating(),
    val isFavorite: Boolean = false,
    val promoCode: PromoCode? = null,
    val delivery: ProductDelivery = ProductDelivery(),
    // Дополнительные поля для детальной информации, если потребуются
    val paymentMethod: String = "",
    val alternativePrice: String? = null,
    val sellerLogo: String = "",
    val sellerRating: Double = 0.0,
    val sellerReviewsCount: String = "",
    val isSellerFavorite: Boolean = false,
) {
    /**
     * Computed property для получения первого URL изображения
     */
    val url: String?
        get() = imageUrls.firstOrNull()

    /**
     * Computed property для получения процента скидки как строки
     */
    val discountPercentage: String?
        get() = discountPercent?.toString()

    /**
     * Есть ли скидка на товар
     */
    val hasDiscount: Boolean
        get() = discountPercent != null && discountPercent > 0
}

/**
 * Модель рейтинга товара
 */
data class ProductRating(
    val score: Double = 0.0,
    val reviewsCount: Int = 0,
)

/**
 * Модель доставки товара
 */
data class ProductDelivery(
    val provider: String = "",
    val options: List<ProductDeliveryOption> = emptyList(),
)

/**
 * Модель опции доставки
 */
data class ProductDeliveryOption(
    val type: String = "",
    val date: String = "",
    val details: String = "",
    val isSelected: Boolean = false,
)

/**
 * Модель характеристики товара
 */
data class ProductFeature(
    val iconName: String,
    val title: String,
)
