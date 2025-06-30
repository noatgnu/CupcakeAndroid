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
    @Json(name = "updated_by") val updatedBy: UserBasic?
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