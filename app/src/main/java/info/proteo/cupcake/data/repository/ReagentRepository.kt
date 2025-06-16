package info.proteo.cupcake.data.repository

import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import info.proteo.cupcake.shared.data.model.reagent.Reagent
import info.proteo.cupcake.data.remote.service.ReagentService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReagentRepository @Inject constructor(
    private val reagentService: ReagentService
) {

    suspend fun getReagents(
        offset: Int = 0,
        limit: Int = 20,
        search: String? = null
    ): Result<LimitOffsetResponse<Reagent>> {
        return reagentService.getReagents(offset, limit, search)
    }

    suspend fun getReagentById(id: Int): Result<Reagent> {
        return reagentService.getReagentById(id)
    }

    suspend fun createReagent(reagent: Reagent): Result<Reagent> {
        return reagentService.createReagent(reagent)
    }

    suspend fun updateReagent(id: Int, reagent: Reagent): Result<Reagent> {
        return reagentService.updateReagent(id, reagent)
    }

    suspend fun deleteReagent(id: Int): Result<Unit> {
        return reagentService.deleteReagent(id)
    }

    suspend fun saveReagent(reagent: Reagent): Result<Reagent> {
        return if (reagent.id > 0) {
            updateReagent(reagent.id, reagent)
        } else {
            createReagent(reagent)
        }
    }
}