package info.proteo.cupcake.data.remote.service

import android.util.Log
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import info.proteo.cupcake.shared.data.model.instrument.InstrumentUsage
import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import info.proteo.cupcake.data.local.dao.instrument.InstrumentDao
import info.proteo.cupcake.data.local.dao.instrument.InstrumentUsageDao
import info.proteo.cupcake.data.local.dao.user.UserDao
import info.proteo.cupcake.data.local.entity.instrument.InstrumentUsageEntity
import info.proteo.cupcake.shared.data.model.instrument.DelayUsageRequest

import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

@JsonClass(generateAdapter = true)
data class CreateInstrumentUsageRequest(
    val instrument: Int,
    @Json(name = "time_started") val timeStarted: String,
    @Json(name = "time_ended") val timeEnded: String,
    val description: String,
    val repeat: Int? = null,
    @Json(name = "repeat_until") val repeatUntil: String? = null,
    val maintenance: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class UpdateInstrumentUsageRequest(
    @Json(name = "time_started") val timeStarted: String? = null,
    @Json(name = "time_ended") val timeEnded: String? = null,
    val approved: Boolean? = null,
    val maintenance: Boolean? = null,
    val description: String? = null
)

@JsonClass(generateAdapter = true)
data class PatchInstrumentUsageRequest(
    val instrument: Int? = null,
    val annotation: Int? = null,
    @Json(name = "time_started") val timeStarted: String? = null,
    @Json(name = "time_ended") val timeEnded: String? = null,
    val description: String? = null,
    val approved: Boolean? = null,
    val maintenance: Boolean? = null
)


@JsonClass(generateAdapter = true)
data class ExportUsageRequest(
    val instruments: List<Int>? = null,
    @Json(name = "lab_group") val labGroup: List<Int>? = null,
    val user: List<Int>? = null,
    @Json(name = "time_started") val timeStarted: String? = null,
    @Json(name = "time_ended") val timeEnded: String? = null,
    val mode: String? = null,
    @Json(name = "file_format") val fileFormat: String? = null,
    @Json(name = "calculate_duration_with_cutoff") val calculateDurationWithCutoff: Boolean? = null,
    @Json(name = "instance_id") val instanceId: String? = null,
    @Json(name = "includes_maintenance") val includesMaintenance: Boolean? = null,
    @Json(name = "approved_only") val approvedOnly: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class ExportUsageResponse(
    @Json(name = "job_id") val jobId: String
)


interface InstrumentUsageApiService {

    @GET("api/instrument_usage/")
    suspend fun getInstrumentUsages(
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null,
        @Query("time_started") timeStarted: String? = null,
        @Query("time_ended") timeEnded: String? = null,
        @Query("instrument") instrument: String? = null,
        @Query("users") users: String? = null,
        @Query("search_type") searchType: String? = null,
        @Query("search") search: String? = null,
        @Query("ordering") ordering: String? = null
    ): Response<LimitOffsetResponse<InstrumentUsage>>

    @POST("api/instrument_usage/")
    suspend fun createInstrumentUsage(@Body usageRequest: CreateInstrumentUsageRequest): Response<InstrumentUsage>

    @GET("api/instrument_usage/{id}/")
    suspend fun getInstrumentUsageById(@Path("id") id: Int): Response<InstrumentUsage>

    @PUT("api/instrument_usage/{id}/")
    suspend fun updateInstrumentUsage(
        @Path("id") id: Int,
        @Body usageRequest: UpdateInstrumentUsageRequest
    ): Response<InstrumentUsage>

    @PATCH("api/instrument_usage/{id}/")
    suspend fun partialUpdateInstrumentUsage(
        @Path("id") id: Int,
        @Body usageRequest: PatchInstrumentUsageRequest
    ): Response<InstrumentUsage>

    @DELETE("api/instrument_usage/{id}/")
    suspend fun deleteInstrumentUsage(@Path("id") id: Int): Response<Unit>

    @GET("api/instrument_usage/get_user_instrument_usage/")
    suspend fun getCurrentUserInstrumentUsage(
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): Response<LimitOffsetResponse<InstrumentUsage>> // Assuming this might also be paginated

    @DELETE("api/instrument_usage/{id}/delete_usage/")
    suspend fun deleteUsageAction(@Path("id") id: Int): Response<Unit>

    @HTTP(method = "GET", path = "api/instrument_usage/{id}/delay_usage/", hasBody = true)
    suspend fun delayUsage(
        @Path("id") id: Int,
        @Body delayRequest: DelayUsageRequest
    ): Response<InstrumentUsage>

    @POST("api/instrument_usage/export_usage/")
    suspend fun exportUsage(@Body exportRequest: ExportUsageRequest): Response<ExportUsageResponse>

    @POST("api/instrument_usage/{id}/approve_usage_toggle/")
    suspend fun approveUsageToggle(@Path("id") id: Int): Response<InstrumentUsage>
}

interface InstrumentUsageService {

    suspend fun getInstrumentUsages(
        limit: Int? = null,
        offset: Int? = null,
        timeStarted: String? = null,
        timeEnded: String? = null,
        instrument: String? = null,
        users: String? = null,
        searchType: String? = null,
        search: String? = null,
        ordering: String? = null
    ): Result<LimitOffsetResponse<InstrumentUsage>>

    suspend fun createInstrumentUsage(usageRequest: CreateInstrumentUsageRequest): Result<InstrumentUsage>

    suspend fun getInstrumentUsageById(id: Int): Result<InstrumentUsage>

    suspend fun updateInstrumentUsage(id: Int, usageRequest: UpdateInstrumentUsageRequest): Result<InstrumentUsage>

    suspend fun partialUpdateInstrumentUsage(id: Int, usageRequest: PatchInstrumentUsageRequest): Result<InstrumentUsage>

    suspend fun deleteInstrumentUsage(id: Int): Result<Unit>

    suspend fun getCurrentUserInstrumentUsage(limit: Int? = null, offset: Int? = null): Result<LimitOffsetResponse<InstrumentUsage>>

    suspend fun deleteUsageAction(id: Int): Result<Unit>

    suspend fun delayUsage(id: Int, delayRequest: DelayUsageRequest): Result<InstrumentUsage>

    suspend fun exportUsage(exportRequest: ExportUsageRequest): Result<ExportUsageResponse>

    suspend fun approveUsageToggle(id: Int): Result<InstrumentUsage>
}


@Singleton
class InstrumentUsageServiceImpl @Inject constructor(
    private val instrumentUsageApiService: InstrumentUsageApiService,
    private val instrumentUsageDao: InstrumentUsageDao,
    private val instrumentDao: InstrumentDao,
    private val userDao: UserDao
) : InstrumentUsageService {

    private val TAG = "InstrumentUsageService"

    override suspend fun getInstrumentUsages(
        limit: Int?,
        offset: Int?,
        timeStarted: String?,
        timeEnded: String?,
        instrument: String?,
        users: String?,
        searchType: String?,
        search: String?,
        ordering: String?
    ): Result<LimitOffsetResponse<InstrumentUsage>> {
        return try {
            val response = instrumentUsageApiService.getInstrumentUsages(
                limit, offset, timeStarted, timeEnded, instrument, users, searchType, search, ordering
            )
            if (response.isSuccessful) {
                response.body()?.let {
                    instrumentUsageDao.insertAll(it.results.toEntities())
                    Result.success(it)
                } ?: Result.failure(Exception("Response body is null"))
            } else {
                Result.failure(Exception("API error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getInstrumentUsages failed", e)
            Result.failure(e)
        }
    }

    override suspend fun createInstrumentUsage(usageRequest: CreateInstrumentUsageRequest): Result<InstrumentUsage> {
        return try {
            val response = instrumentUsageApiService.createInstrumentUsage(usageRequest)
            if (response.isSuccessful) {
                response.body()?.let {
                    instrumentUsageDao.insert(it.toEntity())
                    Result.success(it)
                } ?: Result.failure(Exception("Response body is null"))
            } else {
                Result.failure(Exception("API error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "createInstrumentUsage failed", e)
            Result.failure(e)
        }
    }

    override suspend fun getInstrumentUsageById(id: Int): Result<InstrumentUsage> {
        return try {
            val response = instrumentUsageApiService.getInstrumentUsageById(id)
            if (response.isSuccessful) {
                response.body()?.let {
                    instrumentUsageDao.insert(it.toEntity())
                    Result.success(it)
                } ?: Result.failure(Exception("Response body is null for ID $id"))
            } else {
                Log.w(TAG, "API error fetching usage $id: ${response.code()}, trying cache.")
                instrumentUsageDao.getById(id)?.let {
                    Result.success(it.toDomain())
                } ?: Result.failure(Exception("API error: ${response.code()} and not found in cache"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getInstrumentUsageById $id failed, trying cache", e)
            instrumentUsageDao.getById(id)?.let {
                Result.success(it.toDomain())
            } ?: Result.failure(e)
        }
    }

    override suspend fun updateInstrumentUsage(id: Int, usageRequest: UpdateInstrumentUsageRequest): Result<InstrumentUsage> {
        return try {
            val response = instrumentUsageApiService.updateInstrumentUsage(id, usageRequest)
            if (response.isSuccessful) {
                response.body()?.let {
                    instrumentUsageDao.insert(it.toEntity()) // Update cache
                    Result.success(it)
                } ?: Result.failure(Exception("Response body is null"))
            } else {
                Result.failure(Exception("API error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "updateInstrumentUsage $id failed", e)
            Result.failure(e)
        }
    }

    override suspend fun partialUpdateInstrumentUsage(id: Int, usageRequest: PatchInstrumentUsageRequest): Result<InstrumentUsage> {
        return try {
            val response = instrumentUsageApiService.partialUpdateInstrumentUsage(id, usageRequest)
            if (response.isSuccessful) {
                response.body()?.let {
                    instrumentUsageDao.insert(it.toEntity())
                    Result.success(it)
                } ?: Result.failure(Exception("Response body is null"))
            } else {
                Result.failure(Exception("API error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "partialUpdateInstrumentUsage $id failed", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteInstrumentUsage(id: Int): Result<Unit> {
        return try {
            val response = instrumentUsageApiService.deleteInstrumentUsage(id)
            if (response.isSuccessful) {
                val entityToDelete = instrumentUsageDao.getById(id)
                entityToDelete?.let { instrumentUsageDao.delete(it) }
                Result.success(Unit)
            } else {
                Result.failure(Exception("API error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "deleteInstrumentUsage $id failed", e)
            Result.failure(e)
        }
    }

    override suspend fun getCurrentUserInstrumentUsage(limit: Int?, offset: Int?): Result<LimitOffsetResponse<InstrumentUsage>> {
        return try {
            val response = instrumentUsageApiService.getCurrentUserInstrumentUsage(limit, offset)
            if (response.isSuccessful) {
                response.body()?.let {
                    instrumentUsageDao.insertAll(it.results.toEntities())
                    Result.success(it)
                } ?: Result.failure(Exception("Response body is null"))
            } else {
                Result.failure(Exception("API error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "getCurrentUserInstrumentUsage failed", e)
            Result.failure(e)
        }
    }

    override suspend fun deleteUsageAction(id: Int): Result<Unit> {
        return try {
            val response = instrumentUsageApiService.deleteUsageAction(id)
            if (response.isSuccessful) {
                val entityToDelete = instrumentUsageDao.getById(id)
                entityToDelete?.let { instrumentUsageDao.delete(it) }
                Result.success(Unit)
            } else {
                Result.failure(Exception("API error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "deleteUsageAction $id failed", e)
            Result.failure(e)
        }
    }

    override suspend fun delayUsage(id: Int, delayRequest: DelayUsageRequest): Result<InstrumentUsage> {
        return try {
            val response = instrumentUsageApiService.delayUsage(id, delayRequest)
            if (response.isSuccessful) {
                response.body()?.let {
                    instrumentUsageDao.insert(it.toEntity())
                    Result.success(it)
                } ?: Result.failure(Exception("Response body is null"))
            } else {
                Result.failure(Exception("API error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "delayUsage $id failed", e)
            Result.failure(e)
        }
    }

    override suspend fun exportUsage(exportRequest: ExportUsageRequest): Result<ExportUsageResponse> {
        return try {
            val response = instrumentUsageApiService.exportUsage(exportRequest)
            if (response.isSuccessful) {
                response.body()?.let {
                    Result.success(it)
                } ?: Result.failure(Exception("Response body is null"))
            } else {
                Result.failure(Exception("API error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "exportUsage failed", e)
            Result.failure(e)
        }
    }

    override suspend fun approveUsageToggle(id: Int): Result<InstrumentUsage> {
        return try {
            val response = instrumentUsageApiService.approveUsageToggle(id)
            if (response.isSuccessful) {
                response.body()?.let {
                    instrumentUsageDao.insert(it.toEntity())
                    Result.success(it)
                } ?: Result.failure(Exception("Response body is null"))
            } else {
                Result.failure(Exception("API error: ${response.code()} ${response.message()}"))
            }
        } catch (e: Exception) {
            Log.e(TAG, "approveUsageToggle $id failed", e)
            Result.failure(e)
        }
    }

    private suspend fun List<InstrumentUsage>.toEntities(): List<InstrumentUsageEntity> {
        return this.map { it.toEntity() }
    }

    private suspend fun getUserIdByUsername(username: String): Int? {
        return userDao.getByUsername(username)?.id ?: null
    }

    private suspend fun InstrumentUsage.toEntity(): InstrumentUsageEntity {

        val userId = user?.let { getUserIdByUsername(it) }

        return InstrumentUsageEntity(
            id = id,
            instrument = instrument,
            annotation = annotation,
            createdAt = createdAt,
            updatedAt = updatedAt,
            timeStarted = timeStarted,
            timeEnded = timeEnded,
            user = userId,
            description = description,
            approved = approved,
            maintenance = maintenance,
            approvedBy = approvedBy
        )
    }

    private suspend fun InstrumentUsageEntity.toDomain(): InstrumentUsage {
        val instrument = instrumentDao.getById(instrument)
        if (instrument == null) {
            throw IllegalArgumentException("Instrument not found for ID $instrument")
        }
        var username: String? = null
        if (user != null) {
            val instrumentUser = userDao.getById(user)
            username = instrumentUser?.username
        }

        return InstrumentUsage(
            id = id,
            instrument = instrument.id,
            timeStarted = timeStarted,
            timeEnded = timeEnded,
            description = description,
            approved = approved,
            maintenance = maintenance,
            annotation = annotation,
            createdAt = createdAt,
            updatedAt = updatedAt,
            user = username,
            approvedBy = approvedBy
        )
    }


}