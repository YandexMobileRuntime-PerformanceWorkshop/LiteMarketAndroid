package ru.yandex.speed.workshop.android.data.network.dto

import kotlinx.serialization.Serializable

/**
 * DTO для получения данных о промокоде с сервера
 */
@Serializable
data class PromoCodeDto(
    val code: String = "",
    val discount: String = "",
    val minOrder: String? = null,
    val expiryDate: String? = null
)
