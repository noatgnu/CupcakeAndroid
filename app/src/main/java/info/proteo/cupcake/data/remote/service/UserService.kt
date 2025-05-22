package info.proteo.cupcake.data.remote.service

import info.proteo.cupcake.data.remote.model.LimitOffsetResponse
import info.proteo.cupcake.data.remote.model.user.User
import info.proteo.cupcake.data.remote.model.user.UserBasic
import retrofit2.http.GET
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

interface UserApiService {
    @GET("api/user/current/")
    suspend fun getCurrentUser(): User

    @GET("api/user/")
    suspend fun searchUsers(@Query("search") query: String): LimitOffsetResponse<UserBasic>

    @GET("api/user/{id}/")
    suspend fun getUserById(@Path("id") id: Int): User

    @GET("api/user/")
    suspend fun getUsersInLabGroup(@Query("lab_group") labGroupId: Int): LimitOffsetResponse<UserBasic>

    @GET("api/user/")
    suspend fun getAccessibleUsers(@Query("stored_reagent") storedReagentId: Int): LimitOffsetResponse<UserBasic>
}

interface UserService {
    suspend fun getCurrentUser(): User
    suspend fun searchUsers(query: String): LimitOffsetResponse<UserBasic>
    suspend fun getUserById(id: Int): User
    suspend fun getUsersInLabGroup(labGroupId: Int): LimitOffsetResponse<UserBasic>
    suspend fun getAccessibleUsers(storedReagentId: Int): LimitOffsetResponse<UserBasic>
}

@Singleton
class UserServiceImpl @Inject constructor(
    private val userApiService: UserApiService
) : UserService {

    override suspend fun getCurrentUser(): User {
        return userApiService.getCurrentUser()
    }

    override suspend fun searchUsers(query: String): LimitOffsetResponse<UserBasic> {
        return userApiService.searchUsers(query)
    }

    override suspend fun getUserById(id: Int): User {
        return userApiService.getUserById(id)
    }

    override suspend fun getUsersInLabGroup(labGroupId: Int): LimitOffsetResponse<UserBasic> {
        return userApiService.getUsersInLabGroup(labGroupId)
    }

    override suspend fun getAccessibleUsers(storedReagentId: Int): LimitOffsetResponse<UserBasic> {
        return userApiService.getAccessibleUsers(storedReagentId)
    }
}