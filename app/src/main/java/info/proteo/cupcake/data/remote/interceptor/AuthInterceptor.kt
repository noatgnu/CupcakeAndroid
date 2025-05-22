package info.proteo.cupcake.data.remote.interceptor

import info.proteo.cupcake.data.local.dao.user.UserPreferencesDao
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import javax.inject.Inject

class AuthInterceptor @Inject constructor(
    private val userPreferencesDao: UserPreferencesDao,
    private val baseUrl: String
) : Interceptor {

    override fun intercept(chain: Interceptor.Chain): Response {
        val originalRequest = chain.request()
        val requestUrl = originalRequest.url.toString()

        val isLoginRequest = requestUrl.contains("/login") ||
                requestUrl.contains("/token-auth")

        if (!isLoginRequest && requestUrl.startsWith(baseUrl)) {
            val activePreferences = runBlocking {
                userPreferencesDao.getCurrentlyActivePreference()
            }

            activePreferences?.let { preferences ->
                val requestBuilder = originalRequest.newBuilder()
                preferences.authToken?.let { token ->
                    requestBuilder.header("Authorization", "Token $token")
                }

                // Add any other headers if needed, like session token
                preferences.sessionToken?.let { session ->
                    requestBuilder.header("X-Cupcake-Instance-Id", session)
                }

                return chain.proceed(requestBuilder.build())
            }
        }

        return chain.proceed(originalRequest)
    }
}