package info.proteo.cupcake.data.repository

import info.proteo.cupcake.data.remote.model.LimitOffsetResponse
import info.proteo.cupcake.data.remote.model.protocol.TimeKeeper
import info.proteo.cupcake.data.remote.service.TimeKeeperService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeKeeperRepository @Inject constructor(
    private val timeKeeperService: TimeKeeperService
) {
    suspend fun getTimeKeepers(
        offset: Int? = null,
        limit: Int? = null,
        sessionId: Int? = null,
        stepId: Int? = null,
        started: Boolean? = null
    ): Result<LimitOffsetResponse<TimeKeeper>> {
        return timeKeeperService.getTimeKeepers(offset, limit, sessionId, stepId, started)
    }

    suspend fun getTimeKeeperById(id: Int): Result<TimeKeeper> {
        return timeKeeperService.getTimeKeeperById(id)
    }



    suspend fun createTimeKeeper(timeKeeper: TimeKeeper): Result<TimeKeeper> {
        return timeKeeperService.createTimeKeeper(timeKeeper)
    }

    suspend fun updateTimeKeeper(id: Int, timeKeeper: TimeKeeper): Result<TimeKeeper> {
        return timeKeeperService.updateTimeKeeper(id, timeKeeper)
    }

    suspend fun deleteTimeKeeper(id: Int): Result<Unit> {
        return timeKeeperService.deleteTimeKeeper(id)
    }

    suspend fun saveTimeKeeper(timeKeeper: TimeKeeper): Result<TimeKeeper> {
        return if (timeKeeper.id > 0) {
            updateTimeKeeper(timeKeeper.id, timeKeeper)
        } else {
            createTimeKeeper(timeKeeper)
        }
    }
}