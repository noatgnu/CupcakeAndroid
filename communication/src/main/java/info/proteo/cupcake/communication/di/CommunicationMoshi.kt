package info.proteo.cupcake.communication.di

import javax.inject.Qualifier

/**
 * Qualifier annotation to identify the Moshi instance used for Wear OS communication
 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class CommunicationMoshi
