package ru.yandex.speed.workshop.android.data.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import timber.log.Timber
import java.util.concurrent.TimeUnit
class NetworkPrewarmer {
    
    private val prewarmScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val baseUrl = "https://bbapkfh3cnqi1rvo0gla.containers.yandexcloud.net"
    
    private val prewarmClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.SECONDS)
        .build()

    fun startPrewarming() {
        Timber.d("Starting network connection prewarming")
        
        prewarmScope.launch {
            prewarmConnection()
        }
    }
    
    private suspend fun prewarmConnection() {
        try {
            val url = "$baseUrl/ping"
            val request = Request.Builder()
                .url(url)
                .head()
                .build()
            
            Timber.d("Prewarming connection to: $url")
            
            val response = prewarmClient.newCall(request).execute()
            response.close()
            
            Timber.d("Prewarmed connection - DNS resolved, TLS handshake completed. Status: ${response.code}")
        } catch (e: Exception) {
            Timber.w(e, "Failed to prewarm connection")
        }
    }
}
