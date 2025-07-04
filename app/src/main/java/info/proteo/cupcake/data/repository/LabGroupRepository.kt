package info.proteo.cupcake.data.repository

import android.util.Log
import info.proteo.cupcake.data.remote.service.LabGroupService
import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import info.proteo.cupcake.shared.data.model.user.LabGroup
import info.proteo.cupcake.shared.data.model.user.User
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LabGroupRepository @Inject constructor(
    private val labGroupService: LabGroupService
) {

    suspend fun getLabGroups(
        offset: Int,
        limit: Int,
        search: String? = null,
        ordering: String? = null,
        storedReagentId: Int? = null,
        storageObjectId: Int? = null,
        isProfessional: Boolean? = null
    ): Result<LimitOffsetResponse<LabGroup>> {
        return try {
            val result = labGroupService.getLabGroups(
                offset = offset,
                limit = limit,
                search = search,
                ordering = ordering,
                storedReagentId = storedReagentId,
                storageObjectId = storageObjectId,
                isProfessional = isProfessional
            )
            result
        } catch (e: Exception) {
            Log.e("LabGroupRepository", "Error getting lab groups: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getLabGroupById(id: Int): Result<LabGroup> {
        return try {
            val result = labGroupService.getLabGroupById(id)
            result
        } catch (e: Exception) {
            Log.e("LabGroupRepository", "Error getting lab group by id: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun createLabGroup(
        name: String,
        description: String,
        isProfessional: Boolean
    ): Result<LabGroup> {
        return try {
            val result = labGroupService.createLabGroup(name, description, isProfessional)
            result
        } catch (e: Exception) {
            Log.e("LabGroupRepository", "Error creating lab group: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun updateLabGroup(id: Int, updates: Map<String, Any>): Result<LabGroup> {
        return try {
            val result = labGroupService.updateLabGroup(id, updates)
            result
        } catch (e: Exception) {
            Log.e("LabGroupRepository", "Error updating lab group: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun deleteLabGroup(id: Int): Result<Unit> {
        return try {
            val result = labGroupService.deleteLabGroup(id)
            result
        } catch (e: Exception) {
            Log.e("LabGroupRepository", "Error deleting lab group: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun addUser(labGroupId: Int, userId: Int): Result<LabGroup> {
        return try {
            val result = labGroupService.addUser(labGroupId, userId)
            result
        } catch (e: Exception) {
            Log.e("LabGroupRepository", "Error adding user to lab group: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun removeUser(labGroupId: Int, userId: Int): Result<LabGroup> {
        return try {
            val result = labGroupService.removeUser(labGroupId, userId)
            result
        } catch (e: Exception) {
            Log.e("LabGroupRepository", "Error removing user from lab group: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getUsers(labGroupId: Int): Result<List<User>> {
        return try {
            val result = labGroupService.getUsers(labGroupId)
            result
        } catch (e: Exception) {
            Log.e("LabGroupRepository", "Error getting lab group users: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getManagers(labGroupId: Int): Result<List<User>> {
        return try {
            val result = labGroupService.getManagers(labGroupId)
            result
        } catch (e: Exception) {
            Log.e("LabGroupRepository", "Error getting lab group managers: ${e.message}")
            Result.failure(e)
        }
    }
}