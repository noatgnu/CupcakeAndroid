package info.proteo.cupcake.communication.constants

/**
 * Constants used for Wear OS data layer communication
 */
object DataLayerConstants {
    // Paths
    const val PATH_TIMEKEEPER = "/timekeeper"
    const val PATH_TIMEKEEPER_ACTION = "/timekeeper/action"

    // Keys
    const val KEY_TIMEKEEPER_DATA = "timekeeper_data"
    const val KEY_ACTION = "action"

    // Actions
    const val ACTION_START = "start"
    const val ACTION_STOP = "stop"
    const val ACTION_RESET = "reset"
}
