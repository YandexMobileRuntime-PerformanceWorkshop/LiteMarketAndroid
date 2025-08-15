package ru.yandex.speed.workshop.android.presentation.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import ru.yandex.speed.workshop.android.data.network.ProductService
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.cachedIn
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.flatMapLatest
import ru.yandex.speed.workshop.android.domain.models.Product
import kotlinx.coroutines.launch
import kotlinx.coroutines.ExperimentalCoroutinesApi

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class CatalogViewModel(private val productService: ProductService) : ViewModel() {
    private val queryFlow = MutableStateFlow("")
    

    private val favoriteProductsState = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val favoriteProducts: StateFlow<Map<String, Boolean>> = favoriteProductsState

    val productsFlow = queryFlow.flatMapLatest { query ->
        Pager(
            config = PagingConfig(pageSize = 20, prefetchDistance = 6, enablePlaceholders = false),
            pagingSourceFactory = { ProductsPagingSource(productService, query) }
        ).flow.cachedIn(viewModelScope)
    }

    fun setQuery(query: String) {
        queryFlow.value = query
    }
    

    fun onFavoriteClicked(product: Product) {
        updateProductFavoriteStatus(product.id, product.isFavorite)
    }
    
    fun updateProductFavoriteStatus(productId: String?, isFavorite: Boolean) {
        if (productId == null) return
        
        val currentFavorites = favoriteProductsState.value.toMutableMap()
        currentFavorites[productId] = isFavorite
        favoriteProductsState.value = currentFavorites
    }
    

    fun onProductClicked(product: Product, navigateAction: (String) -> Unit) {
        navigateAction(product.id)
    }
}