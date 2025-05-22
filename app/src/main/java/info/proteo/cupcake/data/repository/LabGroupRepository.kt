package info.proteo.cupcake.data.repository

import info.proteo.cupcake.data.remote.model.LimitOffsetResponse
import info.proteo.cupcake.data.remote.model.user.LabGroup
import info.proteo.cupcake.data.remote.model.user.User
import info.proteo.cupcake.data.remote.service.LabGroupService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

interface LabGroupRepository {
    fun getLabGroups(
        offset: Int,
        limit: Int,
        search: String? = null,
        ordering: String? = null,
        storedReagentId: Int? = null,
        storageObjectId: Int? = null,
        isProfessional: Boolean? = null
    ): Flow<Result<LimitOffsetResponse<LabGroup>>>

    fun getLabGroupById(id: Int): Flow<Result<LabGroup>>
    suspend fun createLabGroup(name: String, description: String, isProfessional: Boolean): Result<LabGroup>
    suspend fun updateLabGroup(id: Int, updates: Map<String, Any>): Result<LabGroup>
    suspend fun deleteLabGroup(id: Int): Result<Unit>
    suspend fun removeUser(labGroupId: Int, userId: Int): Result<LabGroup>
    suspend fun addUser(labGroupId: Int, userId: Int): Result<LabGroup>
    fun getUsers(labGroupId: Int): Flow<Result<List<User>>>
    fun getManagers(labGroupId: Int): Flow<Result<List<User>>>
}

@Singleton
class LabGroupRepositoryImpl @Inject constructor(
    private val labGroupService: LabGroupService
) : LabGroupRepository {

    override fun getLabGroups(
        offset: Int,
        limit: Int,
        search: String?,
        ordering: String?,
        storedReagentId: Int?,
        storageObjectId: Int?,
        isProfessional: Boolean?
    ): Flow<Result<LimitOffsetResponse<LabGroup>>> = flow {
        emit(labGroupService.getLabGroups(
            offset, limit, search, ordering, storedReagentId, storageObjectId, isProfessional
        ))
    }

    override fun getLabGroupById(id: Int): Flow<Result<LabGroup>> = flow {
        emit(labGroupService.getLabGroupById(id))
    }

    override suspend fun createLabGroup(
        name: String,
        description: String,
        isProfessional: Boolean
    ): Result<LabGroup> {
        return labGroupService.createLabGroup(name, description, isProfessional)
    }

    override suspend fun updateLabGroup(id: Int, updates: Map<String, Any>): Result<LabGroup> {
        return labGroupService.updateLabGroup(id, updates)
    }

    override suspend fun deleteLabGroup(id: Int): Result<Unit> {
        return labGroupService.deleteLabGroup(id)
    }

    override suspend fun removeUser(labGroupId: Int, userId: Int): Result<LabGroup> {
        return labGroupService.removeUser(labGroupId, userId)
    }

    override suspend fun addUser(labGroupId: Int, userId: Int): Result<LabGroup> {
        return labGroupService.addUser(labGroupId, userId)
    }

    override fun getUsers(labGroupId: Int): Flow<Result<List<User>>> = flow {
        emit(labGroupService.getUsers(labGroupId))
    }

    override fun getManagers(labGroupId: Int): Flow<Result<List<User>>> = flow {
        emit(labGroupService.getManagers(labGroupId))
    }
}