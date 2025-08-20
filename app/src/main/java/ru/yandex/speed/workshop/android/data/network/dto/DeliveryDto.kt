package ru.yandex.speed.workshop.android.data.network.dto

import kotlinx.serialization.Serializable

/**
 * DTO для получения данных о доставке с сервера
 */
@Serializable
data class DeliveryDto(
    val provider: String? = null,
    val options: List<DeliveryOptionDto>? = null
)
