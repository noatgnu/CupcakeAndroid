package info.proteo.cupcake.data.remote.interceptor

import info.proteo.cupcake.data.local.dao.user.UserPreferencesDao
import kotlinx.coroutines.runBlocking
import okhttp3.Interceptor
import okhttp3.Response
import java.util.UUID
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

                    if (preferences.sessionToken == null) {
                        val newSessionToken = StringBuilder().append(UUID.randomUUID().toString()).append("android").toString()

                        runBlocking {
                            userPreferencesDao.updateSessionToken(
                                preferences.userId,
                                preferences.hostname,
                                newSessionToken
                            )
                        }

                        requestBuilder.header("X-Cupcake-Instance-Id", newSessionToken)
                    } else {
                        requestBuilder.header("X-Cupcake-Instance-Id", preferences.sessionToken)
                    }
                }

                return chain.proceed(requestBuilder.build())
            }
        }

        return chain.proceed(originalRequest)
    }
}