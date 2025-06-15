package info.proteo.cupcake.communication.service

import android.content.Context
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.squareup.moshi.Moshi
import info.proteo.cupcake.communication.constants.DataLayerConstants
import info.proteo.cupcake.shared.model.TimeKeeperData
import kotlinx.coroutines.tasks.await

/**
 * Base implementation of TimeKeeperCommunicationService with common functionality
 * for both phone and wearable devices
 */
abstract class BaseTimeKeeperCommunicationService(
    protected val context: Context,
    protected val moshi: Moshi
) : TimeKeeperCommunicationService {

    protected val dataClient: DataClient by lazy { Wearable.getDataClient(context) }

    private val dataChangedListener = object : DataClient.OnDataChangedListener {
        override fun onDataChanged(dataEvents: DataEventBuffer) {
            processDataEvents(dataEvents)
        }
    }

    override fun startListening() {
        dataClient.addListener(dataChangedListener)
    }

    override fun stopListening() {
        dataClient.removeListener(dataChangedListener)
    }

    override suspend fun sendTimeKeeperData(data: TimeKeeperData) {
        val adapter = moshi.adapter(TimeKeeperData::class.java)
        val json = adapter.toJson(data)

        val request = PutDataMapRequest.create(DataLayerConstants.PATH_TIMEKEEPER).apply {
            dataMap.putString(DataLayerConstants.KEY_TIMEKEEPER_DATA, json)
        }.asPutDataRequest().setUrgent()

        dataClient.putDataItem(request).await()
    }

    override suspend fun sendAction(action: String) {
        val request = PutDataMapRequest.create(DataLayerConstants.PATH_TIMEKEEPER_ACTION).apply {
            dataMap.putString(DataLayerConstants.KEY_ACTION, action)
        }.asPutDataRequest().setUrgent()

        dataClient.putDataItem(request).await()
    }

    /**
     * Process data events from the Wear Data Layer
     */
    protected fun processDataEvents(dataEvents: DataEventBuffer) {
        dataEvents.forEach { event ->
            if (event.type == DataEvent.TYPE_CHANGED) {
                when (event.dataItem.uri.path) {
                    DataLayerConstants.PATH_TIMEKEEPER -> {
                        val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                        val jsonData = dataMap.getString(DataLayerConstants.KEY_TIMEKEEPER_DATA)
                        jsonData?.let {
                            val adapter = moshi.adapter(TimeKeeperData::class.java)
                            val timeKeeperData = adapter.fromJson(it)
                            onTimeKeeperDataReceived(timeKeeperData)
                        }
                    }
                    DataLayerConstants.PATH_TIMEKEEPER_ACTION -> {
                        val dataMap = DataMapItem.fromDataItem(event.dataItem).dataMap
                        val action = dataMap.getString(DataLayerConstants.KEY_ACTION)
                        action?.let { onActionReceived(it) }
                    }
                }
            }
        }
    }

    /**
     * Called when TimeKeeper data is received from the connected device
     */
    protected abstract fun onTimeKeeperDataReceived(data: TimeKeeperData?)

    /**
     * Called when an action command is received from the connected device
     */
    protected abstract fun onActionReceived(action: String)
}
