# План рефакторинга кода SpeedWorkshopAndroid

## 1. Выявленные проблемы и "костыли" в коде

### 1.2. Проблемы с UI и отображением данных

#### 1.2.1. Жёстко закодированные значения по умолчанию

**Проблема**: В коде UI используются жёстко закодированные значения по умолчанию:

```kotlin
updateTextWithoutFlicker(binding.ratingText, formattedRating ?: "4.3")
updateTextWithoutFlicker(binding.reviewsCountText, reviewsCount?.let { "($it)" } ?: "(288)")
updateTextWithoutFlicker(binding.sellerRatingText, "4.5 • Отзывы оценок")
```

**Последствия**: Трудности с локализацией и поддержкой кода, потенциальные ошибки при изменении требований.

#### 1.2.2. Сложная логика обновления UI

**Проблема**: В `ProductDetailFragment` используется сложная логика для обновления UI с множеством условий:

```kotlin
if (discountPercent != null && discountPercent > 0) {
    // Используем значение из API
    updateTextWithoutFlicker(binding.discountText, "$discountPercent%")
    binding.discountText.visibility = View.VISIBLE
} else if (!discountPercentage.isNullOrEmpty() && discountPercentage.isNotBlank()) {
    // Используем значение из вложенного объекта
    val formattedDiscount = if (discountPercentage.endsWith("%")) discountPercentage else "$discountPercentage%"
    updateTextWithoutFlicker(binding.discountText, formattedDiscount)
    binding.discountText.visibility = View.VISIBLE
} else {
    // Скрываем скидку, если её нет
    binding.discountText.visibility = View.GONE
}
```

**Последствия**: Код становится сложным для понимания и поддержки, увеличивается вероятность ошибок.

#### 1.2.3. Дублирование кода в ProductDetailFragment

**Проблема**: Методы `updateProductDetailContent` и `updateAllProductDetails` содержат дублирующийся код для обновления UI.

**Последствия**: Нарушение принципа DRY (Don't Repeat Yourself), сложности с поддержкой кода.

### 1.3. Проблемы с сетевыми запросами и обработкой ошибок

#### 1.3.1. Несогласованная обработка ошибок

**Проблема**: В разных частях кода используются разные подходы к обработке ошибок:

```kotlin
// В ProductService
if (response.isSuccessful && response.body() != null) {
    response.body()!!
} else {
    ProductListResponse(emptyList(), false) // Возвращаем пустой объект
}

// В другом месте
if (response.isSuccessful && response.body() != null) {
    // ...
} else {
    throw NetworkException.HttpError(response.code(), response.message()) // Бросаем исключение
}
```

**Последствия**: Непредсказуемое поведение приложения при ошибках, сложности с отладкой.

#### 1.3.2. Неоптимальное использование Retrofit

**Проблема**: Вместо использования корутин с Retrofit напрямую, используется блокирующий вызов `.execute()`:

```kotlin
val response = api.getProductsList(page, perPage).execute()
```

**Последствия**: Потенциальные проблемы с производительностью и блокировкой потоков.

## 2. План рефакторинга

### 2.2. Улучшение UI-кода

#### 2.2.1. Выделение презентеров для UI-компонентов

1. Создать отдельные классы для отображения различных частей UI:
   - `ProductImagePresenter` для работы с галереей изображений
   - `ProductPricePresenter` для работы с ценами и скидками
   - `ProductInfoPresenter` для работы с основной информацией

2. Пример реализации:

```kotlin
class ProductPricePresenter {
    fun formatPrice(product: ProductDetail): FormattedPrice {
        return FormattedPrice(
            currentPrice = product.currentPrice,
            oldPrice = product.oldPrice,
            discountText = formatDiscount(product),
            showOldPrice = product.oldPrice != null,
            showDiscount = product.hasDiscount
        )
    }
    
    private fun formatDiscount(product: ProductDetail): String {
        return when {
            product.discountPercent != null -> "${product.discountPercent}%"
            product.discountPercentage != null -> 
                if (product.discountPercentage.endsWith("%")) 
                    product.discountPercentage 
                else 
                    "${product.discountPercentage}%"
            else -> ""
        }
    }
}

data class FormattedPrice(
    val currentPrice: String,
    val oldPrice: String?,
    val discountText: String,
    val showOldPrice: Boolean,
    val showDiscount: Boolean
)
```

#### 2.2.2. Создание расширений для упрощения работы с UI

```kotlin
fun TextView.setTextIfChanged(newText: String) {
    if (text.toString() != newText) {
        text = newText
    }
}

fun View.setVisibleIf(condition: Boolean) {
    visibility = if (condition) View.VISIBLE else View.GONE
}
```

#### 2.2.3. Использование ресурсов вместо жёстко закодированных значений

1. Переместить все строковые константы в `strings.xml`:

```xml
<string name="default_rating">4.3</string>
<string name="default_reviews_count">(288)</string>
<string name="default_seller_rating">4.5 • Отзывы оценок</string>
```

2. Использовать их в коде:

```kotlin
val formattedRating = ratingScore?.let { String.format("%.1f", it) } ?: getString(R.string.default_rating)
```

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

2. Упростить код в сервисах:

```kotlin
suspend fun getProductsList(page: Int = 1, perPage: Int = 20): Result<ProductListResponse> {
    return try {
        val response = api.getProductsList(page, perPage)
        if (response.isSuccessful) {
            Result.Success(response.body() ?: ProductListResponse(emptyList(), false))
        } else {
            Result.Error(NetworkException.HttpError(response.code(), response.message()))
        }
    } catch (e: Exception) {
        Result.Error(NetworkException.NetworkError("Failed to get products list", e))
    }
}
```

#### 2.3.2. Унификация обработки ошибок

1. Создать общий обработчик ответов API:

```kotlin
suspend fun <T> safeApiCall(call: suspend () -> Response<T>): Result<T> {
    return try {
        val response = call()
        if (response.isSuccessful) {
            val body = response.body()
            if (body != null) {
                Result.Success(body)
            } else {
                Result.Error(NetworkException.EmptyResponseError("Empty response body"))
            }
        } else {
            Result.Error(NetworkException.HttpError(response.code(), response.message()))
        }
    } catch (e: Exception) {
        Result.Error(NetworkException.NetworkError("Network request failed", e))
    }
}
```

2. Использовать его во всех сетевых вызовах:

```kotlin
suspend fun getProductsList(page: Int, perPage: Int): Result<ProductListResponse> {
    return safeApiCall { api.getProductsList(page, perPage) }
}
```

### 2.4. Улучшение кэширования

#### 2.4.1. Использование Room для кэширования

1. Создать сущности для базы данных:

```kotlin
@Entity(tableName = "products")
data class ProductEntity(
    @PrimaryKey val id: String,
    val title: String,
    val price: String,
    val oldPrice: String?,
    // ...
)
```

2. Создать DAO для работы с базой данных:

```kotlin
@Dao
interface ProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProduct(product: ProductEntity)
    
    @Query("SELECT * FROM products WHERE id = :id")
    suspend fun getProductById(id: String): ProductEntity?
    
    // ...
}
```

3. Обновить репозиторий для использования локальной базы данных:

```kotlin
override suspend fun getProductDetail(id: String): Result<ProductDetail> {
    // Сначала проверяем кэш в базе данных
    val cachedProduct = productDao.getProductById(id)
    if (cachedProduct != null) {
        // Асинхронно обновляем кэш
        refreshProductDetail(id)
        return Result.Success(cachedProduct.toDomain())
    }
    
    // Если нет в кэше, загружаем с API
    return fetchProductDetail(id)
}
```

## 3. Приоритеты рефакторинга

1. **Высокий приоритет**:
   - ✅ Реорганизация моделей данных для устранения дублирования
   - Унификация обработки ошибок в сетевом слое
   - ✅ Выделение презентеров для UI-компонентов

2. **Средний приоритет**:
   - Переход на корутины в Retrofit
   - ✅ Использование ресурсов вместо жёстко закодированных значений
   - ✅ Создание расширений для упрощения работы с UI

3. **Низкий приоритет**:
   - Улучшение кэширования с использованием Room
   - Дополнительная оптимизация производительности

## 4. Оценка времени и ресурсов

1. **Реорганизация моделей данных**: ✅ Выполнено
2. **Улучшение UI-кода**: ✅ Выполнено
3. **Улучшение сетевого слоя**: 1-2 дня
4. **Улучшение кэширования**: 2-3 дня

**Общая оценка**: 5-8 дней работы одного разработчика

## 5. Риски и их митигация

1. **Риск**: Регрессии в функциональности после рефакторинга
   - **Митигация**: Написание автоматических тестов до начала рефакторинга

2. **Риск**: Увеличение размера приложения из-за добавления новых зависимостей
   - **Митигация**: Тщательный анализ добавляемых зависимостей и их влияния на размер APK

3. **Риск**: Ухудшение производительности из-за изменений в архитектуре
   - **Митигация**: Профилирование приложения до и после рефакторинга для сравнения производительности