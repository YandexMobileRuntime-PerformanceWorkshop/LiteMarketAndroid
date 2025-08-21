package ru.yandex.speed.workshop.android.utils

import android.os.SystemClock
import java.util.concurrent.TimeUnit

/**
 * Класс для точного измерения временных меток производительности
 * Аналог PerformanceTimestamp из iOS приложения
 */
class PerformanceTimestamp constructor(private val nanoTime: Long) {
    companion object {
        /**
         * Получить текущую временную метку
         */
        fun now(): PerformanceTimestamp {
            return PerformanceTimestamp(SystemClock.elapsedRealtimeNanos())
        }
        
        /**
         * Создать временную метку из сохраненного значения в миллисекундах
         */
        fun fromMilliseconds(millis: Long): PerformanceTimestamp {
            // Преобразуем обратно в наносекунды для внутреннего представления
            return PerformanceTimestamp(TimeUnit.MILLISECONDS.toNanos(millis))
        }

        // Статическая переменная для хранения метки времени старта приложения
        private var appStartTimestamp: PerformanceTimestamp? = null
        
        /**
         * Инициализировать временную метку старта приложения
         * Должен вызываться как можно раньше в Application.onCreate()
         */
        fun initializeAppStart() {
            if (appStartTimestamp == null) {
                appStartTimestamp = now()
            }
        }
        
        /**
         * Получить временную метку старта процесса
         */
        fun processStartTime(): PerformanceTimestamp {
            // Используем сохраненную метку времени старта или создаем новую, если не инициализирована
            return appStartTimestamp ?: now().also { appStartTimestamp = it }
        }
    }

    /**
     * Рассчитать время, прошедшее с другой временной метки
     * @param timestamp Начальная временная метка
     * @return Время в наносекундах
     */
    fun elapsedSince(timestamp: PerformanceTimestamp): Long {
        return nanoTime - timestamp.nanoTime
    }

    /**
     * Конвертировать в миллисекунды
     */
    fun toMilliseconds(): Long {
        return TimeUnit.NANOSECONDS.toMillis(nanoTime)
    }
    
    /**
     * Получить временную метку в формате Unix time (секунды с 1970)
     * Аналог toSince1970() из iOS
     */
    fun toSince1970(): Double {
        val currentTimeMillis = System.currentTimeMillis()
        val elapsedDelta = now().elapsedSince(this)
        val elapsedDeltaMillis = TimeUnit.NANOSECONDS.toMillis(elapsedDelta)
        return (currentTimeMillis - elapsedDeltaMillis) / 1000.0
    }
}
