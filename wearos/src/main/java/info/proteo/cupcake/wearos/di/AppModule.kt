package info.proteo.cupcake.wearos.di

import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import info.proteo.cupcake.communication.service.TimeKeeperCommunicationService
import info.proteo.cupcake.communication.service.WearableTimeKeeperCommunicationService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class AppModule {

    @Binds
    @Singleton
    abstract fun bindTimeKeeperCommunicationService(
        wearableService: WearableTimeKeeperCommunicationService
    ): TimeKeeperCommunicationService
}
