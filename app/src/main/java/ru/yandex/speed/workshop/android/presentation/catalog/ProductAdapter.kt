package ru.yandex.speed.workshop.android.presentation.catalog

import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import ru.yandex.speed.workshop.android.R
import ru.yandex.speed.workshop.android.domain.models.Product

/**
 * Адаптер для отображения товаров в RecyclerView (аналог iOS ProductCell)
 */
class ProductAdapter(
    private val onProductClick: (Product) -> Unit,
    private val onFavoriteClick: (Product) -> Unit
) : ListAdapter<ProductAdapter.Item, RecyclerView.ViewHolder>(ProductDiffCallback()) {
    
    private var recyclerView: RecyclerView? = null
    
    override fun onAttachedToRecyclerView(recyclerView: RecyclerView) {
        super.onAttachedToRecyclerView(recyclerView)
        this.recyclerView = recyclerView
    }
    
    override fun onDetachedFromRecyclerView(recyclerView: RecyclerView) {
        super.onDetachedFromRecyclerView(recyclerView)
        this.recyclerView = null
    }
    
    companion object {
        private const val TYPE_PRODUCT = 0
        private const val TYPE_LOADING = 1
        private const val TYPE_SKELETON = 2
    }
    
    sealed class Item {
        data class ProductItem(val product: Product) : Item()
        data object LoadingItem : Item()
        data object SkeletonItem : Item()
    }
    
    override fun getItemViewType(position: Int): Int {
        return when (getItem(position)) {
            is Item.ProductItem -> TYPE_PRODUCT
            is Item.LoadingItem -> TYPE_LOADING
            is Item.SkeletonItem -> TYPE_SKELETON
        }
    }
    
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {
            TYPE_PRODUCT -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_product, parent, false)
                ProductViewHolder(view)
            }
            TYPE_LOADING -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_loading, parent, false)
                LoadingViewHolder(view)
            }
            TYPE_SKELETON -> {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_product_skeleton, parent, false)
                SkeletonViewHolder(view)
            }
            else -> throw IllegalArgumentException("Unknown view type: $viewType")
        }
    }
    
    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (val item = getItem(position)) {
            is Item.ProductItem -> (holder as ProductViewHolder).bind(item.product)
            is Item.LoadingItem -> {
                // Loading item doesn't need binding
            }
            is Item.SkeletonItem -> {
                // Skeleton item doesn't need binding
            }
        }
    }
    
    /**
     * ViewHolder для товара (аналог iOS ProductCell)
     */
    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val productImageView: ImageView = itemView.findViewById(R.id.productImageView)
        private val favoriteImageView: ImageView = itemView.findViewById(R.id.favoriteImageView)
        private val productTitleTextView: TextView = itemView.findViewById(R.id.productTitleTextView)
        private val productPriceTextView: TextView = itemView.findViewById(R.id.productPriceTextView)
        
        fun bind(product: Product) {
            // Load product image using Glide (аналог SDWebImage в iOS)
            Glide.with(itemView.context)
                .load(product.url)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(productImageView)
            
            // Set product title
            productTitleTextView.text = product.title
            
            // Set product price
            productPriceTextView.text = product.price
            
            // Set favorite icon and color
            updateFavoriteIcon(product, favoriteImageView)
            
            // Set click listeners
            itemView.setOnClickListener {
                onProductClick(product)
            }
            
            favoriteImageView.setOnClickListener {
                // Toggle favorite status immediately for UI feedback
                product.isFavorite = !product.isFavorite
                
                // Update icon and color immediately
                updateFavoriteIcon(product, favoriteImageView)
                
                // Call the callback for business logic
                onFavoriteClick(product)
            }
        }
        
        private fun updateFavoriteIcon(product: Product, favoriteImageView: ImageView) {
            // Set favorite icon
            val favoriteIcon = if (product.isFavorite) {
                R.drawable.ic_heart_filled
            } else {
                R.drawable.ic_heart
            }
            favoriteImageView.setImageResource(favoriteIcon)
            
            // Set favorite icon color using imageTintList for better compatibility
            val favoriteColor = if (product.isFavorite) {
                android.R.color.holo_red_dark
            } else {
                android.R.color.darker_gray
            }
            favoriteImageView.imageTintList = android.content.res.ColorStateList.valueOf(
                itemView.context.getColor(favoriteColor)
            )
        }
    }
    
    /**
     * ViewHolder для индикатора загрузки
     */
    class LoadingViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    
    /**
     * ViewHolder для скелетона карточки товара
     */
    class SkeletonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        init {
            // Запускаем shimmer анимацию на каждом элементе с небольшой задержкой
            val animation = android.view.animation.AnimationUtils.loadAnimation(
                itemView.context, 
                R.anim.skeleton_shimmer
            )
            
            val skeletonViews = listOf(
                R.id.imageViewSkeleton,
                R.id.favoriteIconSkeleton,
                R.id.titleSkeleton,
                R.id.titleSkeleton2,
                R.id.priceSkeleton
            )
            
            skeletonViews.forEachIndexed { index, viewId ->
                itemView.findViewById<View>(viewId)?.let { view ->
                    // Добавляем небольшую задержку для создания волны
                    view.postDelayed({
                        view.startAnimation(animation)
                    }, (index * 100).toLong())
                }
            }
        }
    }
    
    /**
     * Обновляет список с добавлением loading item если нужно
     */
    fun updateProducts(products: List<Product>, showLoading: Boolean) {
        Log.d("ProductAdapter", "updateProducts called with ${products.size} products, showLoading=$showLoading")
        val items = mutableListOf<Item>()
        products.forEach { product ->
            items.add(Item.ProductItem(product))
        }
        if (showLoading) {
            items.add(Item.LoadingItem)
        }
        Log.d("ProductAdapter", "About to submitList with ${items.size} items (${items.count { it is Item.ProductItem }} products + ${if (showLoading) 1 else 0} loading)")
        
        // Create a new list to ensure DiffUtil detects the change
        val newList = items.toList()
        submitList(newList)
        Log.d("ProductAdapter", "submitList completed with ${newList.size} items")
    }
    
        /**
     * Smart method for adding products - uses fast pagination for appending items,
     * DiffUtil for full list replacement
     */
    fun addProducts(newProducts: List<Product>, showLoading: Boolean) {
        Log.d("ProductAdapter", "addProducts: Starting with ${currentList.size} items, adding ${newProducts.size}")
        
        val existingProducts = currentList.filterIsInstance<Item.ProductItem>().map { it.product }
        val existingIds = existingProducts.map { it.id }.toSet()
        val uniqueNewProducts = newProducts.filter { it.id !in existingIds }
        
        if (uniqueNewProducts.size != newProducts.size) {
            Log.d("ProductAdapter", "Filtered out ${newProducts.size - uniqueNewProducts.size} duplicate products")
        }
        
        val allProducts = existingProducts + uniqueNewProducts
        val newItems = mutableListOf<Item>()
        
        allProducts.forEach { product ->
            newItems.add(Item.ProductItem(product))
        }
        
        if (showLoading) {
            newItems.add(Item.LoadingItem)
        }
        
        Log.d("ProductAdapter", "Final list: ${newItems.size} items (${allProducts.size} products)")
        
        // Save scroll position before potential list clear
        val layoutManager = recyclerView?.layoutManager as? androidx.recyclerview.widget.GridLayoutManager
        val scrollPosition = layoutManager?.findFirstVisibleItemPosition() ?: 0
        val scrollOffset = layoutManager?.findViewByPosition(scrollPosition)?.top ?: 0
        
        // Determine operation type: pagination (append) vs full refresh (replace)
        val oldProductsCount = currentList.count { it is Item.ProductItem }
        val newProductsCount = newItems.count { it is Item.ProductItem }
        val isPagination = newProductsCount > oldProductsCount
        
        if (isPagination) {
            Log.d("ProductAdapter", "Using pagination method")
            // Fast pagination: clear and resubmit to bypass DiffUtil complexity
            submitList(null) 
            recyclerView?.post {
                submitList(newItems.toList())
                // Restore scroll position after pagination
                layoutManager?.scrollToPositionWithOffset(scrollPosition, scrollOffset)
                Log.d("ProductAdapter", "Pagination complete: ${allProducts.size} products")
            }
        } else {
            Log.d("ProductAdapter", "Using DiffUtil for full refresh")
            submitList(newItems.toList())
        }
    }
    
    fun showSkeletons(count: Int = 6) {
        val skeletonItems = (0 until count).map { Item.SkeletonItem }
        submitList(skeletonItems)
    }
    
    fun hideSkeletons() {
        // Real data will replace skeletons automatically when updateProducts() is called
    }
}

class ProductDiffCallback : DiffUtil.ItemCallback<ProductAdapter.Item>() {
    override fun areItemsTheSame(oldItem: ProductAdapter.Item, newItem: ProductAdapter.Item): Boolean {
        return when {
            oldItem is ProductAdapter.Item.ProductItem && newItem is ProductAdapter.Item.ProductItem ->
                oldItem.product.id == newItem.product.id
            oldItem is ProductAdapter.Item.LoadingItem && newItem is ProductAdapter.Item.LoadingItem -> true
            oldItem is ProductAdapter.Item.SkeletonItem && newItem is ProductAdapter.Item.SkeletonItem -> true
            else -> false
        }
    }
    
    override fun areContentsTheSame(oldItem: ProductAdapter.Item, newItem: ProductAdapter.Item): Boolean {
        return when {
            oldItem is ProductAdapter.Item.ProductItem && newItem is ProductAdapter.Item.ProductItem ->
                oldItem.product == newItem.product
            else -> oldItem == newItem
        }
    }
} 