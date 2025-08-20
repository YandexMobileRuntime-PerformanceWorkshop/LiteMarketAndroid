package ru.yandex.speed.workshop.android.presentation.productdetail

import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import com.google.android.material.tabs.TabLayoutMediator
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import ru.yandex.speed.workshop.android.R
import ru.yandex.speed.workshop.android.databinding.FragmentProductDetailBinding
import ru.yandex.speed.workshop.android.domain.models.ProductDetail
import ru.yandex.speed.workshop.android.presentation.common.SnackbarUtils
import ru.yandex.speed.workshop.android.presentation.ui.UiState
import ru.yandex.speed.workshop.android.utils.ImageLoader
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ProductDetailFragment : Fragment() {
    private val viewModel: ProductDetailViewModel by viewModels()

    @Inject
    lateinit var imageLoader: ImageLoader

    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!

    // Контейнеры для основного контента и скелетона
    private lateinit var contentContainer: FrameLayout
    private lateinit var skeletonContainer: FrameLayout

    // Состояние избранного
    private var isFavorite: Boolean = false
    private var isSellerFavorite: Boolean = false
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentProductDetailBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)
        
        // Инициализация контейнеров
        contentContainer = binding.contentContainer
        skeletonContainer = binding.skeletonContainer

        setupBackButton()
        setupFavoriteButton()
        setupSellerFavoriteButton()
        setupShareButton()
        setupDeliveryTabs()
        setupPaymentMethodButton()

        // Получаем ID продукта из аргументов
        arguments?.getString("productId")?.let { productId ->
            Timber.d("Loading product with ID: $productId")
            // ID продукта уже установлен через SavedStateHandle
        loadProductDetail(productId)
        } ?: run {
            Timber.e("No product ID provided in arguments")
            SnackbarUtils.showError(requireView(), getString(R.string.error_no_product_id))
            findNavController().navigateUp()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    private fun setupBackButton() {
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }
    }

    private fun setupFavoriteButton() {
        binding.favoriteButton.setOnClickListener {
            isFavorite = !isFavorite
            viewModel.setFavorite(isFavorite)
            updateFavoriteButton()
            
            val message =
                if (isFavorite) {
                    getString(R.string.message_product_added_to_favorites)
                } else {
                    getString(R.string.message_product_removed_from_favorites)
                }

            SnackbarUtils.showFavoriteAction(
                view = requireView(),
                message = message,
                isAdded = isFavorite,
                undoAction = {
                    // Undo действие - возвращаем статус обратно
                    isFavorite = !isFavorite
                    viewModel.setFavorite(isFavorite)
                    updateFavoriteButton()
                },
            )
        }
    }

    private fun updateFavoriteButton() {
        binding.favoriteButton.setImageResource(
            if (isFavorite) {
                R.drawable.ic_heart_filled
            } else {
                R.drawable.ic_heart
            },
        )
    }

    private fun setupShareButton() {
        binding.shareButton.setOnClickListener {
            Timber.d("Share button clicked")
            SnackbarUtils.showPlaceholder(requireView(), getString(R.string.message_share_product))
        }
    }

    private fun setupSellerFavoriteButton() {
        binding.sellerFavoriteButton.setOnClickListener {
            isSellerFavorite = !isSellerFavorite
            updateSellerFavoriteButton()
            
            val message =
                if (isSellerFavorite) {
                    getString(R.string.message_seller_added_to_favorites)
                } else {
                    getString(R.string.message_seller_removed_from_favorites)
                }

            SnackbarUtils.showFavoriteAction(
                view = requireView(),
                message = message,
                isAdded = isSellerFavorite,
                undoAction = {
                    // Undo действие - возвращаем статус продавца обратно
                    isSellerFavorite = !isSellerFavorite
                    updateSellerFavoriteButton()
                },
            )
        }
        
        binding.sellerDetailsButton.setOnClickListener {
            Timber.d("Seller details button clicked")
            SnackbarUtils.showPlaceholder(requireView(), getString(R.string.message_open_seller_page))
        }
    }
    
    private fun loadProductDetail(productId: String) {
        Timber.d("Loading product detail for ID: $productId")

        // Проверяем, есть ли данные из каталога
        val hasDataFromCatalog = checkCatalogDataEarly()

        if (hasDataFromCatalog) {
            // У нас есть данные из каталога - сразу их показываем
            Timber.d("Showing catalog data from arguments first")
            val catalogImages = tryGetImagesFromCatalog(productId)
            if (catalogImages != null) {
                // Предзагружаем первое изображение для избежания мигания
                if (catalogImages.isNotEmpty()) {
                    imageLoader.preloadImage(catalogImages[0])
                }

                setupGallery(catalogImages)

                // Затем загружаем полные данные с сервера для обновления
                viewModel.loadProductDetail()
            }
        } else {
            // Нет данных из каталога - загружаем с сервера
            viewModel.loadProductDetail()
        }

        // Подписываемся на обновления UI состояния
        lifecycleScope.launch {
            viewModel.uiState.collect { state ->
                when (state) {
                    is UiState.Loading -> {
                        if (!hasDataFromCatalog) {
                            showSkeleton()
                        }
                    }
                    is UiState.Success -> {
                        hideSkeletonShowContent()
                        updateProductDetailContent(state.data)
                    }
                    is UiState.PreloadedData -> {
                        if (state.isUpdating) {
                            if (contentContainer.visibility == View.VISIBLE) {
                                updateProductDetailContent(state.data)
                            } else {
                                hideSkeletonShowContent()
                                updateProductDetailContent(state.data)
                            }
                        } else {
                    hideSkeletonShowContent()
                            updateProductDetailContent(state.data)
                        }
                    }
                    is UiState.Error -> {
                        if (!hasDataFromCatalog) {
                            // Если нет данных из каталога, показываем ошибку
                            SnackbarUtils.showError(requireView(), state.message)
                } else {
                            // Если есть данные из каталога, показываем их, но уведомляем о проблеме с обновлением
                            SnackbarUtils.showError(
                                requireView(),
                                getString(R.string.error_failed_to_update, state.message)
                            )
                        }
                    }
                }
            }
        }

        // Подписываемся на обновления состояния избранного
        lifecycleScope.launch {
            viewModel.isFavorite.collect { isFavoriteState ->
                isFavorite = isFavoriteState
                updateFavoriteButton()
            }
        }
    }
    
    private fun tryGetImagesFromCatalog(productId: String): List<String>? {
        return try {
            // Check if we have product data passed via arguments
            val productTitle = arguments?.getString("productTitle")
            val productPrice = arguments?.getString("productPrice")
            val productImages = arguments?.getStringArray("productImages")
            
            if (productTitle != null && productImages != null) {
                Timber.d("Using complete product data from arguments: $productTitle")
                productImages.toList()
            } else {
                Timber.d("No product data in arguments, using fallback")
                null
            }
        } catch (e: Exception) {
            Timber.e(e, "Error accessing catalog data: ${e.message}")
            null
        }
    }

    // Переменная для хранения текущего состояния детальной информации о продукте
    private var currentProductDetail: ProductDetail? = null

    private fun updateProductDetailContent(productDetail: ProductDetail) {
        Timber.d("Updating product detail content: ${productDetail.title}")

        // Если это первое обновление, просто заполняем все данные
        if (currentProductDetail == null) {
            updateAllProductDetails(productDetail)
            currentProductDetail = productDetail
            return
        }

        // Создаем объект для сравнения старых и новых данных
        val diffCallback = ProductDetailDiffCallback(currentProductDetail!!, productDetail)

        // Обновляем только изменившиеся части UI
        if (diffCallback.hasBasicInfoChanged()) {
            val manufacturerName = productDetail.manufacturer.takeIf { it.isNotEmpty() } 
                ?: getString(R.string.unknown_manufacturer)
            updateTextWithoutFlicker(
                binding.manufacturerText, 
                getString(R.string.format_manufacturer_with_arrow, manufacturerName)
            )
            updateTextWithoutFlicker(binding.productTitle, productDetail.title)
        }

        if (diffCallback.hasPriceInfoChanged()) {
            // Используем текущую цену из доменной модели
            updateTextWithoutFlicker(binding.currentPriceText, productDetail.currentPrice)

            // Handle old price (может быть null или пустой)
            val oldPrice = productDetail.oldPrice
            
            if (!oldPrice.isNullOrEmpty() && oldPrice.isNotBlank()) {
                updateTextWithoutFlicker(binding.oldPriceText, oldPrice)
                binding.oldPriceText.paintFlags = binding.oldPriceText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                binding.oldPriceText.visibility = View.VISIBLE
            } else {
                // Скрываем старую цену, если её нет
                binding.oldPriceText.visibility = View.GONE
            }

            // Handle discount
            val discountPercent = productDetail.discountPercent
            
            if (discountPercent != null && discountPercent > 0) {
                // Используем значение из доменной модели
                updateTextWithoutFlicker(
                    binding.discountText,
                    getString(R.string.format_discount_percent, discountPercent)
                )
                binding.discountText.visibility = View.VISIBLE
            } else if (!productDetail.discountPercentage.isNullOrEmpty() && 
                       productDetail.discountPercentage.isNotBlank()) {
                // Используем значение из discountPercentage
                val formattedDiscount = if (productDetail.discountPercentage.endsWith("%")) {
                    productDetail.discountPercentage
                } else {
                    "${productDetail.discountPercentage}%"
                }
                updateTextWithoutFlicker(binding.discountText, formattedDiscount)
                binding.discountText.visibility = View.VISIBLE
            } else {
                // Скрываем скидку, если её нет
                binding.discountText.visibility = View.GONE
            }
        }

        if (diffCallback.hasRatingChanged()) {
            // Rating с проверкой на nullable и форматированием до 1 знака после запятой
            val ratingScore = productDetail.rating.score
            val formattedRating = ratingScore?.takeIf { it > 0 }?.let { 
                String.format("%.1f", it) 
            } ?: getString(R.string.default_rating)
            updateTextWithoutFlicker(binding.ratingText, formattedRating)

            val reviewsCount = productDetail.rating.reviewsCount
            val formattedReviews = reviewsCount?.takeIf { it > 0 }?.let { 
                getString(R.string.format_reviews_count, it) 
            } ?: getString(R.string.default_reviews_count)
            updateTextWithoutFlicker(binding.reviewsCountText, formattedReviews)
        }

        if (diffCallback.hasSellerChanged()) {
            // Seller data with fallbacks
            val sellerName = productDetail.seller.takeIf { it.isNotEmpty() } 
                ?: getString(R.string.default_seller_name)
            updateTextWithoutFlicker(binding.sellerNameText, sellerName)

            val sellerRating = productDetail.sellerRating
            val sellerReviews = productDetail.sellerReviewsCount
            if (sellerRating > 0 && sellerReviews.isNotEmpty()) {
                updateTextWithoutFlicker(
                    binding.sellerRatingText, 
                    getString(R.string.format_seller_rating, sellerRating.toString(), sellerReviews)
                )
            } else {
                updateTextWithoutFlicker(
                    binding.sellerRatingText, 
                    getString(R.string.default_seller_rating)
                )
            }
        }

        if (diffCallback.hasPromoCodeChanged()) {
            // Handle promo code
            productDetail.promoCode?.let { promo ->
                updateTextWithoutFlicker(binding.promoDiscountText, promo.discount)
                val promoText =
                    if (promo.minOrder != null && promo.expiryDate != null) {
                        getString(
                            R.string.format_promo_code,
                            promo.code,
                            promo.minOrder,
                            promo.expiryDate
                        )
                    } else {
                        getString(R.string.format_promo_code_simple, promo.code)
                    }
                updateTextWithoutFlicker(binding.promoCodeText, promoText)
                binding.promoDiscountText.visibility = View.VISIBLE
                binding.promoCodeText.visibility = View.VISIBLE
            } ?: run {
                // Show sample promo code for demonstration
                updateTextWithoutFlicker(
                    binding.promoDiscountText, 
                    getString(R.string.default_promo_discount)
                )
                updateTextWithoutFlicker(
                    binding.promoCodeText,
                    getString(R.string.default_promo_code)
                )
                binding.promoDiscountText.visibility = View.VISIBLE
                binding.promoCodeText.visibility = View.VISIBLE
            }
        }

        // Определяем список изображений для галереи
        val galleryImages = productDetail.imageUrls

        // Логируем информацию об изображениях
        Timber.d("Gallery images: ${galleryImages.size}")

        // Обновляем галерею только если изменились изображения и есть что показывать
        if (diffCallback.hasImagesChanged() && galleryImages.isNotEmpty()) {
            setupGallery(galleryImages)
        }

        // Обновляем текущее состояние
        currentProductDetail = productDetail
    }

    /**
     * Обновляет все данные о продукте без проверок (используется при первом отображении)
     */
    private fun updateAllProductDetails(productDetail: ProductDetail) {
        // Базовая информация
        val manufacturerName = productDetail.manufacturer.takeIf { it.isNotEmpty() } 
            ?: getString(R.string.unknown_manufacturer)
        updateTextWithoutFlicker(
            binding.manufacturerText, 
            getString(R.string.format_manufacturer_with_arrow, manufacturerName)
        )
        updateTextWithoutFlicker(binding.productTitle, productDetail.title)
        
        // Используем текущую цену из доменной модели
        updateTextWithoutFlicker(binding.currentPriceText, productDetail.currentPrice)
        
        // Handle old price (может быть null или пустой)
        val oldPrice = productDetail.oldPrice
        
        if (!oldPrice.isNullOrEmpty() && oldPrice.isNotBlank()) {
            updateTextWithoutFlicker(binding.oldPriceText, oldPrice)
            binding.oldPriceText.paintFlags = binding.oldPriceText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            binding.oldPriceText.visibility = View.VISIBLE
        } else {
            // Скрываем старую цену, если её нет
            binding.oldPriceText.visibility = View.GONE
        }
        
        // Handle discount
        val discountPercent = productDetail.discountPercent
        
        if (discountPercent != null && discountPercent > 0) {
            // Используем значение из доменной модели
            updateTextWithoutFlicker(
                binding.discountText,
                getString(R.string.format_discount_percent, discountPercent)
            )
            binding.discountText.visibility = View.VISIBLE
        } else if (!productDetail.discountPercentage.isNullOrEmpty() && 
                  productDetail.discountPercentage.isNotBlank()) {
            // Используем значение из discountPercentage
            val formattedDiscount = if (productDetail.discountPercentage.endsWith("%")) {
                productDetail.discountPercentage
            } else {
                "${productDetail.discountPercentage}%"
            }
            updateTextWithoutFlicker(binding.discountText, formattedDiscount)
            binding.discountText.visibility = View.VISIBLE
        } else {
            // Скрываем скидку, если её нет
            binding.discountText.visibility = View.GONE
        }
        
        // Rating с проверкой на nullable и форматированием до 1 знака после запятой
        val ratingScore = productDetail.rating.score
        val formattedRating = ratingScore?.takeIf { it > 0 }?.let { 
            String.format("%.1f", it) 
        } ?: getString(R.string.default_rating)
        updateTextWithoutFlicker(binding.ratingText, formattedRating)
        
        val reviewsCount = productDetail.rating.reviewsCount
        val formattedReviews = reviewsCount?.takeIf { it > 0 }?.let { 
            getString(R.string.format_reviews_count, it) 
        } ?: getString(R.string.default_reviews_count)
        updateTextWithoutFlicker(binding.reviewsCountText, formattedReviews)
        
        // Seller data with fallbacks
        val sellerName = productDetail.seller.takeIf { it.isNotEmpty() } 
            ?: getString(R.string.default_seller_name)
        updateTextWithoutFlicker(binding.sellerNameText, sellerName)
        
        val sellerRating = productDetail.sellerRating
        val sellerReviews = productDetail.sellerReviewsCount
        if (sellerRating > 0 && sellerReviews.isNotEmpty()) {
            updateTextWithoutFlicker(
                binding.sellerRatingText, 
                getString(R.string.format_seller_rating, sellerRating.toString(), sellerReviews)
            )
        } else {
            updateTextWithoutFlicker(
                binding.sellerRatingText, 
                getString(R.string.default_seller_rating)
            )
        }
        
        // Handle promo code
        productDetail.promoCode?.let { promo ->
            updateTextWithoutFlicker(binding.promoDiscountText, promo.discount)
            // Обрабатываем префикс "Промокод" в коде промокода
            val cleanCode = if (promo.code.startsWith(getString(R.string.prefix_promo_code))) {
                promo.code.substring(getString(R.string.prefix_promo_code).length)
            } else {
                promo.code
            }
            
            val promoText =
                if (promo.minOrder != null && promo.expiryDate != null) {
                    getString(
                        R.string.format_promo_code,
                        cleanCode,
                        promo.minOrder,
                        promo.expiryDate
                    )
                } else {
                    getString(R.string.format_promo_code_simple, cleanCode)
                }
            updateTextWithoutFlicker(binding.promoCodeText, promoText)
            binding.promoDiscountText.visibility = View.VISIBLE
            binding.promoCodeText.visibility = View.VISIBLE
        } ?: run {
            // Show sample promo code for demonstration
            updateTextWithoutFlicker(
                binding.promoDiscountText, 
                getString(R.string.default_promo_discount)
            )
            updateTextWithoutFlicker(
                binding.promoCodeText,
                getString(R.string.default_promo_code)
            )
            binding.promoDiscountText.visibility = View.VISIBLE
            binding.promoCodeText.visibility = View.VISIBLE
        }

        // Определяем список изображений для галереи
        val galleryImages = productDetail.imageUrls

        // Логируем информацию об изображениях
        Timber.d("Gallery images for initial setup: ${galleryImages.size}")

        // Настраиваем галерею только если есть изображения
        if (galleryImages.isNotEmpty()) {
            setupGallery(galleryImages)
        }
    }

    // Обновляем текст без мерцания, только если он изменился
    private fun updateTextWithoutFlicker(
        textView: TextView,
        newText: String,
    ) {
        if (textView.text.toString() != newText) {
            textView.text = newText
        }
    }
    
    private fun setupGallery(imageUrls: List<String>) {
        Timber.d("Setting up gallery with ${imageUrls.size} images")
        
        // Always setup gallery, even if empty
        val galleryAdapter = ProductGalleryAdapter(requireContext(), imageLoader)
        binding.productGallery.adapter = galleryAdapter
        galleryAdapter.submitList(imageUrls)

        // Setup indicators
        TabLayoutMediator(binding.galleryIndicator, binding.productGallery) { tab, _ ->
            // Пустой конфигуратор, нам нужны только индикаторы
        }.attach()
    }

    private fun setupDeliveryTabs() {
        // Настраиваем табы доставки
        val tabLayout = binding.deliveryMethodTabs
        tabLayout.removeAllTabs()

        val deliveryTab = tabLayout.newTab().setText(getString(R.string.tab_delivery))
        val pickupTab = tabLayout.newTab().setText(getString(R.string.tab_pickup))

        tabLayout.addTab(deliveryTab)
        tabLayout.addTab(pickupTab)
    }

    private fun setupPaymentMethodButton() {
        binding.paymentMethodButton.setOnClickListener {
            Timber.d("Payment method button clicked")
            SnackbarUtils.showPlaceholder(
                requireView(), 
                getString(R.string.message_select_payment_method)
            )
        }
    }
    
    private fun updateSellerFavoriteButton() {
        binding.sellerFavoriteButton.setImageResource(
            if (isSellerFavorite) {
            R.drawable.ic_heart_filled
        } else {
            R.drawable.ic_heart
            },
        )
    }

    private fun showSkeleton() {
        Timber.d("Showing skeleton")
        skeletonContainer.visibility = View.VISIBLE
        contentContainer.visibility = View.GONE
    }

    private fun hideSkeletonShowContent() {
        Timber.d("Hiding skeleton, showing content")
        skeletonContainer.visibility = View.GONE
        contentContainer.visibility = View.VISIBLE
    }

    private fun checkCatalogDataEarly(): Boolean {
        val hasTitle = arguments?.getString("productTitle") != null
        val hasImages = arguments?.getStringArray("productImages") != null
        val hasPrice = arguments?.getString("productPrice") != null
        return hasTitle && hasImages && hasPrice
    }
}