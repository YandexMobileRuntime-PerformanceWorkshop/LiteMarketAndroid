package ru.yandex.speed.workshop.android.data.network

import android.util.Log
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * HTTP клиент на основе OkHttp (временная замена Cronet)
 */
class HttpClient private constructor() {
    
    companion object {
        private const val TAG = "HttpClient"
        
        @Volatile
        private var INSTANCE: HttpClient? = null
        
        fun getInstance(): HttpClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HttpClient().also { INSTANCE = it }
            }
        }
    }
    
    private val okHttpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private val gson = Gson()
    private val baseURL = "https://bbapkfh3cnqi1rvo0gla.containers.yandexcloud.net"  // Yandex Cloud API
    
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
        needAuthorization: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        val url = buildUrlWithParams(constructURL(path), parameters)
        Log.d(TAG, "GET request: $url")
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
        needAuthorization: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        val url = constructURL(path)
        val jsonBody = body?.let { gson.toJson(it) }
        Log.d(TAG, "POST request: $url, body: $jsonBody")
        val request = buildRequest("POST", url, jsonBody, headers, needAuthorization)
        executeRequest(request)
    }
    
    /**
     * Выполняет типизированный запрос с автоматической десериализацией
     */
    suspend fun <T> request(
        method: String,
        path: String,
        parameters: Map<String, Any> = emptyMap(),
        body: Any? = null,
        headers: Map<String, String> = emptyMap(),
        needAuthorization: Boolean = false,
        responseClass: Class<T>
    ): T {
        val response = when (method.uppercase()) {
            "GET" -> get(path, parameters, headers, needAuthorization)
            "POST" -> post(path, body, headers, needAuthorization)
            else -> throw IllegalArgumentException("Unsupported HTTP method: $method")
        }
        
        Log.d(TAG, "Response: $response")
        
        return try {
            gson.fromJson(response, responseClass)
        } catch (e: Exception) {
            Log.e(TAG, "JSON parsing error: ${e.message}", e)
            throw NetworkException.DecodingError("Failed to decode response: ${e.message}", e)
        }
    }
    
    private fun buildUrlWithParams(baseUrl: String, parameters: Map<String, Any>): String {
        if (parameters.isEmpty()) return baseUrl
        
        val params = parameters.map { "${it.key}=${it.value}" }.joinToString("&")
        return "$baseUrl?$params"
    }
    
    private fun buildRequest(
        method: String,
        url: String,
        bodyJson: String?,
        headers: Map<String, String>,
        needAuthorization: Boolean
    ): Request {
        val requestBuilder = Request.Builder().url(url)
        
        // Добавляем заголовки
        headers.forEach { (key, value) ->
            requestBuilder.addHeader(key, value)
        }
        
        // Authorization removed - not needed for current implementation
        
        // Настройка метода запроса
        when (method.uppercase()) {
            "GET" -> requestBuilder.get()
            "POST" -> {
                val requestBody = if (bodyJson != null) {
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
            Log.d(TAG, "Executing request: ${request.method} ${request.url}")
            val response = okHttpClient.newCall(request).execute()
            
            Log.d(TAG, "Response code: ${response.code}")
            
            if (!response.isSuccessful) {
                val errorBody = response.body?.string() ?: ""
                Log.e(TAG, "HTTP error ${response.code}: $errorBody")
                throw NetworkException.HttpError(response.code, errorBody)
            }
            
            val responseBody = response.body?.string() ?: ""
            Log.d(TAG, "Response body: $responseBody")
            return responseBody
            
        } catch (e: IOException) {
            Log.e(TAG, "Network error: ${e.message}", e)
            throw NetworkException.NetworkError("Network request failed", e)
        } catch (e: NetworkException) {
            throw e
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected error: ${e.message}", e)
            throw NetworkException.NetworkError("Unexpected error", e)
        }
    }
} 