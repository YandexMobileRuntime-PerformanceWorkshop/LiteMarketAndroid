package ru.yandex.speed.workshop.android.utils

import java.text.NumberFormat
import java.util.Locale

/**
 * Утилиты для работы с числами с учетом локализации
 */
object NumberUtils {
    /**
     * Форматирует число с плавающей точкой в строку с учетом локали
     * Всегда использует точку в качестве разделителя десятичной части
     *
     * @param value Число для форматирования
     * @param fractionDigits Количество знаков после запятой
     * @return Отформатированная строка
     */
    fun formatDouble(value: Double, fractionDigits: Int = 1): String {
        val formatter = NumberFormat.getInstance(Locale.US).apply {
            minimumFractionDigits = fractionDigits
            maximumFractionDigits = fractionDigits
        }
        return formatter.format(value)
    }

    /**
     * Безопасно преобразует строку в число с плавающей точкой
     * Поддерживает как точку, так и запятую в качестве разделителя
     *
     * @param value Строка для преобразования
     * @param defaultValue Значение по умолчанию в случае ошибки
     * @return Число с плавающей точкой
     */
    fun parseDouble(value: String?, defaultValue: Double = 0.0): Double {
        if (value.isNullOrBlank()) return defaultValue
        
        return try {
            // Пробуем преобразовать напрямую
            value.toDouble()
        } catch (e: NumberFormatException) {
            try {
                // Если не получилось, заменяем запятую на точку и пробуем снова
                value.replace(',', '.').toDouble()
            } catch (e: NumberFormatException) {
                defaultValue
            }
        }
    }

    /**
     * Безопасно преобразует строку в целое число
     *
     * @param value Строка для преобразования
     * @param defaultValue Значение по умолчанию в случае ошибки
     * @return Целое число
     */
    fun parseInt(value: String?, defaultValue: Int = 0): Int {
        if (value.isNullOrBlank()) return defaultValue
        
        return try {
            value.toInt()
        } catch (e: NumberFormatException) {
            defaultValue
        }
    }
}
