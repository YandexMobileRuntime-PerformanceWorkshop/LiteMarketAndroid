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
import androidx.recyclerview.widget.RecyclerView.RecycledViewPool
import com.google.android.material.tabs.TabLayout
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.yandex.speed.workshop.android.R
import ru.yandex.speed.workshop.android.databinding.FragmentCatalogBinding
import ru.yandex.speed.workshop.android.domain.models.Product
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class CatalogFragment : Fragment() {
    private var _binding: FragmentCatalogBinding? = null
    private val binding get() = checkNotNull(_binding)

    @Inject
    lateinit var skeletonHelper: SkeletonHelper

    private lateinit var skeletonAdapter: SkeletonHelper.SkeletonAdapter

    private val viewModel: CatalogViewModel by viewModels()

    private val productAdapter by lazy {
        ProductPagingAdapter(
            onProductClick = { product -> navigateToProductDetail(product) },
            onFavoriteClick = { productId -> viewModel.onFavoriteClicked(productId) },
            isFavorite = { productId -> viewModel.isProductFavorite(productId) },
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

        setupTabs()
        setupRecyclerView()
        setupSearch()
        setupSwipeToRefresh()
        setupErrorView()
        setupSkeletonView()

        // Показываем скелетоны при первой загрузке
            showSkeletons()

        // Подписываемся на изменения в данных
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.productsPagingFlow.collectLatest { pagingData ->
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

        // Подписываемся на изменения в избранном
        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.favoriteProductsState.collectLatest { favorites ->
                    // Обновляем адаптер при изменении избранного
                    productAdapter.notifyDataSetChanged()
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
            productAdapter.refresh()
        }
    }

    private fun setupErrorView() {
        binding.errorRetryButton.setOnClickListener {
            productAdapter.retry()
        }
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
            }
        }
    }

    private fun navigateToProductDetail(product: Product) {
        try {
            val navController = findNavController()
            val action = navController.currentDestination?.getAction(R.id.action_catalog_to_product_detail)

            if (action != null) {
                val bundle =
                    Bundle().apply {
                        putString("productId", product.id)
                        putString("productTitle", product.title)
                        putString("productPrice", product.currentPrice)
                        putString("productOldPrice", product.oldPrice)
                        putInt("productDiscountPercent", product.discountPercent ?: 0)
                        putFloat("productRatingScore", (product.rating?.score ?: 0.0).toFloat())
                        putInt("productRatingReviews", product.rating?.reviewsCount?.toInt() ?: 0)
                        putString("productVendor", product.manufacturer)
                        putString("productShopName", product.seller)
                        putBoolean("isFavorite", viewModel.isProductFavorite(product.id))
                        putStringArray("productImages", product.imageUrls.toTypedArray())
                    }
                navController.navigate(R.id.action_catalog_to_product_detail, bundle)
            }
        } catch (e: Exception) {
            Timber.e(e, "Error navigating to product detail")
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        hideSkeletons()
        _binding = null
    }
}