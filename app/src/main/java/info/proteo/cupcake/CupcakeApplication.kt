package info.proteo.cupcake

import android.app.Application
import androidx.lifecycle.ProcessLifecycleOwner
import dagger.hilt.android.HiltAndroidApp
import info.proteo.cupcake.data.remote.service.WebSocketManager
import javax.inject.Inject

@HiltAndroidApp
class CupcakeApplication : Application() {
    @Inject lateinit var webSocketManager: WebSocketManager

    override fun onCreate() {
        super.onCreate()

        ProcessLifecycleOwner.get().lifecycle.addObserver(webSocketManager)
    }

}