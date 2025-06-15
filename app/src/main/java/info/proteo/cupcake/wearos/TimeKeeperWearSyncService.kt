package info.proteo.cupcake.wearos

import info.proteo.cupcake.communication.service.PhoneTimeKeeperCommunicationService
import info.proteo.cupcake.data.remote.model.protocol.TimeKeeper
import info.proteo.cupcake.data.repository.SessionRepository
import info.proteo.cupcake.data.repository.TimeKeeperRepository
import info.proteo.cupcake.shared.model.TimeKeeperData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service that synchronizes TimeKeeper data from the phone to the Wear OS app
 */
@Singleton
class TimeKeeperSyncService @Inject constructor(
    private val timeKeeperRepository: TimeKeeperRepository,
    private val sessionRepository: SessionRepository,
    private val communicationService: PhoneTimeKeeperCommunicationService
) {
    init {
        // Start listening for commands from wearable
        communicationService.startListening()

        // Set up command processing
        CoroutineScope(Dispatchers.IO).launch {
            communicationService.actionFlow.collect { action ->
                processAction(action)
            }
        }
    }

    suspend fun syncTimeKeeper(timeKeeper: TimeKeeper) {
        // Try to get session name
        val sessionName = timeKeeper.session?.let { sessionId ->
            sessionRepository.getSessions(limit = 1, search = sessionId.toString())
                .getOrNull()?.results?.firstOrNull()?.name
        }

        // Convert to TimeKeeperData
        val timeKeeperData = TimeKeeperData(
            id = timeKeeper.id,
            startTime = timeKeeper.startTime,
            session = timeKeeper.session,
            step = timeKeeper.step,
            started = timeKeeper.started,
            currentDuration = timeKeeper.currentDuration,
            sessionName = sessionName,
            stepName = "Step ${timeKeeper.step}"
        )

        // Send to wearable
        communicationService.sendTimeKeeperData(timeKeeperData)
    }

    private suspend fun processAction(action: String) {
        // Get current active timekeeper
        val timeKeepers = timeKeeperRepository.getTimeKeepers(limit = 1, started = true)

        timeKeepers.getOrNull()?.results?.firstOrNull()?.let { timeKeeper ->
            when (action) {
                info.proteo.cupcake.communication.constants.DataLayerConstants.ACTION_START -> {
                    val updatedTimeKeeper = timeKeeper.copy(started = true)
                    timeKeeperRepository.saveTimeKeeper(updatedTimeKeeper)
                        .getOrNull()?.let { syncTimeKeeper(it) }
                }
                info.proteo.cupcake.communication.constants.DataLayerConstants.ACTION_STOP -> {
                    val updatedTimeKeeper = timeKeeper.copy(started = false)
                    timeKeeperRepository.saveTimeKeeper(updatedTimeKeeper)
                        .getOrNull()?.let { syncTimeKeeper(it) }
                }
                info.proteo.cupcake.communication.constants.DataLayerConstants.ACTION_RESET -> {
                    val updatedTimeKeeper = timeKeeper.copy(
                        started = false,
                        startTime = null,
                        currentDuration = 0
                    )
                    timeKeeperRepository.saveTimeKeeper(updatedTimeKeeper)
                        .getOrNull()?.let { syncTimeKeeper(it) }
                }
            }
        }
    }
}