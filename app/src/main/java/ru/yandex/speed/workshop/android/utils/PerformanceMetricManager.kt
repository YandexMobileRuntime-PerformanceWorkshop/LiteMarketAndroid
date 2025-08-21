package ru.yandex.speed.workshop.android.utils

import timber.log.Timber
import java.util.concurrent.CopyOnWriteArrayList
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Модель данных метрики производительности
 */
data class PerformanceMetric(
    val name: String,
    val value: Long, // в миллисекундах
    val context: Map<String, Any> = emptyMap(),
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Менеджер для записи и анализа метрик производительности
 * Аналог PerformanceMetricManager из iOS приложения
 */
@Singleton
class PerformanceMetricManager @Inject constructor() {
    
    private val metrics = CopyOnWriteArrayList<PerformanceMetric>()
    
    /**
     * Записать метрику производительности
     * @param name Название метрики
     * @param valueMs Значение в миллисекундах
     * @param context Дополнительный контекст
     */
    fun recordMetric(name: String, valueMs: Long, context: Map<String, Any> = emptyMap()) {
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
            
            Timber.tag("Performance").i("$name: ${metricsForName.size} samples, " +
                    "avg=%.3fms, min=${min}ms, max=${max}ms", average)
            
            // Группировка по контексту
            val contexts = metricsForName.groupBy { it.context["context"]?.toString() ?: "unknown" }
            for ((context, contextMetrics) in contexts) {
                val contextAvg = contextMetrics.map { it.value }.average()
                Timber.tag("Performance").i("  - $context: ${contextMetrics.size} samples, " +
                        "avg=%.3fms", contextAvg)
            }
        }
        Timber.tag("Performance").i("=====================================")
    }
}
