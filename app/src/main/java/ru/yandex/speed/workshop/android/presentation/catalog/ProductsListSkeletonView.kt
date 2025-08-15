package ru.yandex.speed.workshop.android.presentation.catalog

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import ru.yandex.speed.workshop.android.R
import android.graphics.drawable.AnimationDrawable
import android.os.Handler
import android.os.Looper

/**
 * Скелетон для отображения списка товаров во время загрузки
 */
class ProductsListSkeletonView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : RecyclerView(context, attrs, defStyleAttr) {

    private val skeletonAdapter = SkeletonAdapter()
    private var animationDrawable: AnimationDrawable? = null

    init {
        layoutManager = GridLayoutManager(context, 2)
        adapter = skeletonAdapter
        setHasFixedSize(true)
    }

    fun showSkeletons(count: Int = 6) {
        skeletonAdapter.skeletonCount = count
        skeletonAdapter.notifyDataSetChanged()
        

        post {
            startAnimation()
        }
    }

    fun hideSkeletons() {
        stopAnimation()
    }

    private fun startAnimation() {

        val animation = android.view.animation.AnimationUtils.loadAnimation(context, R.anim.skeleton_pulse)
        
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is ViewGroup) {
                applyAnimationToAllViews(child, animation)
            } else {
                child?.startAnimation(animation)
            }
        }
    }
    
    private fun applyAnimationToAllViews(viewGroup: ViewGroup, animation: android.view.animation.Animation) {
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

    private fun stopAnimation() {

        this.clearAnimation()
        
        for (i in 0 until childCount) {
            val child = getChildAt(i)
            if (child is ViewGroup) {
                clearAnimationFromAllViews(child)
            } else {
                child?.clearAnimation()
            }
        }
    }
    
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

    override fun onDetachedFromWindow() {
        stopAnimation()
        super.onDetachedFromWindow()
    }

    private inner class SkeletonAdapter : RecyclerView.Adapter<SkeletonViewHolder>() {
        var skeletonCount: Int = 6
            set(value) {
                field = value
                notifyDataSetChanged()
            }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): SkeletonViewHolder {
            val view = LayoutInflater.from(parent.context)
                .inflate(R.layout.item_product_skeleton, parent, false)
            return SkeletonViewHolder(view)
        }

        override fun getItemCount(): Int = skeletonCount

        override fun onBindViewHolder(holder: SkeletonViewHolder, position: Int) {

        }

        override fun onViewRecycled(holder: SkeletonViewHolder) {
            super.onViewRecycled(holder)

            holder.itemView.clearAnimation()
        }
    }

    private class SkeletonViewHolder(itemView: android.view.View) : RecyclerView.ViewHolder(itemView)
}
