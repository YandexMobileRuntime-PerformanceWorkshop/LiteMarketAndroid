package ru.yandex.speed.workshop.android.presentation.catalog

import androidx.paging.PagingSource
import androidx.paging.PagingState
import ru.yandex.speed.workshop.android.data.network.ProductService
import ru.yandex.speed.workshop.android.domain.models.Product

class ProductsPagingSource(
    private val service: ProductService,
    private val query: String?
) : PagingSource<Int, Product>() {

    override suspend fun load(params: LoadParams<Int>): LoadResult<Int, Product> {
        return try {
            val page = params.key ?: 1
            val perPage = params.loadSize

            val response = if (query.isNullOrBlank()) {
                service.getProductsList(page = page, perPage = perPage)
            } else {
                service.searchProducts(query = query, page = page, perPage = perPage)
            }

            val data = response.products
            val nextKey = if (response.hasMore) page + 1 else null
            val prevKey = if (page > 1) page - 1 else null

            LoadResult.Page(
                data = data,
                prevKey = prevKey,
                nextKey = nextKey
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