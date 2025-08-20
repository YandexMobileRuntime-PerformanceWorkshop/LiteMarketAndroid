package ru.yandex.speed.workshop.android.presentation.ui

/**
 * Класс для представления состояния UI
 * Может содержать данные, ошибку, состояние загрузки или предварительные данные
 */
sealed class UiState<out T> {
    /**
     * Состояние загрузки
     */
    object Loading : UiState<Nothing>()

    /**
     * Успешное состояние с данными
     */
    data class Success<T>(val data: T) : UiState<T>()

    /**
     * Состояние ошибки с сообщением
     */
    data class Error(val message: String) : UiState<Nothing>()

    /**
     * Специальное состояние для предварительных данных с последующим обновлением
     * Используется для мгновенного отображения данных из аргументов
     */
    data class PreloadedData<T>(
        val data: T,
        val isUpdating: Boolean = false,
    ) : UiState<T>()

    /**
     * Проверка, является ли состояние успешным (Success или PreloadedData)
     */
    val isSuccess: Boolean
        get() = this is Success || this is PreloadedData

    /**
     * Получение данных или null
     */
    fun getDataOrNull(): T? =
        when (this) {
            is Success -> data
            is PreloadedData -> data
            else -> null
        }
}
