package ru.yandex.speed.workshop.android.domain.models

import kotlinx.serialization.Serializable

/**
 * Модель ответа API для деталей товара
 */
@Serializable
data class ProductDetailResponse(
    val product: ProductDetail? = null,
)
