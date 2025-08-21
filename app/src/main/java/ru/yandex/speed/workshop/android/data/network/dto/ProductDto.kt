package ru.yandex.speed.workshop.android.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO для получения данных о товаре с сервера
 */
@Serializable
data class ProductDto(
    val id: String = "",
    val title: String = "",
    @SerialName("current_price")
    val currentPrice: String? = null,
    @SerialName("old_price")
    val oldPrice: String? = null,
    @SerialName("discount_percent")
    val discountPercent: Int? = null,
    @SerialName("picture_urls")
    val pictureUrls: List<String> = emptyList(),
    val vendor: String? = null,
    @SerialName("shop_name")
    val shopName: String? = null,
    val rating: RatingDto? = null,
    val isFavorite: Boolean = false,
    @SerialName("promoCode")
    val promoCode: PromoCodeDto? = null,
    val delivery: DeliveryDto? = null,
    @SerialName("payment_method")
    val paymentMethod: String? = null,
    @SerialName("alternative_price")
    val alternativePrice: String? = null,
    @SerialName("seller_logo")
    val sellerLogo: String? = null,
    @SerialName("seller_rating")
    val sellerRating: Double? = null,
    @SerialName("seller_reviews_count")
    val sellerReviewsCount: String? = null,
    @SerialName("is_seller_favorite")
    val isSellerFavorite: Boolean? = null,
)
