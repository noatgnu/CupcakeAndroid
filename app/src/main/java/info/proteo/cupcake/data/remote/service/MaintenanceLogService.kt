package info.proteo.cupcake.data.remote.service

import android.util.Log
import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import info.proteo.cupcake.shared.data.model.maintenance.MaintenanceLog
import info.proteo.cupcake.shared.data.model.maintenance.MaintenanceLogRequest
import info.proteo.cupcake.shared.data.model.maintenance.MaintenanceLogStatusUpdate
import info.proteo.cupcake.shared.data.model.maintenance.MaintenanceStatus
import info.proteo.cupcake.shared.data.model.maintenance.MaintenanceStatusesResponse
import info.proteo.cupcake.shared.data.model.maintenance.MaintenanceType
import info.proteo.cupcake.shared.data.model.maintenance.MaintenanceTypesResponse
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

interface MaintenanceLogApiService {
    @GET("api/maintenance_logs/")
    suspend fun getMaintenanceLogs(
        @Query("instrument_id") instrumentId: Long? = null,
        @Query("maintenance_type") maintenanceType: String? = null,
        @Query("status") status: String? = null,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null,
        @Query("is_template") isTemplate: Boolean? = null,
        @Query("search") search: String? = null,
        @Query("ordering") ordering: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): LimitOffsetResponse<MaintenanceLog>

    @GET("api/maintenance_logs/{id}/")
    suspend fun getMaintenanceLog(@Path("id") id: Long): MaintenanceLog

    @POST("api/maintenance_logs/")
    suspend fun createMaintenanceLog(@Body request: MaintenanceLogRequest): MaintenanceLog

    @PUT("api/maintenance_logs/{id}/")
    suspend fun updateMaintenanceLog(
        @Path("id") id: Long,
        @Body request: MaintenanceLogRequest
    ): MaintenanceLog

    @PATCH("api/maintenance_logs/{id}/")
    suspend fun patchMaintenanceLog(
        @Path("id") id: Long,
        @Body request: MaintenanceLogRequest
    ): MaintenanceLog

    @DELETE("api/maintenance_logs/{id}/")
    suspend fun deleteMaintenanceLog(@Path("id") id: Long): Response<Unit>

    @POST("api/maintenance_logs/{id}/update_status/")
    suspend fun updateMaintenanceLogStatus(
        @Path("id") id: Long,
        @Body statusUpdate: MaintenanceLogStatusUpdate
    ): MaintenanceLog

    @GET("api/maintenance_logs/get_maintenance_types/")
    suspend fun getMaintenanceTypes(): List<MaintenanceType>

    @GET("api/maintenance_logs/get_status_types/")
    suspend fun getMaintenanceStatuses(): List<MaintenanceStatus>

    @POST("api/maintenance_logs/{id}/create_from_template/")
    suspend fun createFromTemplate(
        @Path("id") templateId: Long,
        @Body request: MaintenanceLogRequest
    ): MaintenanceLog
}

interface MaintenanceLogService {
    suspend fun getMaintenanceLogs(
        instrumentId: Long? = null,
        maintenanceType: String? = null,
        status: String? = null,
        startDate: String? = null,
        endDate: String? = null,
        isTemplate: Boolean? = null,
        search: String? = null,
        ordering: String? = null,
        limit: Int? = null,
        offset: Int? = null
    ): Result<LimitOffsetResponse<MaintenanceLog>>

    suspend fun getMaintenanceLog(id: Long): Result<MaintenanceLog>
    suspend fun createMaintenanceLog(request: MaintenanceLogRequest): Result<MaintenanceLog>
    suspend fun updateMaintenanceLog(id: Long, request: MaintenanceLogRequest): Result<MaintenanceLog>
    suspend fun patchMaintenanceLog(id: Long, request: MaintenanceLogRequest): Result<MaintenanceLog>
    suspend fun deleteMaintenanceLog(id: Long): Result<Unit>
    suspend fun updateMaintenanceLogStatus(id: Long, status: String): Result<MaintenanceLog>
    suspend fun getMaintenanceTypes(): Result<List<MaintenanceType>>
    suspend fun getMaintenanceStatuses(): Result<List<MaintenanceStatus>>
    suspend fun createFromTemplate(templateId: Long, request: MaintenanceLogRequest): Result<MaintenanceLog>
}

@Singleton
class MaintenanceLogServiceImpl @Inject constructor(
    private val apiService: MaintenanceLogApiService
) : MaintenanceLogService {

    override suspend fun getMaintenanceLogs(
        instrumentId: Long?,
        maintenanceType: String?,
        status: String?,
        startDate: String?,
        endDate: String?,
        isTemplate: Boolean?,
        search: String?,
        ordering: String?,
        limit: Int?,
        offset: Int?
    ): Result<LimitOffsetResponse<MaintenanceLog>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getMaintenanceLogs(
                    instrumentId = instrumentId,
                    maintenanceType = maintenanceType,
                    status = status,
                    startDate = startDate,
                    endDate = endDate,
                    isTemplate = isTemplate,
                    search = search,
                    ordering = ordering,
                    limit = limit,
                    offset = offset
                )
                Log.d("MaintenanceLogService", "Fetched ${response.results.size} maintenance logs from API")
                Result.success(response)
            } catch (e: Exception) {
                Log.e("MaintenanceLogService", "Error fetching maintenance logs", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun getMaintenanceLog(id: Long): Result<MaintenanceLog> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getMaintenanceLog(id)
                Log.d("MaintenanceLogService", "Fetched maintenance log: ${response.id}")
                Result.success(response)
            } catch (e: Exception) {
                Log.e("MaintenanceLogService", "Error fetching maintenance log $id", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun createMaintenanceLog(request: MaintenanceLogRequest): Result<MaintenanceLog> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.createMaintenanceLog(request)
                Log.d("MaintenanceLogService", "Created maintenance log: ${response.id}")
                Result.success(response)
            } catch (e: Exception) {
                Log.e("MaintenanceLogService", "Error creating maintenance log", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun updateMaintenanceLog(id: Long, request: MaintenanceLogRequest): Result<MaintenanceLog> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateMaintenanceLog(id, request)
                Log.d("MaintenanceLogService", "Updated maintenance log: ${response.id}")
                Result.success(response)
            } catch (e: Exception) {
                Log.e("MaintenanceLogService", "Error updating maintenance log $id", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun patchMaintenanceLog(id: Long, request: MaintenanceLogRequest): Result<MaintenanceLog> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.patchMaintenanceLog(id, request)
                Log.d("MaintenanceLogService", "Patched maintenance log: ${response.id}")
                Result.success(response)
            } catch (e: Exception) {
                Log.e("MaintenanceLogService", "Error patching maintenance log $id", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteMaintenanceLog(id: Long): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.deleteMaintenanceLog(id)
                if (response.isSuccessful) {
                    Log.d("MaintenanceLogService", "Deleted maintenance log: $id")
                    Result.success(Unit)
                } else {
                    Log.e("MaintenanceLogService", "Failed to delete maintenance log $id: ${response.code()}")
                    Result.failure(Exception("Failed to delete maintenance log: ${response.code()}"))
                }
            } catch (e: Exception) {
                Log.e("MaintenanceLogService", "Error deleting maintenance log $id", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun updateMaintenanceLogStatus(id: Long, status: String): Result<MaintenanceLog> {
        return withContext(Dispatchers.IO) {
            try {
                val statusUpdate = MaintenanceLogStatusUpdate(status)
                val response = apiService.updateMaintenanceLogStatus(id, statusUpdate)
                Log.d("MaintenanceLogService", "Updated status for maintenance log $id to $status")
                Result.success(response)
            } catch (e: Exception) {
                Log.e("MaintenanceLogService", "Error updating status for maintenance log $id", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun getMaintenanceTypes(): Result<List<MaintenanceType>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getMaintenanceTypes()
                Log.d("MaintenanceLogService", "Fetched ${response.size} maintenance types")
                Result.success(response)
            } catch (e: Exception) {
                Log.e("MaintenanceLogService", "Error fetching maintenance types", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun getMaintenanceStatuses(): Result<List<MaintenanceStatus>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getMaintenanceStatuses()
                Log.d("MaintenanceLogService", "Fetched ${response.size} maintenance statuses")
                Result.success(response)
            } catch (e: Exception) {
                Log.e("MaintenanceLogService", "Error fetching maintenance statuses", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun createFromTemplate(templateId: Long, request: MaintenanceLogRequest): Result<MaintenanceLog> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.createFromTemplate(templateId, request)
                Log.d("MaintenanceLogService", "Created maintenance log from template $templateId: ${response.id}")
                Result.success(response)
            } catch (e: Exception) {
                Log.e("MaintenanceLogService", "Error creating from template $templateId", e)
                Result.failure(e)
            }
        }
    }
}