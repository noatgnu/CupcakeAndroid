package info.proteo.cupcake.data.remote.service

import info.proteo.cupcake.shared.data.model.LoginRequest
import info.proteo.cupcake.shared.data.model.LoginResponse
import retrofit2.http.Body
import retrofit2.http.POST
import javax.inject.Inject
import javax.inject.Singleton

interface AuthApiService {
    @POST("api/token-auth/")
    suspend fun login(@Body loginRequest: LoginRequest): LoginResponse
}

interface AuthService {
    suspend fun login(request: LoginRequest): LoginResponse
}

@Singleton
class AuthServiceImpl @Inject constructor(
    private val authApiService: AuthApiService
) : AuthService {
    override suspend fun login(request: LoginRequest): LoginResponse {
        return authApiService.login(request)
    }
}