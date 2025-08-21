package ru.yandex.speed.workshop.android.domain.repository

/**
 * Класс для представления результата операции
 * Может содержать данные или ошибку
 */
sealed class Result<out T> {
    /**
     * Успешный результат с данными
     */
    data class Success<T>(val data: T) : Result<T>()

    /**
     * Ошибка с исключением
     */
    data class Error(val exception: Exception) : Result<Nothing>()

    /**
     * Состояние загрузки
     */
    object Loading : Result<Nothing>()

    /**
     * Преобразование результата с помощью функции
     */
    inline fun <R> map(transform: (T) -> R): Result<R> {
        return when (this) {
            is Success -> Success(transform(data))
            is Error -> this
            is Loading -> this
        }
    }

    /**
     * Выполнение действия в случае успеха
     */
    inline fun onSuccess(action: (T) -> Unit): Result<T> {
        if (this is Success) {
            action(data)
        }
        return this
    }

    /**
     * Выполнение действия в случае ошибки
     */
    inline fun onError(action: (Exception) -> Unit): Result<T> {
        if (this is Error) {
            action(exception)
        }
        return this
    }

    /**
     * Получение данных или null в случае ошибки
     */
    fun getOrNull(): T? {
        return when (this) {
            is Success -> data
            else -> null
        }
    }

    /**
     * Получение данных или значения по умолчанию в случае ошибки
     */
    fun getOrDefault(defaultValue: @UnsafeVariance T): T {
        return when (this) {
            is Success -> data
            else -> defaultValue
        }
    }

    /**
     * Получение данных или выброс исключения в случае ошибки
     */
    fun getOrThrow(): T {
        return when (this) {
            is Success -> data
            is Error -> throw exception
            is Loading -> throw IllegalStateException("Result is still loading")
        }
    }
}
