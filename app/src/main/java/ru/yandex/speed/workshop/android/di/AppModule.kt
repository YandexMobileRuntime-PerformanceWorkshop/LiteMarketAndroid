package ru.yandex.speed.workshop.android.di

import android.app.Application
import android.content.Context
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import ru.yandex.speed.workshop.android.data.network.HttpClient
import ru.yandex.speed.workshop.android.data.network.ProductApi
import ru.yandex.speed.workshop.android.data.repository.ProductRepositoryImpl
import ru.yandex.speed.workshop.android.domain.repository.ProductRepository
import ru.yandex.speed.workshop.android.utils.ImageLoader
import ru.yandex.speed.workshop.android.utils.PerformanceMetricManager
import javax.inject.Singleton

/**
 * Модуль Dagger Hilt для предоставления зависимостей уровня приложения
 */
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    /**
     * Предоставляет экземпляр HttpClient
     */
    @Provides
    @Singleton
    fun provideHttpClient(
        @ApplicationContext context: Context,
    ): HttpClient {
        return HttpClient.getInstance(context)
    }

    /**
     * Предоставляет экземпляр ProductApi
     */
    @Provides
    @Singleton
    fun provideProductApi(httpClient: HttpClient): ProductApi {
        return httpClient.getApi()
    }

    /**
     * Предоставляет экземпляр ProductRepository
     */
    @Provides
    @Singleton
    fun provideProductRepository(api: ProductApi): ProductRepository {
        return ProductRepositoryImpl(api)
    }

    /**
     * Предоставляет экземпляр ImageLoader
     * Примечание: ImageLoader имеет конструктор с @Inject, поэтому
     * этот метод не обязателен, но добавлен для полноты
     */
    @Provides
    @Singleton
    fun provideImageLoader(
        application: Application,
        performanceMetricManager: PerformanceMetricManager,
    ): ImageLoader {
        return ImageLoader(application, performanceMetricManager)
    }
}
