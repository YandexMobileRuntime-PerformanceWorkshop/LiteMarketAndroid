package ru.yandex.speed.workshop.android.utils

import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject

/**
 * Определяет точку отсчета для измерения времени
 */
enum class LCPTrackingTime {
    FROM_APP_START,
    FROM_SCREEN_CREATION,
}

/**
 * Класс для отслеживания аналитики экранов, включая LCP (Largest Contentful Paint)
 * Аналог MVIScreenAnalytics из iOS приложения
 */
class MVIScreenAnalytics
    @Inject
    constructor(
        private val performanceMetricManager: PerformanceMetricManager,
    ) {
        private lateinit var creationTime: PerformanceTimestamp

        /**
         * Инициализировать отслеживание с указанным типом измерения времени
         * @param trackingTimeType Тип точки отсчета
         * @param creationTimestamp Временная метка создания экрана (если применимо)
         */
        fun initialize(
            trackingTimeType: LCPTrackingTime,
            creationTimestamp: PerformanceTimestamp? = null,
        ) {
            creationTime =
                when (trackingTimeType) {
                    LCPTrackingTime.FROM_APP_START -> PerformanceTimestamp.processStartTime()
                    LCPTrackingTime.FROM_SCREEN_CREATION -> creationTimestamp ?: PerformanceTimestamp.now()
                }
        }

        /**
         * Логировать метрику LCP (Largest Contentful Paint) для экрана
         * @param screen Имя экрана
         */
        fun logLCP(screen: String) {
            if (!::creationTime.isInitialized) {
                Timber.tag("Performance").w("LCP logging attempt without initialization for $screen")
                return
            }

            val finishTime = PerformanceTimestamp.now()
            val lcpNanos = finishTime.elapsedSince(creationTime)
            val lcpMs = TimeUnit.NANOSECONDS.toMillis(lcpNanos)

            // Добавляем диагностику
            val currentTimeMs = System.currentTimeMillis()

            // Логируем напрямую в лог с дополнительной информацией
            Timber.tag("Performance").i("$screen: LCP = ${lcpMs}ms (measured at $currentTimeMs)")

            // Записываем метрику через менеджер метрик
            performanceMetricManager.recordMetric(
                name = "LCP",
                valueMs = lcpMs,
                context =
                    mapOf(
                        "screen" to screen,
                        "measured_at" to currentTimeMs.toString(),
                    ),
            )

            // Проверяем валидность значения
            if (lcpMs > 60000) { // если больше минуты
                Timber.tag("Performance").w("Warning: Unusually large LCP value for $screen: ${lcpMs}ms")
            }
        }
    }
