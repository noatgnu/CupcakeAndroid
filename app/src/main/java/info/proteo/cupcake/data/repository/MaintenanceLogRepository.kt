package info.proteo.cupcake.data.repository

import android.util.Log
import info.proteo.cupcake.data.remote.service.MaintenanceLogService
import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import info.proteo.cupcake.shared.data.model.maintenance.MaintenanceLog
import info.proteo.cupcake.shared.data.model.maintenance.MaintenanceLogRequest
import info.proteo.cupcake.shared.data.model.maintenance.MaintenanceStatus
import info.proteo.cupcake.shared.data.model.maintenance.MaintenanceStatusesResponse
import info.proteo.cupcake.shared.data.model.maintenance.MaintenanceType
import info.proteo.cupcake.shared.data.model.maintenance.MaintenanceTypesResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MaintenanceLogRepository @Inject constructor(
    private val maintenanceLogService: MaintenanceLogService
) {

    /**
     * Get maintenance logs with optional filtering
     */
    fun getMaintenanceLogs(
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
    ): Flow<Result<LimitOffsetResponse<MaintenanceLog>>> = flow {
        try {
            Log.d("MaintenanceLogRepository", "Fetching maintenance logs with filters: instrumentId=$instrumentId, type=$maintenanceType, status=$status")
            val result = maintenanceLogService.getMaintenanceLogs(
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
            emit(result)
        } catch (e: Exception) {
            Log.e("MaintenanceLogRepository", "Error fetching maintenance logs", e)
            emit(Result.failure(e))
        }
    }

    /**
     * Get maintenance logs for a specific instrument
     */
    fun getMaintenanceLogsForInstrument(
        instrumentId: Long,
        limit: Int? = null,
        offset: Int? = null
    ): Flow<Result<LimitOffsetResponse<MaintenanceLog>>> = flow {
        try {
            Log.d("MaintenanceLogRepository", "Fetching maintenance logs for instrument: $instrumentId")
            val result = maintenanceLogService.getMaintenanceLogs(
                instrumentId = instrumentId,
                ordering = "-maintenance_date",
                limit = limit,
                offset = offset
            )
            emit(result)
        } catch (e: Exception) {
            Log.e("MaintenanceLogRepository", "Error fetching maintenance logs for instrument $instrumentId", e)
            emit(Result.failure(e))
        }
    }

    /**
     * Get maintenance log templates
     */
    fun getMaintenanceLogTemplates(
        instrumentId: Long? = null,
        limit: Int? = null,
        offset: Int? = null
    ): Flow<Result<LimitOffsetResponse<MaintenanceLog>>> = flow {
        try {
            Log.d("MaintenanceLogRepository", "Fetching maintenance log templates")
            val result = maintenanceLogService.getMaintenanceLogs(
                instrumentId = instrumentId,
                isTemplate = true,
                ordering = "maintenance_type,maintenance_description",
                limit = limit,
                offset = offset
            )
            emit(result)
        } catch (e: Exception) {
            Log.e("MaintenanceLogRepository", "Error fetching maintenance log templates", e)
            emit(Result.failure(e))
        }
    }

    /**
     * Get a specific maintenance log by ID
     */
    fun getMaintenanceLog(id: Long): Flow<Result<MaintenanceLog>> = flow {
        try {
            Log.d("MaintenanceLogRepository", "Fetching maintenance log: $id")
            val result = maintenanceLogService.getMaintenanceLog(id)
            emit(result)
        } catch (e: Exception) {
            Log.e("MaintenanceLogRepository", "Error fetching maintenance log $id", e)
            emit(Result.failure(e))
        }
    }

    /**
     * Create a new maintenance log
     */
    fun createMaintenanceLog(request: MaintenanceLogRequest): Flow<Result<MaintenanceLog>> = flow {
        try {
            Log.d("MaintenanceLogRepository", "Creating maintenance log for instrument: ${request.instrumentId}")
            val result = maintenanceLogService.createMaintenanceLog(request)
            emit(result)
        } catch (e: Exception) {
            Log.e("MaintenanceLogRepository", "Error creating maintenance log", e)
            emit(Result.failure(e))
        }
    }

    /**
     * Update an existing maintenance log
     */
    fun updateMaintenanceLog(id: Long, request: MaintenanceLogRequest): Flow<Result<MaintenanceLog>> = flow {
        try {
            Log.d("MaintenanceLogRepository", "Updating maintenance log: $id")
            val result = maintenanceLogService.updateMaintenanceLog(id, request)
            emit(result)
        } catch (e: Exception) {
            Log.e("MaintenanceLogRepository", "Error updating maintenance log $id", e)
            emit(Result.failure(e))
        }
    }

    /**
     * Partially update a maintenance log
     */
    fun patchMaintenanceLog(id: Long, request: MaintenanceLogRequest): Flow<Result<MaintenanceLog>> = flow {
        try {
            Log.d("MaintenanceLogRepository", "Patching maintenance log: $id")
            val result = maintenanceLogService.patchMaintenanceLog(id, request)
            emit(result)
        } catch (e: Exception) {
            Log.e("MaintenanceLogRepository", "Error patching maintenance log $id", e)
            emit(Result.failure(e))
        }
    }

    /**
     * Delete a maintenance log
     */
    fun deleteMaintenanceLog(id: Long): Flow<Result<Unit>> = flow {
        try {
            Log.d("MaintenanceLogRepository", "Deleting maintenance log: $id")
            val result = maintenanceLogService.deleteMaintenanceLog(id)
            emit(result)
        } catch (e: Exception) {
            Log.e("MaintenanceLogRepository", "Error deleting maintenance log $id", e)
            emit(Result.failure(e))
        }
    }

    /**
     * Update only the status of a maintenance log
     */
    fun updateMaintenanceLogStatus(id: Long, status: String): Flow<Result<MaintenanceLog>> = flow {
        try {
            Log.d("MaintenanceLogRepository", "Updating status for maintenance log $id to $status")
            val result = maintenanceLogService.updateMaintenanceLogStatus(id, status)
            emit(result)
        } catch (e: Exception) {
            Log.e("MaintenanceLogRepository", "Error updating status for maintenance log $id", e)
            emit(Result.failure(e))
        }
    }

    /**
     * Create a maintenance log from a template
     */
    fun createFromTemplate(templateId: Long, request: MaintenanceLogRequest): Flow<Result<MaintenanceLog>> = flow {
        try {
            Log.d("MaintenanceLogRepository", "Creating maintenance log from template: $templateId")
            val result = maintenanceLogService.createFromTemplate(templateId, request)
            emit(result)
        } catch (e: Exception) {
            Log.e("MaintenanceLogRepository", "Error creating from template $templateId", e)
            emit(Result.failure(e))
        }
    }

    /**
     * Get available maintenance types
     */
    fun getMaintenanceTypes(): Flow<Result<List<MaintenanceType>>> = flow {
        try {
            Log.d("MaintenanceLogRepository", "Fetching maintenance types")
            val result = maintenanceLogService.getMaintenanceTypes()
            emit(result)
        } catch (e: Exception) {
            Log.e("MaintenanceLogRepository", "Error fetching maintenance types", e)
            emit(Result.failure(e))
        }
    }

    /**
     * Get available maintenance statuses
     */
    fun getMaintenanceStatuses(): Flow<Result<List<MaintenanceStatus>>> = flow {
        try {
            Log.d("MaintenanceLogRepository", "Fetching maintenance statuses")
            val result = maintenanceLogService.getMaintenanceStatuses()
            emit(result)
        } catch (e: Exception) {
            Log.e("MaintenanceLogRepository", "Error fetching maintenance statuses", e)
            emit(Result.failure(e))
        }
    }

    /**
     * Search maintenance logs by description or notes
     */
    fun searchMaintenanceLogs(
        query: String,
        instrumentId: Long? = null,
        limit: Int? = null,
        offset: Int? = null
    ): Flow<Result<LimitOffsetResponse<MaintenanceLog>>> = flow {
        try {
            Log.d("MaintenanceLogRepository", "Searching maintenance logs with query: $query")
            val result = maintenanceLogService.getMaintenanceLogs(
                instrumentId = instrumentId,
                search = query,
                ordering = "-maintenance_date",
                limit = limit,
                offset = offset
            )
            emit(result)
        } catch (e: Exception) {
            Log.e("MaintenanceLogRepository", "Error searching maintenance logs", e)
            emit(Result.failure(e))
        }
    }

    /**
     * Get pending maintenance logs for an instrument
     */
    fun getPendingMaintenanceLogs(instrumentId: Long): Flow<Result<LimitOffsetResponse<MaintenanceLog>>> = flow {
        try {
            Log.d("MaintenanceLogRepository", "Fetching pending maintenance logs for instrument: $instrumentId")
            val result = maintenanceLogService.getMaintenanceLogs(
                instrumentId = instrumentId,
                status = "pending",
                ordering = "maintenance_date",
                limit = 50
            )
            emit(result)
        } catch (e: Exception) {
            Log.e("MaintenanceLogRepository", "Error fetching pending maintenance logs", e)
            emit(Result.failure(e))
        }
    }

    /**
     * Get maintenance logs in a date range
     */
    fun getMaintenanceLogsInDateRange(
        instrumentId: Long? = null,
        startDate: String,
        endDate: String,
        limit: Int? = null,
        offset: Int? = null
    ): Flow<Result<LimitOffsetResponse<MaintenanceLog>>> = flow {
        try {
            Log.d("MaintenanceLogRepository", "Fetching maintenance logs from $startDate to $endDate")
            val result = maintenanceLogService.getMaintenanceLogs(
                instrumentId = instrumentId,
                startDate = startDate,
                endDate = endDate,
                ordering = "-maintenance_date",
                limit = limit,
                offset = offset
            )
            emit(result)
        } catch (e: Exception) {
            Log.e("MaintenanceLogRepository", "Error fetching maintenance logs in date range", e)
            emit(Result.failure(e))
        }
    }
}