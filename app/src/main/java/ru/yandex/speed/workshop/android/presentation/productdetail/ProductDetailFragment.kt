package ru.yandex.speed.workshop.android.presentation.productdetail

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import timber.log.Timber
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
import ru.yandex.speed.workshop.android.databinding.FragmentProductDetailBinding
import ru.yandex.speed.workshop.android.databinding.FragmentProductDetailSkeletonBinding
import ru.yandex.speed.workshop.android.data.network.HttpClient
import ru.yandex.speed.workshop.android.data.network.ProductService
import ru.yandex.speed.workshop.android.domain.models.ProductDetail
import ru.yandex.speed.workshop.android.presentation.common.SnackbarUtils
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import android.graphics.Paint

class ProductDetailFragment : Fragment() {
    
    private lateinit var productId: String
    private var isFavorite: Boolean = false
    private var isSellerFavorite: Boolean = false

    // ViewBinding - используем делегат для избегания null-safety проблем
    private var _binding: FragmentProductDetailBinding? = null
    private val binding get() = checkNotNull(_binding) { "Binding не должен использоваться после onDestroyView()" }
    private var _skeletonBinding: FragmentProductDetailSkeletonBinding? = null
    private val skeletonBinding get() = checkNotNull(_skeletonBinding) { "SkeletonBinding не должен использоваться после onDestroyView()" }

    private lateinit var productService: ProductService

    private lateinit var contentContainer: View
    private lateinit var skeletonContainer: View
    
    // Timber автоматически добавляет имя класса в логи, константа TAG больше не нужна
    
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        Timber.d("onCreateView for productId: ${arguments?.getString("productId")}")
        
        // Create container with both skeleton and content layouts
        val containerLayout = FrameLayout(requireContext())
        containerLayout.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT,
            ViewGroup.LayoutParams.MATCH_PARENT
        )
        
        // Inflate skeleton layout with ViewBinding
        _skeletonBinding = FragmentProductDetailSkeletonBinding.inflate(inflater, containerLayout, false)
        skeletonContainer = skeletonBinding.root
        containerLayout.addView(skeletonBinding.root)
        
        // Inflate content layout with ViewBinding
        _binding = FragmentProductDetailBinding.inflate(inflater, containerLayout, false)
        contentContainer = binding.root
        contentContainer.visibility = View.GONE
        containerLayout.addView(binding.root)
        
        return containerLayout
    }
    
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Get productId and favorite status from arguments
        productId = arguments?.getString("productId") ?: "unknown"
        isFavorite = arguments?.getBoolean("isFavorite", false) ?: false

        // Show skeleton first
        showSkeleton()
        setupClickListeners()
        updateFavoriteButton() // Update favorite button with passed status
        updateSellerFavoriteButton() // Initialize seller favorite button
        initProductService()
        loadProductDetail(productId)
    }
    
    override fun onDestroyView() {
        super.onDestroyView()
        // Останавливаем анимацию скелетона
        val background = skeletonBinding.skeletonContainer.background
        if (background is android.graphics.drawable.AnimationDrawable) {
            background.stop()
        }
        _binding = null
        _skeletonBinding = null
    }
    
    private fun setupClickListeners() {
        binding.backButton.setOnClickListener {
            Timber.d("Back button clicked")
            sendFavoriteStatusBack()
            findNavController().popBackStack()
        }
        
        binding.shareButton.setOnClickListener {
            Timber.d("Share button clicked")
            SnackbarUtils.showPlaceholder(requireView(), "Поделиться товаром")
        }
        
        binding.favoriteButton.setOnClickListener {
            // Toggle favorite status
            isFavorite = !isFavorite
            updateFavoriteButton()
            
            // Send result back to catalog
            sendFavoriteStatusBack()
            
            Timber.d("Favorite button clicked, new status: $isFavorite")
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
        
        binding.similarButton.setOnClickListener {
            Timber.d("Similar button clicked")
            SnackbarUtils.showPlaceholder(requireView(), "Показать похожие товары")
        }
        
        binding.addToCartButton.setOnClickListener {
            Timber.d("Add to cart button clicked")
            SnackbarUtils.showSuccess(requireView(), "Добавлено в корзину", duration = 3000)
        }
        
        binding.buyNowButton.setOnClickListener {
            Timber.d("Buy now button clicked")
            SnackbarUtils.showPlaceholder(requireView(), "Купить сейчас")
        }
        
        binding.copyPromoButton.setOnClickListener {
            Timber.d("Copy promo button clicked")
            SnackbarUtils.showCopied(requireView(), "Промокод скопирован")
        }
        
        binding.sellerFavoriteButton.setOnClickListener {
            // Toggle seller favorite status
            isSellerFavorite = !isSellerFavorite
            updateSellerFavoriteButton()
            
            Timber.d("Seller favorite button clicked, new status: $isSellerFavorite")
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
        
        binding.sellerDetailsButton.setOnClickListener {
            Timber.d("Seller details button clicked")
            SnackbarUtils.showPlaceholder(requireView(), "Открыть страницу продавца")
        }
    }
    
    private fun initProductService() {
        val httpClient = HttpClient.getInstance(requireContext())
        productService = ProductService(httpClient.getApi())
    }
    
    private fun loadProductDetail(productId: String) {
        Timber.d("Loading product detail for ID: $productId")
        
        // Сначала показываем данные из аргументов, если они есть
        val catalogImages = tryGetImagesFromCatalog(productId)
        if (catalogImages != null) {
            // У нас есть данные из каталога - сразу их показываем
            Timber.d("Showing catalog data from arguments first")
            setupGallery(catalogImages)
            hideSkeletonShowContent()
            
            // Затем параллельно загружаем полные данные с сервера для обновления
            loadProductFromAPI(productId)
        } else {
            // Нет данных из каталога - загружаем с сервера
            loadProductFromAPI(productId)
        }
    }
    
    private fun loadProductFromAPI(productId: String) {
        viewLifecycleOwner.lifecycleScope.launch {
            try {
                Timber.d("Trying to load product detail from API for ID: $productId")
                
                // Загружаем с API
                val productDetailResponse = productService.getProductDetail(productId)
                val productDetail = productDetailResponse.product
                
                Timber.d("Successfully loaded product detail from API: ${productDetail.title}")
                showProductDetail(productDetail)
                
            } catch (e: Exception) {
                Timber.w("Failed to load from API: ${e.message}")
                
                // Если мы еще не показали данные из каталога, проверяем их наличие
                if (contentContainer.visibility != View.VISIBLE) {
                    val catalogImages = tryGetImagesFromCatalog(productId)
                    
                    if (catalogImages != null) {
                        // У нас есть данные из каталога - используем их
                        Timber.d("Using catalog data from arguments as fallback")
                        setupGallery(catalogImages)
                        hideSkeletonShowContent()
                    } else {
                        // Нет данных из каталога - используем примерные данные
                        Timber.d("Using sample data as last resort")
                        showSampleProductDetail(productId)
                    }
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
                Timber.d("Using complete product data from arguments: $productTitle")
                
                // Update UI with ALL passed data
                productTitle.let { 
                    binding.productTitle.text = it 
                }
                productPrice?.let { 
                    binding.currentPriceText.text = it 
                }
                
                // Use real vendor/manufacturer from catalog
                val vendor = arguments?.getString("productVendor")
                if (!vendor.isNullOrEmpty()) {
                    binding.manufacturerText.text = "$vendor >"
                } else {
                    binding.manufacturerText.text = "Производитель >"
                }
                
                // Use real old price from catalog
                val oldPrice = arguments?.getString("productOldPrice")
                if (!oldPrice.isNullOrEmpty()) {
                    binding.oldPriceText.text = oldPrice
                    binding.oldPriceText.paintFlags = binding.oldPriceText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
                    binding.oldPriceText.visibility = View.VISIBLE
                } else {
                    binding.oldPriceText.visibility = View.GONE
                }
                
                // Use real discount from catalog
                val discountPercent = arguments?.getInt("productDiscountPercent", 0) ?: 0
                if (discountPercent > 0) {
                    binding.discountText.text = "$discountPercent%"
                    binding.discountText.visibility = View.VISIBLE
                } else {
                    binding.discountText.visibility = View.GONE
                }
                
                // Use real rating from catalog
                val ratingScore = arguments?.getDouble("productRatingScore", 0.0) ?: 0.0
                val ratingReviews = arguments?.getInt("productRatingReviews", 0) ?: 0
                binding.ratingText.text = if (ratingScore > 0) ratingScore.toString() else "4.3"
                binding.reviewsCountText.text = if (ratingReviews > 0) "($ratingReviews)" else "(288)"
                
                // Use real shop name from catalog
                val shopName = arguments?.getString("productShopName")
                if (!shopName.isNullOrEmpty()) {
                    binding.sellerNameText.text = shopName
                    binding.sellerRatingText.text = "4.5 • Отзывы оценок" // Shop rating не передается из каталога
                } else {
                    binding.sellerNameText.text = "Яндекс Фабрика"
                    binding.sellerRatingText.text = "4.5 • Отзывы оценок"
                }
                
                // Use real promo code from catalog
                val promoCode = arguments?.getString("promoCode")
                val promoDiscount = arguments?.getString("promoDiscount")
                val promoMinOrder = arguments?.getString("promoMinOrder")
                val promoExpiryDate = arguments?.getString("promoExpiryDate")
                
                if (!promoCode.isNullOrEmpty() && !promoDiscount.isNullOrEmpty()) {
                    binding.promoDiscountText.text = promoDiscount
                    var promoText = promoCode
                    if (!promoMinOrder.isNullOrEmpty() && !promoExpiryDate.isNullOrEmpty()) {
                        promoText = "$promoCode\n$promoMinOrder • $promoExpiryDate"
                    }
                    binding.promoCodeText.text = promoText
                    binding.promoDiscountText.visibility = View.VISIBLE
                    binding.promoCodeText.visibility = View.VISIBLE
                } else {
                    binding.promoDiscountText.visibility = View.GONE
                    binding.promoCodeText.visibility = View.GONE
                }
                
                return productImages
            }
            
            Timber.d("No product data in arguments, using fallback")
            return null
            
        } catch (e: Exception) {
            Timber.e(e, "Error accessing catalog data: ${e.message}")
            null
        }
    }
    
    private fun showSampleProductDetail(productId: String) {
        Timber.d("Showing sample product detail for ID: $productId")
        
        binding.manufacturerText.text = "Производитель >"
        binding.productTitle.text = "Образец товара"
        binding.currentPriceText.text = "3576 ₽"
        binding.oldPriceText.text = "3612 ₽"
        binding.oldPriceText.paintFlags = binding.oldPriceText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
        binding.discountText.text = "1%"
        binding.ratingText.text = "4.3"
        binding.reviewsCountText.text = "(288)"
        binding.sellerNameText.text = "Яндекс Фабрика"
        
        // Use sample images for the power tool
        val productImages = listOf(
            "https://avatars.mds.yandex.net/get-mpic/5288398/img_id8662451988765623136.jpeg/orig",
            "https://avatars.mds.yandex.net/get-mpic/5288266/img_id8662451988765623137.jpeg/orig", 
            "https://avatars.mds.yandex.net/get-mpic/5288399/img_id8662451988765623138.jpeg/orig"
        )
        
        Timber.d("Setting up gallery with ${productImages.size} sample images")
        setupGallery(productImages)
        
        // Hide skeleton and show content
        hideSkeletonShowContent()
    }
    
    private fun showProductDetail(productDetail: ProductDetail) {
        Timber.d("Showing product detail: ${productDetail.title}")
        
        binding.manufacturerText.text = "${productDetail.manufacturer.name} >"
        binding.productTitle.text = productDetail.title
        binding.currentPriceText.text = productDetail.price.currentPrice
        
        // Handle old price (может быть null или пустой)
        val oldPrice = productDetail.price.oldPrice
        Timber.d("Old price from API: '$oldPrice'")
        if (!oldPrice.isNullOrEmpty() && oldPrice.isNotBlank()) {
            binding.oldPriceText.text = oldPrice
            binding.oldPriceText.paintFlags = binding.oldPriceText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            binding.oldPriceText.visibility = View.VISIBLE
        } else {
            // Показываем sample старую цену для демонстрации
            binding.oldPriceText.text = "3612 ₽"
            binding.oldPriceText.paintFlags = binding.oldPriceText.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            binding.oldPriceText.visibility = View.VISIBLE
        }
        
        // Handle discount (может быть null)
        val discount = productDetail.price.discountPercentage
        Timber.d("Discount from API: '$discount'")
        if (!discount.isNullOrEmpty() && discount.isNotBlank()) {
            // Добавляем % если его нет
            val formattedDiscount = if (discount.endsWith("%")) discount else "$discount%"
            binding.discountText.text = formattedDiscount
            binding.discountText.visibility = View.VISIBLE
        } else {
            // Показываем sample скидку для демонстрации
            binding.discountText.text = "1%"
            binding.discountText.visibility = View.VISIBLE
        }
        
        // Rating с проверкой на nullable как в iOS
        val ratingScore = productDetail.rating.score
        binding.ratingText.text = ratingScore?.takeIf { it > 0 }?.toString() ?: "4.3"
        
        val reviewsCount = productDetail.rating.reviewsCount
        binding.reviewsCountText.text = reviewsCount?.takeIf { it > 0 }?.let { "($it)" } ?: "(288)"
        
        // Seller data with fallbacks
        val sellerName = productDetail.seller.name
        Timber.d("Seller name from API: '$sellerName'")
        if (sellerName.isNotEmpty()) {
            binding.sellerNameText.text = sellerName
        } else {
            binding.sellerNameText.text = "Яндекс Фабрика"
        }
        
        val sellerRating = productDetail.seller.rating
        val sellerReviews = productDetail.seller.reviewsCount
        Timber.d("Seller rating from API: '$sellerRating', reviews: '$sellerReviews'")
        if (sellerRating > 0 && sellerReviews.isNotEmpty()) {
            binding.sellerRatingText.text = "$sellerRating • $sellerReviews"
        } else {
            binding.sellerRatingText.text = "4.5 • Отзывы оценок"
        }
        
        // Handle promo code
        productDetail.promoCode?.let { promo ->
            binding.promoDiscountText.text = promo.discount
            binding.promoCodeText.text = "Промокод ${promo.code}"
            if (promo.minOrder != null && promo.expiryDate != null) {
                binding.promoCodeText.text = "Промокод ${promo.code}\n${promo.minOrder} • ${promo.expiryDate}"
            }
            binding.promoDiscountText.visibility = View.VISIBLE
            binding.promoCodeText.visibility = View.VISIBLE
        } ?: run {
            // Show sample promo code for demonstration
            binding.promoDiscountText.text = "-150 ₽"
            binding.promoCodeText.text = "Промокод WOW500\nЗаказ от 3000₽ • до 20.09"
            binding.promoDiscountText.visibility = View.VISIBLE
            binding.promoCodeText.visibility = View.VISIBLE
        }
        
        // Setup gallery
        setupGallery(productDetail.images)
        
        // Hide skeleton and show content
        hideSkeletonShowContent()
    }
    
    private fun setupGallery(imageUrls: List<String>) {
        Timber.d("Setting up gallery with ${imageUrls.size} images")
        
        // Always setup gallery, even if empty
        val imagesToShow = if (imageUrls.isNotEmpty()) {
            imageUrls
        } else {
            // Fallback to placeholder
            listOf("placeholder")
        }
        
        // Gallery adapter
        class GalleryAdapter(private val imageUrls: List<String>) : 
            androidx.recyclerview.widget.RecyclerView.Adapter<GalleryAdapter.GalleryViewHolder>() {
            
            override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GalleryViewHolder {
                val imageView = ImageView(parent.context).apply {
                    layoutParams = ViewGroup.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.MATCH_PARENT
                    )
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    adjustViewBounds = true
                }
                return GalleryViewHolder(imageView)
            }
            
            override fun getItemCount() = imageUrls.size
            
            override fun onBindViewHolder(holder: GalleryViewHolder, position: Int) {
                val imageUrl = imageUrls[position]
                
                if (imageUrl == "placeholder") {
                    holder.imageView.setImageResource(R.drawable.ic_placeholder_large)
                } else {
                    Glide.with(holder.imageView)
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_placeholder_large)
                        .error(R.drawable.ic_placeholder_large)
                        .fitCenter()
                        .into(holder.imageView)
                }
            }
            
            inner class GalleryViewHolder(val imageView: ImageView) : 
                androidx.recyclerview.widget.RecyclerView.ViewHolder(imageView)
        }
        
        val adapter = GalleryAdapter(imagesToShow)
        binding.galleryViewPager.adapter = adapter
        
        // Setup page indicator
        TabLayoutMediator(binding.pageIndicator, binding.galleryViewPager) { _, _ ->
            // Empty implementation - just shows dots
        }.attach()
        
        Timber.d("Gallery setup complete with ${imagesToShow.size} images")
    }
    
    private fun showSkeleton() {
        Timber.d("Showing skeleton loading state")
        skeletonContainer.visibility = View.VISIBLE
        contentContainer.visibility = View.GONE
        
        // Используем drawable animation вместо view animation для лучшей производительности
        val background = skeletonBinding.skeletonContainer.background
        if (background is android.graphics.drawable.AnimationDrawable) {
            background.start()
        } else {
            // Устанавливаем анимированный фон
            val animatedBackground = requireContext().getDrawable(R.drawable.skeleton_shimmer_animation) as? android.graphics.drawable.AnimationDrawable
            skeletonBinding.skeletonContainer.background = animatedBackground
            animatedBackground?.start()
        }
    }
    
    private fun hideSkeletonShowContent() {
        Timber.d("Hiding skeleton, showing content")
        
        // Останавливаем анимацию скелетона
        val background = skeletonBinding.skeletonContainer.background
        if (background is android.graphics.drawable.AnimationDrawable) {
            background.stop()
        }
        
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
        binding.favoriteButton.setImageResource(favoriteIcon)
        
        // Set favorite icon color
        val favoriteColor = if (isFavorite) {
            android.R.color.holo_red_dark
        } else {
            android.R.color.darker_gray
        }
        binding.favoriteButton.imageTintList = android.content.res.ColorStateList.valueOf(
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
        binding.sellerFavoriteButton.setImageResource(favoriteIcon)
        
        // Set favorite icon color
        val favoriteColor = if (isSellerFavorite) {
            android.R.color.holo_red_dark
        } else {
            android.R.color.darker_gray
        }
        binding.sellerFavoriteButton.imageTintList = android.content.res.ColorStateList.valueOf(
            requireContext().getColor(favoriteColor)
        )
    }
}