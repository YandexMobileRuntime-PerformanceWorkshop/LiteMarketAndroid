package ru.yandex.speed.workshop.android.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO для получения данных о промокоде с сервера
 */
@Serializable
data class PromoCodeDto(
    val code: String = "",
    val discount: String = "",
    
    @SerialName("min_order")
    val minOrder: String? = null,
    
    @SerialName("expiry_date")
    val expiryDate: String? = null
)
