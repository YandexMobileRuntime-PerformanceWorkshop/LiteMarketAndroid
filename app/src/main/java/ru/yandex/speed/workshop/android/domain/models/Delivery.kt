package ru.yandex.speed.workshop.android.domain.models

/**
 * Модель доставки, синхронизированная с iOS
 */
data class Delivery(
    val provider: String?,
    val options: List<DeliveryOption>?
) 