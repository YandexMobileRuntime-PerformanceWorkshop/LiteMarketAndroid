package ru.yandex.speed.workshop.android

import android.app.Application
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber

/**
 * Класс приложения для инициализации глобальных компонентов
 */
@HiltAndroidApp
class SpeedWorkshopApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Инициализация Timber для логирования
        Timber.plant(Timber.DebugTree())

        // ImageLoader инициализируется автоматически через Hilt
    }
}
