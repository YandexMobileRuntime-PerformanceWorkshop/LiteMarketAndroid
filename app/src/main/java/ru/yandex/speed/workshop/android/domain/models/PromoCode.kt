package ru.yandex.speed.workshop.android.domain.models

/**
 * Модель промокода
 */
data class PromoCode(
    val code: String,
    val discount: String,
    val minOrder: String?,
    val expiryDate: String?
) 