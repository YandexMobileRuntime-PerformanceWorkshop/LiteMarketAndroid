package ru.yandex.speed.workshop.android.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO для получения данных об опции доставки с сервера
 */
@Serializable
data class DeliveryOptionDto(
    val type: String? = null,
    val date: String? = null,
    val details: String? = null,
    
    val isSelected: Boolean? = null
)
