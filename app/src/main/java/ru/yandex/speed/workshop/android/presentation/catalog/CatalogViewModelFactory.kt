package ru.yandex.speed.workshop.android.presentation.catalog

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import ru.yandex.speed.workshop.android.data.network.HttpClient
import ru.yandex.speed.workshop.android.data.network.ProductService

class CatalogViewModelFactory(private val context: Context) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CatalogViewModel::class.java)) {
            val httpClient = HttpClient.getInstance(context)
            val api = httpClient.getApi()
            val service = ProductService(api)
            return CatalogViewModel(service) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}