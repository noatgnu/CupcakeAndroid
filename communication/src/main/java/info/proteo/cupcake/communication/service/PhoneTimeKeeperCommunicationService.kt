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
 * Phone implementation of TimeKeeper communication service
 */
@Singleton
class PhoneTimeKeeperCommunicationService @Inject constructor(
    @ApplicationContext context: Context,
    moshi: Moshi
) : BaseTimeKeeperCommunicationService(context, moshi) {

    // Flow of actions received from wearable
    private val _actionFlow = MutableSharedFlow<String>(replay = 0)
    val actionFlow: SharedFlow<String> = _actionFlow

    override fun onTimeKeeperDataReceived(data: TimeKeeperData?) {
        // Phone doesn't need to process incoming TimeKeeper data
        // since it's the source of truth
    }

    override fun onActionReceived(action: String) {
        // Emit action received from wearable to flow
        _actionFlow.tryEmit(action)
    }
}
