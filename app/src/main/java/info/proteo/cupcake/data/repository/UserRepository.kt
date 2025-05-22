package info.proteo.cupcake.data.repository

import info.proteo.cupcake.data.local.dao.user.UserDao
import info.proteo.cupcake.data.local.dao.user.UserPreferencesDao
import info.proteo.cupcake.data.local.entity.user.UserEntity
import info.proteo.cupcake.data.local.entity.user.UserPreferencesEntity
import info.proteo.cupcake.data.remote.model.LimitOffsetResponse
import info.proteo.cupcake.data.remote.model.user.User
import info.proteo.cupcake.data.remote.model.user.UserBasic
import info.proteo.cupcake.data.remote.service.UserService
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
        val userId = activePreference.userId.toIntOrNull() ?: return null

        // Try to get from local cache first
        val localUser = userDao.getById(userId)
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

        // If not in cache, fetch from network
        return getUserById(userId).getOrNull()
    }


}