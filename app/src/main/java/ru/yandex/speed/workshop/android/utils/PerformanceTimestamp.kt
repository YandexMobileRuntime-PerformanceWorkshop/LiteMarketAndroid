package ru.yandex.speed.workshop.android.utils

import android.os.SystemClock
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Класс для точного измерения временных меток производительности
 * Аналог PerformanceTimestamp из iOS приложения
 */
class PerformanceTimestamp constructor(internal val nanoTime: Long) {
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

        // Отслеживание факта инициализации
        private var isAppStartInitialized = false

        /**
         * Инициализировать временную метку старта приложения
         * Должен вызываться как можно раньше в Application.onCreate()
         */
        fun initializeAppStart() {
            if (!isAppStartInitialized) {
                // Используем текущее время как базовое
                appStartTimestamp = now()

                // Учитываем время, прошедшее с момента запуска процесса
                try {
                    // Пробуем использовать Process.getStartElapsedRealtime() если доступно (API 24+)
                    val processClass = Class.forName("android.os.Process")
                    val getStartElapsedRealtimeMethod = processClass.getMethod("getStartElapsedRealtime")
                    val startElapsedRealtimeMs = getStartElapsedRealtimeMethod.invoke(null) as Long
                    val currentElapsedRealtimeMs = SystemClock.elapsedRealtime()
                    val processRunningTimeMs = currentElapsedRealtimeMs - startElapsedRealtimeMs

                    // Корректируем время старта на время, которое процесс уже работал
                    val adjustedStartNanos = appStartTimestamp!!.nanoTime - TimeUnit.MILLISECONDS.toNanos(processRunningTimeMs)
                    appStartTimestamp = PerformanceTimestamp(adjustedStartNanos)
                    Timber.d("Process.getStartElapsedRealtime() успешно использован. Скорректировано время запуска.")
                } catch (e: Exception) {
                    // Если метод недоступен, используем стандартное время
                    Timber.d("Process.getStartElapsedRealtime() недоступен, используем стандартное время запуска.")
                }

                Timber.tag("Performance").d("App start timestamp initialized: ${appStartTimestamp?.toMilliseconds()}ms")

                isAppStartInitialized = true
            }
        }

        /**
         * Получить временную метку старта процесса
         */
        fun processStartTime(): PerformanceTimestamp {
            // Используем сохраненную метку времени старта или создаем новую, если не инициализирована
            return appStartTimestamp ?: now().also {
                appStartTimestamp = it
                Timber.tag("Performance").w("Warning: processStartTime() вызван до initializeAppStart()")
            }
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
