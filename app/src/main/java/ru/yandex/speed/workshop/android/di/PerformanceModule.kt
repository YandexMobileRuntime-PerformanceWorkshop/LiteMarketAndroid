package ru.yandex.speed.workshop.android.di

import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import ru.yandex.speed.workshop.android.utils.MVIScreenAnalytics
import ru.yandex.speed.workshop.android.utils.PerformanceMetricManager
import ru.yandex.speed.workshop.android.utils.VisibilityAnalytics
import javax.inject.Singleton

/**
 * Dagger Hilt модуль для предоставления классов, связанных с измерением производительности
 */
@Module
@InstallIn(SingletonComponent::class)
object PerformanceModule {
    /**
     * Предоставляет синглтон PerformanceMetricManager
     */
    @Provides
    @Singleton
    fun providePerformanceMetricManager(): PerformanceMetricManager {
        return PerformanceMetricManager()
    }

    /**
     * Предоставляет экземпляр MVIScreenAnalytics
     */
    @Provides
    fun provideMVIScreenAnalytics(performanceMetricManager: PerformanceMetricManager): MVIScreenAnalytics {
        return MVIScreenAnalytics(performanceMetricManager)
    }

    @Provides
    @Singleton
    fun provideVisibilityAnalytics(): VisibilityAnalytics {
        return VisibilityAnalytics()
    }
}
