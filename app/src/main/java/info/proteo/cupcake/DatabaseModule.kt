package info.proteo.cupcake

import android.content.Context
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import info.proteo.cupcake.data.local.AppDatabase
import info.proteo.cupcake.data.local.RoomMigrations
import javax.inject.Singleton
import info.proteo.cupcake.data.local.dao.annotation.*
import info.proteo.cupcake.data.local.dao.generic.*
import info.proteo.cupcake.data.local.dao.instrument.*
import info.proteo.cupcake.data.local.dao.message.*
import info.proteo.cupcake.data.local.dao.metadatacolumn.*
import info.proteo.cupcake.data.local.dao.project.*
import info.proteo.cupcake.data.local.dao.reagent.*
import info.proteo.cupcake.data.local.dao.protocol.*
import info.proteo.cupcake.data.local.dao.storage.*
import info.proteo.cupcake.data.local.dao.user.*
import info.proteo.cupcake.data.local.dao.tag.*
import info.proteo.cupcake.data.remote.service.BarcodeGenerator
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import javax.inject.Inject

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabaseAccessManager(): DatabaseAccessManager {
        return DatabaseAccessManager()
    }

    @Provides
    @Singleton
    fun provideBarcodeGenerator(): BarcodeGenerator {
        return BarcodeGenerator()
    }

    @Provides
    @Singleton
    fun provideSessionManager(@ApplicationContext context: Context): SessionManager {
        return SessionManager(context)
    }

    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        ).addCallback(object : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                RoomMigrations.createAllTables(db)
            }
        }).addMigrations(RoomMigrations.MIGRATION_1_2).build()
    }


    @Provides
    fun provideUserBasicDao(database: AppDatabase): UserBasicDao = database.userBasicDao()

    @Provides
    fun provideLabGroupBasicDao(database: AppDatabase): LabGroupBasicDao = database.labGroupBasicDao()

    @Provides
    fun provideExternalContactDetailsDao(database: AppDatabase): ExternalContactDetailsDao = database.externalContactDetailsDao()

    @Provides
    fun provideExternalContactDao(database: AppDatabase): ExternalContactDao = database.externalContactDao()

    @Provides
    fun provideSupportInformationDao(database: AppDatabase): SupportInformationDao = database.supportInformationDao()

    @Provides
    fun provideProtocolModelDao(database: AppDatabase): ProtocolModelDao = database.protocolModelDao()

    @Provides
    fun provideProtocolStepDao(database: AppDatabase): ProtocolStepDao = database.protocolStepDao()

    @Provides
    fun provideProtocolSectionDao(database: AppDatabase): ProtocolSectionDao = database.protocolSectionDao()

    @Provides
    fun provideAnnotationDao(database: AppDatabase): AnnotationDao = database.annotationDao()

    @Provides
    fun provideAnnotationFolderPathDao(database: AppDatabase): AnnotationFolderPathDao = database.annotationFolderPathDao()

    @Provides
    fun provideStepVariationDao(database: AppDatabase): StepVariationDao = database.stepVariationDao()

    @Provides
    fun provideSessionDao(database: AppDatabase): SessionDao = database.sessionDao()

    @Provides
    fun provideTimeKeeperDao(database: AppDatabase): TimeKeeperDao = database.timeKeeperDao()

    @Provides
    fun provideUserDao(database: AppDatabase): UserDao = database.userDao()

    @Provides
    fun provideProtocolRatingDao(database: AppDatabase): ProtocolRatingDao = database.protocolRatingDao()

    @Provides
    fun provideReagentDao(database: AppDatabase): ReagentDao = database.reagentDao()

    @Provides
    fun provideProtocolReagentDao(database: AppDatabase): ProtocolReagentDao = database.protocolReagentDao()

    @Provides
    fun provideStepReagentDao(database: AppDatabase): StepReagentDao = database.stepReagentDao()

    @Provides
    fun provideStepTagDao(database: AppDatabase): StepTagDao = database.stepTagDao()

    @Provides
    fun provideProtocolTagDao(database: AppDatabase): ProtocolTagDao = database.protocolTagDao()

    @Provides
    fun provideTagDao(database: AppDatabase): TagDao = database.tagDao()

    @Provides
    fun provideAnnotationFolderDao(database: AppDatabase): AnnotationFolderDao = database.annotationFolderDao()

    @Provides
    fun provideProjectDao(database: AppDatabase): ProjectDao = database.projectDao()

    @Provides
    fun provideMaintenanceLogDao(database: AppDatabase): MaintenanceLogDao = database.maintenanceLogDao()

    @Provides
    fun provideInstrumentDao(database: AppDatabase): InstrumentDao = database.instrumentDao()

    @Provides
    fun provideInstrumentUsageDao(database: AppDatabase): InstrumentUsageDao = database.instrumentUsageDao()

    @Provides
    fun provideStorageObjectDao(database: AppDatabase): StorageObjectDao = database.storageObjectDao()

    @Provides
    fun provideReagentSubscriptionDao(database: AppDatabase): ReagentSubscriptionDao = database.reagentSubscriptionDao()

    @Provides
    fun provideStoredReagentDao(database: AppDatabase): StoredReagentDao = database.storedReagentDao()

    @Provides
    fun provideReagentActionDao(database: AppDatabase): ReagentActionDao = database.reagentActionDao()

    @Provides
    fun provideLabGroupDao(database: AppDatabase): LabGroupDao = database.labGroupDao()

    @Provides
    fun provideMetadataColumnDao(database: AppDatabase): MetadataColumnDao = database.metadataColumnDao()

    @Provides
    fun provideMSUniqueVocabulariesDao(database: AppDatabase): MSUniqueVocabulariesDao = database.msUniqueVocabulariesDao()

    @Provides
    fun provideHumanDiseaseDao(database: AppDatabase): HumanDiseaseDao = database.humanDiseaseDao()

    @Provides
    fun provideTissueDao(database: AppDatabase): TissueDao = database.tissueDao()

    @Provides
    fun provideSubcellularLocationDao(database: AppDatabase): SubcellularLocationDao = database.subcellularLocationDao()

    @Provides
    fun provideSpeciesDao(database: AppDatabase): SpeciesDao = database.speciesDao()

    @Provides
    fun provideUnimodDao(database: AppDatabase): UnimodDao = database.unimodDao()

    @Provides
    fun provideInstrumentJobDao(database: AppDatabase): InstrumentJobDao = database.instrumentJobDao()

    @Provides
    fun provideFavouriteMetadataOptionDao(database: AppDatabase): FavouriteMetadataOptionDao = database.favouriteMetadataOptionDao()

    @Provides
    fun providePresetDao(database: AppDatabase): PresetDao = database.presetDao()

    @Provides
    fun provideMetadataTableTemplateDao(database: AppDatabase): MetadataTableTemplateDao = database.metadataTableTemplateDao()

    @Provides
    fun provideMessageAttachmentDao(database: AppDatabase): MessageAttachmentDao = database.messageAttachmentDao()

    @Provides
    fun provideMessageRecipientDao(database: AppDatabase): MessageRecipientDao = database.messageRecipientDao()

    @Provides
    fun provideMessageDao(database: AppDatabase): MessageDao = database.messageDao()

    @Provides
    fun provideMessageThreadDao(database: AppDatabase): MessageThreadDao = database.messageThreadDao()

    @Provides
    fun provideLimitOffsetCacheDao(database: AppDatabase): LimitOffsetCacheDao = database.limitOffsetCacheDao()

    @Provides
    fun provideUserPreferencesDao(database: AppDatabase): UserPreferencesDao = database.userPreferencesDao()
}

@Singleton
class DatabaseAccessManager @Inject constructor() {
    private val mutex = Mutex()

    suspend fun <T> withDatabaseAccess(block: suspend () -> T): T {
        return mutex.withLock {
            block()
        }
    }
}