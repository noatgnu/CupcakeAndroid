package info.proteo.cupcake.data.repository

import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import info.proteo.cupcake.shared.data.model.reagent.ReagentAction
import info.proteo.cupcake.data.remote.service.ReagentActionService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReagentActionRepository @Inject constructor(
    private val reagentActionService: ReagentActionService
) {
    fun getReagentActions(
        offset: Int,
        limit: Int,
        reagentId: Int? = null
    ): Flow<Result<LimitOffsetResponse<ReagentAction>>> = flow {
        emit(reagentActionService.getReagentActions(offset, limit, reagentId))
    }

    fun getReagentActionById(id: Int): Flow<Result<ReagentAction>> = flow {
        emit(reagentActionService.getReagentActionById(id))
    }

    suspend fun createReagentAction(
        reagentId: Int,
        actionType: String,
        quantity: Float,
        notes: String?,
        stepReagent: Int? = null,
        session: String? = null
    ): Result<ReagentAction> {
        return reagentActionService.createReagentAction(reagentId, actionType, quantity, notes, stepReagent, session)
    }

    suspend fun deleteReagentAction(id: Int): Result<Unit> {
        return reagentActionService.deleteReagentAction(id)
    }

    fun getReagentActionRange(
        reagentId: Int,
        startDate: String? = null,
        endDate: String? = null
    ): Flow<Result<List<ReagentAction>>> = flow {
        emit(reagentActionService.getReagentActionRange(reagentId, startDate, endDate))
    }
}