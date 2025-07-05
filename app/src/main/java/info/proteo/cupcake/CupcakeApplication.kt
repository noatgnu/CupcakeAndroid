package info.proteo.cupcake

import android.app.Application
import android.content.Context
import androidx.appcompat.app.AppCompatDelegate
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.HiltAndroidApp
import info.proteo.cupcake.data.remote.service.WebSocketManager
import javax.inject.Inject

@HiltAndroidApp
class CupcakeApplication : Application() {
    @Inject lateinit var webSocketManager: WebSocketManager

    override fun onCreate() {
        super.onCreate()

        ProcessLifecycleOwner.get().lifecycle.addObserver(webSocketManager)
        
        // Apply saved dark mode preference
        applyDarkModePreference()
    }

    private fun applyDarkModePreference() {
        val sharedPreferences = getSharedPreferences("app_settings", Context.MODE_PRIVATE)
        val darkMode = sharedPreferences.getInt("dark_mode", 0) // 0 = follow system
        
        val nightMode = when (darkMode) {
            1 -> AppCompatDelegate.MODE_NIGHT_NO  // Light mode
            2 -> AppCompatDelegate.MODE_NIGHT_YES // Dark mode
            else -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM // Follow system
        }
        
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

}