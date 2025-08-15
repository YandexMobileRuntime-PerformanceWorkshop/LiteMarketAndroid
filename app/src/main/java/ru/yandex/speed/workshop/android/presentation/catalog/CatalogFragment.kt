package ru.yandex.speed.workshop.android.presentation.catalog

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import timber.log.Timber
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import ru.yandex.speed.workshop.android.R
import ru.yandex.speed.workshop.android.data.network.HttpClient
import ru.yandex.speed.workshop.android.data.network.ProductService
import ru.yandex.speed.workshop.android.domain.models.Product
import ru.yandex.speed.workshop.android.presentation.common.SnackbarUtils
import androidx.fragment.app.viewModels

/**
 * Fragment каталога товаров (аналог iOS ProductsViewController)
 */
class CatalogFragment : Fragment() {
    
    companion object {
        private const val TAG = "CatalogFragment"
        
        fun newInstance(): CatalogFragment {
            return CatalogFragment()
        }
    }
    
    private lateinit var searchEditText: EditText
    private lateinit var categoriesRecyclerView: RecyclerView
    private lateinit var productsRecyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var errorTextView: TextView
    private lateinit var skeletonView: ProductsListSkeletonView
    private lateinit var productAdapter: ProductPagingAdapter
    private lateinit var categoryAdapter: CategoryAdapter

    private var searchJob: Job? = null
    private val categories = listOf("Для вас", "Ниже рынка", "Ultima", "Одежда", "Дом")
    
    private val viewModel by viewModels<CatalogViewModel> { CatalogViewModelFactory(requireContext()) }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Timber.d("onCreateView called")
        return inflater.inflate(R.layout.fragment_catalog, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Timber.d("onViewCreated called")
        
        initViews(view)
        initPresenter()
        setupRecyclerViews()
        setupSearchBar()
        setupSwipeRefresh()
        setupFavoriteResultListener()

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.productsFlow.collectLatest { pagingData ->
                    productAdapter.submitData(pagingData)
                }
            }
        }
        setupLoadStateListener()
        productsRecyclerView.apply {
            setRecycledViewPool(recycledViewPool)
            setItemViewCacheSize(12)
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
    }
    
    private fun initViews(view: View) {
        searchEditText = view.findViewById(R.id.searchEditText)
        categoriesRecyclerView = view.findViewById(R.id.categoriesRecyclerView)
        productsRecyclerView = view.findViewById(R.id.productsRecyclerView)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar)
        errorTextView = view.findViewById(R.id.errorTextView)
        skeletonView = view.findViewById(R.id.skeletonView)
    }
    
    private fun initPresenter() {
        // Оставлено для обратной совместимости
        // Основная логика теперь в ViewModel
    }
    
    private fun setupRecyclerViews() {

        productAdapter = ProductPagingAdapter(
            onProductClick = { product ->

                viewModel.onProductClicked(product) { productId ->
                    navigateToProductDetail(productId)
                }
            },
            onFavoriteClick = { product ->
                val wasAdded = product.isFavorite

                viewModel.onFavoriteClicked(product)
                val message = if (wasAdded) "Добавлено в избранное" else "Удалено из избранного"
                

                SnackbarUtils.showFavoriteAction(
                    view = requireView(),
                    message = message,
                    isAdded = wasAdded,
                    undoAction = {

                        product.isFavorite = !product.isFavorite
                        viewModel.updateProductFavoriteStatus(product.id, product.isFavorite)
                    }
                )
            }
        )
        
        val gridLayoutManager = GridLayoutManager(requireContext(), 2)
        
        productsRecyclerView.apply {
            layoutManager = gridLayoutManager
            adapter = productAdapter.withLoadStateFooter(
                footer = ProductsLoadStateAdapter { productAdapter.retry() }
            )
            itemAnimator = null
        }
        productsRecyclerView.setHasFixedSize(true)
        

        categoryAdapter = CategoryAdapter(categories) { category, position ->
            categoryAdapter.setSelectedPosition(position)
            // TODO: Implement category filtering
        }
        
        categoriesRecyclerView.apply {
            layoutManager = LinearLayoutManager(requireContext(), LinearLayoutManager.HORIZONTAL, false)
            adapter = categoryAdapter
        }
    }
    
    private fun setupSearchBar() {
        searchEditText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
            
            override fun afterTextChanged(s: Editable?) {
                val query = s?.toString()?.trim() ?: ""
                searchJob?.cancel()
                searchJob = viewLifecycleOwner.lifecycleScope.launch {
                    delay(300)
                    viewModel.setQuery(query)
                }
            }
        })
    }
    
    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            Timber.d("SwipeRefresh triggered")
            productAdapter.refresh()
        }
    }
    

    

    
    fun showLoading(isLoading: Boolean) {
        Timber.d("showLoading: $isLoading")
        if (isLoading) {
            loadingProgressBar.visibility = View.VISIBLE
            hideError()
        } else {
            loadingProgressBar.visibility = View.GONE
            if (swipeRefreshLayout.isRefreshing) {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }
    
    fun showError(message: String) {
        Timber.e("showError: $message")
        loadingProgressBar.visibility = View.GONE
        if (swipeRefreshLayout.isRefreshing) {
            swipeRefreshLayout.isRefreshing = false
        }
        
        errorTextView.text = message
        errorTextView.visibility = View.VISIBLE
    }
    
    fun navigateToProductDetail(productId: String) {
        Timber.d("Navigating to product detail: $productId")
        try {
            // Pass existing product data to avoid re-fetching if available
            val product = getCurrentProductsList().find { it.id == productId }
            
            val bundle = Bundle().apply {
                putString("productId", productId)
                
                product?.let { 
                    putString("productTitle", it.title)
                    putString("productPrice", it.price)
                    putString("productOldPrice", it.oldPrice)
                    putInt("productDiscountPercent", it.discountPercent ?: 0)
                    putString("productShopName", it.shopName)
                    putString("productVendor", it.vendor) // Добавляем производителя
                    putBoolean("isFavorite", it.isFavorite) // Передаем статус избранного
                    
                    // Rating data
                    it.rating?.let { rating ->
                        putDouble("productRatingScore", rating.score ?: 0.0)
                        putInt("productRatingReviews", rating.reviewsCount ?: 0)
                    }
                    
                    // Promo code data
                    it.promoCode?.let { promo ->
                        putString("promoCode", promo.code)
                        putString("promoDiscount", promo.discount)
                        putString("promoMinOrder", promo.minOrder)
                        putString("promoExpiryDate", promo.expiryDate)
                    }
                    
                    if (it.pictureUrls.isNotEmpty()) {
                        putStringArrayList("productImages", ArrayList(it.pictureUrls))
                    }
                    Timber.d("Passing complete product data: ${it.title}, old_price=${it.oldPrice}, shop=${it.shopName}")
                } ?: run {
                    Timber.w("Product not found in current list for ID: $productId")
                }
            }
            
            findNavController().navigate(R.id.productDetailFragment, bundle)
        } catch (e: Exception) {
            Timber.e(e, "Navigation error: ${e.message}")
            // Navigation errors are critical and should be logged, not shown to user
        }
    }
    
    private fun hideError() {
        errorTextView.visibility = View.GONE
    }
    
    private fun showSkeletons() { 
        loadingProgressBar.visibility = View.GONE
        swipeRefreshLayout.visibility = View.GONE
        skeletonView.visibility = View.VISIBLE
        skeletonView.showSkeletons(6) // Показываем 6 скелетонов
    }
    
    private fun hideSkeletons() { 
        skeletonView.hideSkeletons()
        skeletonView.visibility = View.GONE
        swipeRefreshLayout.visibility = View.VISIBLE
    }
    
    private fun setupFavoriteResultListener() {
        // Listen for favorite status changes from ProductDetailFragment
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Bundle>("favorite_result")
            ?.observe(viewLifecycleOwner) { result ->
                val productId = result.getString("productId")
                val isFavorite = result.getBoolean("isFavorite", false)
                
                Timber.d("Received favorite result: productId=$productId, isFavorite=$isFavorite")
                
                // Update product in ViewModel's data
                viewModel.updateProductFavoriteStatus(productId, isFavorite)
                
                // Clear the result to prevent re-triggering
                findNavController().currentBackStackEntry?.savedStateHandle?.remove<Bundle>("favorite_result")
            }
    }
    
        private fun getCurrentProductsList(): List<Product> = productAdapter.snapshot().items
    



    private fun setupLoadStateListener() {
        productAdapter.addLoadStateListener { loadStates ->
            val isRefreshing = loadStates.refresh is androidx.paging.LoadState.Loading
            

            if (isRefreshing) {
                if (productAdapter.itemCount == 0) {
                    showSkeletons()
                } else {
                    loadingProgressBar.visibility = View.VISIBLE
                }
            } else {
                hideSkeletons()
                loadingProgressBar.visibility = View.GONE
            }
            

            if (swipeRefreshLayout.isRefreshing && !isRefreshing) {
                swipeRefreshLayout.isRefreshing = false
            }


            val errorState = loadStates.refresh as? androidx.paging.LoadState.Error
                ?: loadStates.append as? androidx.paging.LoadState.Error
                ?: loadStates.prepend as? androidx.paging.LoadState.Error
            errorState?.let {
                hideSkeletons()
                showError(it.error.message ?: "Ошибка загрузки")
            }
        }
    }

    private val recycledViewPool = RecyclerView.RecycledViewPool()

 
} 