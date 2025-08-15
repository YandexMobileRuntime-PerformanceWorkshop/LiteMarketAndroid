package ru.yandex.speed.workshop.android.presentation.catalog

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.MemoryCategory
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.resource.bitmap.DownsampleStrategy
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions
import ru.yandex.speed.workshop.android.R
import ru.yandex.speed.workshop.android.domain.models.Product

class ProductPagingAdapter(
    private val onProductClick: (Product) -> Unit,
    private val onFavoriteClick: (Product) -> Unit
) : PagingDataAdapter<Product, ProductPagingAdapter.ProductViewHolder>(Diff) {

    object Diff : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(oldItem: Product, newItem: Product): Boolean = oldItem.id == newItem.id
        override fun areContentsTheSame(oldItem: Product, newItem: Product): Boolean = oldItem == newItem
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ProductViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_product, parent, false)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(holder: ProductViewHolder, position: Int) {
        getItem(position)?.let { holder.bind(it) }
    }

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val productImageView: ImageView = itemView.findViewById(R.id.productImageView)
        private val favoriteImageView: ImageView = itemView.findViewById(R.id.favoriteImageView)
        private val productTitleTextView: TextView = itemView.findViewById(R.id.productTitleTextView)
        private val productPriceTextView: TextView = itemView.findViewById(R.id.productPriceTextView)

        fun bind(product: Product) {
            val targetWidth = itemView.resources.displayMetrics.widthPixels / 2 - itemView.resources.getDimensionPixelSize(R.dimen.catalog_grid_spacing)
            val targetHeight = (targetWidth * 4f / 3f).toInt()

            Glide.get(itemView.context).setMemoryCategory(MemoryCategory.HIGH)

            Glide.with(itemView.context)
                .load(product.url)
                .placeholder(R.drawable.ic_placeholder)
                .error(R.drawable.ic_placeholder)
                .centerCrop()
                .override(targetWidth, targetHeight)
                .downsample(DownsampleStrategy.AT_MOST)
                .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC)
                .thumbnail(0.25f)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(productImageView)

            productTitleTextView.text = product.title
            productPriceTextView.text = product.price

            updateFavoriteIcon(product, favoriteImageView)

            itemView.setOnClickListener { onProductClick(product) }
            favoriteImageView.setOnClickListener {
                product.isFavorite = !product.isFavorite
                updateFavoriteIcon(product, favoriteImageView)
                onFavoriteClick(product)
            }
        }

        private fun updateFavoriteIcon(product: Product, favoriteImageView: ImageView) {
            val favoriteIcon = if (product.isFavorite) R.drawable.ic_heart_filled else R.drawable.ic_heart
            favoriteImageView.setImageResource(favoriteIcon)
            val favoriteColor = if (product.isFavorite) android.R.color.holo_red_dark else android.R.color.darker_gray
            favoriteImageView.imageTintList = android.content.res.ColorStateList.valueOf(
                itemView.context.getColor(favoriteColor)
            )
        }
    }
}


