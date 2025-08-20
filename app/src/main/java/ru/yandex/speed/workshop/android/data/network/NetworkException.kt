package ru.yandex.speed.workshop.android.data.network

/**
 * Базовый класс для сетевых исключений
 * Обеспечивает унифицированную обработку всех типов ошибок, возникающих при сетевых запросах
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
    
    /**
     * Ошибка пустого ответа от сервера
     */
    data class EmptyResponseError(
        val errorMessage: String
    ) : NetworkException("Empty response: $errorMessage")
    
    /**
     * Ошибка отмены запроса
     */
    object CancellationError : NetworkException("Request was cancelled")
    
    /**
     * Ошибка таймаута
     */
    data class TimeoutError(
        val timeoutMs: Long
    ) : NetworkException("Request timed out after $timeoutMs ms")
    
    /**
     * Ошибка сервера (5xx)
     */
    data class ServerError(
        val statusCode: Int,
        val responseBody: String
    ) : NetworkException("Server error $statusCode: $responseBody")
    
    /**
     * Ошибка клиента (4xx)
     */
    data class ClientError(
        val statusCode: Int,
        val responseBody: String
    ) : NetworkException("Client error $statusCode: $responseBody")
    
    /**
     * Неизвестная ошибка
     */
    data class UnknownError(
        val errorMessage: String,
        val originalException: Throwable? = null
    ) : NetworkException("Unknown error: $errorMessage", originalException)
}
