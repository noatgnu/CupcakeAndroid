package info.proteo.cupcake.data.remote.service

import android.util.Log
import info.proteo.cupcake.data.local.dao.reagent.ReagentDao
import info.proteo.cupcake.data.local.entity.reagent.ReagentEntity
import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import info.proteo.cupcake.shared.data.model.reagent.Reagent
import kotlinx.coroutines.flow.first
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

interface ReagentApiService {
    @GET("api/reagent/")
    suspend fun getReagents(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @Query("search") search: String? = null
    ): LimitOffsetResponse<Reagent>

    @GET("api/reagent/{id}/")
    suspend fun getReagentById(@Path("id") id: Int): Reagent

    @POST("api/reagent/")
    suspend fun createReagent(@Body reagent: Reagent): Reagent

    @PATCH("api/reagent/{id}/")
    suspend fun updateReagent(@Path("id") id: Int, @Body reagent: Reagent): Reagent

    @DELETE("api/reagent/{id}/")
    suspend fun deleteReagent(@Path("id") id: Int)
}

interface ReagentService {
    suspend fun getReagents(offset: Int, limit: Int, search: String? = null): Result<LimitOffsetResponse<Reagent>>
    suspend fun getReagentById(id: Int): Result<Reagent>
    suspend fun createReagent(reagent: Reagent): Result<Reagent>
    suspend fun updateReagent(id: Int, reagent: Reagent): Result<Reagent>
    suspend fun deleteReagent(id: Int): Result<Unit>
}

@Singleton
class ReagentServiceImpl @Inject constructor(
    private val apiService: ReagentApiService,
    private val reagentDao: ReagentDao
) : ReagentService {

    override suspend fun getReagents(
        offset: Int,
        limit: Int,
        search: String?
    ): Result<LimitOffsetResponse<Reagent>> {
        return try {
            val response = apiService.getReagents(offset, limit, search)
            Log.d("ReagentService", "Fetched ${response.results.size} reagents from API")
            response.results.forEach { cacheReagent(it) }
            Result.success(response)
        } catch (e: Exception) {
            try {
                val cachedReagents = reagentDao.getAllReagents().first()
                val count = cachedReagents.size
                val paginatedReagents = cachedReagents
                    .drop(offset)
                    .take(limit)
                    .map { it.toDomain() }

                Result.success(LimitOffsetResponse(count, null, null, paginatedReagents))
            } catch (cacheException: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getReagentById(id: Int): Result<Reagent> {
        return try {
            val response = apiService.getReagentById(id)
            cacheReagent(response)
            Result.success(response)
        } catch (e: Exception) {
            val cachedReagent = reagentDao.getById(id)
            if (cachedReagent != null) {
                Result.success(cachedReagent.toDomain())
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun createReagent(reagent: Reagent): Result<Reagent> {
        return try {
            val response = apiService.createReagent(reagent)
            cacheReagent(response)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateReagent(id: Int, reagent: Reagent): Result<Reagent> {
        return try {
            val response = apiService.updateReagent(id, reagent)
            cacheReagent(response)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteReagent(id: Int): Result<Unit> {
        return try {
            apiService.deleteReagent(id)
            reagentDao.getById(id)?.let { reagentDao.delete(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun cacheReagent(reagent: Reagent) {
        reagentDao.insert(
            ReagentEntity(
                id = reagent.id,
                name = reagent.name,
                unit = reagent.unit,
                createdAt = reagent.createdAt,
                updatedAt = reagent.updatedAt
            )
        )
    }

    private fun ReagentEntity.toDomain(): Reagent {
        return Reagent(
            id = id,
            name = name,
            unit = unit,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}