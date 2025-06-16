package info.proteo.cupcake.data.repository

import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import info.proteo.cupcake.shared.data.model.reagent.ReagentAction
import info.proteo.cupcake.shared.data.model.reagent.StoredReagent
import info.proteo.cupcake.data.remote.service.StoredReagentService
import info.proteo.cupcake.shared.data.model.reagent.StoredReagentCreateRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.compareTo

@Singleton
class StoredReagentRepository @Inject constructor(
    private val storedReagentService: StoredReagentService
) {
    fun getStoredReagents(
        offset: Int,
        limit: Int,
        storageObjectId: Int? = null,
        labGroupId: Int? = null,
        barcode: String? = null,
        search: String? = null
    ): Flow<Result<LimitOffsetResponse<StoredReagent>>> = flow {
        emit(storedReagentService.getStoredReagents(offset, limit, storageObjectId, labGroupId, barcode, search))
    }

    fun getStoredReagentById(id: Int): Flow<Result<StoredReagent>> = flow {
        emit(storedReagentService.getStoredReagentById(id))
    }

    suspend fun createStoredReagent(request: StoredReagentCreateRequest): Result<StoredReagent> {
        return storedReagentService.createStoredReagent(request)
    }

    suspend fun updateStoredReagent(id: Int, storedReagent: StoredReagent): Result<StoredReagent> {
        return storedReagentService.updateStoredReagent(id, storedReagent)
    }

    suspend fun deleteStoredReagent(id: Int): Result<Unit> {
        return storedReagentService.deleteStoredReagent(id)
    }

    fun getReagentActions(
        id: Int,
        startDate: String? = null,
        endDate: String? = null
    ): Flow<Result<List<ReagentAction>>> = flow {
        emit(storedReagentService.getReagentActions(id, startDate, endDate))
    }

    suspend fun addAccessGroup(id: Int, labGroupId: Int): Result<StoredReagent> {
        return storedReagentService.addAccessGroup(id, labGroupId)
    }

    suspend fun removeAccessGroup(id: Int, labGroupId: Int): Result<StoredReagent> {
        return storedReagentService.removeAccessGroup(id, labGroupId)
    }

    suspend fun subscribe(
        id: Int,
        notifyLowStock: Boolean,
        notifyExpiry: Boolean
    ): Result<Map<String, Any>> {
        return storedReagentService.subscribe(id, notifyLowStock, notifyExpiry)
    }

    suspend fun unsubscribe(
        id: Int,
        notifyLowStock: Boolean,
        notifyExpiry: Boolean
    ): Result<Map<String, Any>> {
        return storedReagentService.unsubscribe(id, notifyLowStock, notifyExpiry)
    }


}