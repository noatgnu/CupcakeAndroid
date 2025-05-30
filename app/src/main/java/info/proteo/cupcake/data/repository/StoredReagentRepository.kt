package info.proteo.cupcake.data.repository

import info.proteo.cupcake.data.remote.model.LimitOffsetResponse
import info.proteo.cupcake.data.remote.model.reagent.ReagentAction
import info.proteo.cupcake.data.remote.model.reagent.StoredReagent
import info.proteo.cupcake.data.remote.service.StoredReagentService
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

    suspend fun createStoredReagent(storedReagent: StoredReagent): Result<StoredReagent> {
        return storedReagentService.createStoredReagent(storedReagent)
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

    suspend fun saveStoredReagent(storedReagent: StoredReagent): Result<StoredReagent> {
        return if (storedReagent.id > 0) {
            updateStoredReagent(storedReagent.id, storedReagent)
        } else {
            createStoredReagent(storedReagent)
        }
    }
}