package ru.yandex.speed.workshop.android.presentation.catalog

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.core.widget.doAfterTextChanged
import androidx.fragment.app.Fragment
import androidx.fragment.app.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.paging.LoadState
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.yandex.speed.workshop.android.R
import ru.yandex.speed.workshop.android.databinding.FragmentCatalogBinding
import ru.yandex.speed.workshop.android.domain.models.Product
import ru.yandex.speed.workshop.android.utils.ApplicationStartupTracker
import ru.yandex.speed.workshop.android.utils.ImageLoader
import ru.yandex.speed.workshop.android.utils.LCPTrackingTime
import ru.yandex.speed.workshop.android.utils.MVIScreenAnalytics
import ru.yandex.speed.workshop.android.utils.PerformanceMetricManager
import ru.yandex.speed.workshop.android.utils.PerformanceTimestamp
import ru.yandex.speed.workshop.android.utils.StatusBarUtils
import ru.yandex.speed.workshop.android.utils.VisibilityAnalytics
import timber.log.Timber
import javax.inject.Inject
import kotlin.system.measureTimeMillis

@AndroidEntryPoint
class CatalogFragment : Fragment() {
    private var _binding: FragmentCatalogBinding? = null
    private val binding get() = checkNotNull(_binding)

    @Inject
    lateinit var skeletonHelper: SkeletonHelper

    @Inject
    lateinit var mviScreenAnalytics: MVIScreenAnalytics

    @Inject
    lateinit var performanceMetricManager: PerformanceMetricManager

    @Inject
    lateinit var imageLoader: ImageLoader

    @Inject
    lateinit var startupTracker: ApplicationStartupTracker

    @Inject
    lateinit var visibilityAnalytics: VisibilityAnalytics

    private lateinit var skeletonAdapter: SkeletonHelper.SkeletonAdapter

    // Флаг для отслеживания первой успешной загрузки данных (для LCP)
    private var isFirstSuccessfulLoad = true

    private val viewModel: CatalogViewModel by viewModels()

    private val productAdapter by lazy {
        ProductPagingAdapter(
            onProductClick = { product -> navigateToProductDetail(product) },
            isFavorite = { productId -> viewModel.isProductFavorite(productId) },
            imageLoader = imageLoader,
        )
    }

    private val loadStateAdapter by lazy {
        ProductsLoadStateAdapter { productAdapter.retry() }
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        _binding = FragmentCatalogBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(
        view: View,
        savedInstanceState: Bundle?,
    ) {
        super.onViewCreated(view, savedInstanceState)

        // Fix status bar overlap issue
        setupStatusBarPadding()

        // Инициализируем LCP трекинг для экрана каталога
        initLCPTracking()

        setupTabs()
        setupRecyclerView()
        setupSearch()
        setupSwipeToRefresh()
        setupErrorView()
        setupSkeletonView()

        // Показываем скелетоны при первой загрузке
            showSkeletons()

        // Сбрасываем флаг первой загрузки для правильного измерения LCP
        isFirstSuccessfulLoad = true

        // Подписываемся на изменения в данных
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.productsPagingFlow.collectLatest { pagingData ->
                    // Сбрасываем трекер видимости при получении новых данных
                    visibilityAnalytics.reset()
                    productAdapter.submitData(pagingData)
                }
            }
        }

        // Подписываемся на изменения в состоянии загрузки
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                productAdapter.loadStateFlow.collectLatest { loadStates ->
                    // Обновляем UI в зависимости от состояния загрузки
                    handleLoadStates(loadStates.refresh)
                }
            }
        }

        // Подписываемся на изменения в поисковом запросе
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.searchQuery.collectLatest { query ->
                    // Устанавливаем текст в поле поиска, если он отличается
                    if (binding.searchEditText.text.toString() != query) {
                        binding.searchEditText.setText(query)
                    }
                }
            }
        }
    }

    private fun setupTabs() {
        // Устанавливаем слушатель для табов
        binding.tabLayout.addOnTabSelectedListener(
            object : TabLayout.OnTabSelectedListener {
                override fun onTabSelected(tab: TabLayout.Tab) {
                    // Здесь можно добавить логику фильтрации по категории
                    Timber.d("Selected tab: ${tab.text}")
                }

                override fun onTabUnselected(tab: TabLayout.Tab) {
                    // Не требуется действий
                }

                override fun onTabReselected(tab: TabLayout.Tab) {
                    // Можно добавить логику для повторного нажатия на таб
                }
            },
        )
    }

    private fun setupRecyclerView() {
        val spacing = resources.getDimensionPixelSize(R.dimen.catalog_grid_spacing)
        val gridLayoutManager = GridLayoutManager(requireContext(), 2)

        // Настраиваем отображение элементов загрузки на всю ширину
        gridLayoutManager.spanSizeLookup =
            object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                    return if (productAdapter.getItemViewType(position) == ProductPagingAdapter.VIEW_TYPE_PRODUCT) 1 else 2
                }
            }

        // Создаем общий пул для переиспользования ViewHolder'ов
        val viewPool =
            RecycledViewPool().apply {
                setMaxRecycledViews(ProductPagingAdapter.VIEW_TYPE_PRODUCT, 20)
        }
        
        binding.productsRecyclerView.apply {
            layoutManager = gridLayoutManager
            setHasFixedSize(true)
            setRecycledViewPool(viewPool)
            setItemViewCacheSize(12)
            itemAnimator = null
            addItemDecoration(GridSpacingItemDecoration(2, spacing, true))
            adapter = productAdapter.withLoadStateFooter(loadStateAdapter)

            addOnScrollListener(
                object : RecyclerView.OnScrollListener() {
                    override fun onScrollStateChanged(
                        recyclerView: RecyclerView,
                        newState: Int,
                    ) {
                        when (newState) {
                            RecyclerView.SCROLL_STATE_DRAGGING -> {
                                // Начало скролла - запускаем измерение
                                performanceMetricManager.start(measureName = "catalog_scroll")
                                Timber.d("Scroll started - measuring performance")
                            }
                            RecyclerView.SCROLL_STATE_IDLE -> {
                                // Окончание скролла без инерции или после инерции
                                performanceMetricManager.stop(name = "catalog_scroll")
                                Timber.d("Scroll ended - measuring performance complete")
                            }
                        }
                    }

                    override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                        super.onScrolled(recyclerView, dx, dy)
                        try {
                            visibilityAnalytics.checkVisibilityAndTrack(
                                recyclerView = recyclerView,
                                adapter = productAdapter
                            )
                        } catch (e: Exception) {
                            Timber.e(e, "Error in inefficient visibility tracking")
                        }
                    }
                },
            )
        }
    }

    private fun setupSearch() {
        binding.searchEditText.doAfterTextChanged { text ->
            val query = text.toString().trim()
            viewModel.updateSearchQuery(query)

            // Показываем или скрываем кнопку очистки в зависимости от наличия текста
            binding.searchClearButton.visibility = if (query.isNotEmpty()) View.VISIBLE else View.GONE
        }

        binding.searchClearButton.setOnClickListener {
            binding.searchEditText.text?.clear()
            viewModel.updateSearchQuery("")
            binding.searchClearButton.visibility = View.GONE
        }
    }

    private fun setupSwipeToRefresh() {
        binding.swipeRefreshLayout.setOnRefreshListener {
            visibilityAnalytics.reset()
            productAdapter.refresh()
        }
    }

    private fun setupErrorView() {
        binding.errorRetryButton.setOnClickListener {
            productAdapter.retry()
        }
    }

    /**
     * Инициализация трекинга LCP для экрана каталога
     */
    private fun initLCPTracking() {
        // Для каталога используем точку отсчета от запуска приложения,
        // так как это обычно первый экран, который видит пользователь
        mviScreenAnalytics.initialize(trackingTimeType = LCPTrackingTime.FROM_APP_START)
        Timber.d("Initialized LCP tracking for catalog screen from app start")
    }

    private fun setupSkeletonView() {
        skeletonAdapter =
            skeletonHelper.setupSkeletonRecyclerView(
                binding.skeletonRecyclerView,
                requireContext(),
            )
    }
    
    private fun showSkeletons() {
        binding.skeletonRecyclerView.isVisible = true
        skeletonAdapter.skeletonCount = 6
        skeletonAdapter.notifyDataSetChanged()
        skeletonHelper.startSkeletonAnimation(binding.skeletonRecyclerView, requireContext())
    }
    
    private fun hideSkeletons() {
        binding.skeletonRecyclerView.isVisible = false
        skeletonHelper.stopSkeletonAnimation(binding.skeletonRecyclerView)
    }

    private fun handleLoadStates(loadState: LoadState) {
        when (loadState) {
            is LoadState.Loading -> {
                // Показываем скелетоны только при первой загрузке
                if (productAdapter.itemCount == 0) {
                    showSkeletons()
                    binding.productsRecyclerView.isVisible = false
                    binding.errorTextView.isVisible = false
                }
                binding.swipeRefreshLayout.isRefreshing = productAdapter.itemCount > 0
            }
            is LoadState.Error -> {
                hideSkeletons()
                binding.swipeRefreshLayout.isRefreshing = false

                if (productAdapter.itemCount == 0) {
                    // Показываем ошибку, только если нет данных
                    binding.errorTextView.isVisible = true
                    binding.productsRecyclerView.isVisible = false
                    binding.errorTextView.text = loadState.error.localizedMessage
                        ?: getString(R.string.error_loading_products)

                    Timber.e(loadState.error, "Error loading products")
        } else {
                    // Если есть данные, показываем snackbar
                    binding.errorTextView.isVisible = false
                    binding.productsRecyclerView.isVisible = true
                }
            }
            is LoadState.NotLoading -> {
                hideSkeletons()
                binding.swipeRefreshLayout.isRefreshing = false
                binding.errorTextView.isVisible = false
                binding.productsRecyclerView.isVisible = true

                // Если это первая успешная загрузка данных и у нас есть элементы для отображения
                if (isFirstSuccessfulLoad && productAdapter.itemCount > 0) {
                    isFirstSuccessfulLoad = false
                    // Логируем LCP, так как первая загрузка каталога завершена
                    Timber.d("Logging LCP for catalog screen with ${productAdapter.itemCount} items loaded")
                    mviScreenAnalytics.logLCP("Catalog")

                    // Отмечаем завершение загрузки первых данных для метрик старта приложения
                    startupTracker.onFirstDataLoaded()

                    // Дополнительное логирование для удобства тестирования
                    val productIds = mutableListOf<String>()
                    for (i in 0 until minOf(3, productAdapter.itemCount)) {
                        try {
                            productAdapter.getItemId(i)?.let { productIds.add(it.toString()) }
                        } catch (e: Exception) {
                            // Игнорируем ошибки при получении ID
                        }
                    }
                    Timber.d("First visible products: ${productIds.joinToString(", ")}")
                }
            }
        }
    }

    private fun navigateToProductDetail(product: Product) {
        try {
            val navController = findNavController()
            val action = navController.currentDestination?.getAction(R.id.action_catalog_to_product_detail)

            if (action != null) {
                // Создаем временную метку для измерения LCP
                val timestamp = PerformanceTimestamp.now()
                Timber.d("Creating timestamp for LCP tracking: ${timestamp.toMilliseconds()}")

                val bundle =
                    Bundle().apply {
                        putString("productId", product.id)
                        putString("productTitle", product.title)
                        putString("productPrice", product.currentPrice)
                        putString("productOldPrice", product.oldPrice)
                        putInt("productDiscountPercent", product.discountPercent ?: 0)
                        // Используем напрямую значение рейтинга без форматирования
                        putFloat("productRatingScore", (product.rating?.score ?: 0.0).toFloat())
                        putInt("productRatingReviews", product.rating?.reviewsCount?.toInt() ?: 0)
                        putString("productVendor", product.manufacturer)
                        putString("productShopName", product.seller)
                        putBoolean("isFavorite", viewModel.isProductFavorite(product.id))
                        putStringArray("productImages", product.imageUrls.toTypedArray())

                        // Добавляем временную метку для LCP трекинга
                        putLong("screen_creation_timestamp", timestamp.toMilliseconds())

                        // Добавляем информацию о промокоде, если он есть
                        product.promoCode?.let { promoCode ->
                            putString("promoCode", promoCode.code)
                            putString("promoDiscount", promoCode.discount)
                            putString("promoMinOrder", promoCode.minOrder)
                            putString("promoExpiryDate", promoCode.expiryDate)
                        }

                        // Добавляем информацию о доставке, если она есть
                        product.delivery?.let { delivery ->
                            putString("deliveryProvider", delivery.provider)
                            val deliveryOptions = delivery.options
                            if (deliveryOptions.isNotEmpty()) {
                                val firstOption = deliveryOptions.first()
                                putString("deliveryType", firstOption.type)
                                putString("deliveryDate", firstOption.date)
                                putString("deliveryDetails", firstOption.details)
                            }
                        }
                    }
                navController.navigate(R.id.action_catalog_to_product_detail, bundle)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error navigating to product detail")
        }
    }

    /**
     * Setup status bar padding to prevent content from being hidden under status bar
     */
    private fun setupStatusBarPadding() {
        // Apply status bar insets to the search container
        StatusBarUtils.applyStatusBarInsetsAsMargin(binding.searchContainer)
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hideSkeletons()

        // Анализ загрузки изображений перед уничтожением представления
        imageLoader.analyzeImageLoadingMetrics()

        _binding = null
    }
}
