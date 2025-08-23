package ru.yandex.speed.workshop.android.data.network

import android.content.Context
import com.jakewharton.retrofit2.converter.kotlinx.serialization.asConverterFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import retrofit2.Retrofit
import timber.log.Timber
import java.io.File
import java.io.IOException
import java.net.URLEncoder
import java.util.concurrent.TimeUnit

/**
 * HTTP клиент на основе OkHttp (временная замена Cronet)
 */
class HttpClient private constructor(private val appContext: Context) {
    companion object {
        @Volatile
        private var instance: HttpClient? = null

        fun getInstance(context: Context): HttpClient {
            return instance ?: synchronized(this) {
                instance ?: HttpClient(context.applicationContext).also { instance = it }
            }
        }

        // Создаем Json для Kotlinx Serialization
        val json =
            Json {
                ignoreUnknownKeys = true
                coerceInputValues = true
                isLenient = true
                allowSpecialFloatingPointValues = true
                useAlternativeNames = true
            }
    }

    private val cacheDir = File(appContext.cacheDir, "http_cache")
    private val cache = Cache(cacheDir, 20L * 1024 * 1024)
    
    private val mainHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .cache(cache)
            .addNetworkInterceptor { chain ->
                val request = chain.request()
                val response = chain.proceed(request)
                if (request.method.equals("GET", ignoreCase = true)) {
                    val cacheControl = response.header("Cache-Control")
                    if (cacheControl.isNullOrBlank() ||
                        cacheControl.contains("no-store", true) ||
                        cacheControl.contains("no-cache", true) ||
                        cacheControl.contains("must-revalidate", true)
                    ) {
                        response.newBuilder()
                            .header("Cache-Control", "public, max-age=60")
                            .build()
                    } else {
                        response
                    }
                } else {
                    response
                }
            }
            .build()
    }

    private val baseURL = "https://bbapkfh3cnqi1rvo0gla.containers.yandexcloud.net" // Yandex Cloud API

    private fun constructURL(path: String): String {
        val cleanPath = if (path.startsWith("/")) path.substring(1) else path
        return if (path.isEmpty()) baseURL else "$baseURL/$cleanPath"
    }

    /**
     * Выполняет GET запрос
     */
    suspend fun get(
        path: String,
        parameters: Map<String, Any> = emptyMap(),
        headers: Map<String, String> = emptyMap(),
        needAuthorization: Boolean = false,
    ): String =
        withContext(Dispatchers.IO) {
            val url = buildUrlWithParams(constructURL(path), parameters)
            Timber.d("GET request: $url")
            val request = buildRequest("GET", url, null, headers, needAuthorization)
            executeRequest(request)
        }

    /**
     * Выполняет POST запрос
     */
    suspend fun post(
        path: String,
        body: Any? = null,
        headers: Map<String, String> = emptyMap(),
        needAuthorization: Boolean = false,
    ): String =
        withContext(Dispatchers.IO) {
            val url = constructURL(path)
            // Используем строковое представление для body
            val jsonBody = body?.toString()
            Timber.d("POST request: $url, body: $jsonBody")
            val request = buildRequest("POST", url, jsonBody, headers, needAuthorization)
            executeRequest(request)
        }

    /**
     * Выполняет типизированный запрос с автоматической десериализацией
     */
    suspend inline fun <reified T> request(
        method: String,
        path: String,
        parameters: Map<String, Any> = emptyMap(),
        body: Any? = null,
        headers: Map<String, String> = emptyMap(),
        needAuthorization: Boolean = false,
    ): T {
        val response =
            when (method.uppercase()) {
                "GET" -> get(path, parameters, headers, needAuthorization)
                "POST" -> post(path, body, headers, needAuthorization)
                else -> throw IllegalArgumentException("Unsupported HTTP method: $method")
            }

        Timber.d("Response: $response")
        return try {
            withContext(Dispatchers.Default) {
                // Используем kotlinx.serialization для десериализации
                HttpClient.json.decodeFromString<T>(response)
            }
        } catch (e: Exception) {
            Timber.e(e, "JSON parsing error: ${e.message}")
            throw NetworkException.DecodingError("Failed to decode response: ${e.message}", e)
        }
    }

    private fun buildUrlWithParams(
        baseUrl: String,
        parameters: Map<String, Any>,
    ): String {
        if (parameters.isEmpty()) return baseUrl

        val params =
            parameters.map { (key, value) ->
                val encKey = URLEncoder.encode(key, "UTF-8")
                val encVal = URLEncoder.encode(value.toString(), "UTF-8")
                "$encKey=$encVal"
            }.joinToString("&")
        return "$baseUrl?$params"
    }

    private fun buildRequest(
        method: String,
        url: String,
        bodyJson: String?,
        headers: Map<String, String>,
        needAuthorization: Boolean,
    ): Request {
        val requestBuilder = Request.Builder().url(url)

        headers.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }

        when (method.uppercase()) {
            "GET" -> requestBuilder.get()
            "POST" -> {
                val requestBody =
                    if (bodyJson != null) {
                        requestBuilder.addHeader("Content-Type", "application/json")
                        bodyJson.toRequestBody("application/json".toMediaType())
                    } else {
                        "".toRequestBody()
                    }
                requestBuilder.post(requestBody)
            }
        }

        return requestBuilder.build()
    }

    private fun executeRequest(request: Request): String {
        try {
            Timber.d("Executing request: ${request.method} ${request.url}")
            val response = mainHttpClient.newCall(request).execute()

            Timber.d("Response code: ${response.code}")

            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                Timber.e("HTTP error ${response.code}: $errorBody")
                throw NetworkException.HttpError(response.code, errorBody)
            }

            val responseBody = response.body?.string() ?: ""
            Timber.d("Response body: $responseBody")
            return responseBody
        } catch (e: IOException) {
            Timber.e(e, "Network error: ${e.message}")
            throw NetworkException.NetworkError("Network request failed", e)
        } catch (e: NetworkException) {
            throw e
        } catch (e: Exception) {
            Timber.e(e, "Unexpected error: ${e.message}")
            throw NetworkException.NetworkError("Unexpected error", e)
        }
    }

    // Создаем конвертер для Retrofit с kotlinx.serialization
    private val contentType = "application/json".toMediaType()
    private val jsonConverter = HttpClient.json.asConverterFactory(contentType)

    private val retrofit by lazy {
        Retrofit.Builder()
            .baseUrl(baseURL)
            .addConverterFactory(jsonConverter)
            .client(mainHttpClient)
            .build()
    }

    private val productApiInstance by lazy {
        retrofit.create(ProductApi::class.java)
    }

    fun getApi(): ProductApi = productApiInstance


    suspend fun prewarmConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val url = "$baseURL/ping"
            val request = Request.Builder()
                .url(url)
                .head()
                .build()
            
            Timber.d("Prewarming connection to: $url")
            
            val response = mainHttpClient.newCall(request).execute()
            response.close()
            
            Timber.d("Prewarmed connection - DNS resolved, TLS handshake completed. Status: ${response.code}")
            true
        } catch (e: Exception) {
            Timber.w(e, "Failed to prewarm connection")
            false
        }
    }
}
