package ru.yandex.speed.workshop.android.utils

import android.app.Activity
import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.view.ViewTreeObserver
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleObserver
import androidx.lifecycle.OnLifecycleEvent
import androidx.lifecycle.ProcessLifecycleOwner
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Фазы запуска приложения для отслеживания
 */
enum class StartupPhase {
    PRE_ONCREATE,        // Фаза до вызова onCreate в Application
    ONCREATE_COMPLETE,   // Фаза завершения onCreate в Application
    FIRST_ACTIVITY_DRAW, // Фаза первой отрисовки Activity
    FIRST_DATA_LOADED,   // Фаза загрузки первых данных
    STARTUP_COMPLETE     // Фаза полного завершения запуска
}

/**
 * Тип запуска приложения
 */
enum class StartupType {
    COLD_START,  // Холодный старт (первый запуск процесса)
    WARM_START,  // Теплый старт (возврат из фона)
    HOT_START    // Горячий старт (переключение между активити)
}

/**
 * Класс для отслеживания времени старта приложения по фазам
 */
@Singleton
class ApplicationStartupTracker @Inject constructor(
    private val performanceMetricManager: PerformanceMetricManager
) : LifecycleObserver, Application.ActivityLifecycleCallbacks {

    // Хранение временных меток для каждой фазы
    private val phaseTimestamps = mutableMapOf<StartupPhase, PerformanceTimestamp>()
    
    // Хранение типа текущего запуска
    private var currentStartupType: StartupType = StartupType.COLD_START
    
    // Флаги для отслеживания состояния
    private var isFirstActivityCreated = false
    private var isFirstActivityDrawn = false
    private var isFirstDataLoaded = false
    private var isStartupComplete = false
    
    // Время последнего перехода в фоновый режим
    private var lastBackgroundTimestamp: Long = 0
    
    // Константы
    companion object {
        private const val PREF_NAME = "application_startup_tracker"
        private const val PREF_LAST_RUN_TIMESTAMP = "last_run_timestamp"
        private const val WARM_START_THRESHOLD_MS = 30 * 60 * 1000 // 30 минут
    }
    
    // SharedPreferences для хранения информации между запусками
    private lateinit var preferences: SharedPreferences
    
    /**
     * Инициализация трекера при создании приложения
     */
    fun initialize(application: Application) {
        // Инициализируем SharedPreferences
        preferences = application.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
        
        // Определяем тип запуска
        determineStartupType()
        
        // Регистрируем колбэки жизненного цикла
        application.registerActivityLifecycleCallbacks(this)
        ProcessLifecycleOwner.get().lifecycle.addObserver(this)
        
        // Записываем временную метку для PRE_ONCREATE
        // Используем processStartTime() для получения времени старта процесса
        recordPhaseTimestamp(StartupPhase.PRE_ONCREATE, PerformanceTimestamp.processStartTime())
        
        Timber.tag("Performance").d("ApplicationStartupTracker initialized with startup type: $currentStartupType")
    }
    
    /**
     * Определение типа запуска приложения
     */
    private fun determineStartupType() {
        val currentTime = System.currentTimeMillis()
        val lastRunTime = preferences.getLong(PREF_LAST_RUN_TIMESTAMP, 0)
        
        currentStartupType = when {
            // Если нет записи о предыдущем запуске или прошло много времени - холодный старт
            lastRunTime == 0L || (currentTime - lastRunTime > WARM_START_THRESHOLD_MS) -> StartupType.COLD_START
            
            // Если приложение было в фоне и вернулось - теплый старт
            lastBackgroundTimestamp > 0 && (currentTime - lastBackgroundTimestamp < WARM_START_THRESHOLD_MS) -> StartupType.WARM_START
            
            // В остальных случаях - горячий старт
            else -> StartupType.HOT_START
        }
        
        // Обновляем время последнего запуска
        preferences.edit().putLong(PREF_LAST_RUN_TIMESTAMP, currentTime).apply()
    }
    
    /**
     * Запись временной метки для указанной фазы
     */
    fun recordPhaseTimestamp(phase: StartupPhase, timestamp: PerformanceTimestamp = PerformanceTimestamp.now()) {
        phaseTimestamps[phase] = timestamp
        Timber.tag("Performance").d("Recorded phase timestamp: $phase at ${timestamp.toMilliseconds()}ms")
        
        // Если это завершающая фаза, логируем все метрики
        if (phase == StartupPhase.STARTUP_COMPLETE && !isStartupComplete) {
            logStartupMetrics()
            isStartupComplete = true
        }
    }
    
    /**
     * Запись фазы завершения onCreate в Application
     */
    fun onApplicationCreated() {
        recordPhaseTimestamp(StartupPhase.ONCREATE_COMPLETE)
    }
    
    /**
     * Запись фазы загрузки первых данных
     */
    fun onFirstDataLoaded() {
        if (!isFirstDataLoaded) {
            isFirstDataLoaded = true
            recordPhaseTimestamp(StartupPhase.FIRST_DATA_LOADED)
            
            // Проверяем, все ли фазы завершены для логирования полного старта
            checkStartupCompletion()
        }
    }
    
    /**
     * Проверка завершения всех фаз запуска
     */
    private fun checkStartupCompletion() {
        if (isFirstActivityDrawn && isFirstDataLoaded && !isStartupComplete) {
            recordPhaseTimestamp(StartupPhase.STARTUP_COMPLETE)
        }
    }
    
    /**
     * Логирование метрик запуска приложения
     */
    private fun logStartupMetrics() {
        val preOnCreateTime = phaseTimestamps[StartupPhase.PRE_ONCREATE] ?: return
        val onCreateCompleteTime = phaseTimestamps[StartupPhase.ONCREATE_COMPLETE]
        val firstActivityDrawTime = phaseTimestamps[StartupPhase.FIRST_ACTIVITY_DRAW]
        val firstDataLoadedTime = phaseTimestamps[StartupPhase.FIRST_DATA_LOADED]
        val startupCompleteTime = phaseTimestamps[StartupPhase.STARTUP_COMPLETE] ?: return
        
        // Базовый контекст для всех метрик
        val baseContext = mapOf(
            "startup_type" to currentStartupType.name
        )
        
        // Общее время запуска
        val totalStartupTimeMs = TimeUnit.NANOSECONDS.toMillis(startupCompleteTime.elapsedSince(preOnCreateTime))
        performanceMetricManager.recordMetric(
            name = "TotalStartupTime",
            valueMs = totalStartupTimeMs,
            context = baseContext + mapOf(
                "pre_oncreate_ms" to preOnCreateTime.toMilliseconds(),
                "startup_complete_ms" to startupCompleteTime.toMilliseconds()
            )
        )
        
        // Время от запуска до onCreate
        onCreateCompleteTime?.let {
            val onCreateTimeMs = TimeUnit.NANOSECONDS.toMillis(it.elapsedSince(preOnCreateTime))
            performanceMetricManager.recordMetric(
                name = "ApplicationOnCreateTime",
                valueMs = onCreateTimeMs,
                context = baseContext
            )
        }
        
        // Время до первой отрисовки
        firstActivityDrawTime?.let {
            val firstDrawTimeMs = TimeUnit.NANOSECONDS.toMillis(it.elapsedSince(preOnCreateTime))
            performanceMetricManager.recordMetric(
                name = "FirstActivityDrawTime",
                valueMs = firstDrawTimeMs,
                context = baseContext
            )
        }
        
        // Время до загрузки данных
        firstDataLoadedTime?.let {
            val dataLoadTimeMs = TimeUnit.NANOSECONDS.toMillis(it.elapsedSince(preOnCreateTime))
            performanceMetricManager.recordMetric(
                name = "FirstDataLoadedTime",
                valueMs = dataLoadTimeMs,
                context = baseContext
            )
        }
        
        // Выводим сводку в лог
        Timber.tag("Performance").i("=== APP STARTUP METRICS ===")
        Timber.tag("Performance").i("Startup Type: $currentStartupType")
        Timber.tag("Performance").i("Total Startup Time: ${totalStartupTimeMs}ms")
        onCreateCompleteTime?.let {
            val time = TimeUnit.NANOSECONDS.toMillis(it.elapsedSince(preOnCreateTime))
            Timber.tag("Performance").i("Application OnCreate Time: ${time}ms")
        }
        firstActivityDrawTime?.let {
            val time = TimeUnit.NANOSECONDS.toMillis(it.elapsedSince(preOnCreateTime))
            Timber.tag("Performance").i("First Activity Draw Time: ${time}ms")
        }
        firstDataLoadedTime?.let {
            val time = TimeUnit.NANOSECONDS.toMillis(it.elapsedSince(preOnCreateTime))
            Timber.tag("Performance").i("First Data Loaded Time: ${time}ms")
        }
        Timber.tag("Performance").i("==============================")
    }
    
    /**
     * Настройка отслеживания первой отрисовки Activity
     */
    private fun setupActivityDrawTracking(activity: Activity) {
        if (!isFirstActivityDrawn) {
            val rootView = activity.window.decorView
            rootView.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
                override fun onPreDraw(): Boolean {
                    if (!isFirstActivityDrawn) {
                        // Небольшая задержка, чтобы убедиться, что отрисовка действительно произошла
                        Handler(Looper.getMainLooper()).postDelayed({
                            isFirstActivityDrawn = true
                            recordPhaseTimestamp(StartupPhase.FIRST_ACTIVITY_DRAW)
                            
                            // Проверяем, все ли фазы завершены
                            checkStartupCompletion()
                        }, 100)
                    }
                    
                    // Удаляем слушатель после первого вызова
                    rootView.viewTreeObserver.removeOnPreDrawListener(this)
                    return true
                }
            })
        }
    }
    
    // Методы ActivityLifecycleCallbacks
    
    override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
        if (!isFirstActivityCreated) {
            isFirstActivityCreated = true
            setupActivityDrawTracking(activity)
        }
    }
    
    override fun onActivityStarted(activity: Activity) {}
    
    override fun onActivityResumed(activity: Activity) {}
    
    override fun onActivityPaused(activity: Activity) {}
    
    override fun onActivityStopped(activity: Activity) {}
    
    override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) {}
    
    override fun onActivityDestroyed(activity: Activity) {}
    
    // Методы LifecycleObserver
    
    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    fun onAppForeground() {
        // Приложение вернулось на передний план
        if (lastBackgroundTimestamp > 0) {
            val timeInBackgroundMs = System.currentTimeMillis() - lastBackgroundTimestamp
            Timber.tag("Performance").d("App returned to foreground after ${timeInBackgroundMs}ms in background")
        }
    }
    
    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    fun onAppBackground() {
        // Приложение ушло в фон
        lastBackgroundTimestamp = System.currentTimeMillis()
        Timber.tag("Performance").d("App went to background at $lastBackgroundTimestamp")
    }
}
