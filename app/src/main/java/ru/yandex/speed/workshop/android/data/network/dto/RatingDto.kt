package ru.yandex.speed.workshop.android.data.network.dto

import kotlinx.serialization.Serializable

/**
 * DTO для получения данных о рейтинге с сервера
 */
@Serializable
data class RatingDto(
    val score: Double = 0.0,
    
    // Поле в ответе от сервера называется "reviewsCount", а не "reviews_count"
    val reviewsCount: Int = 0
)
