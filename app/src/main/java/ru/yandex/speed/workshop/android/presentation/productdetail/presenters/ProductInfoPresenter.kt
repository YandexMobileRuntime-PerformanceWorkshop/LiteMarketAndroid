package ru.yandex.speed.workshop.android.presentation.productdetail.presenters

import android.content.Context
import ru.yandex.speed.workshop.android.R
import ru.yandex.speed.workshop.android.domain.models.ProductDetail
import ru.yandex.speed.workshop.android.domain.models.PromoCode

/**
 * Презентер для работы с основной информацией о продукте
 */
class ProductInfoPresenter(private val context: Context) {
    
    /**
     * Форматирует основную информацию о продукте
     *
     * @param product Модель данных продукта
     * @return Отформатированная основная информация
     */
    fun formatBasicInfo(product: ProductDetail): BasicProductInfo {
        val manufacturerName = product.manufacturer.takeIf { it.isNotEmpty() } 
            ?: context.getString(R.string.unknown_manufacturer)
            
        return BasicProductInfo(
            title = product.title,
            manufacturerFormatted = context.getString(
                R.string.format_manufacturer_with_arrow,
                manufacturerName
            )
        )
    }
    
    /**
     * Форматирует информацию о рейтинге продукта
     *
     * @param product Модель данных продукта
     * @return Отформатированная информация о рейтинге
     */
    fun formatRating(product: ProductDetail): ProductRatingInfo {
        val ratingScore = product.rating.score
        val formattedRating = ratingScore.takeIf { it > 0 }?.let { 
            String.format("%.1f", it) 
        } ?: context.getString(R.string.default_rating)
        
        // Всегда используем фактическое количество отзывов, даже если оно 0
        val reviewsCount = product.rating.reviewsCount
        val formattedReviews = context.getString(R.string.format_reviews_count, reviewsCount)
        
        return ProductRatingInfo(
            formattedRating = formattedRating,
            formattedReviewsCount = formattedReviews
        )
    }
    
    /**
     * Форматирует информацию о продавце
     *
     * @param product Модель данных продукта
     * @return Отформатированная информация о продавце
     */
    fun formatSellerInfo(product: ProductDetail): SellerInfo {
        val sellerName = product.seller.takeIf { it.isNotEmpty() } 
            ?: context.getString(R.string.default_seller_name)
            
        val sellerRating = product.sellerRating
        val sellerReviews = product.sellerReviewsCount
        
        val formattedRating = if (sellerRating > 0 && sellerReviews.isNotEmpty()) {
            context.getString(
                R.string.format_seller_rating,
                sellerRating.toString(),
                sellerReviews
            )
        } else {
            context.getString(R.string.default_seller_rating)
        }
        
        return SellerInfo(
            name = sellerName,
            rating = formattedRating
        )
    }
    
    /**
     * Форматирует информацию о промо-коде
     *
     * @param product Модель данных продукта
     * @return Отформатированная информация о промо-коде
     */
    fun formatPromoCode(product: ProductDetail): PromoCodeInfo {
        return product.promoCode?.let { promo ->
            formatPromoCodeData(promo)
        } ?: PromoCodeInfo(
            discountText = context.getString(R.string.default_promo_discount),
            codeText = context.getString(R.string.default_promo_code),
            hasPromoCode = true
        )
    }
    
    private fun formatPromoCodeData(promo: PromoCode): PromoCodeInfo {
        // Обрабатываем префикс "Промокод" в коде промокода
        val cleanCode = if (promo.code.startsWith(context.getString(R.string.prefix_promo_code))) {
            promo.code.substring(context.getString(R.string.prefix_promo_code).length)
        } else {
            promo.code
        }
        
        val promoText = if (promo.minOrder != null && promo.expiryDate != null) {
            context.getString(
                R.string.format_promo_code,
                cleanCode,
                promo.minOrder,
                promo.expiryDate
            )
        } else {
            context.getString(R.string.format_promo_code_simple, cleanCode)
        }
        
        return PromoCodeInfo(
            discountText = promo.discount,
            codeText = promoText,
            hasPromoCode = true
        )
    }
}

/**
 * Модель базовой информации о продукте
 */
data class BasicProductInfo(
    val title: String,
    val manufacturerFormatted: String
)

/**
 * Модель информации о рейтинге продукта
 */
data class ProductRatingInfo(
    val formattedRating: String,
    val formattedReviewsCount: String
)

/**
 * Модель информации о продавце
 */
data class SellerInfo(
    val name: String,
    val rating: String
)

/**
 * Модель информации о промо-коде
 */
data class PromoCodeInfo(
    val discountText: String,
    val codeText: String,
    val hasPromoCode: Boolean
)