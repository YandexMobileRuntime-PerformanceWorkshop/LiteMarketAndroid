package ru.yandex.speed.workshop.android.data.network

import android.content.Context
import com.google.gson.Gson
import timber.log.Timber
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.*
import okhttp3.Cache
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.IOException
import java.io.File
import java.util.concurrent.TimeUnit
import java.net.URLEncoder
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

/**
 * HTTP клиент на основе OkHttp (временная замена Cronet)
 */
class HttpClient private constructor(private val appContext: Context) {
    
    companion object {
        // Timber автоматически добавляет имя класса в логи, константа TAG больше не нужна
        
        @Volatile
        private var INSTANCE: HttpClient? = null
        
        fun getInstance(context: Context): HttpClient {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: HttpClient(context.applicationContext).also { INSTANCE = it }
            }
        }
    }
    
    private val okHttpClient: OkHttpClient

    init {
        val cacheDir = File(appContext.cacheDir, "http_cache")
        val cache = Cache(cacheDir, 20L * 1024 * 1024) // 20MB

        okHttpClient = OkHttpClient.Builder()
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
        needAuthorization: Boolean = false
    ): String = withContext(Dispatchers.IO) {
        val url = constructURL(path)
        val jsonBody = body?.let { gson.toJson(it) }
        Timber.d("POST request: $url, body: $jsonBody")
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
        
        Timber.d("Response: $response")
        return try {
            withContext(Dispatchers.Default) {
                gson.fromJson(response, responseClass)
            }
        } catch (e: Exception) {
            Timber.e(e, "JSON parsing error: ${e.message}")
            throw NetworkException.DecodingError("Failed to decode response: ${e.message}", e)
        }
    }
    
    private fun buildUrlWithParams(baseUrl: String, parameters: Map<String, Any>): String {
        if (parameters.isEmpty()) return baseUrl
        
        val params = parameters.map { (key, value) ->
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
            Timber.d("Executing request: ${request.method} ${request.url}")
            val response = okHttpClient.newCall(request).execute()
            
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

    private val retrofit = Retrofit.Builder()
        .baseUrl(baseURL)
        .addConverterFactory(GsonConverterFactory.create(gson))
        .client(okHttpClient)
        .build()

    fun getApi(): ProductApi = retrofit.create(ProductApi::class.java)
} 