package info.proteo.cupcake.data.repository

import android.util.Log
import info.proteo.cupcake.SessionManager
import info.proteo.cupcake.data.remote.service.AuthService
import info.proteo.cupcake.data.remote.model.LoginRequest
import info.proteo.cupcake.data.remote.model.LoginResponse
import javax.inject.Inject

class AuthRepository @Inject constructor(
    private val authService: AuthService,
    private val sessionManager: SessionManager
) {
    suspend fun login(username: String, password: String, hostname: String): Result<LoginResponse> {
        return try {
            val normalizedHost = normalizeHostname(hostname)
            Log.d("AuthRepository", "Normalized Host: $normalizedHost")
            sessionManager.saveBaseUrl(normalizedHost)
            val response = authService.login(LoginRequest(username, password))
            sessionManager.saveToken(response.token)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun normalizeHostname(hostname: String): String {
        var normalizedHost = hostname
        if (!normalizedHost.startsWith("http")) {
            normalizedHost = "https://$normalizedHost"
        }
        if (!normalizedHost.endsWith("/")) {
            normalizedHost = "$normalizedHost/"
        }
        return normalizedHost
    }
}