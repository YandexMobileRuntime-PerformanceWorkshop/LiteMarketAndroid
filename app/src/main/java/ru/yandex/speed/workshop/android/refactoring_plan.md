# План рефакторинга кода SpeedWorkshopAndroid

## 1. Выявленные проблемы и "костыли" в коде

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
   - Унификация обработки ошибок в сетевом слое

2. **Средний приоритет**:
   - Переход на корутины в Retrofit

3. **Низкий приоритет**:
   - Улучшение кэширования с использованием Room
   - Дополнительная оптимизация производительности

## 4. Оценка времени и ресурсов

1. **Улучшение сетевого слоя**: 1-2 дня
2. **Улучшение кэширования**: 2-3 дня

**Общая оценка**: 3-5 дней работы одного разработчика

## 5. Риски и их митигация

1. **Риск**: Регрессии в функциональности после рефакторинга
   - **Митигация**: Написание автоматических тестов до начала рефакторинга

2. **Риск**: Увеличение размера приложения из-за добавления новых зависимостей
   - **Митигация**: Тщательный анализ добавляемых зависимостей и их влияния на размер APK

3. **Риск**: Ухудшение производительности из-за изменений в архитектуре
   - **Митигация**: Профилирование приложения до и после рефакторинга для сравнения производительности