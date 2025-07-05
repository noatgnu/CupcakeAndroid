package info.proteo.cupcake.data.offline

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.util.Log
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Monitors network connectivity changes and provides real-time status
 */
@Singleton
class NetworkConnectivityMonitor @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "NetworkConnectivityMonitor"
    }
    
    private val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    /**
     * Flow that emits network status changes
     */
    fun networkStatusFlow(): Flow<NetworkStatus> = callbackFlow {
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                val status = getCurrentNetworkStatus()
                Log.d(TAG, "Network available: $status")
                trySend(status)
            }
            
            override fun onLost(network: Network) {
                val status = NetworkStatus(
                    isConnected = false,
                    connectionType = ConnectionType.NONE,
                    isMetered = false
                )
                Log.d(TAG, "Network lost")
                trySend(status)
            }
            
            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                val status = getCurrentNetworkStatus()
                Log.d(TAG, "Network capabilities changed: $status")
                trySend(status)
            }
        }
        
        val networkRequest = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(networkRequest, networkCallback)
        
        // Emit initial status
        trySend(getCurrentNetworkStatus())
        
        awaitClose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
        }
    }.distinctUntilChanged()
    
    /**
     * Check if network is currently connected
     */
    fun isConnected(): Boolean {
        return getCurrentNetworkStatus().isConnected
    }
    
    /**
     * Check if connection is metered (cellular data)
     */
    fun isMetered(): Boolean {
        return getCurrentNetworkStatus().isMetered
    }
    
    /**
     * Get current connection type
     */
    fun getConnectionType(): ConnectionType {
        return getCurrentNetworkStatus().connectionType
    }
    
    /**
     * Check if connected to WiFi
     */
    fun isWifiConnected(): Boolean {
        return getCurrentNetworkStatus().connectionType == ConnectionType.WIFI
    }
    
    /**
     * Check if suitable for large downloads (WiFi or unlimited)
     */
    fun isSuitableForLargeDownloads(): Boolean {
        val status = getCurrentNetworkStatus()
        return status.isConnected && (status.connectionType == ConnectionType.WIFI || !status.isMetered)
    }
    
    /**
     * Get current network status
     */
    fun getCurrentNetworkStatus(): NetworkStatus {
        val activeNetwork = connectivityManager.activeNetwork
        val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)
        
        if (activeNetwork == null || networkCapabilities == null) {
            return NetworkStatus(
                isConnected = false,
                connectionType = ConnectionType.NONE,
                isMetered = false
            )
        }
        
        val isConnected = networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
                         networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
        
        val connectionType = when {
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.CELLULAR
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
            else -> ConnectionType.NONE
        }
        
        val isMetered = !networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
        
        return NetworkStatus(
            isConnected = isConnected,
            connectionType = connectionType,
            isMetered = isMetered
        )
    }
    
    /**
     * Wait for network connection
     */
    suspend fun waitForConnection(): NetworkStatus {
        return networkStatusFlow()
            .first { it.isConnected }
    }
    
    /**
     * Wait for suitable connection for sync
     */
    suspend fun waitForSuitableConnection(): NetworkStatus {
        return networkStatusFlow()
            .first { it.isConnected && (it.connectionType == ConnectionType.WIFI || !it.isMetered) }
    }
}

/**
 * Extension function to get first value from Flow
 */
private suspend fun <T> Flow<T>.first(predicate: (T) -> Boolean): T {
    var result: T? = null
    collect { value ->
        if (predicate(value)) {
            result = value
            return@collect
        }
    }
    return result!!
}