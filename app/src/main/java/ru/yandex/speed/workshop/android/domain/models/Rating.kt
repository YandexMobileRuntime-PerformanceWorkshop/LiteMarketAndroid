package ru.yandex.speed.workshop.android.domain.models

/**
 * Модель рейтинга, синхронизированная с iOS
 */
data class Rating(
    val score: Double?,
    val reviewsCount: Int?
) 