package ru.yandex.speed.workshop.android.presentation.productdetail.presenters

import ru.yandex.speed.workshop.android.domain.models.ProductDetail

/**
 * Презентер для работы с ценами и скидками продукта
 */
class ProductPricePresenter {

    /**
     * Форматирует ценовую информацию о продукте
     *
     * @param product Модель данных продукта
     * @return Отформатированная ценовая информация
     */
    fun formatPrice(product: ProductDetail): FormattedPrice {
        val hasOldPrice = !product.oldPrice.isNullOrEmpty() && product.oldPrice.isNotBlank()
        
        val discountText = formatDiscount(product)
        val showDiscount = discountText.isNotEmpty()
        
        return FormattedPrice(
            currentPrice = product.currentPrice,
            oldPrice = product.oldPrice,
            discountText = discountText,
            showOldPrice = hasOldPrice,
            showDiscount = showDiscount
        )
    }
    
    /**
     * Форматирует текст скидки
     */
    private fun formatDiscount(product: ProductDetail): String {
        return when {
            // Используем значение из доменной модели, если оно есть
            product.discountPercent != null && product.discountPercent > 0 -> {
                "${product.discountPercent}%"
            }
            // Нет альтернативного представления в новой структуре
            false -> {
                ""
            }
            // Нет скидки
            else -> ""
        }
    }
}

/**
 * Модель форматированных цен продукта
 */
data class FormattedPrice(
    val currentPrice: String,
    val oldPrice: String?,
    val discountText: String,
    val showOldPrice: Boolean,
    val showDiscount: Boolean
)
