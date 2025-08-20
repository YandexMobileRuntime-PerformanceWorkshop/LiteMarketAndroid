package ru.yandex.speed.workshop.android.data.network

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import ru.yandex.speed.workshop.android.domain.models.ProductDetailResponse
import ru.yandex.speed.workshop.android.domain.models.ProductListResponse

interface ProductApi {
    @GET("api/products")
    fun getProductsList(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
    ): Call<ProductListResponse>

    @GET("api/product/{id}")
    fun getProductDetail(
        @Path("id") id: String,
    ): Call<ProductDetailResponse>

    @GET("api/products/search")
    fun searchProducts(
        @Query("q") query: String,
        @Query("page") page: Int,
        @Query("per_page") perPage: Int,
    ): Call<ProductListResponse>

    // Удален неиспользуемый метод getProductsByCategory
}
