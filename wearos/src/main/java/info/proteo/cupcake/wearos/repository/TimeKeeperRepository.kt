package info.proteo.cupcake.wearos.repository

import info.proteo.cupcake.communication.service.WearableTimeKeeperCommunicationService
import info.proteo.cupcake.shared.model.TimeKeeperData
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TimeKeeperRepository @Inject constructor(
    private val communicationService: WearableTimeKeeperCommunicationService
) {
    fun observeTimeKeeper(): Flow<TimeKeeperData?> = communicationService.timeKeeperFlow

    suspend fun sendAction(action: String) {
        communicationService.sendAction(action)
    }
}