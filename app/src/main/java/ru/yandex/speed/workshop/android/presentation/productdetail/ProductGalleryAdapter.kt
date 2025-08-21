package ru.yandex.speed.workshop.android.presentation.productdetail

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import ru.yandex.speed.workshop.android.R
import ru.yandex.speed.workshop.android.utils.ImageLoader

/**
 * Адаптер для галереи изображений продукта
 */
class ProductGalleryAdapter(
    private val context: Context,
    private val imageLoader: ImageLoader,
) : ListAdapter<String, ProductGalleryAdapter.GalleryViewHolder>(GalleryDiffCallback()) {
    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): GalleryViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_product_gallery, parent, false)
        return GalleryViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: GalleryViewHolder,
        position: Int,
    ) {
        val imageUrl = getItem(position)

        // Для первого изображения используем специальный метод загрузки без placeholder
        if (position == 0) {
            imageLoader.loadDetailFirstImage(
                imageView = holder.imageView,
                url = imageUrl,
                trackingId = "gallery_first_$position",
            )
        } else {
            imageLoader.loadDetailImage(
                imageView = holder.imageView,
                url = imageUrl,
                trackingId = "gallery_other_$position",
            )
        }
    }

    class GalleryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val imageView: ImageView = itemView.findViewById(R.id.productImage)
    }

    /**
     * DiffCallback для сравнения элементов галереи
     */
    private class GalleryDiffCallback : DiffUtil.ItemCallback<String>() {
        override fun areItemsTheSame(
            oldItem: String,
            newItem: String,
        ): Boolean {
            return oldItem == newItem
        }

        override fun areContentsTheSame(
            oldItem: String,
            newItem: String,
        ): Boolean {
            return oldItem == newItem
        }
    }
}
