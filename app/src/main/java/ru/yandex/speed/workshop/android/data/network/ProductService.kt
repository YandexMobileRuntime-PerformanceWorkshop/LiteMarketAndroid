package ru.yandex.speed.workshop.android.data.network

import ru.yandex.speed.workshop.android.domain.models.ProductDetailResponse
import ru.yandex.speed.workshop.android.domain.models.ProductListResponse

/**
 * API сервис для работы с товарами, аналог iOS ProductRoutes
 */
class ProductService(private val api: ProductApi) {
    suspend fun getProductsList(page: Int = 1, perPage: Int = 20): ProductListResponse = api.getProductsList(page, perPage)
    
    /**
     * Получение деталей одного товара
     * GET /api/product/{id}
     */
    suspend fun getProductDetail(id: String): ProductDetailResponse {
        return api.getProductDetail(id)
    }
    
    /**
     * Поиск товаров
     * GET /api/products/search?q=query&page=1&per_page=20
     */
    suspend fun searchProducts(
        query: String,
        page: Int = 1,
        perPage: Int = 20
    ): ProductListResponse {
        return api.searchProducts(query, page, perPage)
    }
    
    // Удалена неиспользуемая функция getProductsByCategory
} 