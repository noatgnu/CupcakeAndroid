package info.proteo.cupcake

import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import androidx.core.content.edit

@Singleton
class SessionManager @Inject constructor(@ApplicationContext private val context: Context) {
    private val sharedPreferences = context.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    fun saveBaseUrl(baseUrl: String) {
        sharedPreferences.edit { putString("base_url", baseUrl) }
    }

    fun getBaseUrl(): String {
        return sharedPreferences.getString("base_url", "") ?: ""
    }

    fun saveToken(token: String) {
        sharedPreferences.edit { putString("auth_token", token) }
    }

    fun getToken(): String {
        return sharedPreferences.getString("auth_token", "") ?: ""
    }

    fun saveSession(session: String) {
        sharedPreferences.edit { putString("session", session) }
    }

    fun getSession(): String {
        return sharedPreferences.getString("session", "") ?: ""
    }

    fun clearSession() {
        sharedPreferences.edit { clear() }
    }
}