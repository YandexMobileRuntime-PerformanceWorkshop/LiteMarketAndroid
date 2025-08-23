package ru.yandex.speed.workshop.android.utils

import android.view.Choreographer
import timber.log.Timber
import java.util.concurrent.TimeUnit

/**
 * Data class для результатов измерения производительности скролла
 * Аналог PerformanceData из iOS
 */
data class PerformanceData(
    val name: String,
    val hitchRatio: Double,
)

/**
 * Класс для измерения производительности скролла, включая подвисания (hitches)
 * Аналог ScrollPerformanceMeasure из iOS
 */
class ScrollPerformanceMeasure {
    private var isScrollActive = false
    private var startTime = 0L
    private var hitchTime = 0L
    private var lastFrameTimeNanos = 0L
    private var name: String = ""
    private var frameCallback: Choreographer.FrameCallback? = null
    private var frameCount = 0 // Счетчик кадров

    // Целевое время кадра в наносекундах (16.67мс для 60fps)
    private val targetFrameTimeNanos =
        TimeUnit.MILLISECONDS.toNanos(16) +
            TimeUnit.MICROSECONDS.toNanos(670)

    /**
     * Начать измерение производительности скролла
     * @param measureName Имя метрики
     */
    fun start(measureName: String) {
        if (isScrollActive) return

        name = measureName
        startTime = System.nanoTime()
        lastFrameTimeNanos = startTime
        hitchTime = 0L
        frameCount = 0
        isScrollActive = true

        // Создаем callback для регистрации в Choreographer
        frameCallback =
            object : Choreographer.FrameCallback {
                override fun doFrame(frameTimeNanos: Long) {
                    // Если уже остановлены, не обрабатываем
                    if (!isScrollActive) return

                    frameCount++

                    // Пропускаем первый кадр, так как для него нет предыдущего времени
                    if (lastFrameTimeNanos > 0) {
                        // Рассчитываем время, прошедшее с предыдущего кадра
                        val frameDelta = frameTimeNanos - lastFrameTimeNanos

                        // Если время между кадрами больше целевого, это считается "hitch"
                        if (frameDelta > targetFrameTimeNanos) {
                            // Добавляем только превышение над целевым временем
                            val hitchNanos = frameDelta - targetFrameTimeNanos
                            hitchTime += hitchNanos

                            // Логируем большие hitches для отладки
                            if (hitchNanos > TimeUnit.MILLISECONDS.toNanos(100)) {
                                Timber.tag("Performance").d("Large hitch detected: ${TimeUnit.NANOSECONDS.toMillis(hitchNanos)}ms")
                            }
                        }
                    }

                    // Сохраняем время этого кадра для следующей итерации
                    lastFrameTimeNanos = frameTimeNanos

                    // Регистрируем коллбек для следующего кадра
                    if (isScrollActive) {
                        Choreographer.getInstance().postFrameCallback(this)
                    }
                }
            }

        // Регистрируем коллбек в Choreographer
        Choreographer.getInstance().postFrameCallback(frameCallback!!)
    }

    /**
     * Остановить измерение производительности скролла
     * @return PerformanceData с результатами, или null если измерение слишком короткое
     */
    fun stop(): PerformanceData? {
        // Удаляем коллбек из Choreographer
        frameCallback?.let {
            try {
                Choreographer.getInstance().removeFrameCallback(it)
            } catch (e: Exception) {
                // Игнорируем ошибки удаления callback
            }
            frameCallback = null
        }

        // Если не активны, выходим
        if (!isScrollActive) return null

        // Рассчитываем общую продолжительность
        val duration = System.nanoTime() - startTime
        val durationSeconds = TimeUnit.NANOSECONDS.toSeconds(duration)

        isScrollActive = false

        // Если скролл слишком короткий, не учитываем его
        // iOS использует sufficientDuration = 1.0 секунда
        if (durationSeconds < SUFFICIENT_DURATION_SECONDS) {
            return null
        }

        // Добавляем логирование для отладки
        Timber.tag("Performance").d(
            "Scroll stats - duration: ${durationSeconds}s, hitchTime: ${TimeUnit.NANOSECONDS.toMillis(hitchTime)}ms, frames: $frameCount",
        )

        // Рассчитываем hitchRatio аналогично iOS: (hitchTime / duration) * 100
        // Округляем до 2 знаков после запятой для точности до сотых долей процента
        val hitchRatioValue = Math.round(((hitchTime.toDouble() / duration.toDouble()) * 100) * 100) / 100.0

        return PerformanceData(
            name = name,
            hitchRatio = hitchRatioValue,
        )
    }

    // Метод calculateHitchTime больше не используется
    // Расчеты перенесены непосредственно в doFrame

    companion object {
        // Минимальная длительность скролла для учета результатов (как в iOS)
        private const val SUFFICIENT_DURATION_SECONDS = 1L
    }
}
