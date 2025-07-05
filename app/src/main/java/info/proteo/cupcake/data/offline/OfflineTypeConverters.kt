package info.proteo.cupcake.data.offline

import androidx.room.TypeConverter
import com.squareup.moshi.Moshi
import com.squareup.moshi.adapter

/**
 * Type converters for Room database to handle complex offline data types
 */
class OfflineTypeConverters {
    
    private val moshi = Moshi.Builder().build()
    @OptIn(kotlin.ExperimentalStdlibApi::class)
    private val annotationChangesAdapter = moshi.adapter<AnnotationChanges>()
    
    @TypeConverter
    fun fromAnnotationChanges(changes: AnnotationChanges?): String? {
        return changes?.let { annotationChangesAdapter.toJson(it) }
    }
    
    @TypeConverter
    fun toAnnotationChanges(json: String?): AnnotationChanges? {
        return json?.let { annotationChangesAdapter.fromJson(it) }
    }
    
    @TypeConverter
    fun fromChangeType(changeType: ChangeType?): String? {
        return changeType?.name
    }
    
    @TypeConverter
    fun toChangeType(name: String?): ChangeType? {
        return name?.let { ChangeType.valueOf(it) }
    }
    
    @TypeConverter
    fun fromSyncStatus(syncStatus: SyncStatus?): String? {
        return syncStatus?.name
    }
    
    @TypeConverter
    fun toSyncStatus(name: String?): SyncStatus? {
        return name?.let { SyncStatus.valueOf(it) }
    }
    
    @TypeConverter
    fun fromFileOperationType(operationType: FileOperationType?): String? {
        return operationType?.name
    }
    
    @TypeConverter
    fun toFileOperationType(name: String?): FileOperationType? {
        return name?.let { FileOperationType.valueOf(it) }
    }
    
    @TypeConverter
    fun fromConflictResolution(resolution: ConflictResolution?): String? {
        return resolution?.name
    }
    
    @TypeConverter
    fun toConflictResolution(name: String?): ConflictResolution? {
        return name?.let { ConflictResolution.valueOf(it) }
    }
}