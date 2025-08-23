package ru.yandex.speed.workshop.android.presentation.catalog

import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.paging.PagingDataAdapter
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.yandex.analytics.api.YandexAnalytics
import ru.yandex.speed.workshop.android.R
import ru.yandex.speed.workshop.android.domain.models.Product
import ru.yandex.speed.workshop.android.utils.ImageLoader

class ProductPagingAdapter(
    private val onProductClick: (Product) -> Unit,
    private val isFavorite: (String) -> Boolean,
    private val imageLoader: ImageLoader,
) : PagingDataAdapter<Product, ProductPagingAdapter.ProductViewHolder>(Diff) {
    companion object {
        const val VIEW_TYPE_PRODUCT = 0
    }

    object Diff : DiffUtil.ItemCallback<Product>() {
        override fun areItemsTheSame(
            oldItem: Product,
            newItem: Product,
        ): Boolean = oldItem.id == newItem.id

        override fun areContentsTheSame(
            oldItem: Product,
            newItem: Product,
        ): Boolean {
            return oldItem.title == newItem.title &&
                    oldItem.currentPrice == newItem.currentPrice &&
                    oldItem.oldPrice == newItem.oldPrice &&
                    oldItem.discountPercent == newItem.discountPercent &&
                    oldItem.imageUrls == newItem.imageUrls &&
                    oldItem.isFavorite == newItem.isFavorite
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): ProductViewHolder {
        val view = ProductItemView(parent.context)
        return ProductViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: ProductViewHolder,
        position: Int,
    ) {
        getItem(position)?.let { holder.bind(it) }
    }

    inner class ProductViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val productImageView: ImageView = itemView.findViewById(R.id.productImageView)
        private val favoriteImageView: ImageView = itemView.findViewById(R.id.favoriteImageView)
        private val productTitleTextView: TextView = itemView.findViewById(R.id.productTitleTextView)
        private val productPriceTextView: TextView = itemView.findViewById(R.id.productPriceTextView)

        fun bind(product: Product) {
            val spacing = itemView.resources.getDimensionPixelSize(R.dimen.catalog_grid_spacing)
            val targetWidth = itemView.resources.displayMetrics.widthPixels / 2 - spacing
            val targetHeight = (targetWidth * 4f / 3f).toInt()

            // Загружаем изображение через ImageLoader с трекингом времени загрузки
            val imageUrl = product.imageUrls.firstOrNull() ?: ""
            imageLoader.loadCatalogImage(
                imageView = productImageView,
                url = imageUrl,
                width = targetWidth,
                height = targetHeight,
                trackingId = "catalog_${product.id}",
            )

            productTitleTextView.text = product.title
            productPriceTextView.text = product.currentPrice

            updateFavoriteIcon(product.id, favoriteImageView)

            itemView.setOnClickListener {
                // Аналитика при клике на товар
                YandexAnalytics.getInstance().trackEvent(
                    "product_click_from_list",
                    mapOf(
                        "product_id" to product.id,
                        "product_title" to product.title,
                        "position" to bindingAdapterPosition,
                    ),
                )
                onProductClick(product)
            }
            favoriteImageView.setOnClickListener {
                updateFavoriteIcon(product.id, favoriteImageView)
            }
        }

        private fun updateFavoriteIcon(
            productId: String,
            favoriteImageView: ImageView,
        ) {
            val isFav = isFavorite(productId)
            val favoriteIcon = if (isFav) R.drawable.ic_heart_filled else R.drawable.ic_heart
            favoriteImageView.setImageDrawable(ContextCompat.getDrawable(itemView.context, favoriteIcon))
            val favoriteColor = if (isFav) android.R.color.holo_red_dark else android.R.color.darker_gray
            favoriteImageView.imageTintList =
                android.content.res.ColorStateList.valueOf(
                    itemView.context.getColor(favoriteColor),
                )
        }

    }
}
