package info.proteo.cupcake.wearos.service

import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.communication.service.WearableTimeKeeperCommunicationService
import javax.inject.Inject

/**
 * Service that listens for TimeKeeper data updates from the phone
 */
@AndroidEntryPoint
class DataLayerListenerService : WearableListenerService() {

    @Inject
    lateinit var communicationService: WearableTimeKeeperCommunicationService

    override fun onCreate() {
        super.onCreate()
        communicationService.startListening()
    }

    override fun onDestroy() {
        super.onDestroy()
        communicationService.stopListening()
    }
}
