package ru.yandex.speed.workshop.android.data.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Класс для предварительного прогрева сетевых соединений
 * Использует тот же HttpClient, что и основные запросы для эффективного переиспользования соединений
 */
@Singleton
class NetworkPrewarmer @Inject constructor(
    private val httpClient: HttpClient
) {
    
    private val prewarmScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    fun startPrewarming() {
        Timber.d("Starting network connection prewarming with shared HttpClient")
        
        prewarmScope.launch {
            httpClient.prewarmConnection()
        }
    }
}
