package ru.yandex.speed.workshop.android.data.network

/**
 * Базовый класс для сетевых исключений
 */
sealed class NetworkException(message: String, cause: Throwable? = null) : Exception(message, cause) {
    /**
     * Ошибка неправильного URL
     */
    object InvalidUrl : NetworkException("Invalid URL")

    /**
     * HTTP ошибка с кодом статуса
     */
    data class HttpError(
        val statusCode: Int,
        val responseBody: String,
    ) : NetworkException("HTTP error $statusCode: $responseBody")

    /**
     * Ошибка сети (подключение, таймаут и т.д.)
     */
    data class NetworkError(
        val errorMessage: String,
        val originalException: Throwable,
    ) : NetworkException("Network error: $errorMessage", originalException)

    /**
     * Ошибка декодирования ответа
     */
    data class DecodingError(
        val errorMessage: String,
        val originalException: Throwable,
    ) : NetworkException("Decoding error: $errorMessage", originalException)
}
