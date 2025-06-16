package info.proteo.cupcake.data.repository

import info.proteo.cupcake.data.remote.service.CreateInstrumentUsageRequest

import info.proteo.cupcake.data.remote.service.ExportUsageRequest
import info.proteo.cupcake.data.remote.service.ExportUsageResponse
import info.proteo.cupcake.data.remote.service.InstrumentUsageService
import info.proteo.cupcake.data.remote.service.PatchInstrumentUsageRequest
import info.proteo.cupcake.data.remote.service.UpdateInstrumentUsageRequest
import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import info.proteo.cupcake.shared.data.model.instrument.DelayUsageRequest
import info.proteo.cupcake.shared.data.model.instrument.InstrumentUsage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class InstrumentUsageRepository @Inject constructor(
    private val instrumentUsageService: InstrumentUsageService
) {

    fun getInstrumentUsages(
        limit: Int? = null,
        offset: Int? = null,
        timeStarted: String? = null,
        timeEnded: String? = null,
        instrument: String? = null,
        users: String? = null,
        searchType: String? = null,
        search: String? = null,
        ordering: String? = null
    ): Flow<Result<LimitOffsetResponse<InstrumentUsage>>> = flow {
        emit(
            instrumentUsageService.getInstrumentUsages(
                limit = limit,
                offset = offset,
                timeStarted = timeStarted,
                timeEnded = timeEnded,
                instrument = instrument,
                users = users,
                searchType = searchType,
                search = search,
                ordering = ordering
            )
        )
    }

    suspend fun createInstrumentUsage(usageRequest: CreateInstrumentUsageRequest): Result<InstrumentUsage> {
        return instrumentUsageService.createInstrumentUsage(usageRequest)
    }

    fun getInstrumentUsageById(id: Int): Flow<Result<InstrumentUsage>> = flow {
        emit(instrumentUsageService.getInstrumentUsageById(id))
    }

    suspend fun updateInstrumentUsage(
        id: Int,
        usageRequest: UpdateInstrumentUsageRequest
    ): Result<InstrumentUsage> {
        return instrumentUsageService.updateInstrumentUsage(id, usageRequest)
    }

    suspend fun partialUpdateInstrumentUsage(
        id: Int,
        usageRequest: PatchInstrumentUsageRequest
    ): Result<InstrumentUsage> {
        return instrumentUsageService.partialUpdateInstrumentUsage(id, usageRequest)
    }

    suspend fun deleteInstrumentUsage(id: Int): Result<Unit> {
        return instrumentUsageService.deleteInstrumentUsage(id)
    }

    fun getCurrentUserInstrumentUsage(
        limit: Int? = null,
        offset: Int? = null
    ): Flow<Result<LimitOffsetResponse<InstrumentUsage>>> = flow {
        emit(instrumentUsageService.getCurrentUserInstrumentUsage(limit, offset))
    }

    suspend fun deleteUsageAction(id: Int): Result<Unit> {
        return instrumentUsageService.deleteUsageAction(id)
    }

    suspend fun delayUsage(id: Int, delayRequest: DelayUsageRequest): Result<InstrumentUsage> {
        return instrumentUsageService.delayUsage(id, delayRequest)
    }

    suspend fun exportUsage(exportRequest: ExportUsageRequest): Result<ExportUsageResponse> {
        return instrumentUsageService.exportUsage(exportRequest)
    }

    suspend fun approveUsageToggle(id: Int): Result<InstrumentUsage> {
        return instrumentUsageService.approveUsageToggle(id)
    }
}