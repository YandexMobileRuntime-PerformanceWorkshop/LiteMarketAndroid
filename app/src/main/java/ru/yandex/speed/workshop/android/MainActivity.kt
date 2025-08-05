package ru.yandex.speed.workshop.android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ru.yandex.speed.workshop.android.databinding.ActivityMainBinding
import ru.yandex.speed.workshop.android.presentation.catalog.CatalogPresenter

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide action bar for clean iOS look
        supportActionBar?.hide()
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up singleton presenter when app is closing
        if (isFinishing) {
            CatalogPresenter.clearInstance()
        }
    }
}