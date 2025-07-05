package info.proteo.cupcake.data.offline

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import info.proteo.cupcake.shared.data.model.annotation.Annotation

/**
 * Represents changes made to an annotation for offline editing
 */
@JsonClass(generateAdapter = true)
data class AnnotationChanges(
    @Json(name = "annotation") val annotation: String? = null,
    @Json(name = "annotation_name") val annotationName: String? = null,
    @Json(name = "summary") val summary: String? = null,
    @Json(name = "transcription") val transcription: String? = null,
    @Json(name = "translation") val translation: String? = null,
    @Json(name = "fixed") val fixed: Boolean? = null,
    @Json(name = "scratched") val scratched: Boolean? = null,
    @Json(name = "file_path") val filePath: String? = null
) {
    companion object {
        fun fromAnnotation(annotation: Annotation): AnnotationChanges {
            return AnnotationChanges(
                annotation = annotation.annotation,
                annotationName = annotation.annotationName,
                summary = annotation.summary,
                transcription = annotation.transcription,
                translation = annotation.translation,
                fixed = annotation.fixed,
                scratched = annotation.scratched
            )
        }
    }
    
    fun isEmpty(): Boolean {
        return annotation == null && 
               annotationName == null && 
               summary == null && 
               transcription == null && 
               translation == null && 
               fixed == null && 
               scratched == null && 
               filePath == null
    }
}

/**
 * Represents a pending change to be synced
 */
@Entity(tableName = "pending_changes")
data class PendingChange(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val annotationId: Int,
    val changeType: ChangeType,
    val changes: AnnotationChanges,
    val timestamp: Long,
    val syncStatus: SyncStatus,
    val retryCount: Int = 0,
    val lastError: String? = null
)

/**
 * Type of change made to annotation
 */
enum class ChangeType {
    CREATE,
    UPDATE,
    DELETE
}

/**
 * Sync status of pending change
 */
enum class SyncStatus {
    PENDING,
    SYNCING,
    SYNCED,
    FAILED
}

/**
 * Result of sync operation
 */
data class SyncResult(
    val totalChanges: Int,
    val successCount: Int,
    val failureCount: Int,
    val errors: List<Exception>
) {
    val successRate: Float
        get() = if (totalChanges > 0) successCount.toFloat() / totalChanges else 0f
    
    val hasErrors: Boolean
        get() = errors.isNotEmpty()
}

/**
 * Network connectivity status
 */
data class NetworkStatus(
    val isConnected: Boolean,
    val connectionType: ConnectionType,
    val isMetered: Boolean = false
)

/**
 * Connection type enumeration
 */
enum class ConnectionType {
    WIFI,
    CELLULAR,
    ETHERNET,
    NONE
}

/**
 * Offline annotation metadata
 */
@Entity(tableName = "offline_annotations")
data class OfflineAnnotationMetadata(
    @PrimaryKey
    val annotationId: Int,
    val isOfflineCreated: Boolean,
    val hasUnsyncedChanges: Boolean,
    val lastModified: Long,
    val originalHash: String? = null, // Hash of original content for conflict detection
    val conflictResolution: ConflictResolution? = null
)

/**
 * Conflict resolution strategy
 */
enum class ConflictResolution {
    USE_LOCAL,
    USE_REMOTE,
    MERGE,
    MANUAL
}

/**
 * File operation for offline handling
 */
@Entity(tableName = "pending_file_operations")
data class PendingFileOperation(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val annotationId: Int,
    val operationType: FileOperationType,
    val localFilePath: String,
    val targetFileName: String?,
    val syncStatus: SyncStatus,
    val timestamp: Long,
    val fileSize: Long,
    val mimeType: String?
)

/**
 * File operation types
 */
enum class FileOperationType {
    UPLOAD,
    DOWNLOAD,
    DELETE
}

/**
 * Batch sync operation
 */
data class BatchSyncOperation(
    val id: String,
    val pendingChanges: List<PendingChange>,
    val pendingFileOps: List<PendingFileOperation>,
    val startTime: Long,
    val endTime: Long? = null,
    val status: SyncStatus,
    val progress: Float = 0f
)