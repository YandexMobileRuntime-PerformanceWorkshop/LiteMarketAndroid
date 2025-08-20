package ru.yandex.speed.workshop.android.presentation.catalog

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.Animation
import android.view.animation.AnimationUtils
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.yandex.speed.workshop.android.R
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Хелпер для работы со скелетонами, заменяющий кастомные View
 */
@Singleton
class SkeletonHelper
    @Inject
    constructor() {
        /**
         * Настраивает стандартный RecyclerView для отображения скелетона
         */
        fun setupSkeletonRecyclerView(
            recyclerView: RecyclerView,
            context: Context,
        ): SkeletonAdapter {
            val skeletonAdapter = SkeletonAdapter()
            recyclerView.apply {
                layoutManager = GridLayoutManager(context, 2)
                adapter = skeletonAdapter
                setHasFixedSize(true)
            }
            return skeletonAdapter
        }

        /**
         * Запускает анимацию для всех дочерних элементов ViewGroup
         */
        fun startSkeletonAnimation(
            viewGroup: ViewGroup,
            context: Context,
        ) {
            val animation = AnimationUtils.loadAnimation(context, R.anim.skeleton_pulse)
            applyAnimationToAllViews(viewGroup, animation)
        }

        /**
         * Останавливает анимацию для всех дочерних элементов ViewGroup
         */
        fun stopSkeletonAnimation(viewGroup: ViewGroup) {
            clearAnimationFromAllViews(viewGroup)
        }

        /**
         * Применяет анимацию ко всем дочерним элементам ViewGroup
         */
        private fun applyAnimationToAllViews(
            viewGroup: ViewGroup,
            animation: Animation,
        ) {
            viewGroup.startAnimation(animation)

            for (i in 0 until viewGroup.childCount) {
                val child = viewGroup.getChildAt(i)
                if (child is ViewGroup) {
                    applyAnimationToAllViews(child, animation)
                } else {
                    child?.startAnimation(animation)
                }
            }
        }

        /**
         * Очищает анимацию для всех дочерних элементов ViewGroup
         */
        private fun clearAnimationFromAllViews(viewGroup: ViewGroup) {
            viewGroup.clearAnimation()

            for (i in 0 until viewGroup.childCount) {
                val child = viewGroup.getChildAt(i)
                if (child is ViewGroup) {
                    clearAnimationFromAllViews(child)
                } else {
                    child?.clearAnimation()
                }
            }
        }

        /**
         * Адаптер для отображения скелетонов
         */
        class SkeletonAdapter : RecyclerView.Adapter<SkeletonViewHolder>() {
            var skeletonCount: Int = 6
                set(value) {
                    field = value
                    notifyDataSetChanged()
                }

            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int,
            ): SkeletonViewHolder {
                val view =
                    LayoutInflater.from(parent.context)
                        .inflate(R.layout.item_product_skeleton, parent, false)
                return SkeletonViewHolder(view)
            }

            override fun getItemCount(): Int = skeletonCount

            override fun onBindViewHolder(
                holder: SkeletonViewHolder,
                position: Int,
            ) {
                // Ничего не делаем, так как это скелетон
            }

            override fun onViewRecycled(holder: SkeletonViewHolder) {
                super.onViewRecycled(holder)
                holder.itemView.clearAnimation()
            }
        }

        /**
         * ViewHolder для скелетона
         */
        class SkeletonViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView)
    }
