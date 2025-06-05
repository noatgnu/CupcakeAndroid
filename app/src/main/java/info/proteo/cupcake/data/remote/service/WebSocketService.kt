package info.proteo.cupcake.data.remote.service

import android.util.Log
import info.proteo.cupcake.data.local.dao.user.UserPreferencesDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import java.util.UUID
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.min
import kotlin.math.pow

@Singleton
class WebSocketService @Inject constructor(
    private val userPreferencesDao: UserPreferencesDao,
    private val baseUrl: String
) {
    private val TAG = "WebSocketService"
    private val webSocketBaseUrl = baseUrl.replace("http", "ws").trimEnd('/')
    private val maxRetries = 5
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    private var timerWebSocket: WebSocket? = null
    private var annotationWebSocket: WebSocket? = null
    private var userWebSocket: WebSocket? = null
    private var summaryWebSocket: WebSocket? = null
    private var instrumentJobWebSocket: WebSocket? = null
    private var notificationWebSocket: WebSocket? = null


    private var timerConnected = false
    private var annotationConnected = false
    private var userConnected = false
    private var summaryConnected = false
    private var instrumentJobConnected = false
    private var notificationConnected = false


    private var timerIntentionalDisconnect = false
    private var annotationIntentionalDisconnect = false
    private var userIntentionalDisconnect = false
    private var summaryIntentionalDisconnect = false
    private var instrumentJobIntentionalDisconnect = false
    private var notificationIntentionalDisconnect = false

    private val _timerMessages = MutableSharedFlow<String>()
    val timerMessages: Flow<String> = _timerMessages.asSharedFlow()

    private val _annotationMessages = MutableSharedFlow<String>()
    val annotationMessages: Flow<String> = _annotationMessages.asSharedFlow()

    private val _userMessages = MutableSharedFlow<String>()
    val userMessages: Flow<String> = _userMessages.asSharedFlow()

    private val _summaryMessages = MutableSharedFlow<String>()
    val summaryMessages: Flow<String> = _summaryMessages.asSharedFlow()

    private val _instrumentJobMessages = MutableSharedFlow<String>()
    val instrumentJobMessages: Flow<String> = _instrumentJobMessages.asSharedFlow()

    private val _notificationMessages = MutableSharedFlow<String>()
    val notificationMessages: Flow<String> = _notificationMessages.asSharedFlow()


    private val client = OkHttpClient.Builder()
        .readTimeout(0, TimeUnit.MILLISECONDS) // WebSockets can be long-lived
        .build()

    suspend fun connectTimerWS(sessionId: String) {
        timerIntentionalDisconnect = false
        val preferences = userPreferencesDao.getCurrentlyActivePreference()
        val authToken = preferences?.authToken ?: return

        val url = "$webSocketBaseUrl/ws/timer/$sessionId/?token=$authToken"
        Log.d(TAG, "Connecting to timer websocket: $url")

        val request = Request.Builder().url(url).header("Origin", baseUrl)  // Add this line
            .header("User-Agent", "CupcakeAndroid/1.0")
            .build()
        timerWebSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Connected to timer websocket")
                timerConnected = true
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                coroutineScope.launch {
                    _timerMessages.emit(text)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Timer websocket closed: $reason")
                timerConnected = false
                if (!timerIntentionalDisconnect) {
                    reconnectTimerWS(sessionId)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Timer websocket failure", t)
                timerConnected = false
                if (!timerIntentionalDisconnect) {
                    reconnectTimerWS(sessionId)
                }
            }
        })
    }

    private fun reconnectTimerWS(sessionId: String, retryCount: Int = 0) {
        if (timerIntentionalDisconnect || retryCount >= maxRetries) return

        val delay = min(1000 * 2.0.pow(retryCount.toDouble()), 30000.0).toLong()
        Log.d(TAG, "Attempting to reconnect Timer WS in ${delay}ms (attempt ${retryCount + 1})")

        coroutineScope.launch {
            delay(delay)
            if (!timerIntentionalDisconnect) {
                connectTimerWS(sessionId)
            }
        }
    }

    fun closeTimerWS() {
        timerIntentionalDisconnect = true
        timerWebSocket?.close(1000, "Intentional disconnect")
        timerWebSocket = null
    }

    suspend fun connectAnnotationWS(sessionId: String) {
        annotationIntentionalDisconnect = false
        val preferences = userPreferencesDao.getCurrentlyActivePreference()
        val authToken = preferences?.authToken ?: return

        val url = "$webSocketBaseUrl/ws/annotation/$sessionId/?token=$authToken"
        Log.d(TAG, "Connecting to annotation websocket: $url")

        val request = Request.Builder().url(url).header("Origin", baseUrl)  // Add this line
            .header("User-Agent", "CupcakeAndroid/1.0")
            .build()
        annotationWebSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Connected to annotation websocket")
                annotationConnected = true
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                coroutineScope.launch {
                    _annotationMessages.emit(text)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Annotation websocket closed: $reason")
                annotationConnected = false
                if (!annotationIntentionalDisconnect) {
                    reconnectAnnotationWS(sessionId)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Annotation websocket failure", t)
                annotationConnected = false
                if (!annotationIntentionalDisconnect) {
                    reconnectAnnotationWS(sessionId)
                }
            }
        })
    }

    private fun reconnectAnnotationWS(sessionId: String, retryCount: Int = 0) {
        if (annotationIntentionalDisconnect || retryCount >= maxRetries) return

        val delay = min(1000 * 2.0.pow(retryCount.toDouble()), 30000.0).toLong()
        Log.d(TAG, "Attempting to reconnect Annotation WS in ${delay}ms (attempt ${retryCount + 1})")

        coroutineScope.launch {
            delay(delay)
            if (!annotationIntentionalDisconnect) {
                connectAnnotationWS(sessionId)
            }
        }
    }

    fun closeAnnotationWS() {
        annotationIntentionalDisconnect = true
        annotationWebSocket?.close(1000, "Intentional disconnect")
        annotationWebSocket = null
    }

    suspend fun connectSummaryWS() {
        summaryIntentionalDisconnect = false
        val preferences = userPreferencesDao.getCurrentlyActivePreference()
        val authToken = preferences?.authToken ?: return

        val url = "$webSocketBaseUrl/ws/summary/?token=$authToken"
        Log.d(TAG, "Connecting to summary websocket: $url")

        val request = Request.Builder().url(url).header("Origin", baseUrl)  // Add this line
            .header("User-Agent", "CupcakeAndroid/1.0")
            .build()
        summaryWebSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Connected to summary websocket")
                summaryConnected = true
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                coroutineScope.launch {
                    _summaryMessages.emit(text)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Summary websocket closed: $reason")
                summaryConnected = false
                if (!summaryIntentionalDisconnect) {
                    reconnectSummaryWS()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Summary websocket failure", t)
                summaryConnected = false
                if (!summaryIntentionalDisconnect) {
                    reconnectSummaryWS()
                }
            }
        })
    }

    private fun reconnectSummaryWS(retryCount: Int = 0) {
        if (summaryIntentionalDisconnect || retryCount >= maxRetries) return

        val delay = min(1000 * 2.0.pow(retryCount.toDouble()), 30000.0).toLong()
        Log.d(TAG, "Attempting to reconnect Summary WS in ${delay}ms (attempt ${retryCount + 1})")

        coroutineScope.launch {
            delay(delay)
            if (!summaryIntentionalDisconnect) {
                connectSummaryWS()
            }
        }
    }

    fun closeSummaryWS() {
        summaryIntentionalDisconnect = true
        summaryWebSocket?.close(1000, "Intentional disconnect")
        summaryWebSocket = null
    }

    suspend fun connectInstrumentJobWS(sessionId: String) {
        instrumentJobIntentionalDisconnect = false
        val preferences = userPreferencesDao.getCurrentlyActivePreference()
        val authToken = preferences?.authToken ?: return

        val url = "$webSocketBaseUrl/ws/instrument_job/$sessionId/?token=$authToken"
        Log.d(TAG, "Connecting to instrument job websocket: $url")

        val request = Request.Builder().url(url).header("Origin", baseUrl)  // Add this line
            .header("User-Agent", "CupcakeAndroid/1.0")
            .build()
        instrumentJobWebSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Connected to instrument job websocket")
                instrumentJobConnected = true
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                coroutineScope.launch {
                    _instrumentJobMessages.emit(text)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Instrument job websocket closed: $reason")
                instrumentJobConnected = false
                if (!instrumentJobIntentionalDisconnect) {
                    reconnectInstrumentJobWS(sessionId)
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Instrument job websocket failure", t)
                instrumentJobConnected = false
                if (!instrumentJobIntentionalDisconnect) {
                    reconnectInstrumentJobWS(sessionId)
                }
            }
        })
    }

    private fun reconnectInstrumentJobWS(sessionId: String, retryCount: Int = 0) {
        if (instrumentJobIntentionalDisconnect || retryCount >= maxRetries) return

        val delay = min(1000 * 2.0.pow(retryCount.toDouble()), 30000.0).toLong()
        Log.d(TAG, "Attempting to reconnect Instrument Job WS in ${delay}ms (attempt ${retryCount + 1})")

        coroutineScope.launch {
            delay(delay)
            if (!instrumentJobIntentionalDisconnect) {
                connectInstrumentJobWS(sessionId)
            }
        }
    }

    fun closeInstrumentJobWS() {
        instrumentJobIntentionalDisconnect = true
        instrumentJobWebSocket?.close(1000, "Intentional disconnect")
        instrumentJobWebSocket = null
    }

    fun sendSummaryMessage(message: String): Boolean {
        return summaryWebSocket?.send(message) ?: false
    }

    fun sendInstrumentJobMessage(message: String): Boolean {
        return instrumentJobWebSocket?.send(message) ?: false
    }

    suspend fun connectUserWS() {
        userIntentionalDisconnect = false
        val preferences = userPreferencesDao.getCurrentlyActivePreference()
        val authToken = preferences?.authToken ?: return

        val url = "$webSocketBaseUrl/ws/user/?token=$authToken"
        Log.d(TAG, "Connecting to user websocket: $url")

        val request = Request.Builder().url(url).header("Origin", baseUrl)  // Add this line
            .header("User-Agent", "CupcakeAndroid/1.0")
            .build()
        userWebSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Connected to user websocket")
                userConnected = true
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                coroutineScope.launch {
                    _userMessages.emit(text)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                webSocket.close(1000, null)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "User websocket closed: $reason")
                userConnected = false
                if (!userIntentionalDisconnect) {
                    reconnectUserWS()
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "User websocket failure", t)
                userConnected = false
                if (!userIntentionalDisconnect) {
                    reconnectUserWS()
                }
            }
        })
    }

    private fun reconnectUserWS(retryCount: Int = 0) {
        if (userIntentionalDisconnect || retryCount >= maxRetries) return

        val delay = min(1000 * 2.0.pow(retryCount.toDouble()), 30000.0).toLong()
        Log.d(TAG, "Attempting to reconnect User WS in ${delay}ms (attempt ${retryCount + 1})")

        coroutineScope.launch {
            delay(delay)
            if (!userIntentionalDisconnect) {
                connectUserWS()
            }
        }
    }

    fun closeUserWS() {
        userIntentionalDisconnect = true
        userWebSocket?.close(1000, "Intentional disconnect")
        userWebSocket = null
    }

    suspend fun connectNotificationWS() {
        notificationIntentionalDisconnect = false
        val preferences = userPreferencesDao.getCurrentlyActivePreference()
        val authToken = preferences?.authToken ?: return

        val url = "$webSocketBaseUrl/ws/notifications/?token=$authToken"
        Log.d(TAG, "Connecting to notifications websocket: $url")

        val request = Request.Builder()
            .url(url)
            .header("Origin", baseUrl)
            .header("User-Agent", "CupcakeAndroid/1.0")
            .build()

        notificationWebSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onOpen(webSocket: WebSocket, response: Response) {
                Log.d(TAG, "Notification websocket opened")
                notificationConnected = true
            }

            override fun onMessage(webSocket: WebSocket, text: String) {
                Log.d(TAG, "Notification message: $text")
                coroutineScope.launch {
                    _notificationMessages.emit(text)
                }
            }

            override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Notification websocket closing: $code $reason")
                notificationConnected = false
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                Log.d(TAG, "Notification websocket closed: $code $reason")
                notificationConnected = false
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.e(TAG, "Notification websocket failure", t)
                Log.e(TAG, "Response: ${response?.code} ${response?.message}")
                response?.body?.string()?.let { Log.e(TAG, "Body: $it") }
                notificationConnected = false
                if (!notificationIntentionalDisconnect) {
                    reconnectNotificationWS()
                }
            }
        })
    }

    private fun reconnectNotificationWS(retryCount: Int = 0) {
        if (notificationIntentionalDisconnect || retryCount >= maxRetries) return

        val delay = min(1000 * 2.0.pow(retryCount.toDouble()), 30000.0).toLong()
        Log.d(TAG, "Attempting to reconnect Notification WS in ${delay}ms (attempt ${retryCount + 1})")

        coroutineScope.launch {
            delay(delay)
            if (!notificationIntentionalDisconnect) {
                connectNotificationWS()
            }
        }
    }

    fun closeNotificationWS() {
        notificationIntentionalDisconnect = true
        notificationWebSocket?.close(1000, "Intentional disconnect")
        notificationWebSocket = null
    }

    fun sendTimerMessage(message: String): Boolean {
        return timerWebSocket?.send(message) ?: false
    }

    fun sendAnnotationMessage(message: String): Boolean {
        return annotationWebSocket?.send(message) ?: false
    }

    fun sendUserMessage(message: String): Boolean {
        return userWebSocket?.send(message) ?: false
    }

    fun closeAllConnections() {
        closeTimerWS()
        closeAnnotationWS()
        closeUserWS()
        closeSummaryWS()
        closeInstrumentJobWS()
        closeNotificationWS()
    }

    suspend fun connectAvailableWebSockets() {
        val preferences = userPreferencesDao.getCurrentlyActivePreference()
        if (preferences?.authToken == null) {
            Log.w(TAG, "No active user preference found, skipping WebSocket connections")
            return
        }

        connectTimerWS(preferences.sessionToken ?: "")
        connectAnnotationWS(preferences.sessionToken ?: "")
        connectUserWS()
        connectSummaryWS()
        connectInstrumentJobWS(preferences.sessionToken ?: "")
        connectNotificationWS()
    }

    fun sendNotificationMessage(message: String): Boolean {
        return notificationWebSocket?.send(message) ?: false
    }
}