package ru.yandex.speed.workshop.android.presentation.common

/**
 * Базовый интерфейс для всех View в MVP архитектуре
 */
interface BaseView {
    fun showLoading(isLoading: Boolean)
    fun showError(message: String)
}

/**
 * Базовый интерфейс для всех Presenter в MVP архитектуре
 */
interface BasePresenter<V : BaseView> {
    fun attachView(view: V)
    fun detachView()
}