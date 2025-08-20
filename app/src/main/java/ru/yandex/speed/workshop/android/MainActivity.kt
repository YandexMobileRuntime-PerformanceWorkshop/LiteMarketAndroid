package ru.yandex.speed.workshop.android

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import dagger.hilt.android.AndroidEntryPoint
import ru.yandex.speed.workshop.android.databinding.ActivityMainBinding

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Timber уже инициализирован в SpeedWorkshopApplication

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Hide action bar for clean iOS look
        supportActionBar?.hide()
    }

    override fun onDestroy() {
        super.onDestroy()
        // Clean up when app is closing
        // Singleton presenter cleaning no longer needed with ViewModel architecture
    }
}
