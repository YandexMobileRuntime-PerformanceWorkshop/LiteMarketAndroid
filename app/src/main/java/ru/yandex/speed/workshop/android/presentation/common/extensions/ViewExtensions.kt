package ru.yandex.speed.workshop.android.presentation.common.extensions

import android.graphics.Paint
import android.view.View
import android.widget.TextView

/**
 * Набор расширений для упрощения работы с UI
 */

/**
 * Устанавливает текст в TextView только если он отличается от текущего
 * Помогает избежать мерцания при частых обновлениях с одинаковым текстом
 *
 * @param newText Новый текст для установки
 */
fun TextView.setTextIfChanged(newText: String?) {
    val current = this.text?.toString()
    if (current != newText) {
        this.text = newText
    }
}

/**
 * Устанавливает видимость View в зависимости от переданного условия
 *
 * @param condition Условие, при котором View должен быть видимым
 * @param invisibilityState Состояние невидимости (View.GONE или View.INVISIBLE)
 */
fun View.setVisibleIf(
    condition: Boolean,
    invisibilityState: Int = View.GONE,
) {
    visibility = if (condition) View.VISIBLE else invisibilityState
}

/**
 * Устанавливает перечеркнутый стиль текста
 */
fun TextView.setStrikeThrough() {
    paintFlags = paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
}

/**
 * Устанавливает перечеркнутый стиль текста и видимость в зависимости от условия
 *
 * @param text Текст для отображения
 * @param isVisible Условие для отображения
 */
fun TextView.setStrikeThroughText(
    text: String?,
    isVisible: Boolean,
) {
    if (isVisible && !text.isNullOrEmpty()) {
        setTextIfChanged(text)
        setStrikeThrough()
        setVisibleIf(true)
    } else {
        setVisibleIf(false)
    }
}

/**
 * Проверяет, является ли строка null, пустой или состоящей только из пробелов
 */
fun String?.isNullOrEmptyOrBlank(): Boolean {
    return this.isNullOrEmpty() || this.isBlank()
}
