package info.proteo.cupcake.data.repository

import info.proteo.cupcake.data.remote.model.LimitOffsetResponse
import info.proteo.cupcake.data.remote.model.instrument.Instrument
import info.proteo.cupcake.data.remote.model.instrument.SupportInformation
import info.proteo.cupcake.data.remote.service.InstrumentPermission
import info.proteo.cupcake.data.remote.service.InstrumentService
import info.proteo.cupcake.data.remote.service.MaintenanceStatusResponse
import info.proteo.cupcake.data.remote.service.SupportInformationAdditionResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstrumentRepository @Inject constructor(
    private val instrumentService: InstrumentService
) {
    fun getInstruments(search: String? = null, ordering: String? = null, limit: Int? = null, offset: Int? = null): Flow<Result<LimitOffsetResponse<Instrument>>> = flow {
        emit(instrumentService.getInstruments(search, ordering))
    }

    fun getInstrument(id: Int): Flow<Result<Instrument>> = flow {
        emit(instrumentService.getInstrument(id))
    }

    suspend fun createInstrument(name: String, description: String): Result<Instrument> {
        return instrumentService.createInstrument(name, description)
    }

    suspend fun updateInstrument(id: Int, instrument: Instrument): Result<Instrument> {
        return instrumentService.updateInstrument(id, instrument)
    }

    suspend fun deleteInstrument(id: Int): Result<Unit> {
        return instrumentService.deleteInstrument(id)
    }

    suspend fun assignInstrumentPermission(
        id: Int,
        user: String,
        canManage: Boolean,
        canBook: Boolean,
        canView: Boolean
    ): Result<Unit> {
        return instrumentService.assignInstrumentPermission(id, user, canManage, canBook, canView)
    }

    fun getInstrumentPermission(id: Int): Flow<Result<InstrumentPermission>> = flow {
        emit(instrumentService.getInstrumentPermission(id))
    }

    fun getInstrumentPermissionFor(id: Int, username: String): Flow<Result<InstrumentPermission>> = flow {
        emit(instrumentService.getInstrumentPermissionFor(id, username))
    }

    suspend fun delayUsage(id: Int, startDate: String, days: Int): Result<Instrument> {
        return instrumentService.delayUsage(id, startDate, days)
    }

    suspend fun addSupportInformation(id: Int, supportInfoId: Int): Result<SupportInformationAdditionResponse> {
        return instrumentService.addSupportInformation(id, supportInfoId)
    }

    suspend fun removeSupportInformation(id: Int, supportInfoId: Int): Result<SupportInformationAdditionResponse> {
        return instrumentService.removeSupportInformation(id, supportInfoId)
    }

    fun listSupportInformation(id: Int): Flow<Result<List<SupportInformation>>> = flow {
        emit(instrumentService.listSupportInformation(id))
    }

    suspend fun createSupportInformation(id: Int, supportInfo: SupportInformation): Result<SupportInformation> {
        return instrumentService.createSupportInformation(id, supportInfo)
    }

    fun getMaintenanceStatus(id: Int): Flow<Result<MaintenanceStatusResponse>> = flow {
        emit(instrumentService.getMaintenanceStatus(id))
    }

    suspend fun notifySlack(
        id: Int,
        message: String,
        urgent: Boolean = false,
        status: String? = null
    ): Result<SupportInformationAdditionResponse> {
        return instrumentService.notifySlack(id, message, urgent, status)
    }

    suspend fun triggerInstrumentCheck(
        instrumentId: Int? = null,
        daysBeforeWarrantyWarning: Int? = null,
        daysBeforeMaintenanceWarning: Int? = null,
        instanceId: String? = null
    ): Result<SupportInformationAdditionResponse> {
        return instrumentService.triggerInstrumentCheck(
            instrumentId,
            daysBeforeWarrantyWarning,
            daysBeforeMaintenanceWarning,
            instanceId
        )
    }

    suspend fun saveInstrument(instrument: Instrument): Result<Instrument> {
        return if (instrument.id > 0) {
            updateInstrument(instrument.id, instrument)
        } else {
            createInstrument(
                instrument.instrumentName ?: "",
                instrument.instrumentDescription ?: ""
            )
        }
    }
}