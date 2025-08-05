package ru.yandex.speed.workshop.android.domain.models

/**
 * Модель опции доставки, синхронизированная с iOS
 */
data class DeliveryOption(
    val type: String?,
    val date: String?,
    val details: String?,
    val isSelected: Boolean?
) 