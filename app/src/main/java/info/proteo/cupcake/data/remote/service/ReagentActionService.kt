package info.proteo.cupcake.data.remote.service

import com.squareup.moshi.Json
import info.proteo.cupcake.data.local.dao.reagent.ReagentActionDao
import info.proteo.cupcake.data.local.entity.reagent.ReagentActionEntity
import info.proteo.cupcake.data.remote.model.LimitOffsetResponse
import info.proteo.cupcake.data.remote.model.reagent.ReagentAction
import kotlinx.coroutines.flow.first
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

data class ReagentActionRequest(
    val reagent: Int,
    @Json(name = "action_type") val actionType: String,
    val quantity: Float,
    val notes: String,
    @Json(name = "step_reagent") val stepReagent: Int? = null,
    val session: String? = null
)

interface ReagentActionApiService {
    @GET("api/reagent_action/")
    suspend fun getReagentActions(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @Query("reagent") reagentId: Int? = null,
        @Query("ordering") ordering: String = "-created_at"
    ): LimitOffsetResponse<ReagentAction>

    @GET("api/reagent_action/{id}/")
    suspend fun getReagentActionById(@Path("id") id: Int): ReagentAction

    @POST("api/reagent_action/")
    suspend fun createReagentAction(@Body request: ReagentActionRequest): ReagentAction

    @DELETE("api/reagent_action/{id}/")
    suspend fun deleteReagentAction(@Path("id") id: Int)

    @GET("api/reagent_action/get_reagent_action_range/")
    suspend fun getReagentActionRange(
        @Query("reagent") reagentId: Int,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): List<ReagentAction>
}

interface ReagentActionService {
    suspend fun getReagentActions(offset: Int, limit: Int, reagentId: Int? = null, ordering: String = "-created_at"): Result<LimitOffsetResponse<ReagentAction>>
    suspend fun getReagentActionById(id: Int): Result<ReagentAction>
    suspend fun createReagentAction(reagentId: Int, actionType: String, quantity: Float, notes: String?, stepReagent: Int?, session: String?): Result<ReagentAction>
    suspend fun deleteReagentAction(id: Int): Result<Unit>
    suspend fun getReagentActionRange(reagentId: Int, startDate: String? = null, endDate: String? = null): Result<List<ReagentAction>>
}

@Singleton
class ReagentActionServiceImpl @Inject constructor(
    private val apiService: ReagentActionApiService,
    private val reagentActionDao: ReagentActionDao
) : ReagentActionService {

    override suspend fun getReagentActions(
        offset: Int,
        limit: Int,
        reagentId: Int?,
        ordering: String
    ): Result<LimitOffsetResponse<ReagentAction>> {
        return try {
            val response = apiService.getReagentActions(offset, limit, reagentId)
            response.results.forEach {
                cacheReagentAction(it)
            }
            Result.success(response)
        } catch (e: Exception) {
            try {
                val cachedActions = if (reagentId != null) {
                    reagentActionDao.getByReagent(reagentId).first()
                } else {
                    emptyList()
                }

                val domainObjects = cachedActions.map { it.toDomainModel() }
                Result.success(LimitOffsetResponse(domainObjects.size, null, null, domainObjects))
            } catch (cacheException: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getReagentActionById(id: Int): Result<ReagentAction> {
        return try {
            val response = apiService.getReagentActionById(id)
            cacheReagentAction(response)
            Result.success(response)
        } catch (e: Exception) {
            val cachedAction = reagentActionDao.getById(id)
            if (cachedAction != null) {
                Result.success(cachedAction.toDomainModel())
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun createReagentAction(
        reagentId: Int,
        actionType: String,
        quantity: Float,
        notes: String?,
        stepReagent: Int?,
        session: String?
    ): Result<ReagentAction> {
        return try {
            val request = ReagentActionRequest(
                reagent = reagentId,
                actionType = actionType,
                quantity = quantity,
                notes = notes ?: "",
                stepReagent = stepReagent,
                session = session
            )
            val response = apiService.createReagentAction(request)
            cacheReagentAction(response)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteReagentAction(id: Int): Result<Unit> {
        return try {
            apiService.deleteReagentAction(id)
            reagentActionDao.getById(id)?.let { reagentActionDao.delete(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getReagentActionRange(
        reagentId: Int,
        startDate: String?,
        endDate: String?
    ): Result<List<ReagentAction>> {
        return try {
            val response = apiService.getReagentActionRange(reagentId, startDate, endDate)
            // Cache results
            response.forEach {
                cacheReagentAction(it)
            }
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun cacheReagentAction(action: ReagentAction) {
        reagentActionDao.insert(action.toEntity())
    }

    private fun ReagentAction.toEntity(): ReagentActionEntity {
        return ReagentActionEntity(
            id = id,
            reagent = reagent,
            actionType = actionType,
            notes = notes,
            quantity = quantity,
            createdAt = createdAt,
            updatedAt = updatedAt,
            user = user,
            stepReagent = stepReagent
        )
    }

    private fun ReagentActionEntity.toDomainModel(): ReagentAction {
        return ReagentAction(
            id = id,
            reagent = reagent,
            actionType = actionType,
            notes = notes,
            quantity = quantity,
            createdAt = createdAt,
            updatedAt = updatedAt,
            user = user,
            stepReagent = stepReagent
        )
    }
}