package ru.yandex.speed.workshop.android.utils

import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Модель данных метрики производительности
 */
data class PerformanceMetric(
    val name: String,
    // Значение в миллисекундах
    val value: Long,
    val context: Map<String, Any> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis(),
)

/**
 * Менеджер для записи и анализа метрик производительности
 * Аналог PerformanceMetricManager из iOS приложения
 */
@Singleton
class PerformanceMetricManager
    @Inject
    constructor() {
        private val metrics = CopyOnWriteArrayList<PerformanceMetric>()
        private var measure: ScrollPerformanceMeasure? = null

        /**
         * Начать измерение скролла
         * @param measureName Название измерения
         */
        fun start(measureName: String) {
            if (measureName.isEmpty() || measure != null) {
                return
            }

            val newMeasure = ScrollPerformanceMeasure()
            newMeasure.start(measureName)
            measure = newMeasure
            Timber.d("Started scroll performance measure: $measureName")
        }

        /**
         * Остановить текущее измерение скролла
         * @param name Название измерения
         * @param responseInfo Дополнительная информация
         */
        fun stop(
            name: String,
            // Дополнительная информация
            responseInfo: Map<String, String> = emptyMap(),
        ) {
            if (name.isEmpty() || measure == null) {
                measure = null
                return
            }

            val result = measure?.stop()
            measure = null

            if (result != null) {
                // Форматируем вывод до двух знаков после запятой
                val formattedHitchRatio = String.format("%.2f", result.hitchRatio)
                Timber.tag("Performance").i("${result.name}: hitchRatio = $formattedHitchRatio% (>1s scroll)")
            }
        }

        /**
         * Записать метрику производительности
         * @param name Название метрики
         * @param valueMs Значение в миллисекундах
         * @param context Дополнительный контекст
         */
        fun recordMetric(
            name: String,
            valueMs: Long,
            context: Map<String, Any> = emptyMap(),
        ) {
            val metric = PerformanceMetric(name, valueMs, context)
            metrics.add(metric)

            // Логируем метрику
            val contextStr = context.entries.joinToString(", ") { "${it.key}=${it.value}" }
            Timber.tag("Performance").i("$name: ${valueMs}ms [$contextStr]")
        }

        /**
         * Получить метрики с указанным именем
         */
        fun getMetrics(name: String): List<PerformanceMetric> {
            return metrics.filter { it.name == name }
        }

        /**
         * Получить все записанные метрики
         */
        fun getAllMetrics(): List<PerformanceMetric> {
            return metrics.toList()
        }

        /**
         * Очистить все метрики
         */
        fun clearMetrics() {
            metrics.clear()
        }

        /**
         * Логирует время старта приложения.
         * Измеряет время от инициализации приложения до момента, когда UI становится интерактивным.
         *
         * @param uiReadyTime временная метка, когда UI стал интерактивным
         * @param additionalContext дополнительный контекст для логирования
         */
        fun logAppStartTime(
            uiReadyTime: PerformanceTimestamp,
            additionalContext: Map<String, Any> = emptyMap(),
        ) {
            val appStartTime = PerformanceTimestamp.processStartTime()
            val startTimeMs = TimeUnit.NANOSECONDS.toMillis(uiReadyTime.elapsedSince(appStartTime))

            val context = HashMap<String, Any>(additionalContext)
            context["ui_ready_time"] = uiReadyTime.toMilliseconds()
            context["app_start_time"] = appStartTime.toMilliseconds()
            context["measured_at"] = System.currentTimeMillis()

            recordMetric(
                name = "AppStartTime",
                valueMs = startTimeMs,
                context = context,
            )

            Timber.tag("Performance").i("App Start Time: ${startTimeMs}ms")
        }

        /**
         * Вывести сводку всех записанных метрик
         */
        fun printMetricsSummary() {
            Timber.tag("Performance").i("=== PERFORMANCE METRICS SUMMARY ===")
            Timber.tag("Performance").i("Total metrics recorded: ${metrics.size}")

            // Группировка метрик по имени
            val groupedMetrics = metrics.groupBy { it.name }

            for ((name, metricsForName) in groupedMetrics) {
                val values = metricsForName.map { it.value }
                val average = values.average()
                val min = values.minOrNull() ?: 0
                val max = values.maxOrNull() ?: 0

                Timber.tag("Performance").i(
                    "$name: ${metricsForName.size} samples, " +
                        "avg=%.3fms, min=${min}ms, max=${max}ms",
                    average,
                )

                // Группировка по контексту
                val contexts = metricsForName.groupBy { it.context["context"]?.toString() ?: "unknown" }
                for ((context, contextMetrics) in contexts) {
                    val contextAvg = contextMetrics.map { it.value }.average()
                    Timber.tag("Performance").i(
                        "  - $context: ${contextMetrics.size} samples, " +
                            "avg=%.3fms",
                        contextAvg,
                    )
                }
            }
            Timber.tag("Performance").i("=====================================")
        }
    }
