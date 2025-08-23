package ru.yandex.speed.workshop.android.data.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.yandex.speed.workshop.android.domain.models.ProductDetailResponse
import ru.yandex.speed.workshop.android.domain.models.ProductListResponse
import timber.log.Timber

/**
 * API сервис для работы с товарами, аналог iOS ProductRoutes
 */
class ProductService(private val api: ProductApi) {
    suspend fun getProductsList(
        page: Int = 1,
        perPage: Int = 20,
    ): ProductListResponse {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getProductsList(page, perPage).execute()
                if (response.isSuccessful && response.body() != null) {
                    response.body()!!
                } else {
                    Timber.e("Error getting products list: ${response.code()} ${response.message()}")
                    ProductListResponse(emptyList(), false)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error getting products list: ${e.message}")
                // Возвращаем пустой ответ в случае ошибки
                ProductListResponse(emptyList(), false)
            }
        }
    }

    /**
     * Получение деталей одного товара
     * GET /api/product/{id}
     */
    suspend fun getProductDetail(id: String): ProductDetailResponse {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getProductDetail(id).execute()
                if (response.isSuccessful && response.body() != null) {
                    response.body()!!
                } else {
                    Timber.e("Error getting product details: ${response.code()} ${response.message()}")
                    throw NetworkException.HttpError(response.code(), response.message())
                }
            } catch (e: NetworkException) {
                Timber.e(e, "Network error getting product details: ${e.message}")
                throw e
            } catch (e: Exception) {
                Timber.e(e, "Error getting product details: ${e.message}")
                throw NetworkException.NetworkError("Failed to get product details", e)
            }
        }
    }

    /**
     * Поиск товаров
     * GET /api/products/search?q=query&page=1&per_page=20
     */
    suspend fun searchProducts(
        query: String,
        page: Int = 1,
        perPage: Int = 20,
    ): ProductListResponse {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.searchProducts(query, page, perPage).execute()
                if (response.isSuccessful && response.body() != null) {
                    response.body()!!
                } else {
                    Timber.e("Error searching products: ${response.code()} ${response.message()}")
                    ProductListResponse(emptyList(), false)
                }
            } catch (e: Exception) {
                Timber.e(e, "Error searching products: ${e.message}")
                // Возвращаем пустой ответ в случае ошибки
                ProductListResponse(emptyList(), false)
            }
        }
    }

    // Удалена неиспользуемая функция getProductsByCategory
}
