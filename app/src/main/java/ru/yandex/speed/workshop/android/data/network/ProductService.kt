package ru.yandex.speed.workshop.android.data.network

import ru.yandex.speed.workshop.android.domain.models.ProductDetailResponse
import ru.yandex.speed.workshop.android.domain.models.ProductListResponse

/**
 * API сервис для работы с товарами, аналог iOS ProductRoutes
 */
class ProductService(private val httpClient: HttpClient) {
    
    /**
     * Получение списка товаров с пагинацией
     * GET /api/products?page=1&per_page=20
     */
    suspend fun getProductsList(page: Int = 1, perPage: Int = 20): ProductListResponse {
        val parameters = mapOf(
            "page" to page,
            "per_page" to perPage
        )
        
        return httpClient.request(
            method = "GET",
            path = "api/products",
            parameters = parameters,
            responseClass = ProductListResponse::class.java
        )
    }
    
    /**
     * Получение деталей одного товара
     * GET /api/product/{id}
     */
    suspend fun getProductDetail(id: String): ProductDetailResponse {
        return httpClient.request(
            method = "GET",
            path = "api/product/$id",
            responseClass = ProductDetailResponse::class.java
        )
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
        val parameters = mapOf(
            "q" to query,
            "page" to page,
            "per_page" to perPage
        )
        
        return httpClient.request(
            method = "GET",
            path = "api/products/search",
            parameters = parameters,
            responseClass = ProductListResponse::class.java
        )
    }
    
    /**
     * Получение товаров по категории
     * GET /api/products/category/{category}?page=1&per_page=20
     */
    suspend fun getProductsByCategory(
        category: String,
        page: Int = 1,
        perPage: Int = 20
    ): ProductListResponse {
        val parameters = mapOf(
            "page" to page,
            "per_page" to perPage
        )
        
        return httpClient.request(
            method = "GET",
            path = "api/products/category/$category",
            parameters = parameters,
            responseClass = ProductListResponse::class.java
        )
    }
} 