package ru.yandex.speed.workshop.android.presentation.productdetail

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.google.android.material.tabs.TabLayout
import com.google.android.material.tabs.TabLayoutMediator
import ru.yandex.speed.workshop.android.R
import ru.yandex.speed.workshop.android.data.network.HttpClient
import ru.yandex.speed.workshop.android.data.network.ProductService
import ru.yandex.speed.workshop.android.domain.models.ProductDetail
import ru.yandex.speed.workshop.android.presentation.common.SnackbarUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import android.graphics.Paint

class ProductDetailFragment : Fragment() {
    
    private lateinit var productId: String
    private var isFavorite: Boolean = false
    private var isSellerFavorite: Boolean = false
    
    // UI Elements
    private lateinit var backButton: ImageView
    private lateinit var shareButton: ImageView
    private lateinit var favoriteButton: ImageView
    private lateinit var galleryViewPager: ViewPager2
    private lateinit var pageIndicator: TabLayout
    private lateinit var similarButton: TextView
    private lateinit var manufacturerText: TextView
    private lateinit var productTitle: TextView
    private lateinit var currentPriceText: TextView
    private lateinit var oldPriceText: TextView
    private lateinit var discountText: TextView
    private lateinit var ratingText: TextView
    private lateinit var reviewsCountText: TextView
    private lateinit var sellerNameText: TextView
    private lateinit var addToCartButton: TextView
    private lateinit var buyNowButton: TextView
    
    // Promo code elements
    private lateinit var promoDiscountText: TextView
    private lateinit var promoCodeText: TextView
    
    // Seller rating element  
    private lateinit var sellerRatingText: TextView
    
    // New clickable elements
    private lateinit var copyPromoButton: ImageView
    private lateinit var sellerFavoriteButton: ImageView
    private lateinit var sellerDetailsButton: ImageView
    
    private lateinit var productService: ProductService
    private val fragmentScope = CoroutineScope(Dispatchers.Main)
    
    // Container views for content switching
    private lateinit var contentContainer: View
    private lateinit var skeletonContainer: View
    
    companion object {
        private const val TAG = "ProductDetailFragment"
    }
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Log.d(TAG, "onCreateView for productId: ${arguments?.getString("productId")}")
        
        // Create container with both skeleton and content layouts
        val containerLayout = FrameLayout(requireContext())
        containerLayout.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        
        // Inflate skeleton layout
        skeletonContainer = inflater.inflate(R.layout.fragment_product_detail_skeleton, containerLayout, false)
        containerLayout.addView(skeletonContainer)
        
        // Inflate content layout
        contentContainer = inflater.inflate(R.layout.fragment_product_detail, containerLayout, false)
        contentContainer.visibility = View.GONE
        containerLayout.addView(contentContainer)
        
        return containerLayout
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        // Get productId and favorite status from arguments
        productId = arguments?.getString("productId") ?: "unknown"
        isFavorite = arguments?.getBoolean("isFavorite", false) ?: false
        
        // Show skeleton first
        showSkeleton()
        
        initViews(contentContainer)
        setupClickListeners()
        updateFavoriteButton() // Update favorite button with passed status
        updateSellerFavoriteButton() // Initialize seller favorite button
        initProductService()
        loadProductDetail(productId)
    }
    
    private fun initViews(view: View) {
        backButton = view.findViewById(R.id.backButton)
        shareButton = view.findViewById(R.id.shareButton)
        favoriteButton = view.findViewById(R.id.favoriteButton)
        galleryViewPager = view.findViewById(R.id.galleryViewPager)
        pageIndicator = view.findViewById(R.id.pageIndicator)
        similarButton = view.findViewById(R.id.similarButton)
        manufacturerText = view.findViewById(R.id.manufacturerText)
        productTitle = view.findViewById(R.id.productTitle)
        currentPriceText = view.findViewById(R.id.currentPriceText)
        oldPriceText = view.findViewById(R.id.oldPriceText)
        discountText = view.findViewById(R.id.discountText)
        ratingText = view.findViewById(R.id.ratingText)
        reviewsCountText = view.findViewById(R.id.reviewsCountText)
        sellerNameText = view.findViewById(R.id.sellerNameText)
        addToCartButton = view.findViewById(R.id.addToCartButton)
        buyNowButton = view.findViewById(R.id.buyNowButton)
        
        // Promo code elements
        promoDiscountText = view.findViewById(R.id.promoDiscountText)
        promoCodeText = view.findViewById(R.id.promoCodeText)
        
        // Seller rating element  
        sellerRatingText = view.findViewById(R.id.sellerRatingText)
        
        // New clickable elements
        copyPromoButton = view.findViewById(R.id.copyPromoButton)
        sellerFavoriteButton = view.findViewById(R.id.sellerFavoriteButton)
        sellerDetailsButton = view.findViewById(R.id.sellerDetailsButton)
    }
    
    private fun setupClickListeners() {
        backButton.setOnClickListener {
            Log.d(TAG, "Back button clicked")
            sendFavoriteStatusBack()
            findNavController().popBackStack()
        }
        
        shareButton.setOnClickListener {
            Log.d(TAG, "Share button clicked")
            SnackbarUtils.showPlaceholder(requireView(), "Поделиться товаром")
        }
        
        favoriteButton.setOnClickListener {
            // Toggle favorite status
            isFavorite = !isFavorite
            updateFavoriteButton()
            
            // Send result back to catalog
            sendFavoriteStatusBack()
            
            Log.d(TAG, "Favorite button clicked, new status: $isFavorite")
            val message = if (isFavorite) "Добавлено в избранное" else "Удалено из избранного"
            
            // Показываем стилизованный Snackbar с undo действием
            SnackbarUtils.showFavoriteAction(
                view = requireView(),
                message = message,
                isAdded = isFavorite,
                undoAction = {
                    // Undo действие - возвращаем статус обратно
                    isFavorite = !isFavorite
                    updateFavoriteButton()
                    sendFavoriteStatusBack()
                }
            )
        }
        
        similarButton.setOnClickListener {
            Log.d(TAG, "Similar button clicked")
            SnackbarUtils.showPlaceholder(requireView(), "Показать похожие товары")
        }
        
        addToCartButton.setOnClickListener {
            Log.d(TAG, "Add to cart button clicked")
            SnackbarUtils.showSuccess(requireView(), "Добавлено в корзину", duration = 3000)
        }
        
        buyNowButton.setOnClickListener {
            Log.d(TAG, "Buy now button clicked")
            SnackbarUtils.showPlaceholder(requireView(), "Купить сейчас")
        }
        
        copyPromoButton.setOnClickListener {
            Log.d(TAG, "Copy promo button clicked")
            SnackbarUtils.showCopied(requireView(), "Промокод скопирован")
        }
        
        sellerFavoriteButton.setOnClickListener {
            // Toggle seller favorite status
            isSellerFavorite = !isSellerFavorite
            updateSellerFavoriteButton()
            
            Log.d(TAG, "Seller favorite button clicked, new status: $isSellerFavorite")
            val message = if (isSellerFavorite) "Продавец добавлен в избранное" else "Продавец удален из избранного"
            
            // Показываем стилизованный Snackbar с undo действием
            SnackbarUtils.showFavoriteAction(
                view = requireView(),
                message = message,
                isAdded = isSellerFavorite,
                undoAction = {
                    // Undo действие - возвращаем статус продавца обратно
                    isSellerFavorite = !isSellerFavorite
                    updateSellerFavoriteButton()
                }
            )
        }
        
        sellerDetailsButton.setOnClickListener {
            Log.d(TAG, "Seller details button clicked")
            SnackbarUtils.showPlaceholder(requireView(), "Открыть страницу продавца")
        }
    }
    
    private fun initProductService() {
        val httpClient = HttpClient.getInstance()
        productService = ProductService(httpClient)
    }
    
    private fun loadProductDetail(productId: String) {
        Log.d(TAG, "Loading product detail for ID: $productId")
        
        // First try to get product from catalog presenter (if available)
        loadProductFromCatalog(productId)
        
        // TODO: Later implement API call for detailed product info
        // For now use catalog data
    }
    
    private fun loadProductFromCatalog(productId: String) {
        fragmentScope.launch {
            try {
                Log.d(TAG, "Trying to load product detail from API for ID: $productId")
                
                // Try to load from API first
                val productDetailResponse = productService.getProductDetail(productId)
                val productDetail = productDetailResponse.product
                
                Log.d(TAG, "Successfully loaded product detail from API: ${productDetail.title}")
                showProductDetail(productDetail)
                
            } catch (e: Exception) {
                Log.w(TAG, "Failed to load from API: ${e.message}")
                
                // Try to get images from catalog data passed via arguments
                val catalogImages = tryGetImagesFromCatalog(productId)
                
                if (catalogImages != null) {
                    // We have catalog data - use it completely (title, price, images already set in tryGetImagesFromCatalog)
                    Log.d(TAG, "Using catalog data from arguments")
                    setupGallery(catalogImages)
                    hideSkeletonShowContent()
                } else {
                    // No catalog data - use sample data for everything
                    Log.d(TAG, "Using sample data")
                    showSampleProductDetail(productId)
                }
            }
        }
    }
    
    private fun tryGetImagesFromCatalog(productId: String): List<String>? {
        return try {
            // Check if we have product data passed via arguments
            val productTitle = arguments?.getString("productTitle")
            val productPrice = arguments?.getString("productPrice")
            val productImages = arguments?.getStringArrayList("productImages")
            
            if (productTitle != null && productImages != null) {
                Log.d(TAG, "Using complete product data from arguments: $productTitle")
                
                // Update UI with ALL passed data
                productTitle.let { 
                    this.productTitle.text = it 
                }
                productPrice?.let { 
                    this.currentPriceText.text = it 
                }
                
                // Use real vendor/manufacturer from catalog
                val vendor = arguments?.getString("productVendor")
                if (!vendor.isNullOrEmpty()) {
                    manufacturerText.text = "$vendor >"
                } else {
                    manufacturerText.text = "Производитель >"
                }
                
                // Use real old price from catalog
                val oldPrice = arguments?.getString("productOldPrice")
                if (!oldPrice.isNullOrEmpty()) {
                    oldPriceText.text = oldPrice
                    oldPriceText.paintFlags = oldPriceText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    oldPriceText.visibility = View.VISIBLE
                } else {
                    oldPriceText.visibility = View.GONE
                }
                
                // Use real discount from catalog
                val discountPercent = arguments?.getInt("productDiscountPercent", 0) ?: 0
                if (discountPercent > 0) {
                    discountText.text = "$discountPercent%"
                    discountText.visibility = View.VISIBLE
                } else {
                    discountText.visibility = View.GONE
                }
                
                // Use real rating from catalog
                val ratingScore = arguments?.getDouble("productRatingScore", 0.0) ?: 0.0
                val ratingReviews = arguments?.getInt("productRatingReviews", 0) ?: 0
                ratingText.text = if (ratingScore > 0) ratingScore.toString() else "4.3"
                reviewsCountText.text = if (ratingReviews > 0) "($ratingReviews)" else "(288)"
                
                // Use real shop name from catalog
                val shopName = arguments?.getString("productShopName")
                if (!shopName.isNullOrEmpty()) {
                    sellerNameText.text = shopName
                    sellerRatingText.text = "4.5 • Отзывы оценок" // Shop rating не передается из каталога
                } else {
                    sellerNameText.text = "Яндекс Фабрика"
                    sellerRatingText.text = "4.5 • Отзывы оценок"
                }
                
                // Use real promo code from catalog
                val promoCode = arguments?.getString("promoCode")
                val promoDiscount = arguments?.getString("promoDiscount")
                val promoMinOrder = arguments?.getString("promoMinOrder")
                val promoExpiryDate = arguments?.getString("promoExpiryDate")
                
                if (!promoCode.isNullOrEmpty() && !promoDiscount.isNullOrEmpty()) {
                    promoDiscountText.text = promoDiscount
                    var promoText = promoCode
                    if (!promoMinOrder.isNullOrEmpty() && !promoExpiryDate.isNullOrEmpty()) {
                        promoText = "$promoCode\n$promoMinOrder • $promoExpiryDate"
                    }
                    promoCodeText.text = promoText
                    promoDiscountText.visibility = View.VISIBLE
                    promoCodeText.visibility = View.VISIBLE
                } else {
                    promoDiscountText.visibility = View.GONE
                    promoCodeText.visibility = View.GONE
                }
                
                return productImages
            }
            
            Log.d(TAG, "No product data in arguments, using fallback")
            return null
            
        } catch (e: Exception) {
            Log.e(TAG, "Error accessing catalog data: ${e.message}", e)
            null
        }
    }
    
    private fun showSampleProductDetail(productId: String) {
        Log.d(TAG, "Showing sample product detail for ID: $productId")
        
        manufacturerText.text = "Производитель >"
        productTitle.text = "Образец товара"
        currentPriceText.text = "3576 ₽"
        oldPriceText.text = "3612 ₽"
        oldPriceText.paintFlags = oldPriceText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        discountText.text = "1%"
        ratingText.text = "4.3"
        reviewsCountText.text = "(288)"
        sellerNameText.text = "Яндекс Фабрика"
        
        // Use sample images for the power tool
        val productImages = listOf(
            "https://avatars.mds.yandex.net/get-mpic/5288398/img_id8662451988765623136.jpeg/orig",
            "https://avatars.mds.yandex.net/get-mpic/5288266/img_id8662451988765623137.jpeg/orig", 
            "https://avatars.mds.yandex.net/get-mpic/5288399/img_id8662451988765623138.jpeg/orig"
        )
        
        Log.d(TAG, "Setting up gallery with ${productImages.size} sample images")
        setupGallery(productImages)
        
        // Hide skeleton and show content
        hideSkeletonShowContent()
    }
    
    private fun showProductDetail(productDetail: ProductDetail) {
        Log.d(TAG, "Showing product detail: ${productDetail.title}")
        
        manufacturerText.text = "${productDetail.manufacturer.name} >"
        productTitle.text = productDetail.title
        currentPriceText.text = productDetail.price.currentPrice
        
        // Handle old price (может быть null или пустой)
        val oldPrice = productDetail.price.oldPrice
        Log.d(TAG, "Old price from API: '$oldPrice'")
        if (!oldPrice.isNullOrEmpty() && oldPrice.isNotBlank()) {
            oldPriceText.text = oldPrice
            oldPriceText.paintFlags = oldPriceText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            oldPriceText.visibility = View.VISIBLE
        } else {
            // Показываем sample старую цену для демонстрации
            oldPriceText.text = "3612 ₽"
            oldPriceText.paintFlags = oldPriceText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            oldPriceText.visibility = View.VISIBLE
        }
        
        // Handle discount (может быть null)
        val discount = productDetail.price.discountPercentage
        Log.d(TAG, "Discount from API: '$discount'")
        if (!discount.isNullOrEmpty() && discount.isNotBlank()) {
            // Добавляем % если его нет
            val formattedDiscount = if (discount.endsWith("%")) discount else "$discount%"
            discountText.text = formattedDiscount
            discountText.visibility = View.VISIBLE
        } else {
            // Показываем sample скидку для демонстрации
            discountText.text = "1%"
            discountText.visibility = View.VISIBLE
        }
        
        // Rating с проверкой на nullable как в iOS
        val ratingScore = productDetail.rating.score
        ratingText.text = ratingScore?.takeIf { it > 0 }?.toString() ?: "4.3"
        
        val reviewsCount = productDetail.rating.reviewsCount
        reviewsCountText.text = reviewsCount?.takeIf { it > 0 }?.let { "($it)" } ?: "(288)"
        
        // Seller data with fallbacks
        val sellerName = productDetail.seller.name
        Log.d(TAG, "Seller name from API: '$sellerName'")
        if (sellerName.isNotEmpty()) {
            sellerNameText.text = sellerName
        } else {
            sellerNameText.text = "Яндекс Фабрика"
        }
        
        val sellerRating = productDetail.seller.rating
        val sellerReviews = productDetail.seller.reviewsCount
        Log.d(TAG, "Seller rating from API: '$sellerRating', reviews: '$sellerReviews'")
        if (sellerRating > 0 && sellerReviews.isNotEmpty()) {
            sellerRatingText.text = "$sellerRating • $sellerReviews"
        } else {
            sellerRatingText.text = "4.5 • Отзывы оценок"
        }
        
        // Handle promo code
        productDetail.promoCode?.let { promo ->
            promoDiscountText.text = promo.discount
            promoCodeText.text = "Промокод ${promo.code}"
            if (promo.minOrder != null && promo.expiryDate != null) {
                promoCodeText.text = "Промокод ${promo.code}\n${promo.minOrder} • ${promo.expiryDate}"
            }
            promoDiscountText.visibility = View.VISIBLE
            promoCodeText.visibility = View.VISIBLE
        } ?: run {
            // Show sample promo code for demonstration
            promoDiscountText.text = "-150 ₽"
            promoCodeText.text = "Промокод WOW500\nЗаказ от 3000₽ • до 20.09"
            promoDiscountText.visibility = View.VISIBLE
            promoCodeText.visibility = View.VISIBLE
        }
        
        // Setup gallery
        setupGallery(productDetail.images)
        
        // Hide skeleton and show content
        hideSkeletonShowContent()
    }
    
    private fun setupGallery(imageUrls: List<String>) {
        Log.d(TAG, "Setting up gallery with ${imageUrls.size} images")
        
        // Always setup gallery, even if empty
        val imagesToShow = if (imageUrls.isNotEmpty()) {
            imageUrls
        } else {
            // Fallback to placeholder
            listOf("placeholder")
        }
        
        val adapter = ProductGalleryAdapter(imagesToShow)
        galleryViewPager.adapter = adapter
        
        // Setup page indicator
        TabLayoutMediator(pageIndicator, galleryViewPager) { _, _ ->
            // Empty implementation - just shows dots
        }.attach()
        
        Log.d(TAG, "Gallery setup complete with ${imagesToShow.size} images")
    }
    
    private fun showSkeleton() {
        Log.d(TAG, "Showing skeleton loading state")
        skeletonContainer.visibility = View.VISIBLE
        contentContainer.visibility = View.GONE
        
        // Запускаем анимацию переливания для скелетона
        val animation = android.view.animation.AnimationUtils.loadAnimation(
            requireContext(), 
            R.anim.skeleton_shimmer
        )
        skeletonContainer.startAnimation(animation)
    }
    
    private fun hideSkeletonShowContent() {
        Log.d(TAG, "Hiding skeleton, showing content")
        
        // Animate transition for better UX
        skeletonContainer.animate()
            .alpha(0f)
            .setDuration(300)
            .withEndAction {
                skeletonContainer.visibility = View.GONE
                contentContainer.alpha = 0f
                contentContainer.visibility = View.VISIBLE
                contentContainer.animate()
                    .alpha(1f)
                    .setDuration(300)
                    .start()
            }
            .start()
    }
    
    private fun sendFavoriteStatusBack() {
        // Send favorite status back to catalog fragment
        val result = Bundle().apply {
            putString("productId", productId)
            putBoolean("isFavorite", isFavorite)
        }
        findNavController().previousBackStackEntry?.savedStateHandle?.set("favorite_result", result)
    }
    
    private fun updateFavoriteButton() {
        // Update main favorite button
        val favoriteIcon = if (isFavorite) {
            R.drawable.ic_heart_filled
        } else {
            R.drawable.ic_heart
        }
        favoriteButton.setImageResource(favoriteIcon)
        
        // Set favorite icon color
        val favoriteColor = if (isFavorite) {
            android.R.color.holo_red_dark
        } else {
            android.R.color.darker_gray
        }
        favoriteButton.imageTintList = android.content.res.ColorStateList.valueOf(
            requireContext().getColor(favoriteColor)
        )
    }
    
    private fun updateSellerFavoriteButton() {
        // Update seller favorite button
        val favoriteIcon = if (isSellerFavorite) {
            R.drawable.ic_heart_filled
        } else {
            R.drawable.ic_heart
        }
        sellerFavoriteButton.setImageResource(favoriteIcon)
        
        // Set favorite icon color
        val favoriteColor = if (isSellerFavorite) {
            android.R.color.holo_red_dark
        } else {
            android.R.color.darker_gray
        }
        sellerFavoriteButton.imageTintList = android.content.res.ColorStateList.valueOf(
            requireContext().getColor(favoriteColor)
        )
    }
}

// Simple adapter for product gallery
class ProductGalleryAdapter(private val imageUrls: List<String>) : 
    androidx.recyclerview.widget.RecyclerView.Adapter<ProductGalleryAdapter.ImageViewHolder>() {
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ImageViewHolder {
        val imageView = ImageView(parent.context).apply {
            layoutParams = ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT
            )
            scaleType = ImageView.ScaleType.FIT_CENTER // Показывать всю картинку целиком
            adjustViewBounds = true // Сохранять пропорции
        }
        return ImageViewHolder(imageView)
    }
    
    override fun onBindViewHolder(holder: ImageViewHolder, position: Int) {
        val imageUrl = imageUrls[position]
        
        if (imageUrl == "placeholder") {
            // Show placeholder drawable
            holder.imageView.setImageResource(R.drawable.ic_placeholder_large)
            holder.imageView.scaleType = ImageView.ScaleType.FIT_CENTER
        } else {
            // Load actual image from network - показываем всю картинку целиком
            Log.d("ProductGallery", "Loading image: $imageUrl")
            holder.imageView.scaleType = ImageView.ScaleType.FIT_CENTER
            Glide.with(holder.imageView)
                .load(imageUrl)
                .placeholder(R.drawable.ic_placeholder_large)
                .error(R.drawable.ic_placeholder_large)
                .fitCenter() // Показывать всю картинку без обрезки
                .into(holder.imageView)
        }
    }
    
    override fun getItemCount() = imageUrls.size
    
    class ImageViewHolder(val imageView: ImageView) : 
        androidx.recyclerview.widget.RecyclerView.ViewHolder(imageView)
} 