package ru.yandex.speed.workshop.android.presentation.catalog

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
import ru.yandex.speed.workshop.android.data.network.ProductApi
import ru.yandex.speed.workshop.android.domain.models.Product
import ru.yandex.speed.workshop.android.domain.repository.ProductRepository
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class CatalogViewModel
    @Inject
    constructor(
        private val repository: ProductRepository,
        private val api: ProductApi,
    ) : ViewModel() {
        // Состояние поискового запроса
        private val _searchQuery = MutableStateFlow("")
        val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

        // Конфигурация пагинации
        private val pagingConfig =
            PagingConfig(
                pageSize = 12,
                enablePlaceholders = false,
                prefetchDistance = 4,
                initialLoadSize = 12,
            )

        init {}

        @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
        val productsPagingFlow: Flow<PagingData<Product>> =
            _searchQuery
                .debounce(300) // Добавляем задержку для предотвращения частых запросов при вводе
                .distinctUntilChanged()
                .flatMapLatest { query ->
                    Timber.d("Search query changed: '$query'")
                    if (query.isEmpty()) {
                        // Если поисковый запрос пуст, загружаем все товары
                        Timber.d("Loading all products")
                        Pager(pagingConfig) {
                            ProductsPagingSource(api, isSearch = false)
                        }.flow
                    } else {
                        // Иначе выполняем поиск
                        Timber.d("Searching for: $query")
                        Pager(pagingConfig) {
                            ProductsPagingSource(api, isSearch = true, query = query)
                        }.flow
                    }
                }
                .cachedIn(viewModelScope) // Кэшируем результаты

        /**
         * Обновление поискового запроса
         */
        fun updateSearchQuery(query: String) {
            Timber.d("Updating search query to: '$query'")
            _searchQuery.value = query
        }

        fun isProductFavorite(productId: String): Boolean {
            return  false
        }
    }
