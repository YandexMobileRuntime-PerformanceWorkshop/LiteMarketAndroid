package ru.yandex.speed.workshop.android.domain.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Модель ответа API для списка товаров с пагинацией
 * Соответствует iOS ProductListResponse
 */
@Serializable
data class ProductListResponse(
    val products: List<Product> = emptyList(),
    @SerialName("has_more")
    val hasMore: Boolean = false,
    @SerialName("current_page")
    val currentPage: Int? = null,
    val totalPages: Int? = null,
    val totalCount: Int? = null,
)
