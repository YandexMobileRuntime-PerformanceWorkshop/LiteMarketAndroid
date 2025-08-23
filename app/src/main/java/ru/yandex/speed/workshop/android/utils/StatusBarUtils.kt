package ru.yandex.speed.workshop.android.utils

import android.content.Context
import android.content.res.Resources
import android.view.View
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

/**
 * Utility for handling status bar height and safe area padding
 */
object StatusBarUtils {
    
    /**
     * Get the status bar height in pixels
     */
    fun getStatusBarHeight(context: Context): Int {
        var result = 0
        val resourceId = context.resources.getIdentifier("status_bar_height", "dimen", "android")
        if (resourceId > 0) {
            result = context.resources.getDimensionPixelSize(resourceId)
        }
        return result
    }
    
    /**
     * Add status bar height as top padding to a view
     */
    fun addStatusBarPadding(view: View) {
        val statusBarHeight = getStatusBarHeight(view.context)
        view.setPadding(
            view.paddingLeft,
            view.paddingTop + statusBarHeight,
            view.paddingRight,
            view.paddingBottom
        )
    }
    
    /**
     * Add status bar height as top margin to a view
     */
    fun addStatusBarMargin(view: View) {
        val statusBarHeight = getStatusBarHeight(view.context)
        val layoutParams = view.layoutParams as? android.view.ViewGroup.MarginLayoutParams
        layoutParams?.let {
            it.topMargin = it.topMargin + statusBarHeight
            view.layoutParams = it
        }
    }
    
    /**
     * Apply status bar insets using modern WindowInsets API (preferred method)
     */
    fun applyStatusBarInsets(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            v.setPadding(
                v.paddingLeft,
                insets.top,
                v.paddingRight,
                v.paddingBottom
            )
            windowInsets
        }
    }
    
    /**
     * Apply status bar insets as margin using modern WindowInsets API
     */
    fun applyStatusBarInsetsAsMargin(view: View) {
        ViewCompat.setOnApplyWindowInsetsListener(view) { v, windowInsets ->
            val insets = windowInsets.getInsets(WindowInsetsCompat.Type.statusBars())
            val layoutParams = v.layoutParams as? android.view.ViewGroup.MarginLayoutParams
            layoutParams?.let {
                it.topMargin = insets.top
                v.layoutParams = it
            }
            windowInsets
        }
    }
}
