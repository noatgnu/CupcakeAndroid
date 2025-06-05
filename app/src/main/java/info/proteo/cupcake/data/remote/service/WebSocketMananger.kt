package info.proteo.cupcake.data.remote.service

import android.util.Log
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import info.proteo.cupcake.data.local.dao.user.UserPreferencesDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class WebSocketManager @Inject constructor(
    private val webSocketService: WebSocketService,
    private val userPreferencesDao: UserPreferencesDao
) : DefaultLifecycleObserver {
    private val TAG = "WebSocketManager"
    private val managerScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // Track active session WebSockets
    private var activeSessionId: String? = null

    init {
        // Monitor for user login/logout
        monitorAuthState()
    }

    private fun monitorAuthState() {
        managerScope.launch {
            try {
                val preferences = userPreferencesDao.getCurrentlyActivePreference()
                if (preferences?.authToken != null) {
                    Log.d(TAG, "User is logged in, connecting to global WebSockets")
                    webSocketService.connectUserWS()
                    webSocketService.connectSummaryWS()
                } else {
                    Log.d(TAG, "No active user session found")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error connecting to WebSockets", e)
            }
        }
    }

    // Called from activities/fragments that need session-specific WebSockets
    suspend fun connectSessionWebSockets(sessionId: String) {
        activeSessionId = sessionId
        webSocketService.connectTimerWS(sessionId)
        webSocketService.connectAnnotationWS(sessionId)
        webSocketService.connectInstrumentJobWS(sessionId)
    }

    override fun onStart(owner: LifecycleOwner) {
        managerScope.launch {
            val preferences = userPreferencesDao.getCurrentlyActivePreference()
            if (preferences?.authToken != null) {
                webSocketService.connectUserWS()
                webSocketService.connectSummaryWS()
                webSocketService.connectNotificationWS()

                activeSessionId?.let { sessionId ->
                    webSocketService.connectTimerWS(sessionId)
                    webSocketService.connectAnnotationWS(sessionId)
                    webSocketService.connectInstrumentJobWS(sessionId)
                }
            }
        }
    }

    override fun onStop(owner: LifecycleOwner) {
        webSocketService.closeAllConnections()
    }

    override fun onDestroy(owner: LifecycleOwner) {
        webSocketService.closeAllConnections()
    }
}