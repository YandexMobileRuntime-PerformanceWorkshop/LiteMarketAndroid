package ru.yandex.speed.workshop.android.domain.models

import com.google.gson.annotations.SerializedName

/**
 * Модель ответа API для списка товаров с пагинацией
 * Соответствует iOS ProductListResponse
 */
data class ProductListResponse(
    val products: List<Product>,
    @SerializedName("has_more")
    val hasMore: Boolean,
    @SerializedName("current_page")
    val currentPage: Int? = null,
    val totalPages: Int? = null,
    val totalCount: Int? = null
) 