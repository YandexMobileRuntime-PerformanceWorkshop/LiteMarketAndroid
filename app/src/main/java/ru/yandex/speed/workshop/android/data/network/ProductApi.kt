package ru.yandex.speed.workshop.android.data.network

import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import ru.yandex.speed.workshop.android.domain.models.ProductDetailResponse
import ru.yandex.speed.workshop.android.domain.models.ProductListResponse

interface ProductApi {
    @GET("api/products")
    suspend fun getProductsList(@Query("page") page: Int, @Query("per_page") perPage: Int): ProductListResponse

    @GET("api/product/{id}")
    suspend fun getProductDetail(@Path("id") id: String): ProductDetailResponse

    @GET("api/products/search")
    suspend fun searchProducts(@Query("q") query: String, @Query("page") page: Int, @Query("per_page") perPage: Int): ProductListResponse

    // Удален неиспользуемый метод getProductsByCategory
}
