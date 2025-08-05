package ru.yandex.speed.workshop.android.presentation.catalog

import android.util.Log
import kotlinx.coroutines.*
import ru.yandex.speed.workshop.android.data.network.NetworkException
import ru.yandex.speed.workshop.android.data.network.ProductService
import ru.yandex.speed.workshop.android.domain.models.Product

/**
 * Singleton presenter to preserve data across fragment recreations
 */
class CatalogPresenter private constructor(
    private val productService: ProductService
) : CatalogContract.Presenter {
    
    companion object {
        private const val TAG = "CatalogPresenter"
        
        @Volatile
        private var INSTANCE: CatalogPresenter? = null
        
        fun getInstance(productService: ProductService): CatalogPresenter {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CatalogPresenter(productService).also { INSTANCE = it }
            }
        }
        
        fun clearInstance() {
            INSTANCE?.cleanup()
            INSTANCE = null
        }
    }
    
    private var view: CatalogContract.View? = null
    private val presenterScope = CoroutineScope(Dispatchers.Main)
    
    // Data state preserved across fragment recreations
    private val allProducts: MutableList<Product> = mutableListOf()
    private var filteredProducts: List<Product> = emptyList()
    private var currentPage = 1
    private var hasMorePages = true
    private var isLoading = false
    private var currentSearchQuery = ""
    private val itemsPerPage = 20

    override fun attachView(view: CatalogContract.View) {
        Log.d(TAG, "attachView called - current data: ${allProducts.size} products")
        this.view = view
        
        // Restore view state if we have data
        if (allProducts.isNotEmpty()) {
            Log.d(TAG, "Restoring data to view immediately")
            restoreViewState()
        }
    }

    override fun detachView() {
        Log.d(TAG, "detachView called - preserving data: ${allProducts.size} products")
        this.view = null
        // Don't cancel scope or clear data - keep it for next attachment
    }

    override fun loadProducts(refresh: Boolean) {
        if (isLoading) return
        
        Log.d(TAG, "Loading products: refresh=$refresh, page=$currentPage")
        
        if (refresh) {
            currentPage = 1
            hasMorePages = true
            allProducts.clear()
            // Don't clear search query on refresh - keep user's search
        }
        
        if (!hasMorePages) return
        
        isLoading = true
        Log.d(TAG, "🔒 Setting isLoading=true for ${if (refresh) "refresh" else "pagination"}")
        
        // Show appropriate loading indicator
        if (refresh) {
            // Only show loading if we have existing data (not first load)
            if (allProducts.isNotEmpty()) {
                view?.showLoading(true)
            }
            // For first load, skeletons should already be shown by the view
        } else {
            view?.showPaginationLoading(true)
        }
        
        presenterScope.launch {
            try {
                Log.d(TAG, "Making API request: page=$currentPage, perPage=$itemsPerPage")
                val response = productService.getProductsList(
                    page = currentPage,
                    perPage = itemsPerPage
                )
                
                Log.d(TAG, "API response: ${response.products.size} products, hasMore=${response.hasMore}")
                
                // Add new products to the list
                val newProducts = response.products
                allProducts.addAll(newProducts)
                hasMorePages = response.hasMore
                currentPage++
                
                // If no search query, show products
                if (currentSearchQuery.isEmpty()) {
                    filteredProducts = allProducts.toList()
                    
                    if (refresh) {
                        // First load or refresh - replace all products
                        Log.d(TAG, "📄 About to call view?.showProducts with ${filteredProducts.size} products (refresh)")
                        view?.showProducts(filteredProducts)
                        Log.d(TAG, "✅ Called view?.showProducts successfully")
                    } else {
                        // Pagination - add new products to existing list
                        Log.d(TAG, "➕ About to call view?.addProducts with ${newProducts.size} new products (pagination)")
                        view?.addProducts(newProducts)
                        Log.d(TAG, "✅ Called view?.addProducts successfully")
                    }
                } else {
                    // Re-apply search filter
                    Log.d(TAG, "Re-applying search filter for: '$currentSearchQuery'")
                    filterProducts(currentSearchQuery)
                }
                
            } catch (e: NetworkException) {
                Log.e(TAG, "Network error: ${e.message}", e)
                view?.showError(getErrorMessage(e))
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error: ${e.message}", e)
                view?.showError("Произошла неожиданная ошибка: ${e.message}")
            } finally {
                isLoading = false
                Log.d(TAG, "🔓 Setting isLoading=false, calling view?.showLoading(false)")
                view?.showLoading(false)
                // Don't call showPaginationLoading(false) for refresh - only for actual pagination
                if (!refresh) {
                    Log.d(TAG, "🔓 Calling showPaginationLoading(false) for pagination")
                    view?.showPaginationLoading(false)
                }
            }
        }
    }

    override fun loadNextPageIfNeeded() {
        Log.d(TAG, "loadNextPageIfNeeded called: isLoading=$isLoading, hasMorePages=$hasMorePages, query='$currentSearchQuery', currentPage=$currentPage")
        
        // Don't load if already loading, no more pages, or searching
        if (isLoading || !hasMorePages || currentSearchQuery.isNotEmpty()) {
            Log.d(TAG, "Skipping pagination load: isLoading=$isLoading, hasMorePages=$hasMorePages, searching=${currentSearchQuery.isNotEmpty()}")
            return
        }
        
        Log.d(TAG, "✅ Proceeding with pagination load for page $currentPage")
        loadProducts(refresh = false)
    }

    override fun searchProducts(query: String) {
        currentSearchQuery = query.trim()
        
        if (currentSearchQuery.isEmpty()) {
            // Show all products if search query is empty
            filteredProducts = allProducts.toList()
            view?.showProducts(filteredProducts)
        } else {
            // Filter existing products
            filterProducts(currentSearchQuery)
        }
    }

    override fun onProductClicked(product: Product) {
        view?.navigateToProductDetail(product.id)
    }

    override fun onFavoriteClicked(product: Product) {
        // Status already changed in adapter for immediate UI feedback
        Log.d(TAG, "Product ${product.title} favorite status changed to: ${product.isFavorite}")
        
        // Here we could add logic to sync with backend/local storage
        // For now, just log the change
    }

    override fun updateProductFavoriteStatus(productId: String?, isFavorite: Boolean) {
        if (productId == null) return
        
        // Find and update product in allProducts list
        val productIndex = allProducts.indexOfFirst { it.id == productId }
        if (productIndex != -1) {
            allProducts[productIndex].isFavorite = isFavorite
            Log.d(TAG, "Updated product $productId favorite status to: $isFavorite")
            
            // Update the current filtered products and refresh UI
            val currentProducts = allProducts.filter { 
                currentSearchQuery.isEmpty() || 
                it.title.contains(currentSearchQuery, ignoreCase = true) 
            }
            view?.showProducts(currentProducts)
        } else {
            Log.w(TAG, "Product with ID $productId not found in allProducts list")
        }
    }

    override fun onRefresh() {
        Log.d(TAG, "onRefresh() called - currentSearchQuery: '$currentSearchQuery'")
        loadProducts(refresh = true)
    }
    
    override fun hasData(): Boolean {
        val hasData = allProducts.isNotEmpty()
        Log.d(TAG, "hasData(): $hasData (${allProducts.size} products)")
        return hasData
    }
    
    override fun restoreViewState() {
        Log.d(TAG, "restoreViewState: currentSearchQuery='$currentSearchQuery', products=${allProducts.size}")
        
        if (currentSearchQuery.isEmpty()) {
            // Show all products
            filteredProducts = allProducts.toList()
            view?.showProducts(filteredProducts)
        } else {
            // Re-apply search filter
            filterProducts(currentSearchQuery)
        }
    }

    private fun filterProducts(query: String) {
        filteredProducts = allProducts.filter { product ->
            product.title.contains(query, ignoreCase = true)
        }
        Log.d(TAG, "filterProducts: '${query}' -> ${filteredProducts.size} products from ${allProducts.size} total")
        view?.showProducts(filteredProducts)
    }

    private fun getErrorMessage(exception: NetworkException): String {
        return when (exception) {
            is NetworkException.NetworkError -> "Ошибка сети. Проверьте подключение к интернету"
            is NetworkException.HttpError -> "Ошибка сервера (${exception.statusCode})"
            is NetworkException.DecodingError -> "Ошибка обработки данных"
            is NetworkException.InvalidUrl -> "Неправильный URL"
        }
    }
    
    private fun cleanup() {
        Log.d(TAG, "cleanup() called")
        view = null
        presenterScope.cancel()
        allProducts.clear()
        filteredProducts = emptyList()
        currentPage = 1
        hasMorePages = true
        isLoading = false
        currentSearchQuery = ""
    }
} 