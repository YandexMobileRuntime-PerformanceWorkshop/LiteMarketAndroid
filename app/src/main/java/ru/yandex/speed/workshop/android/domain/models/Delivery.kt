package ru.yandex.speed.workshop.android.domain.models

import kotlinx.serialization.Serializable

/**
 * Модель доставки, синхронизированная с iOS
 */
@Serializable
data class Delivery(
    val provider: String? = null,
    val options: List<DeliveryOption> = emptyList(),
)
