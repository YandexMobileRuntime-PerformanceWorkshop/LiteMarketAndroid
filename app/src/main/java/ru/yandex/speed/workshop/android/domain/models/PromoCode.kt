package ru.yandex.speed.workshop.android.domain.models

import kotlinx.serialization.Serializable

/**
 * Модель промокода
 */
@Serializable
data class PromoCode(
    val code: String,
    val discount: String,
    val minOrder: String? = null,
    val expiryDate: String? = null,
)
