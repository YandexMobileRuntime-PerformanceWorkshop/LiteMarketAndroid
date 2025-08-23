package ru.yandex.speed.workshop.android.domain.repository

import ru.yandex.speed.workshop.android.domain.models.Product
import ru.yandex.speed.workshop.android.domain.models.ProductDetail
import ru.yandex.speed.workshop.android.domain.models.ProductListResponse

/**
 * Репозиторий для работы с продуктами
 * Обеспечивает доступ к данным с кэшированием
 */
interface ProductRepository {
    /**
     * Получение списка продуктов с пагинацией
     *
     * @param page номер страницы (начиная с 1)
     * @param perPage количество элементов на странице
     * @return результат с данными или ошибкой
     */
    suspend fun getProductsList(
        page: Int,
        perPage: Int,
    ): Result<ProductListResponse>

    /**
     * Поиск продуктов по запросу
     *
     * @param query поисковый запрос
     * @param page номер страницы (начиная с 1)
     * @param perPage количество элементов на странице
     * @return результат с данными или ошибкой
     */
    suspend fun searchProducts(
        query: String,
        page: Int,
        perPage: Int,
    ): Result<ProductListResponse>

    /**
     * Получение детальной информации о продукте
     *
     * @param id идентификатор продукта
     * @return результат с данными или ошибкой
     */
    suspend fun getProductDetail(id: String): Result<ProductDetail>

    /**
     * Предварительное заполнение кэша данными из аргументов
     * Используется для быстрого отображения данных без загрузки с сервера
     *
     * @param id идентификатор продукта
     * @param productDetail предварительные данные о продукте
     */
    fun prefillDetailCache(
        id: String,
        productDetail: ProductDetail,
    )

    /**
     * Предварительное заполнение кэша данными из аргументов
     * Используется для быстрого отображения данных без загрузки с сервера
     *
     * @param product предварительные данные о продукте
     */
    fun prefillProductCache(product: Product)

    /**
     * Очистка кэша
     */
    fun clearCache()
}
