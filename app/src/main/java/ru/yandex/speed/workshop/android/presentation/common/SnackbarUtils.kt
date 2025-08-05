package ru.yandex.speed.workshop.android.presentation.common

import android.view.View
import androidx.core.content.ContextCompat
import com.google.android.material.snackbar.Snackbar
import ru.yandex.speed.workshop.android.R

/**
 * Утилитный класс для создания стилизованных Snackbar
 */
object SnackbarUtils {
    
    /**
     * Показать success Snackbar (зеленый) для положительных действий
     */
    fun showSuccess(view: View, message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        val snackbar = Snackbar.make(view, message, duration)
        snackbar.setBackgroundTint(ContextCompat.getColor(view.context, android.R.color.holo_green_dark))
        snackbar.setTextColor(ContextCompat.getColor(view.context, android.R.color.white))
        snackbar.show()
    }
    
    /**
     * Показать info Snackbar (синий) для информационных сообщений
     */
    fun showInfo(view: View, message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        val snackbar = Snackbar.make(view, message, duration)
        snackbar.setBackgroundTint(ContextCompat.getColor(view.context, android.R.color.holo_blue_dark))
        snackbar.setTextColor(ContextCompat.getColor(view.context, android.R.color.white))
        snackbar.show()
    }
    
    /**
     * Показать warning Snackbar (оранжевый) для предупреждений
     */
    fun showWarning(view: View, message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        val snackbar = Snackbar.make(view, message, duration)
        snackbar.setBackgroundTint(ContextCompat.getColor(view.context, android.R.color.holo_orange_dark))
        snackbar.setTextColor(ContextCompat.getColor(view.context, android.R.color.white))
        snackbar.show()
    }
    
    /**
     * Показать favorite Snackbar с undo действием
     */
    fun showFavoriteAction(
        view: View, 
        message: String, 
        isAdded: Boolean,
        undoAction: () -> Unit = {},
        duration: Int = Snackbar.LENGTH_LONG
    ) {
        val snackbar = Snackbar.make(view, message, duration)
        
        // Цвет в зависимости от действия
        val backgroundColor = if (isAdded) {
            android.R.color.holo_green_dark // Зеленый для избранного
        } else {
            android.R.color.darker_gray // Серый для удаления
        }
        
        snackbar.setBackgroundTint(ContextCompat.getColor(view.context, backgroundColor))
        snackbar.setTextColor(ContextCompat.getColor(view.context, android.R.color.white))
        
        // Добавляем undo действие
        snackbar.setAction("ОТМЕНИТЬ") {
            undoAction()
        }
        snackbar.setActionTextColor(ContextCompat.getColor(view.context, android.R.color.white))
        
        snackbar.show()
    }
    
    /**
     * Показать copied Snackbar (зеленый) для копирования
     */
    fun showCopied(view: View, message: String = "Скопировано", duration: Int = Snackbar.LENGTH_SHORT) {
        val snackbar = Snackbar.make(view, message, duration)
        snackbar.setBackgroundTint(ContextCompat.getColor(view.context, android.R.color.holo_green_dark))
        snackbar.setTextColor(ContextCompat.getColor(view.context, android.R.color.white))
        
        // Добавляем иконку или эмодзи
        snackbar.setText("$message ✓")
        snackbar.show()
    }
    
    /**
     * Показать placeholder Snackbar (серый) для функций в разработке
     */
    fun showPlaceholder(view: View, message: String, duration: Int = Snackbar.LENGTH_SHORT) {
        val snackbar = Snackbar.make(view, message, duration)
        snackbar.setBackgroundTint(ContextCompat.getColor(view.context, android.R.color.darker_gray))
        snackbar.setTextColor(ContextCompat.getColor(view.context, android.R.color.white))
        snackbar.setText("🚧 $message")
        snackbar.show()
    }
} 