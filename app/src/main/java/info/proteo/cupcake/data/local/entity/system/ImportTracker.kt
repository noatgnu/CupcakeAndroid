package info.proteo.cupcake.data.local.entity.system

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "import_tracker")
data class ImportTrackerEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "import_type") val importType: String?,
    @ColumnInfo(name = "import_status") val importStatus: String?, // pending, completed, failed, rolled_back
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "created_by") val createdBy: Int?,
    @ColumnInfo(name = "import_name") val importName: String?,
    @ColumnInfo(name = "import_description") val importDescription: String?,
    @ColumnInfo(name = "total_objects") val totalObjects: Int?,
    @ColumnInfo(name = "processed_objects") val processedObjects: Int?,
    @ColumnInfo(name = "created_objects") val createdObjects: Int?,
    @ColumnInfo(name = "updated_objects") val updatedObjects: Int?,
    @ColumnInfo(name = "failed_objects") val failedObjects: Int?,
    @ColumnInfo(name = "error_log") val errorLog: String?, // JSON string
    @ColumnInfo(name = "import_metadata") val importMetadata: String?, // JSON string
    @ColumnInfo(name = "file_size_bytes") val fileSizeBytes: Long?,
    @ColumnInfo(name = "lab_group") val labGroup: Int?
)

@Entity(tableName = "imported_object")
data class ImportedObjectEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "import_tracker") val importTracker: Int,
    @ColumnInfo(name = "object_type") val objectType: String, // model name
    @ColumnInfo(name = "object_id") val objectId: Int,
    @ColumnInfo(name = "action_type") val actionType: String, // created, updated
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "object_data") val objectData: String? // JSON backup for rollback
)

@Entity(tableName = "imported_file")
data class ImportedFileEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "import_tracker") val importTracker: Int,
    @ColumnInfo(name = "file_path") val filePath: String,
    @ColumnInfo(name = "original_filename") val originalFilename: String?,
    @ColumnInfo(name = "file_size_bytes") val fileSizeBytes: Long?,
    @ColumnInfo(name = "file_hash") val fileHash: String?, // MD5 or SHA256
    @ColumnInfo(name = "created_at") val createdAt: String?
)

@Entity(tableName = "imported_relationship")
data class ImportedRelationshipEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "import_tracker") val importTracker: Int,
    @ColumnInfo(name = "relationship_type") val relationshipType: String, // many-to-many, foreign_key
    @ColumnInfo(name = "parent_model") val parentModel: String,
    @ColumnInfo(name = "parent_id") val parentId: Int,
    @ColumnInfo(name = "child_model") val childModel: String,
    @ColumnInfo(name = "child_id") val childId: Int,
    @ColumnInfo(name = "created_at") val createdAt: String?
)