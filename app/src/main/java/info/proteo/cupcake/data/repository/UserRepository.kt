package info.proteo.cupcake.data.repository

import android.util.Log
import info.proteo.cupcake.data.local.dao.user.UserDao
import info.proteo.cupcake.data.local.dao.user.UserPreferencesDao
import info.proteo.cupcake.data.local.entity.user.UserEntity
import info.proteo.cupcake.data.local.entity.user.UserPreferencesEntity
import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import info.proteo.cupcake.shared.data.model.reagent.StoredReagentPermission
import info.proteo.cupcake.shared.data.model.user.User
import info.proteo.cupcake.shared.data.model.user.UserBasic
import info.proteo.cupcake.data.remote.service.AnnotationsPermissionRequest
import info.proteo.cupcake.data.remote.service.AnnotationsPermissionResponse
import info.proteo.cupcake.data.remote.service.ChangePasswordRequest
import info.proteo.cupcake.data.remote.service.CheckUserInLabGroupRequest
import info.proteo.cupcake.data.remote.service.DryRunImportRequest
import info.proteo.cupcake.data.remote.service.DryRunImportResponse
import info.proteo.cupcake.data.remote.service.ExportDataRequest
import info.proteo.cupcake.data.remote.service.ImportUserDataRequest
import info.proteo.cupcake.data.remote.service.IsStaffResponse
import info.proteo.cupcake.data.remote.service.ProtocolPermissionRequest
import info.proteo.cupcake.data.remote.service.ServerSettings
import info.proteo.cupcake.data.remote.service.SessionPermissionRequest
import info.proteo.cupcake.data.remote.service.SignupRequest
import info.proteo.cupcake.data.remote.service.StoredReagentPermissionRequest
import info.proteo.cupcake.data.remote.service.SummarizeAudioTranscriptRequest
import info.proteo.cupcake.data.remote.service.SummarizePromptRequest
import info.proteo.cupcake.data.remote.service.SummarizeStepsRequest
import info.proteo.cupcake.data.remote.service.TurnCredentials
import info.proteo.cupcake.data.remote.service.UpdateProfileRequest
import info.proteo.cupcake.data.remote.service.UserPermissionResponse
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

    suspend fun checkSessionPermission(uniqueId: String): Result<UserPermissionResponse> {
        return try {
            val request = SessionPermissionRequest(session = uniqueId)
            Result.success(userService.checkSessionPermission(request))
        } catch (e: Exception) {
            Log.e("UserRepository", "Error checking session permission: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun checkProtocolPermission(protocolId: Int): Result<UserPermissionResponse> {
        return try {
            val request = ProtocolPermissionRequest(protocol = protocolId)
            Result.success(userService.checkProtocolPermission(request))
        } catch (e: Exception) {
            Log.e("UserRepository", "Error checking protocol permission: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun checkAnnotationsPermission(sessionIds: List<Int>): Result<List<AnnotationsPermissionResponse>> {
        return try {
            val request = AnnotationsPermissionRequest(annotations =  sessionIds)
            Result.success(userService.checkAnnotationsPermission(request))
        } catch (e: Exception) {
            Log.e("UserRepository", "Error checking annotations permission: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getServerSettings(): ServerSettings {
        return try {
            userService.getServerSettings()
        } catch (e: Exception) {
            Log.e("UserRepository", "Error fetching server settings: ${e.message}")
            throw e
        }
    }

    suspend fun updateProfile(request: UpdateProfileRequest): Result<User> {
        return try {
            val user = userService.updateProfile(request)
            // Update local cache
            val userEntity = UserEntity(
                id = user.id,
                username = user.username,
                email = user.email,
                firstName = user.firstName,
                lastName = user.lastName,
                isStaff = user.isStaff ?: false
            )
            userDao.insertOrUpdateUser(userEntity)
            Result.success(user)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error updating profile: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun changePassword(request: ChangePasswordRequest): Result<Unit> {
        return try {
            userService.changePassword(request)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error changing password: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun summarizePrompt(request: SummarizePromptRequest): Result<Unit> {
        return try {
            userService.summarizePrompt(request)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error summarizing prompt: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun summarizeSteps(request: SummarizeStepsRequest): Result<Unit> {
        return try {
            userService.summarizeSteps(request)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error summarizing steps: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun summarizeAudioTranscript(request: SummarizeAudioTranscriptRequest): Result<Unit> {
        return try {
            userService.summarizeAudioTranscript(request)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error summarizing audio transcript: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun generateTurnCredential(): Result<TurnCredentials> {
        return try {
            Result.success(userService.generateTurnCredential())
        } catch (e: Exception) {
            Log.e("UserRepository", "Error generating turn credential: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun exportData(request: ExportDataRequest): Result<Unit> {
        return try {
            userService.exportData(request)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error exporting data: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun importUserData(request: ImportUserDataRequest): Result<Unit> {
        return try {
            userService.importUserData(request)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error importing user data: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun dryRunImportUserData(request: DryRunImportRequest): Result<DryRunImportResponse> {
        return try {
            Result.success(userService.dryRunImportUserData(request))
        } catch (e: Exception) {
            Log.e("UserRepository", "Error in dry run import: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun isStaff(): Result<IsStaffResponse> {
        return try {
            Result.success(userService.isStaff())
        } catch (e: Exception) {
            Log.e("UserRepository", "Error checking staff status: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getUserLabGroups(isProfessional: Boolean?): Result<LimitOffsetResponse<info.proteo.cupcake.shared.data.model.user.LabGroup>> {
        return try {
            Result.success(userService.getUserLabGroups(isProfessional))
        } catch (e: Exception) {
            Log.e("UserRepository", "Error getting user lab groups: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun checkUserInLabGroup(request: CheckUserInLabGroupRequest): Result<Unit> {
        return try {
            userService.checkUserInLabGroup(request)
            Result.success(Unit)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error checking user in lab group: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun signup(request: SignupRequest): Result<User> {
        return try {
            val user = userService.signup(request)
            // Update local cache
            val userEntity = UserEntity(
                id = user.id,
                username = user.username,
                email = user.email,
                firstName = user.firstName,
                lastName = user.lastName,
                isStaff = user.isStaff ?: false
            )
            userDao.insertOrUpdateUser(userEntity)
            Result.success(user)
        } catch (e: Exception) {
            Log.e("UserRepository", "Error signing up: ${e.message}")
            Result.failure(e)
        }
    }

}