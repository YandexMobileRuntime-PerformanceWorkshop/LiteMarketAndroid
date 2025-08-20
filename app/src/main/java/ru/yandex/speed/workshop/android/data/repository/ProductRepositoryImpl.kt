package ru.yandex.speed.workshop.android.data.repository

import android.util.LruCache
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.yandex.speed.workshop.android.data.network.ProductApi
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
            return@withContext try {
                val response = api.getProductsList(page, perPage).execute()
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!

                    // Кэшируем список
                    listCache[cacheKey] = data

                    // Кэшируем отдельные продукты
                    data.products.forEach { product ->
                        productCache.put(product.id, product)
                    }

                    Result.Success(data)
                } else {
                    Timber.e("Error loading products list: ${response.code()} ${response.message()}")
                    Result.Error(Exception("Failed to load products: ${response.code()}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception loading products list")
                Result.Error(e)
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
            return@withContext try {
                val response = api.searchProducts(query, page, perPage).execute()
                if (response.isSuccessful && response.body() != null) {
                    val data = response.body()!!

                    // Кэшируем список
                    listCache[cacheKey] = data

                    // Кэшируем отдельные продукты
                    data.products.forEach { product ->
                        productCache.put(product.id, product)
                    }

                    Result.Success(data)
                } else {
                    Timber.e("Error searching products: ${response.code()} ${response.message()}")
                    Result.Error(Exception("Failed to search products: ${response.code()}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception searching products")
                Result.Error(e)
            }
        }

    override suspend fun getProductDetail(id: String): Result<ProductDetail> =
        withContext(dispatcher) {
            // Проверяем кэш
            detailCache.get(id)?.let { cachedProduct ->
                Timber.d("Using cached product detail for $id")

                // Асинхронно обновляем кэш свежими данными
                try {
                    val response = api.getProductDetail(id).execute()
                    if (response.isSuccessful && response.body() != null) {
                        val freshData = response.body()!!.product
                        detailCache.put(id, freshData)
                        Timber.d("Updated cache for product $id")
                    }
                } catch (e: Exception) {
                    // Логируем ошибку, но не прерываем поток
                    Timber.e(e, "Failed to refresh cache for product $id")
                }

                // Возвращаем кэшированные данные немедленно
                return@withContext Result.Success(cachedProduct)
            }

            // Если нет в кэше, загружаем с API
            return@withContext try {
                val response = api.getProductDetail(id).execute()
                if (response.isSuccessful && response.body() != null) {
                    val productResponse = response.body()!!
                    val data = productResponse.product

                    if (data != null) {
                        // Логируем полученные данные для отладки
                        Timber.d("Product detail received: id=${data.id}, title=${data.title}")
                        Timber.d("Images: ${data.images.size}, picture_urls: ${data.picture_urls.size}"
                        )

                        // Проверяем изображения - всегда используем picture_urls, если они есть
                        val finalImages =
                            when {
                                data.picture_urls.isNotEmpty() -> {
                                    Timber.d("Using picture_urls for product ${data.id}")
                                    data.picture_urls
                                }
                                data.images.isNotEmpty() -> {
                                    Timber.d("Using images for product ${data.id}")
                                    data.images
                                }
                                else -> {
                                    // Если у нас есть кэшированный продукт, проверяем его изображения
                                    val cachedProduct = productCache.get(id)
                                    if (cachedProduct != null && cachedProduct.pictureUrls.isNotEmpty()) {
                                        Timber.d("Using cached pictureUrls for product ${data.id}")
                                        cachedProduct.pictureUrls
                                    } else {
                                        Timber.w("No images found for product ${data.id}")
                                        emptyList()
                                    }
                                }
                            }

                        // Обновляем модель с синхронизированными изображениями и ценой
                        val updatedProduct =
                            data.copy(
                                images = finalImages,
                                picture_urls = finalImages,
                                // Синхронизируем поля между API и вложенными объектами
                                price = data.price.copy(
                                    currentPrice = data.currentPrice ?: data.price.currentPrice,
                                    oldPrice = data.oldPrice ?: data.price.oldPrice,
                                    // Преобразуем discount_percent в строку для discountPercentage
                                    discountPercentage = data.discountPercent?.toString() ?: data.price.discountPercentage
                                ),
                                // Синхронизируем информацию о производителе
                                manufacturer = data.manufacturer.copy(
                                    name = data.manufacturer.name.takeIf { it.isNotEmpty() } ?: data.vendor ?: data.manufacturer.name
                                ),
                                // Синхронизируем информацию о продавце
                                seller = data.seller.copy(
                                    name = data.seller.name.takeIf { it.isNotEmpty() } ?: data.shopName ?: data.seller.name
                                )
                            )

                        // Кэшируем обновленную модель
                        detailCache.put(id, updatedProduct)

                        // Возвращаем обновленную модель
                        Result.Success(updatedProduct)
                    } else {
                        Timber.e("Product detail is null in response")
                        Result.Error(Exception("Product detail is null in response"))
                    }
                } else {
                    Timber.e("Error loading product detail: ${response.code()} ${response.message()}")
                    Result.Error(Exception("Failed to load product detail: ${response.code()}"))
                }
            } catch (e: Exception) {
                Timber.e(e, "Exception loading product detail")
                Result.Error(e)
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
