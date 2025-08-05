package ru.yandex.speed.workshop.android.domain.models

/**
 * Модель детальной информации о товаре
 */
data class ProductDetail(
    val id: String,
    val title: String,
    val price: Pricing,
    val images: List<String>,
    val rating: ProductDetailRating,
    val isFavorite: Boolean,
    val manufacturer: Manufacturer,
    val promoCode: PromoCode?,
    val delivery: ProductDetailDelivery,
    val seller: Seller
) {
    
    data class Pricing(
        val currentPrice: String,
        val oldPrice: String?,
        val discountPercentage: String?,
        val paymentMethod: String,
        val alternativePrice: String?
    )
    
    data class ProductDetailRating(
        val score: Double,
        val reviewsCount: Int
    )
    
    data class Manufacturer(
        val name: String,
        val badge: String,
        val isOriginal: Boolean
    )
    
    data class ProductDetailDelivery(
        val provider: String,
        val options: List<ProductDetailDeliveryOption>
    )
    
    data class ProductDetailDeliveryOption(
        val type: String,
        val date: String,
        val details: String,
        val isSelected: Boolean
    )
    
    data class Seller(
        val name: String,
        val logo: String,
        val rating: Double,
        val reviewsCount: String,
        val isFavorite: Boolean
    )
}

/**
 * Модель характеристики товара
 */
data class ProductFeature(
    val iconName: String,
    val title: String
) 