package ru.yandex.speed.workshop.android.presentation.catalog

import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import ru.yandex.speed.workshop.android.R
import ru.yandex.speed.workshop.android.data.network.HttpClient
import ru.yandex.speed.workshop.android.data.network.ProductService
import ru.yandex.speed.workshop.android.domain.models.Product
import ru.yandex.speed.workshop.android.presentation.common.SnackbarUtils

/**
 * Fragment каталога товаров (аналог iOS ProductsViewController)
 */
class CatalogFragment : Fragment(), CatalogContract.View {
    
    companion object {
        private const val TAG = "CatalogFragment"
        
        fun newInstance(): CatalogFragment {
            return CatalogFragment()
        }
    }
    
    // UI elements
    private lateinit var searchEditText: EditText
    private lateinit var categoriesRecyclerView: RecyclerView
    private lateinit var productsRecyclerView: RecyclerView
    private lateinit var swipeRefreshLayout: SwipeRefreshLayout
    private lateinit var loadingProgressBar: ProgressBar
    private lateinit var errorTextView: TextView
    
    // Adapters
    private lateinit var productAdapter: ProductAdapter
    private lateinit var categoryAdapter: CategoryAdapter
    
    // Presenter
    private lateinit var presenter: CatalogContract.Presenter
    
    // Prevent multiple pagination calls
    private var lastPaginationTrigger = 0L
    private val PAGINATION_THROTTLE_MS = 1000L // 1 second between pagination calls
    
    // Categories (same as iOS version)
    private val categories = listOf("Для вас", "Ниже рынка", "Ultima", "Одежда", "Дом")
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView called")
        return inflater.inflate(R.layout.fragment_catalog, container, false)
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        Log.d(TAG, "onViewCreated called")
        
        initViews(view)
        initPresenter()
        setupRecyclerViews()
        setupSearchBar()
        setupSwipeRefresh()
        setupScrollListener()
        setupFavoriteResultListener()
        setupTestPagination() // Temporary for testing
        
        // Load data only if needed (singleton presenter may already have data)
        Log.d(TAG, "Checking if data needs to be loaded")
        if (!presenter.hasData()) {
            Log.d(TAG, "No data found, starting initial load with skeletons")
            showSkeletons()
            presenter.loadProducts(refresh = true)
        } else {
            Log.d(TAG, "Data already exists in singleton presenter")
            // Data will be restored automatically in attachView
        }
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        presenter.detachView()
    }
    
    private fun initViews(view: View) {
        searchEditText = view.findViewById(R.id.searchEditText)
        categoriesRecyclerView = view.findViewById(R.id.categoriesRecyclerView)
        productsRecyclerView = view.findViewById(R.id.productsRecyclerView)
        swipeRefreshLayout = view.findViewById(R.id.swipeRefreshLayout)
        loadingProgressBar = view.findViewById(R.id.loadingProgressBar)
        errorTextView = view.findViewById(R.id.errorTextView)
    }
    
    private fun initPresenter() {
        val httpClient = HttpClient.getInstance()
        val productService = ProductService(httpClient)
        presenter = CatalogPresenter.getInstance(productService)
        presenter.attachView(this)
    }
    
    private fun setupRecyclerViews() {
        // Setup products RecyclerView
        productAdapter = ProductAdapter(
            onProductClick = { product ->
                presenter.onProductClicked(product)
            },
            onFavoriteClick = { product ->
                val wasAdded = product.isFavorite
                presenter.onFavoriteClicked(product)
                val message = if (wasAdded) "Добавлено в избранное" else "Удалено из избранного"
                
                // Показываем стилизованный Snackbar с undo действием
                SnackbarUtils.showFavoriteAction(
                    view = requireView(),
                    message = message,
                    isAdded = wasAdded,
                    undoAction = {
                        // Undo действие - возвращаем статус обратно
                        product.isFavorite = !product.isFavorite
                        presenter.updateProductFavoriteStatus(product.id, product.isFavorite)
                    }
                )
            }
        )
        
        val gridLayoutManager = GridLayoutManager(requireContext(), 2)
        gridLayoutManager.spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
            override fun getSpanSize(position: Int): Int {
                return when (productAdapter.getItemViewType(position)) {
                    0 -> 1 // Product item takes 1 span
                    1 -> 2 // Loading item takes full width (2 spans)
                    else -> 1
                }
            }
        }
        
        productsRecyclerView.apply {
            layoutManager = gridLayoutManager
            adapter = productAdapter
        }
        
        // Setup categories RecyclerView
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
                presenter.searchProducts(s?.toString() ?: "")
            }
        })
    }
    
    private fun setupSwipeRefresh() {
        swipeRefreshLayout.setOnRefreshListener {
            Log.d(TAG, "SwipeRefresh triggered")
            presenter.onRefresh()
        }
    }
    
    private fun setupScrollListener() {
        productsRecyclerView.addOnScrollListener(object : RecyclerView.OnScrollListener() {
            override fun onScrolled(recyclerView: RecyclerView, dx: Int, dy: Int) {
                super.onScrolled(recyclerView, dx, dy)
                
                val layoutManager = recyclerView.layoutManager as GridLayoutManager
                val totalItemCount = layoutManager.itemCount
                val lastVisibleItemPosition = layoutManager.findLastVisibleItemPosition()
                val visibleItemCount = layoutManager.childCount
                val firstVisibleItemPosition = layoutManager.findFirstVisibleItemPosition()
                
                // Don't trigger pagination if no products loaded yet
                val currentProducts = getCurrentProductsList()
                if (currentProducts.isEmpty()) {
                    return
                }
                
                // Trigger pagination when approaching end of list (GridLayoutManager specific)
                val isNearEnd = (firstVisibleItemPosition + visibleItemCount) >= (totalItemCount - 6)
                val isAtEnd = lastVisibleItemPosition >= totalItemCount - 4
                val shouldTrigger = isNearEnd || isAtEnd
                
                if (shouldTrigger) {
                    // Throttle pagination to prevent multiple rapid API calls
                    val now = System.currentTimeMillis()
                    if (now - lastPaginationTrigger >= PAGINATION_THROTTLE_MS) {
                        lastPaginationTrigger = now
                        Log.d(TAG, "Triggering pagination")
                        presenter.loadNextPageIfNeeded()
                    } else {
                        Log.d(TAG, "Pagination throttled")
                    }
                }
            }
        })
    }
    
    // MARK: - CatalogContract.View Implementation
    
    override fun showProducts(products: List<Product>) {
        Log.d(TAG, "showProducts called with ${products.size} products")
        Log.d(TAG, "First 3 products: ${products.take(3).map { "${it.id}: ${it.title.take(20)}" }}")
        hideError()
        hideSkeletons()
        productAdapter.updateProducts(products, false)
        Log.d(TAG, "showProducts completed, adapter should have ${products.size} items")
    }
    
    override fun addProducts(products: List<Product>) {
        Log.d(TAG, "addProducts called with ${products.size} new products")
        hideError()
        productAdapter.addProducts(products, false)
        Log.d(TAG, "addProducts completed")
    }
    
    override fun showLoading(isLoading: Boolean) {
        Log.d(TAG, "showLoading: $isLoading")
        if (isLoading) {
            // Show loading spinner (for refresh when we already have data)
            loadingProgressBar.visibility = View.VISIBLE
            hideError()
        } else {
            loadingProgressBar.visibility = View.GONE
            if (swipeRefreshLayout.isRefreshing) {
                swipeRefreshLayout.isRefreshing = false
            }
        }
    }
    
    override fun showPaginationLoading(isLoading: Boolean) {
        Log.d(TAG, "showPaginationLoading: $isLoading")
        val currentProducts = getCurrentProductsList()
        if (currentProducts.isNotEmpty()) {
            Log.d(TAG, "Updating pagination with ${currentProducts.size} products, loading=$isLoading")
            productAdapter.updateProducts(currentProducts, isLoading)
        } else {
            Log.d(TAG, "Skipping pagination update - no products available")
        }
    }
    
    override fun showError(message: String) {
        Log.e(TAG, "showError: $message")
        loadingProgressBar.visibility = View.GONE
        if (swipeRefreshLayout.isRefreshing) {
            swipeRefreshLayout.isRefreshing = false
        }
        
        errorTextView.text = message
        errorTextView.visibility = View.VISIBLE
    }
    
    override fun navigateToProductDetail(productId: String) {
        Log.d(TAG, "Navigating to product detail: $productId")
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
                    Log.d(TAG, "Passing complete product data: ${it.title}, old_price=${it.oldPrice}, shop=${it.shopName}")
                } ?: run {
                    Log.w(TAG, "Product not found in current list for ID: $productId")
                }
            }
            
            findNavController().navigate(R.id.productDetailFragment, bundle)
        } catch (e: Exception) {
            Log.e(TAG, "Navigation error: ${e.message}", e)
            // Navigation errors are critical and should be logged, not shown to user
        }
    }
    
    private fun hideError() {
        errorTextView.visibility = View.GONE
    }
    
    private fun showSkeletons() {
        Log.d(TAG, "Showing skeleton placeholders")
        loadingProgressBar.visibility = View.GONE
        productAdapter.showSkeletons(6) // Show 6 skeleton items
    }
    
    private fun hideSkeletons() {
        Log.d(TAG, "hideSkeletons called - DON'T call updateProducts here!")
        // Skeletons will be automatically replaced when real data is submitted to adapter
        // No need to explicitly hide - just clear the skeleton list
    }
    
    private fun setupFavoriteResultListener() {
        // Listen for favorite status changes from ProductDetailFragment
        findNavController().currentBackStackEntry?.savedStateHandle?.getLiveData<Bundle>("favorite_result")
            ?.observe(viewLifecycleOwner) { result ->
                val productId = result.getString("productId")
                val isFavorite = result.getBoolean("isFavorite", false)
                
                Log.d(TAG, "Received favorite result: productId=$productId, isFavorite=$isFavorite")
                
                // Update product in presenter's data
                presenter.updateProductFavoriteStatus(productId, isFavorite)
                
                // Clear the result to prevent re-triggering
                findNavController().currentBackStackEntry?.savedStateHandle?.remove<Bundle>("favorite_result")
            }
    }
    
        private fun getCurrentProductsList(): List<Product> {
        val currentItems = productAdapter.currentList
        val products = currentItems.filterIsInstance<ProductAdapter.Item.ProductItem>()
            .map { it.product }
        Log.d(TAG, "getCurrentProductsList: ${currentItems.size} items total, ${products.size} products")
        return products
    }
    
    // Temporary method for testing pagination
    private fun setupTestPagination() {
        var lastClickTime = 0L
        
        // Add a double tap listener to the search bar for testing pagination
        searchEditText.setOnClickListener {
            val currentTime = System.currentTimeMillis()
            if (currentTime - lastClickTime < 500) { // Double tap within 500ms
                Log.d(TAG, "🧪 MANUAL PAGINATION TEST - Double tap detected, forcing load next page")
                presenter.loadNextPageIfNeeded()
            }
            lastClickTime = currentTime
        }
    }

 
} 