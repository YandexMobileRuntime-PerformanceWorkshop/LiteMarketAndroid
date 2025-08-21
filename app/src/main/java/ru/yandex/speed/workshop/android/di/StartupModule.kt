package ru.yandex.speed.workshop.android.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.yandex.speed.workshop.android.utils.ApplicationStartupTracker
import ru.yandex.speed.workshop.android.utils.PerformanceMetricManager
import javax.inject.Singleton

/**
 * Модуль для предоставления компонентов, связанных с отслеживанием запуска приложения
 */
@Module
@InstallIn(SingletonComponent::class)
object StartupModule {
    /**
     * Предоставляет трекер запуска приложения
     */
    @Provides
    @Singleton
    fun provideApplicationStartupTracker(performanceMetricManager: PerformanceMetricManager): ApplicationStartupTracker {
        return ApplicationStartupTracker(performanceMetricManager)
    }
}
