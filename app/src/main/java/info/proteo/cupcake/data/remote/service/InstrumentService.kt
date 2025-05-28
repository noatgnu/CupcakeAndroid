package info.proteo.cupcake.data.remote.service

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import info.proteo.cupcake.data.local.dao.instrument.InstrumentDao
import info.proteo.cupcake.data.local.dao.instrument.SupportInformationDao
import info.proteo.cupcake.data.local.entity.instrument.InstrumentEntity
import info.proteo.cupcake.data.remote.model.LimitOffsetResponse
import info.proteo.cupcake.data.remote.model.instrument.Instrument
import info.proteo.cupcake.data.remote.model.instrument.SupportInformation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

@JsonClass(generateAdapter = true)
data class InstrumentCreateRequest(
    val name: String,
    val description: String
)

@JsonClass(generateAdapter = true)
data class InstrumentUpdatePermissionRequest(
    val user: String,
    @Json(name="can_manage") val canManage: Boolean,
    @Json(name="can_book") val canBook: Boolean,
    @Json(name="can_view") val canView: Boolean
)

@JsonClass(generateAdapter = true)
data class InstrumentPermission(
    @Json(name = "can_manage") val canManage: Boolean = false,
    @Json(name = "can_book") val canBook: Boolean = false,
    @Json(name = "can_view") val canView: Boolean = false
)

@JsonClass(generateAdapter = true)
data class DelayUsageRequest(
    @Json(name = "start_date") val startDate: String,
    val days: Int
)

@JsonClass(generateAdapter = true)
data class SupportInformationIdRequest(
    @Json(name = "id") val supportInfoId: Int
)

@JsonClass(generateAdapter = true)
data class SupportInformationAdditionResponse(
    @Json(name = "message") val message: String?,
    @Json(name = "status") val status: String?,
    @Json(name = "error") val error: String?,
)

@JsonClass(generateAdapter = true)
data class MaintenanceStatusResponse(
    @Json(name = "is_overdue") val isOverdue: Boolean,
    @Json(name = "next_maintenance_date") val nextMaintenanceDate: String?,
    @Json(name = "days_until_next_maintenance") val daysUntilNextMaintenance: String?,
    @Json(name = "overdue_days") val overdueDays: Int?,
)

@JsonClass(generateAdapter = true)
data class TriggerInstrumentCheckRequest(
    @Json(name = "instrument_id") val instrumentId: Int? = null,
    @Json(name = "days_before_warranty_warning") val daysBeforeWarrantyWarning: Int? = null,
    @Json(name = "days_before_maintenance_warning") val daysBeforeMaintenanceWarning: Int? = null,
    @Json(name = "instance_id") val instanceId: Int? = null
)

interface InstrumentApiService {
    @GET("api/instrument/")
    suspend fun getInstruments(
        @Query("search") search: String? = null,
        @Query("ordering") ordering: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): LimitOffsetResponse<Instrument>

    @GET("api/instrument/{id}/")
    suspend fun getInstrument(@Path("id") id: Int): Instrument

    @POST("api/instrument/")
    suspend fun createInstrument(@Body data: InstrumentCreateRequest): Instrument

    @PUT("api/instrument/{id}/")
    suspend fun updateInstrument(
        @Path("id") id: Int,
        @Body data: Instrument
    ): Instrument

    @DELETE("api/instrument/{id}/")
    suspend fun deleteInstrument(@Path("id") id: Int)

    @POST("api/instrument/{id}/assign_instrument_permission/")
    suspend fun assignInstrumentPermission(
        @Path("id") id: Int,
        @Body permission: InstrumentUpdatePermissionRequest
    )

    @GET("api/instrument/{id}/get_instrument_permission/")
    suspend fun getInstrumentPermission(@Path("id") id: Int): InstrumentPermission

    @GET("api/instrument/{id}/get_instrument_permission_for/")
    suspend fun getInstrumentPermissionFor(
        @Path("id") id: Int,
        @Query("user") username: String
    ): InstrumentPermission

    @POST("api/instrument/{id}/delay_usage/")
    suspend fun delayUsage(
        @Path("id") id: Int,
        @Body delayData: DelayUsageRequest
    ): Instrument

    @POST("api/instrument/{id}/add_support_information/")
    suspend fun addSupportInformation(
        @Path("id") id: Int,
        @Body supportInfo: SupportInformationIdRequest
    ): SupportInformationAdditionResponse

    @POST("api/instrument/{id}/remove_support_information/")
    suspend fun removeSupportInformation(
        @Path("id") id: Int,
        @Body supportInfo: SupportInformationIdRequest
    ): SupportInformationAdditionResponse

    @GET("api/instrument/{id}/list_support_information/")
    suspend fun listSupportInformation(@Path("id") id: Int): List<SupportInformation>

    @POST("api/instrument/{id}/create_support_information/")
    suspend fun createSupportInformation(
        @Path("id") id: Int,
        @Body supportInfo: SupportInformation
    ): SupportInformation

    @GET("api/instrument/{id}/get_maintenance_status/")
    suspend fun getMaintenanceStatus(@Path("id") id: Int): MaintenanceStatusResponse

    @POST("api/instrument/{id}/notify_slack/")
    suspend fun notifySlack(
        @Path("id") id: Int,
        @Body notification: SupportInformationAdditionResponse
    ): SupportInformationAdditionResponse

    @POST("api/instrument/trigger_instrument_check/")
    suspend fun triggerInstrumentCheck(
        @Body checkData: TriggerInstrumentCheckRequest
    ): SupportInformationAdditionResponse
}

interface InstrumentService {
    suspend fun getInstruments(search: String? = null, ordering: String? = null, limit: Int? = null, offset: Int? = null): Result<LimitOffsetResponse<Instrument>>
    suspend fun getInstrument(id: Int): Result<Instrument>
    suspend fun createInstrument(name: String, description: String): Result<Instrument>
    suspend fun updateInstrument(id: Int, instrument: Instrument): Result<Instrument>
    suspend fun deleteInstrument(id: Int): Result<Unit>
    suspend fun assignInstrumentPermission(id: Int, user: String, canManage: Boolean, canBook: Boolean, canView: Boolean): Result<Unit>
    suspend fun getInstrumentPermission(id: Int): Result<InstrumentPermission>
    suspend fun getInstrumentPermissionFor(id: Int, username: String): Result<InstrumentPermission>
    suspend fun delayUsage(id: Int, startDate: String, days: Int): Result<Instrument>
    suspend fun addSupportInformation(id: Int, supportInfoId: Int): Result<SupportInformationAdditionResponse>
    suspend fun removeSupportInformation(id: Int, supportInfoId: Int): Result<SupportInformationAdditionResponse>
    suspend fun listSupportInformation(id: Int): Result<List<SupportInformation>>
    suspend fun createSupportInformation(id: Int, supportInfo: SupportInformation): Result<SupportInformation>
    suspend fun getMaintenanceStatus(id: Int): Result<MaintenanceStatusResponse>
    suspend fun notifySlack(id: Int, message: String, urgent: Boolean = false, status: String? = null): Result<SupportInformationAdditionResponse>
    suspend fun triggerInstrumentCheck(instrumentId: Int? = null, daysBeforeWarrantyWarning: Int? = null, daysBeforeMaintenanceWarning: Int? = null, instanceId: String? = null): Result<SupportInformationAdditionResponse>
}


@Singleton
class InstrumentServiceImpl @Inject constructor(
    private val apiService: InstrumentApiService,
    private val instrumentDao: InstrumentDao,
    private val supportInformationDao: SupportInformationDao
) : InstrumentService {

    override suspend fun getInstruments(search: String?, ordering: String?, limit: Int?, offset: Int?): Result<LimitOffsetResponse<Instrument>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getInstruments(search, ordering, limit, offset)
                response.results.forEach { instrument ->
                    cacheInstrument(instrument)
                }
                Result.success(response)
            } catch (e: Exception) {
                try {
                    val cachedInstruments = instrumentDao.getAllInstruments().first()
                    val instruments = cachedInstruments.map { loadInstrument(it) }
                    val limitOffsetResponse = LimitOffsetResponse(
                        count = instruments.size,
                        next = null,
                        previous = null,
                        results = instruments
                    )
                    Result.success(limitOffsetResponse)
                } catch (cacheException: Exception) {
                    Result.failure(e)
                }
            }
        }
    }

    override suspend fun getInstrument(id: Int): Result<Instrument> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getInstrument(id)
                cacheInstrument(response)
                Result.success(response)
            } catch (e: Exception) {
                try {
                    val cachedInstrument = instrumentDao.getById(id)
                    if (cachedInstrument != null) {
                        Result.success(loadInstrument(cachedInstrument))
                    } else {
                        Result.failure(e)
                    }
                } catch (cacheException: Exception) {
                    Result.failure(e)
                }
            }
        }
    }

    override suspend fun createInstrument(name: String, description: String): Result<Instrument> {
        return withContext(Dispatchers.IO) {
            try {
                val request = InstrumentCreateRequest(name, description)
                val response = apiService.createInstrument(request)
                cacheInstrument(response)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateInstrument(id: Int, instrument: Instrument): Result<Instrument> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateInstrument(id, instrument)
                cacheInstrument(response)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteInstrument(id: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.deleteInstrument(id)
                // Remove from cache
                instrumentDao.getById(id)?.let { instrumentDao.delete(it) }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun assignInstrumentPermission(
        id: Int,
        user: String,
        canManage: Boolean,
        canBook: Boolean,
        canView: Boolean
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val request = InstrumentUpdatePermissionRequest(user, canManage, canBook, canView)
                apiService.assignInstrumentPermission(id, request)
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getInstrumentPermission(id: Int): Result<InstrumentPermission> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getInstrumentPermission(id)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getInstrumentPermissionFor(id: Int, username: String): Result<InstrumentPermission> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getInstrumentPermissionFor(id, username)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun delayUsage(id: Int, startDate: String, days: Int): Result<Instrument> {
        return withContext(Dispatchers.IO) {
            try {
                val request = DelayUsageRequest(startDate, days)
                val response = apiService.delayUsage(id, request)
                cacheInstrument(response)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun addSupportInformation(id: Int, supportInfoId: Int): Result<SupportInformationAdditionResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = SupportInformationIdRequest(supportInfoId)
                val response = apiService.addSupportInformation(id, request)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun removeSupportInformation(id: Int, supportInfoId: Int): Result<SupportInformationAdditionResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = SupportInformationIdRequest(supportInfoId)
                val response = apiService.removeSupportInformation(id, request)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun listSupportInformation(id: Int): Result<List<SupportInformation>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.listSupportInformation(id)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun createSupportInformation(id: Int, supportInfo: SupportInformation): Result<SupportInformation> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.createSupportInformation(id, supportInfo)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getMaintenanceStatus(id: Int): Result<MaintenanceStatusResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getMaintenanceStatus(id)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun notifySlack(
        id: Int,
        message: String,
        urgent: Boolean,
        status: String?
    ): Result<SupportInformationAdditionResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val notification = SupportInformationAdditionResponse(
                    message = message,
                    status = status,
                    error = null
                )
                val response = apiService.notifySlack(id, notification)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun triggerInstrumentCheck(
        instrumentId: Int?,
        daysBeforeWarrantyWarning: Int?,
        daysBeforeMaintenanceWarning: Int?,
        instanceId: String?
    ): Result<SupportInformationAdditionResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val request = TriggerInstrumentCheckRequest(
                    instrumentId = instrumentId,
                    daysBeforeWarrantyWarning = daysBeforeWarrantyWarning,
                    daysBeforeMaintenanceWarning = daysBeforeMaintenanceWarning,
                    instanceId = null // Note: instanceId type mismatch between String and Int
                )
                val response = apiService.triggerInstrumentCheck(request)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun cacheInstrument(instrument: Instrument) {
        val entity = InstrumentEntity(
            id = instrument.id,
            maxDaysAheadPreApproval = instrument.maxDaysAheadPreApproval,
            maxDaysWithinUsagePreApproval = instrument.maxDaysWithinUsagePreApproval,
            instrumentName = instrument.instrumentName ?: "",
            instrumentDescription = instrument.instrumentDescription,
            createdAt = instrument.createdAt,
            updatedAt = instrument.updatedAt,
            enabled = instrument.enabled,
            image = instrument.image,
            lastWarrantyNotificationSent = instrument.lastWarrantyNotificationSent,
            lastMaintenanceNotificationSent = instrument.lastMaintenanceNotificationSent,
            daysBeforeWarrantyNotification = instrument.daysBeforeWarrantyNotification,
            daysBeforeMaintenanceNotification = instrument.daysBeforeMaintenanceNotification
        )
        instrumentDao.insert(entity)
    }

    private fun loadInstrument(entity: InstrumentEntity): Instrument {
        return Instrument(
            id = entity.id,
            maxDaysAheadPreApproval = entity.maxDaysAheadPreApproval,
            maxDaysWithinUsagePreApproval = entity.maxDaysWithinUsagePreApproval,
            instrumentName = entity.instrumentName,
            instrumentDescription = entity.instrumentDescription,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            enabled = entity.enabled,
            metadataColumns = null,
            annotationFolders = null,
            image = entity.image,
            supportInformation = null,
            lastWarrantyNotificationSent = entity.lastWarrantyNotificationSent,
            lastMaintenanceNotificationSent = entity.lastMaintenanceNotificationSent,
            daysBeforeWarrantyNotification = entity.daysBeforeWarrantyNotification,
            daysBeforeMaintenanceNotification = entity.daysBeforeMaintenanceNotification
        )
    }
}