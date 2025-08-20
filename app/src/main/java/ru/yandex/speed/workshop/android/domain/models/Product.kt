package ru.yandex.speed.workshop.android.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Модель товара, соответствующая iOS Product struct
 */
@Serializable
data class Product(
    val id: String,
    val title: String,
    @SerialName("current_price")
    val currentPrice: String? = null,
    @SerialName("price")
    val legacyPrice: String? = null,
    @SerialName("old_price")
    val oldPrice: String? = null,
    @SerialName("discount_percent")
    val discountPercent: Int? = null,
    @SerialName("picture_urls")
    val pictureUrls: List<String> = emptyList(),
    val vendor: String? = null,
    @SerialName("shop_name")
    val shopName: String? = null,
    val delivery: Delivery? = null,
    val promoCode: PromoCode? = null,
    val rating: Rating? = null,
    @SerialName("is_favorite")
    var isFavorite: Boolean = false,
) {
    /**
     * Computed property для получения первого URL изображения
     * для обратной совместимости
     */
    val url: String?
        get() = pictureUrls.firstOrNull()

    /**
     * Computed property для обратной совместимости со старым полем price
     */
    val price: String
        get() = currentPrice ?: legacyPrice ?: "Price not available"

    /**
     * Computed property для получения списка изображений
     */
    val images: List<String>
        get() = pictureUrls

    /**
     * Computed property для получения производителя
     */
    val manufacturer: String
        get() = vendor ?: "Unknown"

    /**
     * Computed property для получения продавца
     */
    val seller: String
        get() = shopName ?: "Unknown"

    /**
     * Computed property для получения процента скидки
     */
    val discountPercentage: String?
        get() = discountPercent?.toString()
}
