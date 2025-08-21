package ru.yandex.speed.workshop.android.data.local

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import timber.log.Timber

/**
 * Менеджер для работы с избранными товарами
 * Сохраняет информацию в SharedPreferences
 */
class FavoritesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(
            PREFS_NAME,
            Context.MODE_PRIVATE,
        )

    // StateFlow для отслеживания изменений в избранном
    private val _favoritesFlow = MutableStateFlow<Set<String>>(getFavorites())
    val favoritesFlow: StateFlow<Set<String>> = _favoritesFlow.asStateFlow()

    /**
     * Получение списка ID избранных товаров
     */
    fun getFavorites(): Set<String> {
        return prefs.getStringSet(KEY_FAVORITES, emptySet()) ?: emptySet()
    }

    /**
     * Проверка, находится ли товар в избранном
     */
    fun isFavorite(productId: String): Boolean {
        return getFavorites().contains(productId)
    }

    /**
     * Добавление товара в избранное
     */
    fun addToFavorites(productId: String) {
        val currentFavorites = getFavorites().toMutableSet()
        if (currentFavorites.add(productId)) {
            saveFavorites(currentFavorites)
            Timber.d("Added product $productId to favorites")
        }
    }

    /**
     * Удаление товара из избранного
     */
    fun removeFromFavorites(productId: String) {
        val currentFavorites = getFavorites().toMutableSet()
        if (currentFavorites.remove(productId)) {
            saveFavorites(currentFavorites)
            Timber.d("Removed product $productId from favorites")
        }
    }

    /**
     * Переключение состояния избранного для товара
     */
    fun toggleFavorite(productId: String): Boolean {
        val currentFavorites = getFavorites().toMutableSet()
        val newState =
            if (currentFavorites.contains(productId)) {
                currentFavorites.remove(productId)
                false
            } else {
                currentFavorites.add(productId)
                true
            }
        saveFavorites(currentFavorites)
        Timber.d("Toggled favorite for product $productId to $newState")
        return newState
    }

    /**
     * Сохранение списка избранных товаров
     */
    private fun saveFavorites(favorites: Set<String>) {
        prefs.edit().putStringSet(KEY_FAVORITES, favorites).apply()
        _favoritesFlow.value = favorites
    }

    companion object {
        private const val PREFS_NAME = "favorites_prefs"
        private const val KEY_FAVORITES = "favorite_products"

        @Volatile
        private var instance: FavoritesManager? = null

        fun getInstance(context: Context): FavoritesManager {
            return instance ?: synchronized(this) {
                instance ?: FavoritesManager(context.applicationContext).also {
                    instance = it
                }
            }
        }
    }
}
