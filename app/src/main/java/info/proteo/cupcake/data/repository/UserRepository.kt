package info.proteo.cupcake.data.repository

import android.util.Log
import info.proteo.cupcake.data.local.dao.user.UserDao
import info.proteo.cupcake.data.local.dao.user.UserPreferencesDao
import info.proteo.cupcake.data.local.entity.user.UserEntity
import info.proteo.cupcake.data.local.entity.user.UserPreferencesEntity
import info.proteo.cupcake.data.remote.model.LimitOffsetResponse
import info.proteo.cupcake.data.remote.model.reagent.StoredReagentPermission
import info.proteo.cupcake.data.remote.model.user.User
import info.proteo.cupcake.data.remote.model.user.UserBasic
import info.proteo.cupcake.data.remote.service.StoredReagentPermissionRequest
import info.proteo.cupcake.data.remote.service.UserService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class UserRepository @Inject constructor(
    private val userService: UserService,
    private val userPreferencesDao: UserPreferencesDao,
    private val userDao: UserDao
) {
    suspend fun getCurrentUser(): Result<User> {
        return try {
            val user = userService.getCurrentUser()
            user.id.let { userId ->
                val userEntity = UserEntity(
                    id = userId,
                    username = user.username,
                    email = user.email,
                    firstName = user.firstName,
                    lastName = user.lastName,
                    isStaff = user.isStaff ?: false
                )
                userDao.insertOrUpdateUser(userEntity)
            }

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun searchUsers(query: String): Result<LimitOffsetResponse<UserBasic>> {
        return try {
            Result.success(userService.searchUsers(query))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUserById(id: Int): Result<User> {
        return try {
            Result.success(userService.getUserById(id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getUsersInLabGroup(labGroupId: Int): Result<LimitOffsetResponse<UserBasic>> {
        return try {
            Result.success(userService.getUsersInLabGroup(labGroupId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAccessibleUsers(storedReagentId: Int): Result<LimitOffsetResponse<UserBasic>> {
        return try {
            Result.success(userService.getAccessibleUsers(storedReagentId))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteAuthToken(userId: String, hostname: String) {
        // remove the entire entry related to the user
        userPreferencesDao.deletePreferences(userId, hostname)

    }

    suspend fun getActiveUserPreference(): UserPreferencesEntity? {
        return userPreferencesDao.getCurrentlyActivePreference()
    }

    suspend fun getUserFromActivePreference(): User? {
        val activePreference = getActiveUserPreference() ?: return null
        Log.d("UserRepository", "Active UserPreference: $activePreference")
        val userId = activePreference.userId

        // Try to get from local cache first
        val localUser = userDao.getByUsername(userId)
        if (localUser != null) {
            return User(
                id = localUser.id,
                username = localUser.username,
                email = localUser.email,
                firstName = localUser.firstName,
                lastName = localUser.lastName,
                isStaff = localUser.isStaff,
                labGroups = emptyList(),
                managedLabGroups = emptyList()
            )
        }
        Log.d("UserRepository", "User not found in local cache, fetching from network")
        // If not in cache, fetch from network
        val user = getCurrentUser().getOrNull()
        return user
    }

    fun checkStoredReagentPermission(request: StoredReagentPermissionRequest): Flow<List<StoredReagentPermission>> = flow {
        try {
            val permissions = userService.checkStoredReagentPermission(request)
            emit(permissions)
        } catch (e: Exception) {
            emit(emptyList())
        }
    }

    suspend fun checkStoredReagentPermissionDirectly(reagentId: Int): Result<StoredReagentPermission?> {
        return try {
            val request = StoredReagentPermissionRequest(listOf(reagentId))
            val permissions = userService.checkStoredReagentPermission(request)
            if (permissions.isNotEmpty()) {
                Result.success(permissions.first())
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }


}