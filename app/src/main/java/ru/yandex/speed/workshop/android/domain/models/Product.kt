package ru.yandex.speed.workshop.android.domain.models

import com.google.gson.annotations.SerializedName

/**
 * Модель товара, соответствующая iOS Product struct
 */
data class Product(
    val id: String,
    val title: String,
    @SerializedName("current_price")
    val currentPrice: String?,
    @SerializedName("price") 
    val legacyPrice: String?,
    @SerializedName("old_price")
    val oldPrice: String?,
    @SerializedName("discount_percent")
    val discountPercent: Int?,
    @SerializedName("picture_urls")
    val pictureUrls: List<String>,
    val vendor: String?,
    @SerializedName("shop_name")
    val shopName: String?,
    val delivery: Delivery?,
    val promoCode: PromoCode?,
    val rating: Rating?,
    @SerializedName("is_favorite")
    var isFavorite: Boolean = false
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
} 