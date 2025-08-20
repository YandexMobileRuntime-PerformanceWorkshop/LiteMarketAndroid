package ru.yandex.speed.workshop.android.presentation.catalog

import android.content.res.ColorStateList
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.chip.Chip
import ru.yandex.speed.workshop.android.R

/**
 * Адаптер для горизонтального списка категорий
 */
class CategoryAdapter(
    private val categories: List<String>,
    private val onCategoryClick: (String, Int) -> Unit,
) : RecyclerView.Adapter<CategoryAdapter.CategoryViewHolder>() {
    private var selectedPosition = 0

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): CategoryViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.item_category_new, parent, false)
        return CategoryViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: CategoryViewHolder,
        position: Int,
    ) {
        holder.bind(categories[position], position == selectedPosition)
    }

    override fun getItemCount(): Int = categories.size

    fun setSelectedPosition(position: Int) {
        val previousPosition = selectedPosition
        selectedPosition = position
        notifyItemChanged(previousPosition)
        notifyItemChanged(selectedPosition)
    }

    inner class CategoryViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val categoryChip: Chip = itemView.findViewById(R.id.categoryChip)

        fun bind(
            category: String,
            isSelected: Boolean,
        ) {
            categoryChip.text = category
            categoryChip.isSelected = isSelected

            // Set colors using Material Design approach - guaranteed to work
            if (isSelected) {
                categoryChip.setTextColor(ContextCompat.getColor(itemView.context, R.color.white))
                categoryChip.chipBackgroundColor =
                    ColorStateList.valueOf(
                        ContextCompat.getColor(itemView.context, R.color.category_selected_bg),
                    )
            } else {
                categoryChip.setTextColor(ContextCompat.getColor(itemView.context, R.color.category_text_unselected))
                categoryChip.chipBackgroundColor =
                    ColorStateList.valueOf(
                        ContextCompat.getColor(itemView.context, R.color.category_unselected_bg),
                    )
            }

            categoryChip.setOnClickListener {
                onCategoryClick(category, bindingAdapterPosition)
            }
        }
    }
}
