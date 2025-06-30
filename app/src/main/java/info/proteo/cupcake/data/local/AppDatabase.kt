package info.proteo.cupcake.data.local

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.TypeConverter
import androidx.room.TypeConverters
import info.proteo.cupcake.data.local.entity.user.*
import info.proteo.cupcake.data.local.entity.annotation.*
import info.proteo.cupcake.data.local.entity.generic.LimitOffsetCacheEntity
import info.proteo.cupcake.data.local.entity.instrument.*
import info.proteo.cupcake.data.local.entity.metadatacolumn.*
import info.proteo.cupcake.data.local.entity.project.ProjectEntity
import info.proteo.cupcake.data.local.entity.reagent.*
import info.proteo.cupcake.data.local.entity.storage.StorageObjectEntity
import info.proteo.cupcake.data.local.entity.instrument.*
import info.proteo.cupcake.data.local.entity.message.*
import info.proteo.cupcake.data.local.entity.protocol.*
import info.proteo.cupcake.data.local.entity.tag.TagEntity
import info.proteo.cupcake.data.local.entity.system.*
import info.proteo.cupcake.data.local.entity.communication.*

import info.proteo.cupcake.data.local.dao.user.*
import info.proteo.cupcake.data.local.dao.annotation.*
import info.proteo.cupcake.data.local.dao.generic.LimitOffsetCacheDao
import info.proteo.cupcake.data.local.dao.instrument.*
import info.proteo.cupcake.data.local.dao.metadatacolumn.*
import info.proteo.cupcake.data.local.dao.project.ProjectDao
import info.proteo.cupcake.data.local.dao.reagent.*
import info.proteo.cupcake.data.local.dao.storage.StorageObjectDao
import info.proteo.cupcake.data.local.dao.instrument.*
import info.proteo.cupcake.data.local.dao.message.*
import info.proteo.cupcake.data.local.dao.protocol.*
import info.proteo.cupcake.data.local.dao.tag.TagDao
import info.proteo.cupcake.data.local.dao.system.*
import info.proteo.cupcake.data.local.dao.communication.*
import kotlinx.coroutines.Dispatchers

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import java.util.Date

@Database(
    entities = [
        UserBasicEntity::class,
        LabGroupBasicEntity::class,
        ExternalContactDetailsEntity::class,
        ExternalContactEntity::class,
        SupportInformationEntity::class,
        SupportInformationVendorContactCrossRef::class,
        SupportInformationManufacturerContactCrossRef::class,
        ProtocolModelEntity::class,
        ProtocolEditorCrossRef::class,
        ProtocolViewerCrossRef::class,
        ProtocolStepEntity::class,
        ProtocolStepNextRelation::class,
        ProtocolSectionEntity::class,
        AnnotationEntity::class,
        AnnotationFolderPathEntity::class,
        StepVariationEntity::class,
        SessionEntity::class,
        RecentSessionEntity::class,
        TimeKeeperEntity::class,
        UserEntity::class,
        ProtocolRatingEntity::class,
        ReagentEntity::class,
        ProtocolReagentEntity::class,
        StepReagentEntity::class,
        StepTagEntity::class,
        ProtocolTagEntity::class,
        TagEntity::class,
        AnnotationFolderEntity::class,
        ProjectEntity::class,
        MaintenanceLogEntity::class,
        InstrumentEntity::class,
        InstrumentUsageEntity::class,
        StorageObjectEntity::class,
        ReagentSubscriptionEntity::class,
        StoredReagentEntity::class,
        ReagentActionEntity::class,
        LabGroupEntity::class,
        MetadataColumnEntity::class,
        MessageEntity::class,
        MessageRecipientEntity::class,
        MessageAttachmentEntity::class,
        MessageThreadEntity::class,
        LimitOffsetCacheEntity::class,
        TissueEntity::class,
        SubcellularLocationEntity::class,
        SpeciesEntity::class,
        UnimodEntity::class,
        InstrumentJobEntity::class,
        FavouriteMetadataOptionEntity::class,
        PresetEntity::class,
        MetadataTableTemplateEntity::class,
        MSUniqueVocabulariesEntity::class,
        HumanDiseaseEntity::class,
        UserPreferencesEntity::class,
        RemoteHostEntity::class,
        SiteSettingsEntity::class,
        BackupLogEntity::class,
        DocumentPermissionEntity::class,
        WebRTCSessionEntity::class,
        WebRTCUserChannelEntity::class,
        WebRTCUserOfferEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun userBasicDao(): UserBasicDao
    abstract fun labGroupBasicDao(): LabGroupBasicDao
    abstract fun externalContactDetailsDao(): ExternalContactDetailsDao
    abstract fun externalContactDao(): ExternalContactDao
    abstract fun supportInformationDao(): SupportInformationDao
    abstract fun protocolModelDao(): ProtocolModelDao
    abstract fun protocolStepDao(): ProtocolStepDao
    abstract fun protocolSectionDao(): ProtocolSectionDao
    abstract fun annotationDao(): AnnotationDao
    abstract fun annotationFolderPathDao(): AnnotationFolderPathDao
    abstract fun stepVariationDao(): StepVariationDao
    abstract fun sessionDao(): SessionDao
    abstract fun timeKeeperDao(): TimeKeeperDao
    abstract fun userDao(): UserDao
    abstract fun protocolRatingDao(): ProtocolRatingDao
    abstract fun reagentDao(): ReagentDao
    abstract fun protocolReagentDao(): ProtocolReagentDao
    abstract fun stepReagentDao(): StepReagentDao
    abstract fun stepTagDao(): StepTagDao
    abstract fun protocolTagDao(): ProtocolTagDao
    abstract fun tagDao(): TagDao
    abstract fun annotationFolderDao(): AnnotationFolderDao
    abstract fun projectDao(): ProjectDao
    abstract fun maintenanceLogDao(): MaintenanceLogDao
    abstract fun instrumentDao(): InstrumentDao
    abstract fun instrumentUsageDao(): InstrumentUsageDao
    abstract fun storageObjectDao(): StorageObjectDao
    abstract fun reagentSubscriptionDao(): ReagentSubscriptionDao
    abstract fun storedReagentDao(): StoredReagentDao
    abstract fun reagentActionDao(): ReagentActionDao
    abstract fun labGroupDao(): LabGroupDao
    abstract fun metadataColumnDao(): MetadataColumnDao
    abstract fun messageDao(): MessageDao
    abstract fun messageRecipientDao(): MessageRecipientDao
    abstract fun messageAttachmentDao(): MessageAttachmentDao
    abstract fun messageThreadDao(): MessageThreadDao
    abstract fun limitOffsetCacheDao(): LimitOffsetCacheDao
    abstract fun tissueDao(): TissueDao
    abstract fun subcellularLocationDao(): SubcellularLocationDao
    abstract fun speciesDao(): SpeciesDao
    abstract fun unimodDao(): UnimodDao
    abstract fun instrumentJobDao(): InstrumentJobDao
    abstract fun favouriteMetadataOptionDao(): FavouriteMetadataOptionDao
    abstract fun presetDao(): PresetDao
    abstract fun metadataTableTemplateDao(): MetadataTableTemplateDao
    abstract fun msUniqueVocabulariesDao(): MSUniqueVocabulariesDao
    abstract fun humanDiseaseDao(): HumanDiseaseDao
    abstract fun userPreferencesDao(): UserPreferencesDao
    abstract fun recentSessionDao(): RecentSessionDao
    
    // System DAOs
    abstract fun remoteHostDao(): RemoteHostDao
    abstract fun siteSettingsDao(): SiteSettingsDao
    abstract fun backupLogDao(): BackupLogDao
    abstract fun documentPermissionDao(): DocumentPermissionDao
    
    // Communication DAOs
    abstract fun webRTCSessionDao(): WebRTCSessionDao
    abstract fun webRTCUserChannelDao(): WebRTCUserChannelDao
    abstract fun webRTCUserOfferDao(): WebRTCUserOfferDao

    companion object {
        const val DATABASE_NAME = "cupcake_database"
    }

    suspend fun <T> executeQuery(query: suspend () -> T): T {
        return withContext(Dispatchers.IO) {
            query()
        }
    }
}

class Converters {
    @TypeConverter
    fun fromTimestamp(value: Long?): Date? {
        return value?.let { Date(it) }
    }

    @TypeConverter
    fun dateToTimestamp(date: Date?): Long? {
        return date?.time
    }

    @TypeConverter
    fun fromStringList(value: String?): List<String>? {
        return value?.split(",")?.map { it.trim() }
    }

    @TypeConverter
    fun toStringList(list: List<String>?): String? {
        return list?.joinToString(",")
    }

    @TypeConverter
    fun fromIntList(value: String?): List<Int>? {
        return value?.split(",")?.map { it.trim().toInt() }
    }

    @TypeConverter
    fun toIntList(list: List<Int>?): String? {
        return list?.joinToString(",")
    }
}