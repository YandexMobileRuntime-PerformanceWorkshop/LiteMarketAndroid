package ru.yandex.speed.workshop.android.presentation.catalog

import android.view.LayoutInflater
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
    private val onFavoriteClick: (String) -> Unit,
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
        ): Boolean = oldItem == newItem

        override fun areContentsTheSame(
            oldItem: Product,
            newItem: Product,
        ): Boolean = false
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
        private val productImageView: ImageView get() = itemView.findViewById(R.id.productImageView)
        private val favoriteImageView: ImageView get() = itemView.findViewById(R.id.favoriteImageView)
        private val productTitleTextView: TextView get() = itemView.findViewById(R.id.productTitleTextView)
        private val productPriceTextView: TextView get() = itemView.findViewById(R.id.productPriceTextView)

        fun bind(product: Product) {
            YandexAnalytics.getInstance().trackEvent(
                "product_impression",
                mapOf(
                    "product_id" to product.id,
                    "product_title" to product.title,
                    "product_price" to product.currentPrice,
                    "position" to bindingAdapterPosition,
                    "has_discount" to product.hasDiscount,
                ),
            )

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
                onFavoriteClick(product.id)
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
