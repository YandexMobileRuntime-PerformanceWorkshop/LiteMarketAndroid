package ru.yandex.speed.workshop.android.data.repository

import android.util.LruCache
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.yandex.speed.workshop.android.data.mappers.toDomain
import ru.yandex.speed.workshop.android.data.mappers.toDomainDetail
import ru.yandex.speed.workshop.android.data.network.NetworkException
import ru.yandex.speed.workshop.android.data.network.ProductApi
import ru.yandex.speed.workshop.android.data.network.safeExecute
import ru.yandex.speed.workshop.android.domain.models.Product
import ru.yandex.speed.workshop.android.domain.models.ProductDetail
import ru.yandex.speed.workshop.android.domain.models.ProductListResponse
import ru.yandex.speed.workshop.android.domain.repository.ProductRepository
import ru.yandex.speed.workshop.android.domain.repository.Result
import timber.log.Timber
import java.util.concurrent.ConcurrentHashMap

/**
 * Реализация репозитория для работы с продуктами
 * Обеспечивает кэширование данных для быстрого доступа
 */
class ProductRepositoryImpl(
    private val api: ProductApi,
    private val dispatcher: CoroutineDispatcher = Dispatchers.IO,
) : ProductRepository {
    // Кэш для списков продуктов (ключ: page_perPage или query_page_perPage)
    private val listCache = ConcurrentHashMap<String, ProductListResponse>()

    // Кэш для детальной информации о продуктах (ключ: id продукта)
    private val detailCache = LruCache<String, ProductDetail>(100)

    // Кэш для отдельных продуктов (ключ: id продукта)
    private val productCache = LruCache<String, Product>(200)

    override suspend fun getProductsList(
        page: Int,
        perPage: Int,
    ): Result<ProductListResponse> =
        withContext(dispatcher) {
            val cacheKey = "page_${page}_$perPage"

            // Проверяем кэш
            listCache[cacheKey]?.let {
                return@withContext Result.Success(it)
            }

            // Если нет в кэше, загружаем с API
            val result = api.getProductsList(page, perPage).safeExecute()
            
            // Обрабатываем результат
            return@withContext when (result) {
                is Result.Success -> {
                    val data = result.data
                    
                    // Кэшируем список
                    listCache[cacheKey] = data

                    // Кэшируем отдельные продукты
                    data.products.forEach { productDto ->
                        val product = productDto.toDomain()
                        productCache.put(product.id, product)
                    }
                    
                    result
                }
                is Result.Error -> {
                    Timber.e(result.exception, "Error loading products list")
                    result
                }
                else -> result
            }
        }

    override suspend fun searchProducts(
        query: String,
        page: Int,
        perPage: Int,
    ): Result<ProductListResponse> =
        withContext(dispatcher) {
            val cacheKey = "search_${query}_${page}_$perPage"

            // Проверяем кэш
            listCache[cacheKey]?.let {
                return@withContext Result.Success(it)
            }

            // Если нет в кэше, загружаем с API
            val result = api.searchProducts(query, page, perPage).safeExecute()
            
            // Обрабатываем результат
            return@withContext when (result) {
                is Result.Success -> {
                    val data = result.data
                    
                    // Кэшируем список
                    listCache[cacheKey] = data

                    // Кэшируем отдельные продукты
                    data.products.forEach { productDto ->
                        val product = productDto.toDomain()
                        productCache.put(product.id, product)
                    }
                    
                    result
                }
                is Result.Error -> {
                    Timber.e(result.exception, "Error searching products")
                    result
                }
                else -> result
            }
        }

    override suspend fun getProductDetail(id: String): Result<ProductDetail> =
        withContext(dispatcher) {
            // Проверяем кэш
            detailCache.get(id)?.let { cachedProduct ->
                Timber.d("Using cached product detail for $id")

                // Асинхронно обновляем кэш свежими данными
                val refreshResult = api.getProductDetail(id).safeExecute()
                if (refreshResult is Result.Success) {
                    val productDto = refreshResult.data.product
                    if (productDto != null) {
                        val freshData = productDto.toDomainDetail()
                        detailCache.put(id, freshData)
                        Timber.d("Updated cache for product $id")
                    }
                } else if (refreshResult is Result.Error) {
                    // Логируем ошибку, но не прерываем поток
                    Timber.e(refreshResult.exception, "Failed to refresh cache for product $id")
                }

                // Возвращаем кэшированные данные немедленно
                return@withContext Result.Success(cachedProduct)
            }

            // Если нет в кэше, загружаем с API
            val result = api.getProductDetail(id).safeExecute()
            
            return@withContext when (result) {
                is Result.Success -> {
                    val productResponse = result.data
                    val productDto = productResponse.product
                    
                    if (productDto != null) {
                        val domainProductDetail = productDto.toDomainDetail()
                        
                        // Логируем полученные данные для отладки
                        Timber.d("Product detail received: id=${domainProductDetail.id}, title=${domainProductDetail.title}")
                        Timber.d("Images count: ${domainProductDetail.imageUrls.size}")

                        // Кэшируем полученную модель
                        detailCache.put(id, domainProductDetail)

                        // Возвращаем полученную модель
                        Result.Success(domainProductDetail)
                    } else {
                        Timber.e("Product detail is null in response")
                        Result.Error(NetworkException.EmptyResponseError("Product detail is null in response"))
                    }
                }
                is Result.Error -> {
                    Timber.e(result.exception, "Error loading product detail")
                    result
                }
                else -> result
            }
        }

    override fun prefillDetailCache(
        id: String,
        productDetail: ProductDetail,
    ) {
        Timber.d("Prefilling detail cache for product $id")
        detailCache.put(id, productDetail)
    }

    override fun prefillProductCache(product: Product) {
        Timber.d("Prefilling product cache for product ${product.id}")
        productCache.put(product.id, product)
    }

    override fun clearCache() {
        Timber.d("Clearing product caches")
        listCache.clear()
        detailCache.evictAll()
        productCache.evictAll()
    }
}