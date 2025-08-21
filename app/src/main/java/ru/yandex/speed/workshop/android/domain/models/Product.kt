package ru.yandex.speed.workshop.android.domain.models

/**
 * Чистая доменная модель товара
 */
data class Product(
    val id: String,
    val title: String,
    val currentPrice: String,
    val oldPrice: String? = null,
    val discountPercent: Int? = null,
    val imageUrls: List<String> = emptyList(),
    val manufacturer: String = "Unknown",
    val seller: String = "Unknown",
    val delivery: Delivery? = null,
    val promoCode: PromoCode? = null,
    val rating: Rating? = null,
    var isFavorite: Boolean = false,
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
