package info.proteo.cupcake.communication.service

import android.content.Context
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.WearableListenerService
import dagger.hilt.android.AndroidEntryPoint
import info.proteo.cupcake.communication.constants.DataLayerConstants
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service that listens for action commands from the Wear OS app
 * This is part of the communication module and handles data layer events
 */
@AndroidEntryPoint
class PhoneActionListenerService : WearableListenerService() {

    // Flow for actions received from the wearable
    private val _actionFlow = MutableSharedFlow<String>(replay = 0)
    val actionFlow: SharedFlow<String> = _actionFlow.asSharedFlow()

    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED) {
                when (event.dataItem.uri.path) {
                    DataLayerConstants.PATH_TIMEKEEPER_ACTION -> {
                        val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                        val action = dataMap.getString(DataLayerConstants.KEY_ACTION)
                        action?.let {
                            coroutineScope.launch {
                                _actionFlow.emit(it)
                            }
                        }
                    }
                }
            }
        }
    }
}
