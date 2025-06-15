package info.proteo.cupcake.communication.service

import info.proteo.cupcake.shared.model.TimeKeeperData

/**
 * Interface for TimeKeeper communication between phone and wearable
 */
interface TimeKeeperCommunicationService {
    /**
     * Send TimeKeeper data to the connected device
     */
    suspend fun sendTimeKeeperData(data: TimeKeeperData)

    /**
     * Send an action command to control the TimeKeeper
     */
    suspend fun sendAction(action: String)

    /**
     * Start listening for data changes
     */
    fun startListening()

    /**
     * Stop listening for data changes
     */
    fun stopListening()
}
