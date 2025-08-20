package ru.yandex.speed.workshop.android.data.network

import retrofit2.Call
import retrofit2.Response
import ru.yandex.speed.workshop.android.domain.repository.Result
import timber.log.Timber

/**
 * Функция для безопасного выполнения API-запросов с унифицированной обработкой ошибок
 * 
 * @param call Функция, выполняющая API-запрос и возвращающая Response<T>
 * @return Result<T> с данными или ошибкой
 */
suspend fun <T> safeApiCall(call: () -> Response<T>): Result<T> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                Result.Success(body)
            } else {
                Timber.e("API call successful but response body is null")
                Result.Error(NetworkException.EmptyResponseError("Empty response body"))
            }
        } else {
            val errorBody = response.errorBody()?.string() ?: "No error body"
            Timber.e("API call failed with code ${response.code()}: $errorBody")
            Result.Error(NetworkException.HttpError(response.code(), errorBody))
        }
    } catch (e: Exception) {
        Timber.e(e, "Exception during API call")
        Result.Error(NetworkException.NetworkError("Network request failed", e))
    }
}

/**
 * Функция-расширение для безопасного выполнения блокирующих API-запросов с унифицированной обработкой ошибок
 * 
 * @return Result<T> с данными или ошибкой
 */
suspend fun <T> Call<T>.safeExecute(): Result<T> {
    return safeApiCall { this.execute() }
}
