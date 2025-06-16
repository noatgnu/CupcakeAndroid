package info.proteo.cupcake.shared.communication

/**
 * Constants shared between the WearOS app and phone app for data communication
 */
object DataLayerConstants {
    // Paths for Data Layer API
    const val PATH_PROTOCOL_DATA = "/protocol/data"
    const val PATH_PROTOCOL_SYNC_REQUEST = "/protocol/sync_request"
    const val PATH_TIMEKEEPER_DATA = "/timekeeper/data"
    const val PATH_TIMEKEEPER_ACTION = "/timekeeper/action"

    // Keys for data maps
    const val KEY_PROTOCOL_DATA = "protocol_data"
    const val KEY_PROTOCOL_ID = "protocol_id"
    const val KEY_TIMEKEEPER_DATA = "timekeeper_data"
    const val KEY_ACTION = "action"

    // Actions
    const val ACTION_START_TIMER = "start_timer"
    const val ACTION_PAUSE_TIMER = "pause_timer"
    const val ACTION_STOP_TIMER = "stop_timer"

    // Capability names
    const val CAPABILITY_PHONE_APP = "phone_app"
    const val CAPABILITY_WEAR_APP = "wear_app"
}
