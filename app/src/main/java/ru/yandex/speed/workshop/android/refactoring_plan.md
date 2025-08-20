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

### 2.4. Дополнительные улучшения производительности

#### 2.4.1. Оптимизация загрузки изображений

1. Настройка Glide для эффективной загрузки изображений:

```kotlin
val requestOptions = RequestOptions()
    .diskCacheStrategy(DiskCacheStrategy.ALL)
    .priority(Priority.HIGH)
    .dontTransform()

Glide.with(context)
    .load(imageUrl)
    .apply(requestOptions)
    .into(imageView)
```

2. Предварительная загрузка изображений для улучшения пользовательского опыта:

```kotlin
fun preloadImages(urls: List<String>) {
    urls.forEach { url ->
        Glide.with(context)
            .load(url)
            .preload()
    }
}
```

#### 2.4.2. Оптимизация RecyclerView

1. Использование DiffUtil для эффективного обновления списков:

```kotlin
class ProductDiffCallback(
    private val oldList: List<Product>,
    private val newList: List<Product>
) : DiffUtil.Callback() {
    // Реализация методов сравнения
}

// Использование
val diffResult = DiffUtil.calculateDiff(ProductDiffCallback(oldProducts, newProducts))
diffResult.dispatchUpdatesTo(adapter)
```

2. Оптимизация привязки данных в ViewHolder:

```kotlin
fun bind(product: Product) {
    // Проверяем, нужно ли обновлять данные
    if (currentProductId != product.id) {
        currentProductId = product.id
        titleView.text = product.title
        priceView.text = product.currentPrice
        // Остальные привязки
    }
}
```

## 3. Приоритеты рефакторинга

1. **Текущий приоритет**:
   - Переход на корутины в Retrofit

2. **Будущие задачи**:
   - Оптимизация загрузки изображений
   - Оптимизация RecyclerView

## 4. Оценка времени и ресурсов

1. **Улучшение сетевого слоя**: ✅ Унификация обработки ошибок выполнена, осталось внедрение корутин в Retrofit (~1 день)
2. **Оптимизация UI и производительности**: 1-2 дня

**Общая оценка**: 2-3 дня работы одного разработчика

## 5. Риски и их митигация

1. **Риск**: Регрессии в функциональности после рефакторинга
   - **Митигация**: Написание автоматических тестов до начала рефакторинга

2. **Риск**: Ухудшение производительности из-за изменений в архитектуре
   - **Митигация**: Профилирование приложения до и после рефакторинга для сравнения производительности

3. **Риск**: Проблемы с совместимостью на старых устройствах
   - **Митигация**: Тестирование на устройствах с разными версиями Android