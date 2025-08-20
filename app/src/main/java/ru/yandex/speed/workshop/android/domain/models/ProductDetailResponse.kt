package ru.yandex.speed.workshop.android.domain.models

import kotlinx.serialization.Serializable
import ru.yandex.speed.workshop.android.data.network.dto.ProductDto

/**
 * Модель ответа API для деталей товара
 */
@Serializable
data class ProductDetailResponse(
    val product: ProductDto? = null,
    val status: String? = null,
)