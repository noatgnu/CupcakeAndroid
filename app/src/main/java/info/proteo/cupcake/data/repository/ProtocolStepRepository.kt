package info.proteo.cupcake.data.repository

import info.proteo.cupcake.data.remote.model.LimitOffsetResponse
import info.proteo.cupcake.data.remote.model.protocol.ProtocolStep
import info.proteo.cupcake.data.remote.model.protocol.StepReagent
import info.proteo.cupcake.data.remote.model.protocol.StepTag
import info.proteo.cupcake.data.remote.model.protocol.TimeKeeper
import info.proteo.cupcake.data.remote.model.reagent.ReagentAction
import info.proteo.cupcake.data.remote.service.ExportAssociatedMetadataResponse
import info.proteo.cupcake.data.remote.service.ProtocolStepService
import info.proteo.cupcake.data.remote.service.SDRFConversionRequest
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProtocolStepRepository @Inject constructor(
    private val protocolStepService: ProtocolStepService
) {
    suspend fun getProtocolSteps(
        offset: Int? = null,
        limit: Int? = null,
        protocolId: Int? = null,
        sectionId: Int? = null,
        ordering: String = "id"
    ): Result<LimitOffsetResponse<ProtocolStep>> {
        return protocolStepService.getProtocolSteps(offset, limit, protocolId, sectionId, ordering)
    }

    suspend fun getProtocolStepById(id: Int): Result<ProtocolStep> {
        return protocolStepService.getProtocolStepById(id)
    }

    suspend fun createProtocolStep(protocolStep: ProtocolStep): Result<ProtocolStep> {
        return protocolStepService.createProtocolStep(protocolStep)
    }

    suspend fun updateProtocolStep(id: Int, protocolStep: ProtocolStep): Result<ProtocolStep> {
        return protocolStepService.updateProtocolStep(id, protocolStep)
    }

    suspend fun deleteProtocolStep(id: Int): Result<Unit> {
        return protocolStepService.deleteProtocolStep(id)
    }

    suspend fun moveStepUp(id: Int): Result<ProtocolStep> {
        return protocolStepService.moveStepUp(id)
    }

    suspend fun moveStepDown(id: Int): Result<ProtocolStep> {
        return protocolStepService.moveStepDown(id)
    }

    suspend fun getTimeKeeper(
        id: Int,
        session: String,
        started: Boolean? = null,
        startTime: String? = null
    ): Result<TimeKeeper> {
        return protocolStepService.getTimeKeeper(id, session, started, startTime)
    }

    suspend fun addProtocolReagent(
        id: Int,
        name: String,
        unit: String,
        quantity: Float,
        scalable: Boolean = false,
        scalableFactor: Float? = null
    ): Result<StepReagent> {
        return protocolStepService.addProtocolReagent(id, name, unit, quantity, scalable, scalableFactor)
    }

    suspend fun removeProtocolReagent(id: Int, reagentId: Int): Result<StepReagent> {
        return protocolStepService.removeProtocolReagent(id, reagentId)
    }

    suspend fun updateProtocolReagent(
        id: Int,
        reagentId: Int,
        quantity: Float,
        scalable: Boolean = false,
        scalableFactor: Float? = null
    ): Result<StepReagent> {
        return protocolStepService.updateProtocolReagent(id, reagentId, quantity, scalable, scalableFactor)
    }

    suspend fun addTagToStep(id: Int, tag: String): Result<StepTag> {
        return protocolStepService.addTagToStep(id, tag)
    }

    suspend fun removeTagFromStep(id: Int, tagId: Int): Result<Unit> {
        return protocolStepService.removeTagFromStep(id, tagId)
    }

    suspend fun getAssociatedReagentActions(id: Int, session: String): Result<List<ReagentAction>> {
        return protocolStepService.getAssociatedReagentActions(id, session)
    }

    suspend fun exportAssociatedMetadata(id: Int, session: String): Result<List<ExportAssociatedMetadataResponse>> {
        return protocolStepService.exportAssociatedMetadata(id, session)
    }

    suspend fun convertMetadataToSdrfTxt(
        id: Int,
        request: SDRFConversionRequest
    ): Result<List<List<String>>> {
        return protocolStepService.convertMetadataToSdrfTxt(id, request)
    }
}