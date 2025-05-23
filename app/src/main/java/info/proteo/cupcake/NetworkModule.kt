package info.proteo.cupcake

import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import info.proteo.cupcake.data.local.dao.message.MessageAttachmentDao
import info.proteo.cupcake.data.local.dao.message.MessageDao
import info.proteo.cupcake.data.local.dao.message.MessageRecipientDao
import info.proteo.cupcake.data.local.dao.message.MessageThreadDao
import info.proteo.cupcake.data.local.dao.reagent.ReagentDao
import info.proteo.cupcake.data.local.dao.reagent.StoredReagentDao
import info.proteo.cupcake.data.local.dao.storage.StorageObjectDao
import info.proteo.cupcake.data.local.dao.user.LabGroupDao
import info.proteo.cupcake.data.local.dao.user.UserDao
import info.proteo.cupcake.data.local.dao.user.UserPreferencesDao
import info.proteo.cupcake.data.remote.LimitOffsetResponseAdapterFactory
import info.proteo.cupcake.data.remote.interceptor.AuthInterceptor
import info.proteo.cupcake.data.remote.service.AuthApiService
import info.proteo.cupcake.data.remote.service.AuthService
import info.proteo.cupcake.data.remote.service.AuthServiceImpl
import info.proteo.cupcake.data.remote.service.LabGroupApiService
import info.proteo.cupcake.data.remote.service.LabGroupService
import info.proteo.cupcake.data.remote.service.LabGroupServiceImpl
import info.proteo.cupcake.data.remote.service.MessageApiService
import info.proteo.cupcake.data.remote.service.MessageService
import info.proteo.cupcake.data.remote.service.MessageServiceImpl
import info.proteo.cupcake.data.remote.service.MessageThreadApiService
import info.proteo.cupcake.data.remote.service.MessageThreadService
import info.proteo.cupcake.data.remote.service.MessageThreadServiceImpl
import info.proteo.cupcake.data.remote.service.StorageObjectApiService
import info.proteo.cupcake.data.remote.service.StorageObjectService
import info.proteo.cupcake.data.remote.service.StorageObjectServiceImpl
import info.proteo.cupcake.data.remote.service.StoredReagentApiService
import info.proteo.cupcake.data.remote.service.StoredReagentService
import info.proteo.cupcake.data.remote.service.StoredReagentServiceImpl
import info.proteo.cupcake.data.remote.service.UserApiService
import info.proteo.cupcake.data.remote.service.UserService
import info.proteo.cupcake.data.remote.service.UserServiceImpl
import info.proteo.cupcake.data.repository.MessageRepository
import info.proteo.cupcake.data.repository.MessageRepositoryImpl
import info.proteo.cupcake.data.repository.MessageThreadRepository
import info.proteo.cupcake.data.repository.MessageThreadRepositoryImpl
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.util.concurrent.TimeUnit
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    @Named("baseUrl")
    fun provideBaseUrl(sessionManager: SessionManager): String {
        val baseUrl = sessionManager.getBaseUrl()
        return if (baseUrl.isEmpty() || !baseUrl.startsWith("http")) {
            "https://cupcake.proteo.info/"
        } else {
            baseUrl
        }
    }

    @Provides
    @Singleton
    fun provideAuthInterceptor(
        userPreferencesDao: UserPreferencesDao,
        @Named("baseUrl") baseUrl: String
    ): AuthInterceptor {
        return AuthInterceptor(userPreferencesDao, baseUrl)
    }

    @Provides
    @Singleton
    @Named("authenticatedClient")
    fun provideAuthenticatedOkHttpClient(authInterceptor: AuthInterceptor): OkHttpClient {
        return OkHttpClient.Builder()
            .addInterceptor(authInterceptor)
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    @Named("unauthenticatedClient")
    fun provideUnauthenticatedOkHttpClient(): OkHttpClient {
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .build()
    }

    @Provides
    @Singleton
    fun provideMoshi(): Moshi {
        return Moshi.Builder()
            .add(LimitOffsetResponseAdapterFactory()).addLast(KotlinJsonAdapterFactory())
            .build()
    }

    @Provides
    @Singleton
    fun provideUserApiService(
        @Named("baseUrl") baseUrl: String,
        @Named("authenticatedClient") okHttpClient: OkHttpClient,
        moshi: Moshi
    ): UserApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(UserApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideUserService(userServiceImpl: UserServiceImpl): UserService {
        return userServiceImpl
    }

    @Provides
    @Singleton
    fun provideAuthService(authServiceImpl: AuthServiceImpl): AuthService {
        return authServiceImpl
    }

    @Provides
    @Singleton
    fun provideAuthApiService(
        @Named("baseUrl") baseUrl: String,
        @Named("unauthenticatedClient") okHttpClient: OkHttpClient,
        moshi: Moshi
    ): AuthApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(AuthApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideStorageObjectApiService(
        @Named("baseUrl") baseUrl: String,
        @Named("authenticatedClient") okHttpClient: OkHttpClient,
        moshi: Moshi
    ): StorageObjectApiService {
        val converter = MoshiConverterFactory.create(moshi)
            .asLenient()
        Log.d("NetworkModule", "Creating StorageObjectApiService with baseUrl: $baseUrl")

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(converter)
            .build()
            .create(StorageObjectApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideStorageObjectService(
        apiService: StorageObjectApiService,
        storageObjectDao: StorageObjectDao
    ): StorageObjectService {
        return StorageObjectServiceImpl(apiService, storageObjectDao)
    }

    @Provides
    @Singleton
    fun provideStoredReagentApiService(
        @Named("baseUrl") baseUrl: String,
        @Named("authenticatedClient") okHttpClient: OkHttpClient,
        moshi: Moshi
    ): StoredReagentApiService {
        val converter = MoshiConverterFactory.create(moshi)
            .asLenient()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(converter)
            .build()
            .create(StoredReagentApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideStoredReagentService(
        apiService: StoredReagentApiService,
        storedReagentDao: StoredReagentDao,
        reagentDao: ReagentDao,
        storageObjectDao: StorageObjectDao,
        userDao: UserDao
    ): StoredReagentService {
        return StoredReagentServiceImpl(
            apiService,
            storedReagentDao,
            reagentDao,
            storageObjectDao,
            userDao
        )
    }


    @Provides
    @Singleton
    fun provideLabGroupApiService(
        @Named("baseUrl") baseUrl: String,
        @Named("authenticatedClient") okHttpClient: OkHttpClient,
        moshi: Moshi
    ): LabGroupApiService {
        val converter = MoshiConverterFactory.create(moshi)
            .asLenient()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(converter)
            .build()
            .create(LabGroupApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideLabGroupService(
        apiService: LabGroupApiService,
        labGroupDao: LabGroupDao,
        storageObjectDao: StorageObjectDao,
        userDao: UserDao
    ): LabGroupService {
        return LabGroupServiceImpl(
            apiService,
            labGroupDao,
            storageObjectDao,
            userDao
        )
    }

    @Provides
    @Singleton
    fun provideMessageApiService(
        @Named("baseUrl") baseUrl: String,
        @Named("authenticatedClient") okHttpClient: OkHttpClient,
        moshi: Moshi
    ): MessageApiService {
        val converter = MoshiConverterFactory.create(moshi)
            .asLenient()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(converter)
            .build()
            .create(MessageApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideMessageThreadApiService(
        @Named("baseUrl") baseUrl: String,
        @Named("authenticatedClient") okHttpClient: OkHttpClient,
        moshi: Moshi
    ): MessageThreadApiService {
        val converter = MoshiConverterFactory.create(moshi)
            .asLenient()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(converter)
            .build()
            .create(MessageThreadApiService::class.java)
    }



    @Provides
    @Singleton
    fun provideMessageService(
        apiService: MessageApiService,
        messageDao: MessageDao,
        userDao: UserDao,
        attachmentDao: MessageAttachmentDao,
        recipientDao: MessageRecipientDao,
        userService: UserService,
    ): MessageService {
        return MessageServiceImpl(
            apiService,
            messageDao,
            userDao,
            attachmentDao,
            recipientDao,
            userService,
        )
    }

    @Provides
    @Singleton
    fun provideMessageThreadService(messageThreadServiceImpl: MessageThreadServiceImpl): MessageThreadService {
        return messageThreadServiceImpl
    }

    @Provides
    @Singleton
    fun provideMessageRepository(messageRepositoryImpl: MessageRepositoryImpl): MessageRepository {
        return messageRepositoryImpl
    }

    @Provides
    @Singleton
    fun provideMessageThreadRepository(messageThreadRepositoryImpl: MessageThreadRepositoryImpl): MessageThreadRepository {
        return messageThreadRepositoryImpl
    }
}

class JsonResponseInterceptor : Interceptor {
    private val TAG = "RawJsonResponse"

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        Log.d(TAG, "REQUEST: ${request.method} ${request.url}")

        val response = chain.proceed(request)
        Log.d(TAG, "RESPONSE: ${response.code} for ${request.url}")

        try {
            if (response.body != null) {
                val rawBody = response.peekBody(Long.MAX_VALUE)
                val bodyString = rawBody.string()
                Log.d(TAG, "BODY: $bodyString")
            } else {
                Log.d(TAG, "BODY: null")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read response body", e)
        }

        return response
    }


}

class HeadersLoggingInterceptor : Interceptor {
    private val TAG = "HttpHeaders"

    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()

        // Log request headers
        Log.d(TAG, "REQUEST HEADERS for ${request.url}")
        request.headers.forEach { (name, value) ->
            Log.d(TAG, "► $name: $value")
        }

        val response = chain.proceed(request)

        // Log response headers
        Log.d(TAG, "RESPONSE HEADERS for ${request.url}: ${response.code}")
        response.headers.forEach { (name, value) ->
            Log.d(TAG, "◄ $name: $value")
        }

        return response
    }
}