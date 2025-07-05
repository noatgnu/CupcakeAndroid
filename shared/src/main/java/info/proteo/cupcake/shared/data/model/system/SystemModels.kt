package info.proteo.cupcake.shared.data.model.system

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import info.proteo.cupcake.shared.data.model.user.UserBasic
import info.proteo.cupcake.shared.data.model.user.LabGroup

@JsonClass(generateAdapter = true)
data class RemoteHost(
    val id: Int,
    @Json(name = "host_name") val hostName: String,
    @Json(name = "host_url") val hostUrl: String,
    @Json(name = "host_token") val hostToken: String?
)

@JsonClass(generateAdapter = true)
data class SiteSettings(
    val id: Int,
    @Json(name = "is_active") val isActive: Boolean,
    @Json(name = "site_name") val siteName: String?,
    @Json(name = "site_tagline") val siteTagline: String?,
    val logo: String?,
    val favicon: String?,
    @Json(name = "banner_enabled") val bannerEnabled: Boolean,
    @Json(name = "banner_text") val bannerText: String?,
    @Json(name = "banner_color") val bannerColor: String?,
    @Json(name = "banner_text_color") val bannerTextColor: String?,
    @Json(name = "banner_dismissible") val bannerDismissible: Boolean,
    @Json(name = "primary_color") val primaryColor: String?,
    @Json(name = "secondary_color") val secondaryColor: String?,
    @Json(name = "footer_text") val footerText: String?,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?,
    @Json(name = "updated_by") val updatedBy: UserBasic?,
    // Import restrictions
    @Json(name = "allow_import_protocols") val allowImportProtocols: Boolean,
    @Json(name = "allow_import_reagents") val allowImportReagents: Boolean,
    @Json(name = "allow_import_storage_objects") val allowImportStorageObjects: Boolean,
    @Json(name = "allow_import_instruments") val allowImportInstruments: Boolean,
    @Json(name = "allow_import_users") val allowImportUsers: Boolean,
    @Json(name = "allow_import_lab_groups") val allowImportLabGroups: Boolean,
    @Json(name = "allow_import_sessions") val allowImportSessions: Boolean,
    @Json(name = "allow_import_projects") val allowImportProjects: Boolean,
    @Json(name = "allow_import_annotations") val allowImportAnnotations: Boolean,
    @Json(name = "allow_import_metadata") val allowImportMetadata: Boolean,
    @Json(name = "staff_only_import_override") val staffOnlyImportOverride: Boolean,
    @Json(name = "import_archive_size_limit_mb") val importArchiveSizeLimitMb: Int,
    @Json(name = "available_import_options") val availableImportOptions: List<String>?
)

@JsonClass(generateAdapter = true)
data class BackupLog(
    val id: Int,
    @Json(name = "backup_type") val backupType: String,
    @Json(name = "backup_type_display") val backupTypeDisplay: String?,
    val status: String,
    @Json(name = "status_display") val statusDisplay: String?,
    @Json(name = "started_at") val startedAt: String?,
    @Json(name = "completed_at") val completedAt: String?,
    @Json(name = "duration_seconds") val durationSeconds: Int?,
    @Json(name = "backup_file_path") val backupFilePath: String?,
    @Json(name = "file_size_bytes") val fileSizeBytes: Long?,
    @Json(name = "file_size_mb") val fileSizeMb: Float?,
    @Json(name = "error_message") val errorMessage: String?,
    @Json(name = "success_message") val successMessage: String?,
    @Json(name = "triggered_by") val triggeredBy: String?,
    @Json(name = "container_id") val containerId: String?
)

@JsonClass(generateAdapter = true)
data class DocumentPermission(
    val id: Int,
    val annotation: Int?,
    val folder: Int?,
    val user: UserBasic?,
    @Json(name = "lab_group") val labGroup: LabGroup?,
    @Json(name = "user_id") val userId: Int?,
    @Json(name = "lab_group_id") val labGroupId: Int?,
    @Json(name = "can_view") val canView: Boolean,
    @Json(name = "can_download") val canDownload: Boolean,
    @Json(name = "can_comment") val canComment: Boolean,
    @Json(name = "can_edit") val canEdit: Boolean,
    @Json(name = "can_share") val canShare: Boolean,
    @Json(name = "can_delete") val canDelete: Boolean,
    @Json(name = "shared_by") val sharedBy: UserBasic?,
    @Json(name = "shared_at") val sharedAt: String?,
    @Json(name = "expires_at") val expiresAt: String?,
    @Json(name = "last_accessed") val lastAccessed: String?,
    @Json(name = "access_count") val accessCount: Int,
    @Json(name = "is_expired") val isExpired: Boolean?
)

@JsonClass(generateAdapter = true)
data class ImportTracker(
    val id: Int,
    @Json(name = "import_type") val importType: String?,
    @Json(name = "import_status") val importStatus: String?,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?,
    @Json(name = "created_by") val createdBy: UserBasic?,
    @Json(name = "import_name") val importName: String?,
    @Json(name = "import_description") val importDescription: String?,
    @Json(name = "total_objects") val totalObjects: Int?,
    @Json(name = "processed_objects") val processedObjects: Int?,
    @Json(name = "created_objects") val createdObjects: Int?,
    @Json(name = "updated_objects") val updatedObjects: Int?,
    @Json(name = "failed_objects") val failedObjects: Int?,
    @Json(name = "error_log") val errorLog: String?,
    @Json(name = "import_metadata") val importMetadata: String?,
    @Json(name = "file_size_bytes") val fileSizeBytes: Long?,
    @Json(name = "lab_group") val labGroup: LabGroup?,
    @Json(name = "imported_objects") val importedObjects: List<ImportedObject>?,
    @Json(name = "imported_files") val importedFiles: List<ImportedFile>?,
    @Json(name = "imported_relationships") val importedRelationships: List<ImportedRelationship>?
)

@JsonClass(generateAdapter = true)
data class ImportedObject(
    val id: Int,
    @Json(name = "import_tracker") val importTracker: Int,
    @Json(name = "object_type") val objectType: String,
    @Json(name = "object_id") val objectId: Int,
    @Json(name = "action_type") val actionType: String,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "object_data") val objectData: String?
)

@JsonClass(generateAdapter = true)
data class ImportedFile(
    val id: Int,
    @Json(name = "import_tracker") val importTracker: Int,
    @Json(name = "file_path") val filePath: String,
    @Json(name = "original_filename") val originalFilename: String?,
    @Json(name = "file_size_bytes") val fileSizeBytes: Int?,
    @Json(name = "file_hash") val fileHash: String?,
    @Json(name = "created_at") val createdAt: String?
)

@JsonClass(generateAdapter = true)
data class ImportedRelationship(
    val id: Int,
    @Json(name = "import_tracker") val importTracker: Int,
    @Json(name = "relationship_type") val relationshipType: String,
    @Json(name = "parent_model") val parentModel: String,
    @Json(name = "parent_id") val parentId: Int,
    @Json(name = "child_model") val childModel: String,
    @Json(name = "child_id") val childId: Int,
    @Json(name = "created_at") val createdAt: String?
)