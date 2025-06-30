package info.proteo.cupcake.data.local.entity.system

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "remote_host")
data class RemoteHostEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "host_name") val hostName: String,
    @ColumnInfo(name = "host_url") val hostUrl: String,
    @ColumnInfo(name = "host_token") val hostToken: String?,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?
)

@Entity(tableName = "site_settings")
data class SiteSettingsEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "is_active") val isActive: Boolean = false,
    @ColumnInfo(name = "site_name") val siteName: String?,
    @ColumnInfo(name = "site_tagline") val siteTagline: String?,
    val logo: String?,
    val favicon: String?,
    @ColumnInfo(name = "banner_enabled") val bannerEnabled: Boolean = false,
    @ColumnInfo(name = "banner_text") val bannerText: String?,
    @ColumnInfo(name = "banner_color") val bannerColor: String?,
    @ColumnInfo(name = "banner_text_color") val bannerTextColor: String?,
    @ColumnInfo(name = "banner_dismissible") val bannerDismissible: Boolean = true,
    @ColumnInfo(name = "primary_color") val primaryColor: String?,
    @ColumnInfo(name = "secondary_color") val secondaryColor: String?,
    @ColumnInfo(name = "footer_text") val footerText: String?,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "updated_by") val updatedBy: Int?
)

@Entity(tableName = "backup_log")
data class BackupLogEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "backup_type") val backupType: String,
    val status: String,
    @ColumnInfo(name = "started_at") val startedAt: String?,
    @ColumnInfo(name = "completed_at") val completedAt: String?,
    @ColumnInfo(name = "duration_seconds") val durationSeconds: Int?,
    @ColumnInfo(name = "backup_file_path") val backupFilePath: String?,
    @ColumnInfo(name = "file_size_bytes") val fileSizeBytes: Long?,
    @ColumnInfo(name = "error_message") val errorMessage: String?,
    @ColumnInfo(name = "success_message") val successMessage: String?,
    @ColumnInfo(name = "triggered_by") val triggeredBy: String?,
    @ColumnInfo(name = "container_id") val containerId: String?
)

@Entity(tableName = "document_permission")
data class DocumentPermissionEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "annotation_id") val annotationId: Int?,
    @ColumnInfo(name = "folder_id") val folderId: Int?,
    @ColumnInfo(name = "user_id") val userId: Int?,
    @ColumnInfo(name = "lab_group_id") val labGroupId: Int?,
    @ColumnInfo(name = "can_view") val canView: Boolean = false,
    @ColumnInfo(name = "can_download") val canDownload: Boolean = false,
    @ColumnInfo(name = "can_comment") val canComment: Boolean = false,
    @ColumnInfo(name = "can_edit") val canEdit: Boolean = false,
    @ColumnInfo(name = "can_share") val canShare: Boolean = false,
    @ColumnInfo(name = "can_delete") val canDelete: Boolean = false,
    @ColumnInfo(name = "shared_by") val sharedBy: Int?,
    @ColumnInfo(name = "shared_at") val sharedAt: String?,
    @ColumnInfo(name = "expires_at") val expiresAt: String?,
    @ColumnInfo(name = "last_accessed") val lastAccessed: String?,
    @ColumnInfo(name = "access_count") val accessCount: Int = 0
)