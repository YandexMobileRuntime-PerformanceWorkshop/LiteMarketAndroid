# План рефакторинга кода SpeedWorkshopAndroid

## 1. Выявленные проблемы и "костыли" в коде

### 1.3. Проблемы с сетевыми запросами и обработкой ошибок

#### 1.3.2. Неоптимальное использование Retrofit

**Проблема**: Вместо использования корутин с Retrofit напрямую, используется блокирующий вызов `.execute()`:

```kotlin
val response = api.getProductsList(page, perPage).execute()
```

**Последствия**: Потенциальные проблемы с производительностью и блокировкой потоков.

## 2. План рефакторинга

### 2.3. Улучшение сетевого слоя

#### 2.3.1. Переход на корутины в Retrofit

1. Изменить интерфейс API для использования корутин:

```kotlin
interface ProductApi {
    @GET("api/products")
    suspend fun getProductsList(
        @Query("page") page: Int,
        @Query("per_page") perPage: Int
    ): Response<ProductListResponse>
    
    // ...
}
```

2. Упростить код в сервисах, используя созданный ранее safeApiCall:

```kotlin
suspend fun getProductsList(page: Int = 1, perPage: Int = 20): Result<ProductListResponse> {
    return safeApiCall { api.getProductsList(page, perPage) }
}
```

### 2.4. Улучшение кэширования

#### 2.4.1. Оптимизация существующего механизма кэширования

1. Добавить политику устаревания кэша:

```kotlin
class CachePolicy(
    val maxAge: Long = TimeUnit.MINUTES.toMillis(5),
    val forceRefresh: Boolean = false
)

// Использование в репозитории
suspend fun getProductDetail(id: String, cachePolicy: CachePolicy = CachePolicy()): Result<ProductDetail> {
    // Проверяем кэш и его актуальность
    val cachedProduct = productCache.get(id)
    val cacheTimestamp = cacheTimestamps[id] ?: 0L
    val isCacheValid = System.currentTimeMillis() - cacheTimestamp < cachePolicy.maxAge
    
    if (!cachePolicy.forceRefresh && cachedProduct != null && isCacheValid) {
        return Result.Success(cachedProduct)
    }
    
    // Загружаем свежие данные с API
    // ...
}
```

2. Улучшить управление памятью для кэша:

```kotlin
// Настройка размеров кэша в зависимости от доступной памяти
private fun calculateCacheSize(): Int {
    val maxMemory = (Runtime.getRuntime().maxMemory() / 1024).toInt()
    return maxMemory / 8  // Используем 1/8 доступной памяти
}

private val productCache = LruCache<String, Product>(calculateCacheSize())
```

3. Добавить возможность предварительной загрузки данных:

```kotlin
suspend fun prefetchProducts(category: String) {
    // Загружаем данные заранее и помещаем в кэш
    val result = safeApiCall { api.getProductsByCategory(category, 1, 20) }
    if (result is Result.Success) {
        // Кэшируем результаты
        result.data.products.forEach { productDto ->
            val product = productDto.toDomain()
            productCache.put(product.id, product)
        }
    }
}
```

## 3. Приоритеты рефакторинга

1. **Текущий приоритет**:
   - Переход на корутины в Retrofit

2. **Будущие задачи**:
   - Оптимизация существующего механизма кэширования
   - Дополнительная оптимизация производительности

## 4. Оценка времени и ресурсов

1. **Улучшение сетевого слоя**: ✅ Унификация обработки ошибок выполнена, осталось внедрение корутин в Retrofit (~1 день)
2. **Оптимизация кэширования**: 1-2 дня

**Общая оценка**: 2-3 дня работы одного разработчика

## 5. Риски и их митигация

1. **Риск**: Регрессии в функциональности после рефакторинга
   - **Митигация**: Написание автоматических тестов до начала рефакторинга

2. **Риск**: Увеличение потребления памяти при неправильной настройке кэша
   - **Митигация**: Тщательное тестирование на устройствах с разным объемом памяти

3. **Риск**: Ухудшение производительности из-за изменений в архитектуре
   - **Митигация**: Профилирование приложения до и после рефакторинга для сравнения производительности