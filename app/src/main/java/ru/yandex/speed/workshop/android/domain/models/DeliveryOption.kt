package ru.yandex.speed.workshop.android.domain.models

import kotlinx.serialization.Serializable

/**
 * Модель опции доставки, синхронизированная с iOS
 */
@Serializable
data class DeliveryOption(
    val type: String? = null,
    val date: String? = null,
    val details: String? = null,
    val isSelected: Boolean? = null,
)
