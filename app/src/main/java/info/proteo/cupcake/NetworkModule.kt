package info.proteo.cupcake

import android.content.Context
import android.util.Log
import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import info.proteo.cupcake.data.local.dao.annotation.AnnotationDao
import info.proteo.cupcake.data.local.dao.annotation.AnnotationFolderDao
import info.proteo.cupcake.data.local.dao.annotation.AnnotationFolderPathDao
import info.proteo.cupcake.data.local.dao.instrument.ExternalContactDao
import info.proteo.cupcake.data.local.dao.instrument.InstrumentDao
import info.proteo.cupcake.data.local.dao.instrument.SupportInformationDao
import info.proteo.cupcake.data.local.dao.message.MessageAttachmentDao
import info.proteo.cupcake.data.local.dao.message.MessageDao
import info.proteo.cupcake.data.local.dao.message.MessageRecipientDao
import info.proteo.cupcake.data.local.dao.message.MessageThreadDao
import info.proteo.cupcake.data.local.dao.protocol.RecentSessionDao
import info.proteo.cupcake.data.local.dao.protocol.TimeKeeperDao
import info.proteo.cupcake.data.local.dao.reagent.ReagentActionDao
import info.proteo.cupcake.data.local.dao.reagent.ReagentDao
import info.proteo.cupcake.data.local.dao.reagent.StoredReagentDao
import info.proteo.cupcake.data.local.dao.storage.StorageObjectDao
import info.proteo.cupcake.data.local.dao.user.LabGroupDao
import info.proteo.cupcake.data.local.dao.user.UserDao
import info.proteo.cupcake.data.local.dao.user.UserPreferencesDao
import info.proteo.cupcake.data.local.dao.system.ImportTrackerDao
import info.proteo.cupcake.data.local.dao.system.ImportedObjectDao
import info.proteo.cupcake.data.local.dao.system.ImportedFileDao
import info.proteo.cupcake.data.local.dao.system.ImportedRelationshipDao
import info.proteo.cupcake.data.local.dao.system.SiteSettingsDao
import info.proteo.cupcake.data.local.dao.system.BackupLogDao
import info.proteo.cupcake.data.local.dao.system.DocumentPermissionDao
import info.proteo.cupcake.data.local.dao.system.RemoteHostDao
import info.proteo.cupcake.data.model.api.user.User
import info.proteo.cupcake.data.remote.LimitOffsetResponseAdapterFactory
import info.proteo.cupcake.data.remote.interceptor.AuthInterceptor
import info.proteo.cupcake.data.remote.service.InstrumentUsageApiService
import info.proteo.cupcake.data.remote.service.InstrumentUsageService
import info.proteo.cupcake.data.remote.service.InstrumentUsageServiceImpl
import info.proteo.cupcake.data.remote.service.AnnotationApiService
import info.proteo.cupcake.data.remote.service.AnnotationService
import info.proteo.cupcake.data.remote.service.AnnotationServiceImpl
import info.proteo.cupcake.data.remote.service.AuthApiService
import info.proteo.cupcake.data.remote.service.AuthService
import info.proteo.cupcake.data.remote.service.AuthServiceImpl
import info.proteo.cupcake.data.remote.service.InstrumentApiService
import info.proteo.cupcake.data.remote.service.InstrumentService
import info.proteo.cupcake.data.remote.service.InstrumentServiceImpl
import info.proteo.cupcake.data.remote.service.LabGroupApiService
import info.proteo.cupcake.data.remote.service.LabGroupService
import info.proteo.cupcake.data.remote.service.LabGroupServiceImpl
import info.proteo.cupcake.data.remote.service.MessageApiService
import info.proteo.cupcake.data.remote.service.MessageService
import info.proteo.cupcake.data.remote.service.MessageServiceImpl
import info.proteo.cupcake.data.remote.service.MessageThreadApiService
import info.proteo.cupcake.data.remote.service.MessageThreadService
import info.proteo.cupcake.data.remote.service.MessageThreadServiceImpl
import info.proteo.cupcake.data.remote.service.ProtocolApiService
import info.proteo.cupcake.data.remote.service.ProtocolSectionApiService
import info.proteo.cupcake.data.remote.service.ProtocolSectionService
import info.proteo.cupcake.data.remote.service.ProtocolSectionServiceImpl
import info.proteo.cupcake.data.remote.service.ProtocolService
import info.proteo.cupcake.data.remote.service.ProtocolServiceImpl
import info.proteo.cupcake.data.remote.service.ProtocolStepApiService
import info.proteo.cupcake.data.remote.service.ProtocolStepService
import info.proteo.cupcake.data.remote.service.ProtocolStepServiceImpl
import info.proteo.cupcake.data.remote.service.ReagentActionApiService
import info.proteo.cupcake.data.remote.service.ReagentActionService
import info.proteo.cupcake.data.remote.service.ReagentActionServiceImpl
import info.proteo.cupcake.data.remote.service.ReagentApiService
import info.proteo.cupcake.data.remote.service.ReagentDocumentApiService
import info.proteo.cupcake.data.remote.service.ReagentDocumentService
import info.proteo.cupcake.data.remote.service.ReagentDocumentServiceImpl
import info.proteo.cupcake.data.remote.service.ReagentService
import info.proteo.cupcake.data.remote.service.ReagentServiceImpl
import info.proteo.cupcake.data.remote.service.SessionApiService
import info.proteo.cupcake.data.remote.service.SessionService
import info.proteo.cupcake.data.remote.service.SessionServiceImpl
import info.proteo.cupcake.data.remote.service.StorageObjectApiService
import info.proteo.cupcake.data.remote.service.StorageObjectService
import info.proteo.cupcake.data.remote.service.StorageObjectServiceImpl
import info.proteo.cupcake.data.remote.service.StoredReagentApiService
import info.proteo.cupcake.data.remote.service.StoredReagentService
import info.proteo.cupcake.data.remote.service.StoredReagentServiceImpl
import info.proteo.cupcake.data.remote.service.SupportInformationApiService
import info.proteo.cupcake.data.remote.service.SupportInformationService
import info.proteo.cupcake.data.remote.service.SupportInformationServiceImpl
import info.proteo.cupcake.data.remote.service.TagApiService
import info.proteo.cupcake.data.remote.service.TagService
import info.proteo.cupcake.data.remote.service.TagServiceImpl
import info.proteo.cupcake.data.remote.service.TimeKeeperApiService
import info.proteo.cupcake.data.remote.service.TimeKeeperService
import info.proteo.cupcake.data.remote.service.TimeKeeperServiceImpl
import info.proteo.cupcake.data.remote.service.UserApiService
import info.proteo.cupcake.data.remote.service.UserService
import info.proteo.cupcake.data.remote.service.UserServiceImpl
import info.proteo.cupcake.data.remote.service.WebSocketManager
import info.proteo.cupcake.data.remote.service.WebSocketService
import info.proteo.cupcake.data.remote.service.MaintenanceLogApiService
import info.proteo.cupcake.data.remote.service.MaintenanceLogService
import info.proteo.cupcake.data.remote.service.MaintenanceLogServiceImpl
import info.proteo.cupcake.data.remote.service.ImportTrackerApiService
import info.proteo.cupcake.data.remote.service.ImportTrackerService
import info.proteo.cupcake.data.remote.service.ImportTrackerServiceImpl
import info.proteo.cupcake.data.remote.service.ImportedObjectApiService
import info.proteo.cupcake.data.remote.service.ImportedObjectService
import info.proteo.cupcake.data.remote.service.ImportedObjectServiceImpl
import info.proteo.cupcake.data.remote.service.ImportedFileApiService
import info.proteo.cupcake.data.remote.service.ImportedFileService
import info.proteo.cupcake.data.remote.service.ImportedFileServiceImpl
import info.proteo.cupcake.data.remote.service.ImportedRelationshipApiService
import info.proteo.cupcake.data.remote.service.ImportedRelationshipService
import info.proteo.cupcake.data.remote.service.ImportedRelationshipServiceImpl
import info.proteo.cupcake.data.remote.service.SiteSettingsApiService
import info.proteo.cupcake.data.remote.service.SiteSettingsService
import info.proteo.cupcake.data.remote.service.SiteSettingsServiceImpl
import info.proteo.cupcake.data.remote.service.BackupLogApiService
import info.proteo.cupcake.data.remote.service.BackupLogService
import info.proteo.cupcake.data.remote.service.BackupLogServiceImpl
import info.proteo.cupcake.data.remote.service.DocumentPermissionApiService
import info.proteo.cupcake.data.remote.service.DocumentPermissionService
import info.proteo.cupcake.data.remote.service.DocumentPermissionServiceImpl
import info.proteo.cupcake.data.remote.service.RemoteHostApiService
import info.proteo.cupcake.data.remote.service.RemoteHostService
import info.proteo.cupcake.data.remote.service.RemoteHostServiceImpl
import info.proteo.cupcake.data.remote.service.SharedDocumentApiService
import info.proteo.cupcake.data.remote.service.SharedDocumentService
import info.proteo.cupcake.data.remote.service.SharedDocumentServiceImpl
import info.proteo.cupcake.data.repository.AnnotationRepository
import info.proteo.cupcake.data.repository.InstrumentRepository
import info.proteo.cupcake.data.repository.InstrumentUsageRepository
import info.proteo.cupcake.data.repository.MessageRepository
import info.proteo.cupcake.data.repository.MessageRepositoryImpl
import info.proteo.cupcake.data.repository.MessageThreadRepository
import info.proteo.cupcake.data.repository.MessageThreadRepositoryImpl
import info.proteo.cupcake.data.repository.ProtocolRepository
import info.proteo.cupcake.data.repository.ProtocolSectionRepository
import info.proteo.cupcake.data.repository.ProtocolStepRepository
import info.proteo.cupcake.data.repository.ReagentActionRepository
import info.proteo.cupcake.data.repository.ReagentDocumentRepository
import info.proteo.cupcake.data.repository.ReagentRepository
import info.proteo.cupcake.data.repository.SessionRepository
import info.proteo.cupcake.data.repository.StoredReagentRepository
import info.proteo.cupcake.data.repository.SupportInformationRepository
import info.proteo.cupcake.data.repository.TagRepository
import info.proteo.cupcake.data.repository.TimeKeeperRepository
import info.proteo.cupcake.data.repository.UserRepository
import info.proteo.cupcake.data.repository.MaintenanceLogRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory
import java.lang.reflect.Type
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
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
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
            .add(LimitOffsetResponseAdapterFactory())
            .addLast(KotlinJsonAdapterFactory())
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
    fun provideStoredReagentRepository(
        storedReagentService: StoredReagentService
    ): StoredReagentRepository {
        return StoredReagentRepository(storedReagentService)
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

    @Provides
    @Singleton
    fun provideReagentActionApiService(
        @Named("baseUrl") baseUrl: String,
        @Named("authenticatedClient") okHttpClient: OkHttpClient,
        moshi: Moshi
    ): ReagentActionApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .build()
            .create(ReagentActionApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideReagentActionService(
        apiService: ReagentActionApiService,
        reagentActionDao: ReagentActionDao
    ): ReagentActionService {
        return ReagentActionServiceImpl(apiService, reagentActionDao)
    }

    @Provides
    @Singleton
    fun provideReagentActionRepository(
        reagentActionService: ReagentActionService
    ): ReagentActionRepository {
        return ReagentActionRepository(reagentActionService)
    }

    @Provides
    @Singleton
    fun provideReagentDocumentApiService(
        @Named("authenticatedClient") okHttpClient: OkHttpClient,
        @Named("baseUrl") baseUrl: String,
        moshi: Moshi
    ): ReagentDocumentApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(ReagentDocumentApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideReagentDocumentService(
        reagentDocumentApiService: ReagentDocumentApiService,
        annotationDao: AnnotationDao,
        annotationFolderDao: AnnotationFolderDao,
        userDao: UserDao,
        annotationFolderPathDao: AnnotationFolderPathDao
    ): ReagentDocumentService {
        return ReagentDocumentServiceImpl(
            reagentDocumentApiService = reagentDocumentApiService,
            annotationDao = annotationDao,
            annotationFolderDao = annotationFolderDao,
            userDao = userDao,
            annotationFolderPathDao = annotationFolderPathDao
        )
    }

    @Provides
    @Singleton
    fun provideReagentDocumentRepository(
        reagentDocumentService: ReagentDocumentService
    ): ReagentDocumentRepository {
        return ReagentDocumentRepository(reagentDocumentService)
    }

    @Provides
    @Singleton
    fun provideInstrumentApiService(
        @Named("authenticatedClient") okHttpClient: OkHttpClient,
        @Named("baseUrl") baseUrl: String,
        moshi: Moshi
    ): InstrumentApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi))
            .build()
            .create(InstrumentApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideInstrumentService(
        instrumentApiService: InstrumentApiService,
        instrumentDao: InstrumentDao,
        supportInformationDao: SupportInformationDao
    ): InstrumentService {
        return InstrumentServiceImpl(instrumentApiService, instrumentDao, supportInformationDao)
    }

    @Provides
    @Singleton
    fun provideInstrumentRepository(
        instrumentService: InstrumentService
    ): InstrumentRepository {
        return InstrumentRepository(instrumentService)
    }

    @Provides
    @Singleton
    fun provideAnnotationApiService(
        @Named("baseUrl") baseUrl: String,
        @Named("authenticatedClient") okHttpClient: OkHttpClient,
        moshi: Moshi
    ): AnnotationApiService {
        val converter = MoshiConverterFactory.create(moshi)
            .asLenient()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(converter)
            .build()
            .create(AnnotationApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideAnnotationService(
        apiService: AnnotationApiService,
        annotationDao: AnnotationDao,
        userDao: UserDao,
        annotationFolderPathDao: AnnotationFolderPathDao
    ): AnnotationService {
        return AnnotationServiceImpl(apiService, annotationDao, userDao, annotationFolderPathDao)
    }

    @Provides
    @Singleton
    fun provideAnnotationRepository(
        annotationService: AnnotationService
    ): AnnotationRepository {
        return AnnotationRepository(annotationService)
    }

    @Provides
    @Singleton
    fun provideSupportInformationApiService(
        @Named("baseUrl") baseUrl: String,
        @Named("authenticatedClient") okHttpClient: OkHttpClient,
        moshi: Moshi
    ): SupportInformationApiService {
        val converter = MoshiConverterFactory.create(moshi)
            .asLenient()

        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(converter)
            .build()
            .create(SupportInformationApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideSupportInformationService(
        apiService: SupportInformationApiService,
        supportInformationDao: SupportInformationDao,
        externalContactDao: ExternalContactDao
    ): SupportInformationService {
        return SupportInformationServiceImpl(
            apiService,
            supportInformationDao,
            externalContactDao
        )
    }

    @Provides
    @Singleton
    fun provideSupportInformationRepository(
        supportInformationService: SupportInformationService
    ): SupportInformationRepository {
        return SupportInformationRepository(supportInformationService)
    }

    @Provides
    @Singleton
    fun provideTimeKeeperApiService(
        @Named("baseUrl") baseUrl: String,
        @Named("authenticatedClient") okHttpClient: OkHttpClient,
        moshi: Moshi
    ): TimeKeeperApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .build()
            .create(TimeKeeperApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideTimeKeeperService(
        apiService: TimeKeeperApiService,
        timeKeeperDao: TimeKeeperDao,
        userRepository: UserRepository,
        userPreferencesDao: UserPreferencesDao
    ): TimeKeeperService {
        return TimeKeeperServiceImpl(
            apiService,
            timeKeeperDao,
            userRepository,
            userPreferencesDao
        )
    }

    @Provides
    @Singleton
    fun provideTimeKeeperRepository(
        timeKeeperService: TimeKeeperService
    ): TimeKeeperRepository {
        return TimeKeeperRepository(timeKeeperService)
    }

    @Provides
    @Singleton
    fun provideProtocolStepApiService(
        @Named("baseUrl") baseUrl: String,
        @Named("authenticatedClient") okHttpClient: OkHttpClient,
        moshi: Moshi
    ): ProtocolStepApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .build()
            .create(ProtocolStepApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideProtocolStepService(
        protocolStepServiceImpl: ProtocolStepServiceImpl
    ): ProtocolStepService {
        return protocolStepServiceImpl
    }

    @Provides
    @Singleton
    fun provideProtocolStepRepository(
        protocolStepService: ProtocolStepService
    ): ProtocolStepRepository {
        return ProtocolStepRepository(protocolStepService)
    }

    @Provides
    @Singleton
    fun provideWebSocketService(
        userPreferencesDao: UserPreferencesDao,
        @Named("baseUrl") baseUrl: String
    ): WebSocketService {
        return WebSocketService(userPreferencesDao, baseUrl)
    }

    @Provides
    @Singleton
    fun provideWebSocketManager(
        webSocketService: WebSocketService,
        userPreferencesDao: UserPreferencesDao
    ): WebSocketManager {
        return WebSocketManager(webSocketService, userPreferencesDao)
    }

    @Provides
    @Singleton
    fun provideTagApiService(
        @Named("baseUrl") baseUrl: String,
        @Named("authenticatedClient") okHttpClient: OkHttpClient,
        moshi: Moshi
    ): TagApiService {
        val converter = MoshiConverterFactory.create(moshi).asLenient()
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(converter)
            .build()
            .create(TagApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideTagService(tagServiceImpl: TagServiceImpl): TagService {
        return tagServiceImpl
    }

    @Provides
    @Singleton
    fun provideTagRepository(tagService: TagService): TagRepository {
        return TagRepository(tagService)
    }

    @Provides
    @Singleton
    fun provideReagentApiService(
        @Named("baseUrl") baseUrl: String,
        @Named("authenticatedClient") okHttpClient: OkHttpClient,
        moshi: Moshi
    ): ReagentApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .build()
            .create(ReagentApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideReagentService(
        reagentServiceImpl: ReagentServiceImpl
    ): ReagentService  = reagentServiceImpl

    @Provides
    @Singleton
    fun provideReagentRepository(
        reagentService: ReagentService
    ): ReagentRepository = ReagentRepository(reagentService)

    @Provides
    @Singleton
    fun provideProtocolApiService(
        @Named("baseUrl") baseUrl: String,
        @Named("authenticatedClient") okHttpClient: OkHttpClient,
        moshi: Moshi
    ): ProtocolApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .build()
            .create(ProtocolApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideProtocolService(
        protocolServiceImpl: ProtocolServiceImpl
    ): ProtocolService = protocolServiceImpl

    @Provides
    @Singleton
    fun provideProtocolRepository(
        protocolService: ProtocolService
    ): ProtocolRepository = ProtocolRepository(protocolService)


    @Provides
    @Singleton
    fun provideProtocolSectionApiService(
        @Named("baseUrl") baseUrl: String,
        @Named("authenticatedClient") okHttpClient: OkHttpClient,
        moshi: Moshi
    ): ProtocolSectionApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .build()
            .create(ProtocolSectionApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideProtocolSectionService(
        protocolSectionServiceImpl: ProtocolSectionServiceImpl
    ): ProtocolSectionService = protocolSectionServiceImpl

    @Provides
    @Singleton
    fun provideProtocolSectionRepository(
        protocolSectionService: ProtocolSectionService
    ): ProtocolSectionRepository = ProtocolSectionRepository(protocolSectionService)

    @Provides
    @Singleton
    fun provideSessionApiService(
        @Named("baseUrl") baseUrl: String,
        @Named("authenticatedClient") okHttpClient: OkHttpClient,
        moshi: Moshi
    ): SessionApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .build()
            .create(SessionApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideSessionService(
        sessionServiceImpl: SessionServiceImpl
    ): SessionService {
        return sessionServiceImpl
    }

    @Provides
    @Singleton
    fun provideSessionRepository(
        sessionService: SessionService,
        recentSessionDao: RecentSessionDao
    ): SessionRepository {
        return SessionRepository(sessionService, recentSessionDao)
    }

    @Provides
    @Singleton
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @Provides
    @Singleton
    fun provideInstrumentUsageApiService(
        @Named("baseUrl") baseUrl: String,
        @Named("authenticatedClient") okHttpClient: OkHttpClient,
        moshi: Moshi
    ): InstrumentUsageApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .build()
            .create(InstrumentUsageApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideInstrumentUsageService(
        instrumentUsageServiceImpl: InstrumentUsageServiceImpl
    ): InstrumentUsageService {
        return instrumentUsageServiceImpl
    }

    @Provides
    @Singleton
    fun provideInstrumentUsageRepository(
        instrumentUsageService: InstrumentUsageService
    ): InstrumentUsageRepository {
        return InstrumentUsageRepository(
            instrumentUsageService
        )
    }

    @Provides
    @Singleton
    fun provideMaintenanceLogApiService(
        @Named("baseUrl") baseUrl: String,
        @Named("authenticatedClient") okHttpClient: OkHttpClient,
        moshi: Moshi
    ): MaintenanceLogApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .build()
            .create(MaintenanceLogApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideMaintenanceLogService(
        maintenanceLogServiceImpl: MaintenanceLogServiceImpl
    ): MaintenanceLogService {
        return maintenanceLogServiceImpl
    }

    @Provides
    @Singleton
    fun provideMaintenanceLogRepository(
        maintenanceLogService: MaintenanceLogService
    ): MaintenanceLogRepository {
        return MaintenanceLogRepository(maintenanceLogService)
    }

    // Import Services
    @Provides
    @Singleton
    fun provideImportTrackerApiService(
        @Named("baseUrl") baseUrl: String,
        @Named("authenticatedClient") okHttpClient: OkHttpClient,
        moshi: Moshi
    ): ImportTrackerApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .build()
            .create(ImportTrackerApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideImportTrackerService(
        apiService: ImportTrackerApiService,
        importTrackerDao: ImportTrackerDao
    ): ImportTrackerService {
        return ImportTrackerServiceImpl(apiService, importTrackerDao)
    }

    @Provides
    @Singleton
    fun provideImportedObjectApiService(
        @Named("baseUrl") baseUrl: String,
        @Named("authenticatedClient") okHttpClient: OkHttpClient,
        moshi: Moshi
    ): ImportedObjectApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .build()
            .create(ImportedObjectApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideImportedObjectService(
        apiService: ImportedObjectApiService,
        importedObjectDao: ImportedObjectDao
    ): ImportedObjectService {
        return ImportedObjectServiceImpl(apiService, importedObjectDao)
    }

    @Provides
    @Singleton
    fun provideImportedFileApiService(
        @Named("baseUrl") baseUrl: String,
        @Named("authenticatedClient") okHttpClient: OkHttpClient,
        moshi: Moshi
    ): ImportedFileApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .build()
            .create(ImportedFileApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideImportedFileService(
        apiService: ImportedFileApiService,
        importedFileDao: ImportedFileDao
    ): ImportedFileService {
        return ImportedFileServiceImpl(apiService, importedFileDao)
    }

    @Provides
    @Singleton
    fun provideImportedRelationshipApiService(
        @Named("baseUrl") baseUrl: String,
        @Named("authenticatedClient") okHttpClient: OkHttpClient,
        moshi: Moshi
    ): ImportedRelationshipApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .build()
            .create(ImportedRelationshipApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideImportedRelationshipService(
        apiService: ImportedRelationshipApiService,
        importedRelationshipDao: ImportedRelationshipDao
    ): ImportedRelationshipService {
        return ImportedRelationshipServiceImpl(apiService, importedRelationshipDao)
    }

    // System Services
    @Provides
    @Singleton
    fun provideSiteSettingsApiService(
        @Named("baseUrl") baseUrl: String,
        @Named("authenticatedClient") okHttpClient: OkHttpClient,
        moshi: Moshi
    ): SiteSettingsApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .build()
            .create(SiteSettingsApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideSiteSettingsService(
        apiService: SiteSettingsApiService,
        siteSettingsDao: SiteSettingsDao
    ): SiteSettingsService {
        return SiteSettingsServiceImpl(apiService, siteSettingsDao)
    }

    @Provides
    @Singleton
    fun provideBackupLogApiService(
        @Named("baseUrl") baseUrl: String,
        @Named("authenticatedClient") okHttpClient: OkHttpClient,
        moshi: Moshi
    ): BackupLogApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .build()
            .create(BackupLogApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideBackupLogService(
        apiService: BackupLogApiService,
        backupLogDao: BackupLogDao
    ): BackupLogService {
        return BackupLogServiceImpl(apiService, backupLogDao)
    }

    @Provides
    @Singleton
    fun provideDocumentPermissionApiService(
        @Named("baseUrl") baseUrl: String,
        @Named("authenticatedClient") okHttpClient: OkHttpClient,
        moshi: Moshi
    ): DocumentPermissionApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .build()
            .create(DocumentPermissionApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideDocumentPermissionService(
        apiService: DocumentPermissionApiService,
        documentPermissionDao: DocumentPermissionDao
    ): DocumentPermissionService {
        return DocumentPermissionServiceImpl(apiService, documentPermissionDao)
    }

    @Provides
    @Singleton
    fun provideRemoteHostApiService(
        @Named("baseUrl") baseUrl: String,
        @Named("authenticatedClient") okHttpClient: OkHttpClient,
        moshi: Moshi
    ): RemoteHostApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .build()
            .create(RemoteHostApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideRemoteHostService(
        apiService: RemoteHostApiService,
        remoteHostDao: RemoteHostDao
    ): RemoteHostService {
        return RemoteHostServiceImpl(apiService, remoteHostDao)
    }

    // Shared Document Services
    @Provides
    @Singleton
    fun provideSharedDocumentApiService(
        @Named("baseUrl") baseUrl: String,
        @Named("authenticatedClient") okHttpClient: OkHttpClient,
        moshi: Moshi
    ): SharedDocumentApiService {
        return Retrofit.Builder()
            .baseUrl(baseUrl)
            .client(okHttpClient)
            .addConverterFactory(MoshiConverterFactory.create(moshi).asLenient())
            .build()
            .create(SharedDocumentApiService::class.java)
    }

    @Provides
    @Singleton
    fun provideSharedDocumentService(
        apiService: SharedDocumentApiService
    ): SharedDocumentService {
        return SharedDocumentServiceImpl(apiService)
    }

    // Cache Management Providers
    
    @Provides
    @Singleton
    fun provideFileCacheManager(@dagger.hilt.android.qualifiers.ApplicationContext context: Context): info.proteo.cupcake.data.cache.FileCacheManager {
        return info.proteo.cupcake.data.cache.FileCacheManager(context)
    }
    
    @Provides
    @Singleton
    fun provideMediaCacheHandler(
        @dagger.hilt.android.qualifiers.ApplicationContext context: Context,
        fileCacheManager: info.proteo.cupcake.data.cache.FileCacheManager
    ): info.proteo.cupcake.data.cache.MediaCacheHandler {
        return info.proteo.cupcake.data.cache.MediaCacheHandler(context, fileCacheManager)
    }
    
    @Provides
    @Singleton
    fun provideCachedAnnotationService(
        annotationService: AnnotationService,
        fileCacheManager: info.proteo.cupcake.data.cache.FileCacheManager
    ): info.proteo.cupcake.data.cache.CachedAnnotationService {
        return info.proteo.cupcake.data.cache.CachedAnnotationService(annotationService, fileCacheManager)
    }
    
    @Provides
    @Singleton
    fun provideCachedSharedDocumentService(
        sharedDocumentService: SharedDocumentService,
        fileCacheManager: info.proteo.cupcake.data.cache.FileCacheManager,
        @Named("authenticatedClient") okHttpClient: OkHttpClient
    ): info.proteo.cupcake.data.cache.CachedSharedDocumentService {
        return info.proteo.cupcake.data.cache.CachedSharedDocumentService(
            sharedDocumentService, 
            fileCacheManager, 
            okHttpClient
        )
    }
    
    @Provides
    @Singleton
    fun provideCacheRepository(
        fileCacheManager: info.proteo.cupcake.data.cache.FileCacheManager,
        mediaCacheHandler: info.proteo.cupcake.data.cache.MediaCacheHandler,
        cachedAnnotationService: info.proteo.cupcake.data.cache.CachedAnnotationService,
        cachedSharedDocumentService: info.proteo.cupcake.data.cache.CachedSharedDocumentService
    ): info.proteo.cupcake.data.repository.CacheRepository {
        return info.proteo.cupcake.data.repository.CacheRepository(
            fileCacheManager,
            mediaCacheHandler,
            cachedAnnotationService,
            cachedSharedDocumentService
        )
    }
    
    // Offline Management Providers
    
    @Provides
    @Singleton
    fun provideNetworkConnectivityMonitor(@dagger.hilt.android.qualifiers.ApplicationContext context: Context): info.proteo.cupcake.data.offline.NetworkConnectivityMonitor {
        return info.proteo.cupcake.data.offline.NetworkConnectivityMonitor(context)
    }
    
    @Provides
    @Singleton
    fun provideOfflineAnnotationHandler(
        annotationDao: AnnotationDao,
        cachedAnnotationService: info.proteo.cupcake.data.cache.CachedAnnotationService,
        fileCacheManager: info.proteo.cupcake.data.cache.FileCacheManager,
        pendingChangesDao: info.proteo.cupcake.data.local.dao.offline.PendingChangesDao,
        networkConnectivityMonitor: info.proteo.cupcake.data.offline.NetworkConnectivityMonitor
    ): info.proteo.cupcake.data.offline.OfflineAnnotationHandler {
        return info.proteo.cupcake.data.offline.OfflineAnnotationHandler(
            annotationDao,
            cachedAnnotationService,
            fileCacheManager,
            pendingChangesDao,
            networkConnectivityMonitor
        )
    }
    
    @Provides
    @Singleton
    fun provideSyncService(
        offlineAnnotationHandler: info.proteo.cupcake.data.offline.OfflineAnnotationHandler,
        networkConnectivityMonitor: info.proteo.cupcake.data.offline.NetworkConnectivityMonitor,
        pendingChangesDao: info.proteo.cupcake.data.local.dao.offline.PendingChangesDao,
        pendingFileOperationsDao: info.proteo.cupcake.data.local.dao.offline.PendingFileOperationsDao,
        cachedAnnotationService: info.proteo.cupcake.data.cache.CachedAnnotationService,
        cachedSharedDocumentService: info.proteo.cupcake.data.cache.CachedSharedDocumentService
    ): info.proteo.cupcake.data.offline.SyncService {
        return info.proteo.cupcake.data.offline.SyncService(
            offlineAnnotationHandler,
            networkConnectivityMonitor,
            pendingChangesDao,
            pendingFileOperationsDao,
            cachedAnnotationService,
            cachedSharedDocumentService
        )
    }
    
    @Provides
    @Singleton
    fun provideOfflineManager(
        offlineAnnotationHandler: info.proteo.cupcake.data.offline.OfflineAnnotationHandler,
        syncService: info.proteo.cupcake.data.offline.SyncService,
        cacheRepository: info.proteo.cupcake.data.repository.CacheRepository,
        fileCacheManager: info.proteo.cupcake.data.cache.FileCacheManager,
        networkConnectivityMonitor: info.proteo.cupcake.data.offline.NetworkConnectivityMonitor,
        pendingChangesDao: info.proteo.cupcake.data.local.dao.offline.PendingChangesDao,
        pendingFileOperationsDao: info.proteo.cupcake.data.local.dao.offline.PendingFileOperationsDao
    ): info.proteo.cupcake.data.offline.OfflineManager {
        return info.proteo.cupcake.data.offline.OfflineManager(
            offlineAnnotationHandler,
            syncService,
            cacheRepository,
            fileCacheManager,
            networkConnectivityMonitor,
            pendingChangesDao,
            pendingFileOperationsDao
        )
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

