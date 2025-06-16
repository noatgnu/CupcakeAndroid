package info.proteo.cupcake.data.repository

import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import info.proteo.cupcake.shared.data.model.protocol.ProtocolSection
import info.proteo.cupcake.shared.data.model.protocol.ProtocolStep
import info.proteo.cupcake.data.remote.service.ProtocolSectionService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProtocolSectionRepository @Inject constructor(
    private val protocolSectionService: ProtocolSectionService
) {
    suspend fun getProtocolSections(
        offset: Int? = null,
        limit: Int? = null,
        protocolId: Int? = null,
        ordering: String? = null
    ): Result<LimitOffsetResponse<ProtocolSection>> =
        protocolSectionService.getProtocolSections(offset, limit, protocolId, ordering)

    suspend fun createProtocolSection(
        protocolSection: ProtocolSection
    ): Result<ProtocolSection> =
        protocolSectionService.createProtocolSection(protocolSection)

    suspend fun getProtocolSectionById(id: Int): Result<ProtocolSection> =
        protocolSectionService.getProtocolSectionById(id)

    suspend fun updateProtocolSection(
        id: Int,
        protocolSection: ProtocolSection
    ): Result<ProtocolSection> =
        protocolSectionService.updateProtocolSection(id, protocolSection)

    suspend fun partialUpdateProtocolSection(
        id: Int,
        data: Map<String, Any>
    ): Result<ProtocolSection> =
        protocolSectionService.partialUpdateProtocolSection(id, data)

    suspend fun deleteProtocolSection(id: Int): Result<Unit> =
        protocolSectionService.deleteProtocolSection(id)

    fun getProtocolSectionByIdFlow(id: Int): Flow<ProtocolSection?> =
        protocolSectionService.getProtocolSectionByIdFlow(id)

    fun getSectionsByProtocolFlow(protocolId: Int): Flow<List<ProtocolSection>> =
        protocolSectionService.getSectionsByProtocolFlow(protocolId)

    suspend fun getSectionSteps(sectionId: Int): Result<List<ProtocolStep>> =
        protocolSectionService.getSectionSteps(sectionId)

    suspend fun updateSectionSteps(
        sectionId: Int,
        steps: List<ProtocolStep>
    ): Result<List<ProtocolStep>> =
        protocolSectionService.updateSectionSteps(sectionId, steps)
}