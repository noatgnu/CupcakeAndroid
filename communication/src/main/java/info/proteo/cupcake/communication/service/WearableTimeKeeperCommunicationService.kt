package info.proteo.cupcake.communication.service

import android.content.Context
import com.squareup.moshi.Moshi
import dagger.hilt.android.qualifiers.ApplicationContext
import info.proteo.cupcake.shared.model.TimeKeeperData
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Wearable implementation of TimeKeeper communication service
 */
@Singleton
class WearableTimeKeeperCommunicationService @Inject constructor(
    @ApplicationContext context: Context,
    moshi: Moshi
) : BaseTimeKeeperCommunicationService(context, moshi) {

    // Flow of TimeKeeper data received from phone
    private val _timeKeeperFlow = MutableSharedFlow<TimeKeeperData>(replay = 1)
    val timeKeeperFlow: SharedFlow<TimeKeeperData> = _timeKeeperFlow

    override fun onTimeKeeperDataReceived(data: TimeKeeperData?) {
        // Emit TimeKeeper data received from phone to flow
        data?.let {
            _timeKeeperFlow.tryEmit(it)
        }
    }

    override fun onActionReceived(action: String) {
        // Wearable doesn't need to process incoming actions
        // since it's the one sending actions to the phone
    }
}
