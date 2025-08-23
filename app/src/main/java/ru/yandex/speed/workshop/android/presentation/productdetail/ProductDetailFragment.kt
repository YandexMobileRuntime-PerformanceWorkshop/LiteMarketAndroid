package ru.yandex.speed.workshop.android.presentation.productdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
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
import ru.yandex.speed.workshop.android.presentation.common.extensions.setStrikeThroughText
import ru.yandex.speed.workshop.android.presentation.common.extensions.setTextIfChanged
import ru.yandex.speed.workshop.android.presentation.common.extensions.setVisibleIf
import ru.yandex.speed.workshop.android.presentation.productdetail.presenters.ProductImagePresenter
import ru.yandex.speed.workshop.android.presentation.productdetail.presenters.ProductInfoPresenter
import ru.yandex.speed.workshop.android.presentation.productdetail.presenters.ProductPricePresenter
import ru.yandex.speed.workshop.android.presentation.ui.UiState
import ru.yandex.speed.workshop.android.utils.ImageLoader
import ru.yandex.speed.workshop.android.utils.LCPTrackingTime
import ru.yandex.speed.workshop.android.utils.MVIScreenAnalytics
import ru.yandex.speed.workshop.android.utils.PerformanceMetricManager
import ru.yandex.speed.workshop.android.utils.PerformanceTimestamp
import ru.yandex.speed.workshop.android.utils.StatusBarUtils
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class ProductDetailFragment : Fragment() {
    private val viewModel: ProductDetailViewModel by viewModels()

    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var mviScreenAnalytics: MVIScreenAnalytics

    @Inject
    lateinit var performanceMetricManager: PerformanceMetricManager

    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = _binding!!
    private var isFirstContentLoad = true

    private lateinit var pricePresenter: ProductPricePresenter
    private lateinit var imagePresenter: ProductImagePresenter
    private lateinit var infoPresenter: ProductInfoPresenter

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

        // Инициализация презентеров
        pricePresenter = ProductPricePresenter()
        imagePresenter = ProductImagePresenter()
        infoPresenter = ProductInfoPresenter(requireContext())

        // Инициализация контейнеров
        contentContainer = binding.contentContainer
        skeletonContainer = binding.skeletonContainer

        // Fix status bar overlap issue
        setupStatusBarPadding()

        // Инициализация MVIScreenAnalytics для трекинга LCP
        initializeLCPTracking()

        setupButtons()
        setupDeliveryTabs()

        // Получаем ID продукта из аргументов
        arguments?.getString("productId")?.let { productId ->
            Timber.d("Loading product with ID: $productId")
            loadProductDetail(productId)
        } ?: run {
            Timber.e("No product ID provided in arguments")
            SnackbarUtils.showError(requireView(), getString(R.string.error_no_product_id))
            findNavController().navigateUp()
        }
    }

    /**
     * Инициализация трекинга LCP на основе переданных аргументов
     */
    private fun initializeLCPTracking() {
        // Получаем timestamp создания экрана из аргументов, если есть
        val screenCreationTimestamp = arguments?.getLong("screen_creation_timestamp", 0L)

        if (screenCreationTimestamp != null && screenCreationTimestamp > 0L) {
            // Создаем timestamp из сохраненного времени (в миллисекундах)
            val timestamp = PerformanceTimestamp.fromMilliseconds(screenCreationTimestamp)
            Timber.d("Initializing LCP tracking from screen creation: $screenCreationTimestamp ms")
            mviScreenAnalytics.initialize(
                trackingTimeType = LCPTrackingTime.FROM_SCREEN_CREATION,
                creationTimestamp = timestamp,
            )
        } else {
            // Инициализируем трекинг от старта приложения
            Timber.d("Initializing LCP tracking from app start")
            mviScreenAnalytics.initialize(trackingTimeType = LCPTrackingTime.FROM_APP_START)
        }

        // Сбрасываем флаг первой загрузки
        isFirstContentLoad = true
    }

    override fun onDestroyView() {
        super.onDestroyView()

        // Анализ загрузки изображений перед уничтожением представления
        imageLoader.analyzeImageLoadingMetrics()

        _binding = null
    }

    private fun setupButtons() {
        // Кнопка возврата
        binding.backButton.setOnClickListener {
            findNavController().navigateUp()
        }

        // Кнопка избранного для продукта
        binding.favoriteButton.setOnClickListener {
            isFavorite = !isFavorite
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
                    isFavorite = !isFavorite
                    updateFavoriteButton()
                },
            )
        }

        // Кнопка поделиться
        binding.shareButton.setOnClickListener {
            Timber.d("Share button clicked")
            SnackbarUtils.showPlaceholder(requireView(), getString(R.string.message_share_product))
        }

        // Кнопка избранного для продавца
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
                    isSellerFavorite = !isSellerFavorite
                    updateSellerFavoriteButton()
                },
            )
        }

        // Кнопка деталей продавца
        binding.sellerDetailsButton.setOnClickListener {
            Timber.d("Seller details button clicked")
            SnackbarUtils.showPlaceholder(requireView(), getString(R.string.message_open_seller_page))
        }

        // Кнопка выбора способа оплаты
        binding.paymentMethodButton.setOnClickListener {
            Timber.d("Payment method button clicked")
            SnackbarUtils.showPlaceholder(requireView(), getString(R.string.message_select_payment_method))
        }
    }

    private fun updateFavoriteButton() {
        binding.favoriteButton.setImageResource(
            if (isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart,
        )
    }

    private fun updateSellerFavoriteButton() {
        binding.sellerFavoriteButton.setImageResource(
            if (isSellerFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart,
        )
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
                                getString(R.string.error_failed_to_update, state.message),
                            )
                        }
                    }
                }
            }
        }
    }

    private fun tryGetImagesFromCatalog(productId: String): List<String>? {
        return try {
            val productImages = arguments?.getStringArray("productImages")

            if (arguments?.getString("productTitle") != null && productImages != null) {
                Timber.d("Using complete product data from arguments: ${arguments?.getString("productTitle")}")
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

        // Если это первое обновление или отсутствует предыдущий продукт для сравнения
        if (currentProductDetail == null) {
            updateAllProductDetails(productDetail)
            currentProductDetail = productDetail

            // Если это первая загрузка контента, логируем LCP
            if (isFirstContentLoad) {
                isFirstContentLoad = false
                Timber.d("Logging LCP for first content load")
                mviScreenAnalytics.logLCP("ProductDetail")
            }
            return
        }

        // Создаем объект для сравнения старых и новых данных
        val diffCallback = ProductDetailDiffCallback(currentProductDetail!!, productDetail)

        // Обновляем только изменившиеся части UI
        if (diffCallback.hasBasicInfoChanged()) {
            updateBasicInfo(productDetail)
        }

        if (diffCallback.hasPriceInfoChanged()) {
            updatePriceInfo(productDetail)
        }

        if (diffCallback.hasRatingChanged()) {
            updateRatingInfo(productDetail)
        }

        if (diffCallback.hasSellerChanged()) {
            updateSellerInfo(productDetail)
        }

        if (diffCallback.hasPromoCodeChanged()) {
            updatePromoCode(productDetail)
        }

        // Обновляем галерею только если изменились изображения
        if (diffCallback.hasImagesChanged()) {
            val galleryImages = imagePresenter.getGalleryImages(productDetail)
            if (!galleryImages.isEmpty) {
                setupGallery(galleryImages.imageUrls)
            }
        }

        // Обновляем текущее состояние
        currentProductDetail = productDetail
    }

    /**
     * Обновляет все данные о продукте без проверок (используется при первом отображении)
     */
    private fun updateAllProductDetails(productDetail: ProductDetail) {
        updateBasicInfo(productDetail)
        updatePriceInfo(productDetail)
        updateRatingInfo(productDetail)
        updateSellerInfo(productDetail)
        updatePromoCode(productDetail)

        // Определяем список изображений для галереи и настраиваем её
        val galleryImages = imagePresenter.getGalleryImages(productDetail)
        if (!galleryImages.isEmpty) {
            setupGallery(galleryImages.imageUrls)
        }
    }

    /**
     * Обновляет основную информацию о продукте
     */
    private fun updateBasicInfo(productDetail: ProductDetail) {
        val basicInfo = infoPresenter.formatBasicInfo(productDetail)

        binding.manufacturerText.setTextIfChanged(basicInfo.manufacturerFormatted)
        binding.productTitle.setTextIfChanged(basicInfo.title)
    }

    /**
     * Обновляет информацию о цене и скидках
     */
    private fun updatePriceInfo(productDetail: ProductDetail) {
        val priceInfo = pricePresenter.formatPrice(productDetail)

        // Устанавливаем текущую цену
        binding.currentPriceText.setTextIfChanged(priceInfo.currentPrice)

        // Устанавливаем старую цену с перечеркиванием если она есть
        binding.oldPriceText.setStrikeThroughText(
            text = priceInfo.oldPrice,
            isVisible = priceInfo.showOldPrice,
        )

        // Устанавливаем текст скидки если она есть
        binding.discountText.setTextIfChanged(priceInfo.discountText)
        binding.discountText.setVisibleIf(priceInfo.showDiscount)
    }

    /**
     * Обновляет информацию о рейтинге
     */
    private fun updateRatingInfo(productDetail: ProductDetail) {
        val ratingInfo = infoPresenter.formatRating(productDetail)

        binding.ratingText.setTextIfChanged(ratingInfo.formattedRating)
        binding.reviewsCountText.setTextIfChanged(ratingInfo.formattedReviewsCount)
    }

    /**
     * Обновляет информацию о продавце
     */
    private fun updateSellerInfo(productDetail: ProductDetail) {
        val sellerInfo = infoPresenter.formatSellerInfo(productDetail)

        binding.sellerNameText.setTextIfChanged(sellerInfo.name)
        binding.sellerRatingText.setTextIfChanged(sellerInfo.rating)
    }

    /**
     * Обновляет информацию о промокоде
     */
    private fun updatePromoCode(productDetail: ProductDetail) {
        val promoInfo = infoPresenter.formatPromoCode(productDetail)

        binding.promoDiscountText.setTextIfChanged(promoInfo.discountText)
        binding.promoCodeText.setTextIfChanged(promoInfo.codeText)

        binding.promoDiscountText.setVisibleIf(promoInfo.hasPromoCode)
        binding.promoCodeText.setVisibleIf(promoInfo.hasPromoCode)
    }

    private fun setupGallery(imageUrls: List<String>) {
        Timber.d("Setting up gallery with ${imageUrls.size} images")

        // Always setup gallery, even if empty
        val galleryAdapter = ProductGalleryAdapter(requireContext(), imageLoader)
        binding.productGallery.adapter = galleryAdapter
        galleryAdapter.submitList(imageUrls)

        // Setup indicators
        TabLayoutMediator(binding.galleryIndicator, binding.productGallery) { _, _ ->
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

    /**
     * Setup status bar padding to prevent content from being hidden under status bar
     */
    private fun setupStatusBarPadding() {
        // Apply status bar insets to the root container
        StatusBarUtils.applyStatusBarInsets(binding.root)
    }

    private fun checkCatalogDataEarly(): Boolean {
        val hasTitle = arguments?.getString("productTitle") != null
        val hasImages = arguments?.getStringArray("productImages") != null
        val hasPrice = arguments?.getString("productPrice") != null
        return hasTitle && hasImages && hasPrice
    }
}
