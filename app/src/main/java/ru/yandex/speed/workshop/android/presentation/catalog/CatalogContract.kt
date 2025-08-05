package ru.yandex.speed.workshop.android.presentation.catalog

import ru.yandex.speed.workshop.android.domain.models.Product
import ru.yandex.speed.workshop.android.presentation.common.BasePresenter
import ru.yandex.speed.workshop.android.presentation.common.BaseView

/**
 * MVP контракт для экрана каталога товаров
 */
interface CatalogContract {
    
    /**
     * View интерфейс для каталога (аналог iOS ProductsView)
     */
    interface View : BaseView {
        fun showProducts(products: List<Product>)
        fun addProducts(products: List<Product>) // Новый метод для пагинации
        fun showPaginationLoading(isLoading: Boolean)
        fun navigateToProductDetail(productId: String)
    }
    
    /**
     * Presenter интерфейс для каталога (аналог iOS ProductsPresenterProtocol)
     */
    interface Presenter : BasePresenter<View> {
        fun loadProducts(refresh: Boolean = false)
        fun loadNextPageIfNeeded()
        fun searchProducts(query: String)
        fun onProductClicked(product: Product)
        fun onFavoriteClicked(product: Product)
        fun updateProductFavoriteStatus(productId: String?, isFavorite: Boolean)
        fun onRefresh()
        fun hasData(): Boolean
        fun restoreViewState()
    }
} 