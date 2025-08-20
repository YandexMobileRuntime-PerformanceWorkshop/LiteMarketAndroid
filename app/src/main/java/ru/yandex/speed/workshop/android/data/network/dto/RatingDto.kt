package ru.yandex.speed.workshop.android.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * DTO для получения данных о рейтинге с сервера
 */
@Serializable
data class RatingDto(
    val score: Double = 0.0,
    
    @SerialName("reviews_count")
    val reviewsCount: Int = 0
)
