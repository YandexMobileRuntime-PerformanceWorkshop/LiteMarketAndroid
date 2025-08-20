package ru.yandex.speed.workshop.android.data.mappers

import ru.yandex.speed.workshop.android.data.network.dto.ProductDto
import ru.yandex.speed.workshop.android.domain.models.Product
import ru.yandex.speed.workshop.android.domain.models.ProductDetail
import ru.yandex.speed.workshop.android.domain.models.ProductDelivery
import ru.yandex.speed.workshop.android.domain.models.ProductDeliveryOption
import ru.yandex.speed.workshop.android.domain.models.ProductRating
import ru.yandex.speed.workshop.android.domain.models.PromoCode

/**
 * Конвертирует DTO в доменную модель Product
 */
fun ProductDto.toDomain(): Product {
    return Product(
        id = id,
        title = title,
        currentPrice = currentPrice ?: "Price not available",
        oldPrice = oldPrice,
        discountPercent = discountPercent,
        imageUrls = pictureUrls,
        manufacturer = vendor ?: "Unknown",
        seller = shopName ?: "Unknown",
        rating = rating?.let {
            ProductRating(
                score = it.score,
                reviewsCount = it.reviewsCount
            )
        },
        isFavorite = isFavorite,
        promoCode = promoCode?.let {
            PromoCode(
                code = it.code,
                discount = it.discount,
                minOrder = it.minOrder,
                expiryDate = it.expiryDate
            )
        }
    )
}

/**
 * Конвертирует DTO в доменную модель ProductDetail
 */
fun ProductDto.toDomainDetail(): ProductDetail {
    return ProductDetail(
        id = id,
        title = title,
        currentPrice = currentPrice ?: "Price not available",
        oldPrice = oldPrice,
        discountPercent = discountPercent,
        imageUrls = pictureUrls,
        manufacturer = vendor ?: "Unknown",
        seller = shopName ?: "Unknown",
        rating = ProductRating(
            score = rating?.score ?: 0.0,
            reviewsCount = rating?.reviewsCount ?: 0
        ),
        isFavorite = isFavorite,
        promoCode = promoCode?.let {
            PromoCode(
                code = it.code,
                discount = it.discount,
                minOrder = it.minOrder,
                expiryDate = it.expiryDate
            )
        },
        delivery = ProductDelivery(
            provider = delivery?.provider ?: "",
            options = delivery?.options?.map {
                ProductDeliveryOption(
                    type = it.type ?: "",
                    date = it.date ?: "",
                    details = it.details ?: "",
                    isSelected = it.isSelected ?: false
                )
            } ?: emptyList()
        ),
        paymentMethod = paymentMethod ?: "",
        alternativePrice = alternativePrice,
        sellerLogo = sellerLogo ?: "",
        sellerRating = sellerRating ?: 0.0,
        sellerReviewsCount = sellerReviewsCount ?: "",
        isSellerFavorite = isSellerFavorite ?: false
    )
}
