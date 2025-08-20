package ru.yandex.speed.workshop.android.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Модель детальной информации о товаре
 */
@Serializable
data class ProductDetail(
    val id: String = "",
    val title: String = "",
    val price: Pricing = Pricing(),
    val images: List<String> = emptyList(),
    val picture_urls: List<String> = emptyList(),
    @SerialName("current_price")
    val currentPrice: String? = null,
    @SerialName("old_price")
    val oldPrice: String? = null,
    @SerialName("discount_percent")
    val discountPercent: Int? = null,
    @SerialName("vendor")
    val vendor: String? = null,
    @SerialName("shop_name")
    val shopName: String? = null,
    val rating: ProductDetailRating = ProductDetailRating(),
    val isFavorite: Boolean = false,
    val manufacturer: Manufacturer = Manufacturer(),
    val promoCode: PromoCode? = null,
    val delivery: ProductDetailDelivery = ProductDetailDelivery(),
    val seller: Seller = Seller(),
) {
    @Serializable
    data class Pricing(
        val currentPrice: String = "",
        val oldPrice: String? = null,
        val discountPercentage: String? = null,
        val paymentMethod: String = "",
        val alternativePrice: String? = null,
    )

    @Serializable
    data class ProductDetailRating(
        val score: Double = 0.0,
        val reviewsCount: Int = 0,
    )

    @Serializable
    data class Manufacturer(
        val name: String = "",
        val badge: String = "",
        val isOriginal: Boolean = false,
    )

    @Serializable
    data class ProductDetailDelivery(
        val provider: String = "",
        val options: List<ProductDetailDeliveryOption> = emptyList(),
    )

    @Serializable
    data class ProductDetailDeliveryOption(
        val type: String = "",
        val date: String = "",
        val details: String = "",
        val isSelected: Boolean = false,
    )

    @Serializable
    data class Seller(
        val name: String = "",
        val logo: String = "",
        val rating: Double = 0.0,
        val reviewsCount: String = "",
        val isFavorite: Boolean = false,
    )
}

/**
 * Модель характеристики товара
 */
@Serializable
data class ProductFeature(
    val iconName: String,
    val title: String,
)
