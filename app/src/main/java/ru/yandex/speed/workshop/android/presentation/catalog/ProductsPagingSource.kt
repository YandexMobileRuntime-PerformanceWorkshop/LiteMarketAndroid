package ru.yandex.speed.workshop.android.presentation.catalog

import androidx.paging.PagingSource
import androidx.paging.PagingState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import ru.yandex.speed.workshop.android.data.mappers.toDomain
import ru.yandex.speed.workshop.android.data.network.ProductApi
import ru.yandex.speed.workshop.android.domain.models.Product

class ProductsPagingSource(
    private val api: ProductApi,
    private val query: String? = null,
    private val isSearch: Boolean = false,
) : PagingSource<Int, Product>() {
    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Product> {
        return try {
            val page = params.key ?: 1
            val perPage = params.loadSize

            val response =
                withContext(Dispatchers.IO) {
                    try {
                        if (!isSearch || query.isNullOrBlank()) {
                            api.getProductsList(page = page, perPage = perPage).execute().body()
                                ?: throw Exception("Empty response body")
                        } else {
                            api.searchProducts(query = query, page = page, perPage = perPage).execute().body()
                                ?: throw Exception("Empty response body")
                        }
                    } catch (e: Exception) {
                        // Логируем ошибку для отладки
                        android.util.Log.e("ProductsPagingSource", "API request failed", e)
                        throw e
                    }
                }

            val data = response.products.map { it.toDomain() }
            val nextKey = if (response.hasMore) page + 1 else null
            val prevKey = if (page > 1) page - 1 else null

            LoadResult.Page(
                data = data,
                prevKey = prevKey,
                nextKey = nextKey,
            )
        } catch (e: Exception) {
            LoadResult.Error(e)
        }
    }

    override fun getRefreshKey(state: PagingState<Int, Product>): Int? {
        val anchorPosition = state.anchorPosition ?: return null
        val anchorPage = state.closestPageToPosition(anchorPosition)
        return anchorPage?.prevKey?.plus(1) ?: anchorPage?.nextKey?.minus(1)
    }
}