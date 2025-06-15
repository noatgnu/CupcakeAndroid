package info.proteo.cupcake.wearos

import android.app.Application
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class CupcakeWearApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        // Initialize any app-wide components here
    }
}
