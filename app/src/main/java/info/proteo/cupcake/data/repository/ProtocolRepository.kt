package info.proteo.cupcake.data.repository

import info.proteo.cupcake.data.remote.model.LimitOffsetResponse
import info.proteo.cupcake.data.remote.model.protocol.ProtocolModel
import info.proteo.cupcake.data.remote.model.protocol.ProtocolReagent
import info.proteo.cupcake.data.remote.model.protocol.ProtocolTag
import info.proteo.cupcake.data.remote.model.user.User
import info.proteo.cupcake.data.remote.service.CreateExportRequest
import info.proteo.cupcake.data.remote.service.CreateProtocolRequest
import info.proteo.cupcake.data.remote.service.MetadataColumnPayload
import info.proteo.cupcake.data.remote.service.ProtocolService
import info.proteo.cupcake.data.remote.service.SessionMinimal
import info.proteo.cupcake.data.remote.service.UpdateProtocolRequest
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProtocolRepository @Inject constructor(
    private val protocolService: ProtocolService
) {
    suspend fun getProtocols(
        offset: Int? = null,
        limit: Int? = null,
        search: String? = null,
        ordering: String? = null,
        protocolTitle: String? = null,
        protocolCreatedOn: String? = null
    ): Result<LimitOffsetResponse<ProtocolModel>> =
        protocolService.getProtocols(offset, limit, search, ordering, protocolTitle, protocolCreatedOn)

    suspend fun createProtocol(request: CreateProtocolRequest): Result<ProtocolModel> =
        protocolService.createProtocol(request)

    suspend fun getProtocolById(id: Int): Result<ProtocolModel> =
        protocolService.getProtocolById(id)

    suspend fun updateProtocol(id: Int, request: UpdateProtocolRequest): Result<ProtocolModel> =
        protocolService.updateProtocol(id, request)

    suspend fun deleteProtocol(id: Int): Result<Unit> =
        protocolService.deleteProtocol(id)

    fun getProtocolByIdFlow(id: Int): Flow<ProtocolModel?> =
        protocolService.getProtocolByIdFlow(id)

    fun getAllProtocolsFlow(): Flow<List<ProtocolModel>> =
        protocolService.getAllProtocolsFlow()

    fun getEnabledProtocolsFlow(): Flow<List<ProtocolModel>> =
        protocolService.getEnabledProtocolsFlow()

    suspend fun getAssociatedSessions(id: Int): Result<List<SessionMinimal>> =
        protocolService.getAssociatedSessions(id)

    suspend fun getUserProtocols(
        offset: Int? = null,
        limit: Int? = null,
        search: String? = null
    ): Result<LimitOffsetResponse<ProtocolModel>> =
        protocolService.getUserProtocols(offset, limit, search)

    suspend fun createExport(id: Int, request: CreateExportRequest): Result<Unit> =
        protocolService.createExport(id, request)

    suspend fun cloneProtocol(id: Int, newTitle: String?, newDescription: String?): Result<ProtocolModel> =
        protocolService.cloneProtocol(id, newTitle, newDescription)

    suspend fun checkIfTitleExists(title: String): Result<Boolean> =
        protocolService.checkIfTitleExists(title)

    suspend fun addUserRole(id: Int, username: String, role: String): Result<Unit> =
        protocolService.addUserRole(id, username, role)

    suspend fun getEditors(id: Int): Result<List<User>> =
        protocolService.getEditors(id)

    suspend fun getViewers(id: Int): Result<List<User>> =
        protocolService.getViewers(id)

    suspend fun removeUserRole(id: Int, username: String, role: String): Result<Unit> =
        protocolService.removeUserRole(id, username, role)

    suspend fun getProtocolReagents(id: Int): Result<List<ProtocolReagent>> =
        protocolService.getProtocolReagents(id)

    suspend fun addTagToProtocol(id: Int, tagName: String): Result<ProtocolTag> =
        protocolService.addTagToProtocol(id, tagName)

    suspend fun removeTagFromProtocol(id: Int, tagId: Int): Result<Unit> =
        protocolService.removeTagFromProtocol(id, tagId)

    suspend fun addMetadataColumns(
        id: Int,
        metadataColumns: List<MetadataColumnPayload>
    ): Result<ProtocolModel> =
        protocolService.addMetadataColumns(id, metadataColumns)
}