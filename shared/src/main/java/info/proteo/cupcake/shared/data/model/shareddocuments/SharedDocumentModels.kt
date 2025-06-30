package info.proteo.cupcake.shared.data.model.shareddocuments

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import info.proteo.cupcake.shared.data.model.annotation.AnnotationFolder
import info.proteo.cupcake.shared.data.model.system.DocumentPermission
import info.proteo.cupcake.shared.data.model.user.UserBasic

@JsonClass(generateAdapter = true)
data class SharedDocument(
    val id: Int,
    val annotation: String,
    @Json(name = "annotation_name") val annotationName: String?,
    val file: String?,
    @Json(name = "annotation_type") val annotationType: String,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?,
    val summary: String?,
    val fixed: Boolean = false,
    val scratched: Boolean = false,
    val transcribed: Boolean = false,
    val transcription: String?,
    val language: String?,
    val translation: String?,
    val user: UserBasic?,
    val session: Int?,
    val step: Int?,
    val folder: AnnotationFolder?,
    @Json(name = "stored_reagent") val storedReagent: Int?,
    @Json(name = "document_permissions") val documentPermissions: List<DocumentPermission>?,
    @Json(name = "user_permissions") val userPermissions: UserDocumentPermissions?,
    @Json(name = "sharing_stats") val sharingStats: SharingStats?,
    @Json(name = "file_info") val fileInfo: FileInfo?
)

@JsonClass(generateAdapter = true)
data class UserDocumentPermissions(
    @Json(name = "can_view") val canView: Boolean = false,
    @Json(name = "can_download") val canDownload: Boolean = false,
    @Json(name = "can_comment") val canComment: Boolean = false,
    @Json(name = "can_edit") val canEdit: Boolean = false,
    @Json(name = "can_share") val canShare: Boolean = false,
    @Json(name = "can_delete") val canDelete: Boolean = false,
    @Json(name = "is_owner") val isOwner: Boolean = false
)

@JsonClass(generateAdapter = true)
data class SharingStats(
    @Json(name = "total_shares") val totalShares: Int = 0,
    @Json(name = "user_shares") val userShares: Int = 0,
    @Json(name = "group_shares") val groupShares: Int = 0,
    @Json(name = "total_downloads") val totalDownloads: Int = 0,
    @Json(name = "unique_viewers") val uniqueViewers: Int = 0,
    @Json(name = "last_accessed") val lastAccessed: String?
)

@JsonClass(generateAdapter = true)
data class FileInfo(
    @Json(name = "file_name") val fileName: String?,
    @Json(name = "file_size") val fileSize: Long?,
    @Json(name = "content_type") val contentType: String?,
    @Json(name = "file_url") val fileUrl: String?
)

@JsonClass(generateAdapter = true)
data class DocumentShare(
    val users: List<Int>? = null,
    @Json(name = "lab_groups") val labGroups: List<Int>? = null,
    val permissions: SharePermissions
)

@JsonClass(generateAdapter = true)
data class SharePermissions(
    @Json(name = "can_view") val canView: Boolean = true,
    @Json(name = "can_download") val canDownload: Boolean = true,
    @Json(name = "can_comment") val canComment: Boolean = false,
    @Json(name = "can_edit") val canEdit: Boolean = false,
    @Json(name = "can_share") val canShare: Boolean = false,
    @Json(name = "can_delete") val canDelete: Boolean = false,
    @Json(name = "expires_at") val expiresAt: String? = null
)

@JsonClass(generateAdapter = true)
data class FolderShare(
    @Json(name = "folder_id") val folderId: Int,
    val users: List<Int>? = null,
    @Json(name = "lab_groups") val labGroups: List<Int>? = null,
    val permissions: SharePermissions
)

@JsonClass(generateAdapter = true)
data class UnshareRequest(
    @Json(name = "user_id") val userId: Int? = null,
    @Json(name = "lab_group_id") val labGroupId: Int? = null
)

@JsonClass(generateAdapter = true)
data class ChunkedUploadBinding(
    @Json(name = "chunked_upload_id") val chunkedUploadId: String,
    @Json(name = "annotation_name") val annotationName: String,
    @Json(name = "folder_id") val folderId: Int? = null
)

@JsonClass(generateAdapter = true)
data class SharedDocumentCreate(
    val annotation: String,
    @Json(name = "annotation_name") val annotationName: String?,
    val file: String?,
    @Json(name = "folder_id") val folderId: Int? = null,
    val summary: String? = null
)

@JsonClass(generateAdapter = true)
data class SharedDocumentUpdate(
    val annotation: String? = null,
    @Json(name = "annotation_name") val annotationName: String? = null,
    val summary: String? = null,
    val fixed: Boolean? = null,
    val scratched: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class DownloadResponse(
    @Json(name = "download_url") val downloadUrl: String,
    @Json(name = "access_token") val accessToken: String? = null,
    @Json(name = "expires_at") val expiresAt: String? = null
)

@JsonClass(generateAdapter = true)
data class SharedWithMeDocument(
    val id: Int,
    val annotation: String,
    @Json(name = "annotation_name") val annotationName: String?,
    val file: String?,
    @Json(name = "annotation_type") val annotationType: String,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?,
    val summary: String?,
    val user: UserBasic?,
    @Json(name = "shared_by") val sharedBy: UserBasic?,
    @Json(name = "shared_at") val sharedAt: String?,
    @Json(name = "expires_at") val expiresAt: String?,
    @Json(name = "folder_path") val folderPath: String?,
    @Json(name = "user_permissions") val userPermissions: UserDocumentPermissions?,
    @Json(name = "file_info") val fileInfo: FileInfo?
)