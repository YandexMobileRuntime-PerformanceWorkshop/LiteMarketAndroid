package ru.yandex.speed.workshop.android

import android.app.Application
import com.yandex.analytics.api.YandexAnalytics
import dagger.hilt.android.HiltAndroidApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import ru.yandex.speed.workshop.android.data.network.NetworkPrewarmer
import ru.yandex.speed.workshop.android.utils.ApplicationStartupTracker
import ru.yandex.speed.workshop.android.utils.PerformanceTimestamp
import timber.log.Timber
import javax.inject.Inject

/**
 * Класс приложения для инициализации глобальных компонентов
 */
@HiltAndroidApp
class SpeedWorkshopApplication : Application() {
    @Inject
    lateinit var startupTracker: ApplicationStartupTracker
    
    private val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        // Инициализация метки времени старта приложения
        // Важно вызвать ДО super.onCreate()
        PerformanceTimestamp.initializeAppStart()

        // Запуск prewarming сетевых соединений как можно раньше
        NetworkPrewarmer().startPrewarming()

        super.onCreate()

        // Инициализация Timber для логирования
        Timber.plant(Timber.DebugTree())

        // Инициализация YandexAnalytics в фоновом потоке для избежания блокировки main thread
        applicationScope.launch(Dispatchers.IO) {
            YandexAnalytics.getInstance().initialize(this@SpeedWorkshopApplication, "demo-api-key-12345")
            Timber.d("YandexAnalytics initialized in background")
        }

        // Инициализация трекера запуска приложения
        startupTracker.initialize(this)
        startupTracker.onApplicationCreated()

        // ImageLoader инициализируется автоматически через Hilt

        Timber.i("Application initialized at ${System.currentTimeMillis()}")
    }
}
