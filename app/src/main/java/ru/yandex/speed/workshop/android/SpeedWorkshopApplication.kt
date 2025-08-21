package ru.yandex.speed.workshop.android

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
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
    
    override fun onCreate() {
        // Инициализация метки времени старта приложения
        // Важно вызвать ДО super.onCreate()
        PerformanceTimestamp.initializeAppStart()

        super.onCreate()

        // Инициализация Timber для логирования
        Timber.plant(Timber.DebugTree())

        // Инициализация трекера запуска приложения
        startupTracker.initialize(this)
        startupTracker.onApplicationCreated()
        
        // ImageLoader инициализируется автоматически через Hilt

        Timber.i("Application initialized at ${System.currentTimeMillis()}")
    }
}
