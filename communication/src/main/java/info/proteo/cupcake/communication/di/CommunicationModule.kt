package info.proteo.cupcake.communication.di

import android.content.Context
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import info.proteo.cupcake.communication.service.TimeKeeperCommunicationService
import info.proteo.cupcake.communication.service.WearableTimeKeeperCommunicationService
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object CommunicationModule {

    @CommunicationMoshi
    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideWearableTimeKeeperCommunicationService(
        @ApplicationContext context: Context,
        @CommunicationMoshi moshi: Moshi
    ): WearableTimeKeeperCommunicationService {
        val service = WearableTimeKeeperCommunicationService(context, moshi)
        service.startListening()
        return service
    }
}
