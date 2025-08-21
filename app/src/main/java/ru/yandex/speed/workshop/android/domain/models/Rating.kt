package ru.yandex.speed.workshop.android.domain.models

import kotlinx.serialization.Serializable

/**
 * Модель рейтинга, синхронизированная с iOS
 */
@Serializable
data class Rating(
    val score: Double? = null,
    val reviewsCount: Int? = null,
)
