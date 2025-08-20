package ru.yandex.speed.workshop.android.presentation.productdetail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import ru.yandex.speed.workshop.android.data.local.FavoritesManager
import ru.yandex.speed.workshop.android.domain.models.ProductDetail
import ru.yandex.speed.workshop.android.domain.models.ProductRating
import ru.yandex.speed.workshop.android.domain.models.ProductDelivery
import ru.yandex.speed.workshop.android.domain.models.ProductDeliveryOption
import ru.yandex.speed.workshop.android.domain.models.PromoCode
import ru.yandex.speed.workshop.android.domain.repository.ProductRepository
import ru.yandex.speed.workshop.android.domain.repository.Result
import ru.yandex.speed.workshop.android.presentation.ui.UiState
import timber.log.Timber
import javax.inject.Inject

/**
 * ViewModel для экрана деталей продукта
 */
@HiltViewModel
class ProductDetailViewModel
    @Inject
    constructor(
        private val repository: ProductRepository,
        private val favoritesManager: FavoritesManager,
        savedStateHandle: SavedStateHandle,
    ) : ViewModel() {
        // Идентификатор продукта
        private val productId: String = savedStateHandle.get<String>("productId") ?: ""

        // UI состояние
        private val _uiState = MutableStateFlow<UiState<ProductDetail>>(UiState.Loading)
        val uiState: StateFlow<UiState<ProductDetail>> = _uiState.asStateFlow()

        // Состояние избранного
        private val _isFavorite =
            MutableStateFlow(
                savedStateHandle.get<Boolean>("isFavorite") ?: favoritesManager.isFavorite(productId),
            )
        val isFavorite: StateFlow<Boolean> = _isFavorite.asStateFlow()

        init {
            Timber.d("ProductDetailViewModel initialized for product: $productId")

            // Проверяем наличие предварительных данных
            val preloadedData = createPreloadedDataFromArgs(savedStateHandle)
            if (preloadedData != null) {
                _uiState.value = UiState.PreloadedData(preloadedData)
            }
        }

        /**
         * Загрузка деталей продукта
         */
        fun loadProductDetail() {
            if (productId.isEmpty()) {
                _uiState.value = UiState.Error("Product ID is empty")
                return
            }

            // Если у нас уже есть предварительные данные, устанавливаем флаг обновления
            val currentState = _uiState.value
            if (currentState is UiState.PreloadedData) {
                _uiState.value = UiState.PreloadedData(currentState.data, isUpdating = true)
            } else {
                _uiState.value = UiState.Loading
            }

            viewModelScope.launch {
                Timber.d("Loading product detail for ID: $productId")

                when (val result = repository.getProductDetail(productId)) {
                    is Result.Success -> {
                        Timber.d("Successfully loaded product detail: ${result.data.title}")
                        _uiState.value = UiState.Success(result.data)
                    }
                    is Result.Error -> {
                        Timber.e(result.exception, "Error loading product detail")

                        // Если у нас есть предварительные данные, оставляем их
                        if (currentState is UiState.PreloadedData) {
                            _uiState.value = UiState.PreloadedData(currentState.data, isUpdating = false)
                        } else {
                            _uiState.value =
                                UiState.Error(
                                    result.exception.localizedMessage ?: "Failed to load product details",
                                )
                        }
                    }
                    is Result.Loading -> {
                        // Уже установлено выше
                    }
                }
            }
        }

        /**
         * Переключение состояния избранного
         */
        fun toggleFavorite() {
            val newState = favoritesManager.toggleFavorite(productId)
            _isFavorite.value = newState
            Timber.d("Product $productId favorite state changed to $newState")
        }

        /**
         * Установка состояния избранного
         */
        fun setFavorite(isFavorite: Boolean) {
            if (isFavorite) {
                favoritesManager.addToFavorites(productId)
            } else {
                favoritesManager.removeFromFavorites(productId)
            }
            _isFavorite.value = isFavorite
        }

        /**
         * Создание предварительных данных из аргументов
         */
        private fun createPreloadedDataFromArgs(savedStateHandle: SavedStateHandle): ProductDetail? {
            return try {
                val productTitle = savedStateHandle.get<String>("productTitle")
                val productImagesArray = savedStateHandle.get<Array<String>>("productImages")

                if (productTitle == null || productImagesArray == null) {
                    return null
                }

                val productImages = productImagesArray.toList()

                val productPrice = savedStateHandle.get<String>("productPrice") ?: ""
                val productOldPrice = savedStateHandle.get<String>("productOldPrice")
                val productDiscountPercent = savedStateHandle.get<Int>("productDiscountPercent") ?: 0
                val productRatingScore = savedStateHandle.get<Float>("productRatingScore") ?: 0f
                val productRatingReviews = savedStateHandle.get<Int>("productRatingReviews") ?: 0
                val productVendor = savedStateHandle.get<String>("productVendor") ?: "Unknown"
                val productShopName = savedStateHandle.get<String>("productShopName") ?: "Unknown"

                // Получаем данные о промокоде, если они есть
                val promoCodeValue = savedStateHandle.get<String>("promoCode")
                val promoDiscount = savedStateHandle.get<String>("promoDiscount")
                val promoMinOrder = savedStateHandle.get<String>("promoMinOrder")
                val promoExpiryDate = savedStateHandle.get<String>("promoExpiryDate")
                
                // Получаем данные о доставке, если они есть
                val deliveryProvider = savedStateHandle.get<String>("deliveryProvider") ?: "Яндекс Доставка"
                val deliveryType = savedStateHandle.get<String>("deliveryType") ?: "Доставка"
                val deliveryDate = savedStateHandle.get<String>("deliveryDate") ?: "Завтра"
                val deliveryDetails = savedStateHandle.get<String>("deliveryDetails") ?: "Бесплатно"
                
                // Создаем предварительные данные с новой структурой модели
                ProductDetail(
                    id = productId,
                    title = productTitle,
                    currentPrice = productPrice,
                    oldPrice = productOldPrice,
                    discountPercent = if (productDiscountPercent > 0) productDiscountPercent else null,
                    imageUrls = productImages,
                    manufacturer = productVendor,
                    seller = productShopName,
                    rating = ProductRating(
                        score = productRatingScore.toDouble(),
                        reviewsCount = productRatingReviews
                    ),
                    isFavorite = _isFavorite.value,
                    // Инициализируем промокод, если данные есть
                    promoCode = if (promoCodeValue != null && promoDiscount != null) {
                        PromoCode(
                            code = promoCodeValue,
                            discount = promoDiscount,
                            minOrder = promoMinOrder,
                            expiryDate = promoExpiryDate
                        )
                    } else null,
                    // Инициализируем доставку с полученными данными
                    delivery = ProductDelivery(
                        provider = deliveryProvider,
                        options = listOf(
                            ProductDeliveryOption(
                                type = deliveryType,
                                date = deliveryDate,
                                details = deliveryDetails,
                                isSelected = true
                            )
                        )
                    ),
                    paymentMethod = "Картой онлайн",
                )
            } catch (e: Exception) {
                Timber.e(e, "Error creating preloaded data from args")
                null
            }
        }
    }