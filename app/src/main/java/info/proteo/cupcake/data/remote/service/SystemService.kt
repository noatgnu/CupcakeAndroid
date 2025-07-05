package info.proteo.cupcake.data.remote.service

import android.util.Log
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import info.proteo.cupcake.data.local.dao.system.BackupLogDao
import info.proteo.cupcake.data.local.dao.system.DocumentPermissionDao
import info.proteo.cupcake.data.local.dao.system.RemoteHostDao
import info.proteo.cupcake.data.local.dao.system.SiteSettingsDao
import info.proteo.cupcake.data.local.entity.system.BackupLogEntity
import info.proteo.cupcake.data.local.entity.system.DocumentPermissionEntity
import info.proteo.cupcake.data.local.entity.system.RemoteHostEntity
import info.proteo.cupcake.data.local.entity.system.SiteSettingsEntity
import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import info.proteo.cupcake.shared.data.model.system.BackupLog
import info.proteo.cupcake.shared.data.model.system.DocumentPermission
import info.proteo.cupcake.shared.data.model.system.RemoteHost
import info.proteo.cupcake.shared.data.model.system.SiteSettings
import info.proteo.cupcake.shared.data.model.user.UserBasic
import info.proteo.cupcake.shared.data.model.user.LabGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

@JsonClass(generateAdapter = true)
data class SiteSettingsRequest(
    @Json(name = "site_name") val siteName: String?,
    @Json(name = "site_tagline") val siteTagline: String?,
    val logo: String?,
    val favicon: String?,
    @Json(name = "banner_enabled") val bannerEnabled: Boolean?,
    @Json(name = "banner_text") val bannerText: String?,
    @Json(name = "banner_color") val bannerColor: String?,
    @Json(name = "banner_text_color") val bannerTextColor: String?,
    @Json(name = "banner_dismissible") val bannerDismissible: Boolean?,
    @Json(name = "primary_color") val primaryColor: String?,
    @Json(name = "secondary_color") val secondaryColor: String?,
    @Json(name = "footer_text") val footerText: String?,
    // Import restrictions
    @Json(name = "allow_import_protocols") val allowImportProtocols: Boolean?,
    @Json(name = "allow_import_reagents") val allowImportReagents: Boolean?,
    @Json(name = "allow_import_storage_objects") val allowImportStorageObjects: Boolean?,
    @Json(name = "allow_import_instruments") val allowImportInstruments: Boolean?,
    @Json(name = "allow_import_users") val allowImportUsers: Boolean?,
    @Json(name = "allow_import_lab_groups") val allowImportLabGroups: Boolean?,
    @Json(name = "allow_import_sessions") val allowImportSessions: Boolean?,
    @Json(name = "allow_import_projects") val allowImportProjects: Boolean?,
    @Json(name = "allow_import_annotations") val allowImportAnnotations: Boolean?,
    @Json(name = "allow_import_metadata") val allowImportMetadata: Boolean?,
    @Json(name = "staff_only_import_override") val staffOnlyImportOverride: Boolean?,
    @Json(name = "import_archive_size_limit_mb") val importArchiveSizeLimitMb: Int?
)

@JsonClass(generateAdapter = true)
data class DocumentPermissionRequest(
    @Json(name = "annotation_id") val annotationId: Int?,
    @Json(name = "folder_id") val folderId: Int?,
    @Json(name = "user_id") val userId: Int?,
    @Json(name = "lab_group_id") val labGroupId: Int?,
    @Json(name = "can_view") val canView: Boolean = false,
    @Json(name = "can_download") val canDownload: Boolean = false,
    @Json(name = "can_comment") val canComment: Boolean = false,
    @Json(name = "can_edit") val canEdit: Boolean = false,
    @Json(name = "can_share") val canShare: Boolean = false,
    @Json(name = "can_delete") val canDelete: Boolean = false,
    @Json(name = "expires_at") val expiresAt: String?
)

@JsonClass(generateAdapter = true)
data class RemoteHostRequest(
    @Json(name = "host_name") val hostName: String,
    @Json(name = "host_url") val hostUrl: String,
    @Json(name = "host_token") val hostToken: String?
)

@JsonClass(generateAdapter = true)
data class ConnectionTestResult(
    val status: String,
    val message: String?,
    @Json(name = "response_time_ms") val responseTimeMs: Long?
)

@JsonClass(generateAdapter = true)
data class BackupStatus(
    @Json(name = "last_successful_backup") val lastSuccessfulBackup: String?,
    @Json(name = "last_failed_backup") val lastFailedBackup: String?,
    @Json(name = "total_backups_last_30_days") val totalBackupsLast30Days: Int,
    @Json(name = "successful_backups_last_30_days") val successfulBackupsLast30Days: Int,
    @Json(name = "failed_backups_last_30_days") val failedBackupsLast30Days: Int
)

interface SiteSettingsApiService {
    @GET("api/site_settings/")
    suspend fun getCurrentSettings(): SiteSettings

    @POST("api/site_settings/")
    suspend fun createSettings(@Body settings: SiteSettingsRequest): SiteSettings

    @PUT("api/site_settings/{id}/")
    suspend fun updateSettings(@Path("id") id: Int, @Body settings: SiteSettingsRequest): SiteSettings

    @PATCH("api/site_settings/{id}/")
    suspend fun patchSettings(@Path("id") id: Int, @Body settings: SiteSettingsRequest): SiteSettings
}

interface BackupLogApiService {
    @GET("api/backup_logs/")
    suspend fun getBackupLogs(
        @Query("backup_type") backupType: String? = null,
        @Query("status") status: String? = null,
        @Query("search") search: String? = null,
        @Query("ordering") ordering: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): LimitOffsetResponse<BackupLog>

    @GET("api/backup_logs/{id}/")
    suspend fun getBackupLog(@Path("id") id: Int): BackupLog

    @GET("api/backup_logs/backup_status/")
    suspend fun getBackupStatus(): BackupStatus
}

interface DocumentPermissionApiService {
    @GET("api/document_permissions/")
    suspend fun getDocumentPermissions(
        @Query("annotation") annotationId: Int? = null,
        @Query("user") userId: Int? = null,
        @Query("lab_group") labGroupId: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): LimitOffsetResponse<DocumentPermission>

    @GET("api/document_permissions/{id}/")
    suspend fun getDocumentPermission(@Path("id") id: Int): DocumentPermission

    @POST("api/document_permissions/")
    suspend fun createPermission(@Body permission: DocumentPermissionRequest): DocumentPermission

    @PUT("api/document_permissions/{id}/")
    suspend fun updatePermission(@Path("id") id: Int, @Body permission: DocumentPermissionRequest): DocumentPermission

    @DELETE("api/document_permissions/{id}/")
    suspend fun deletePermission(@Path("id") id: Int)
}

interface RemoteHostApiService {
    @GET("api/remote_hosts/")
    suspend fun getRemoteHosts(): List<RemoteHost>

    @GET("api/remote_hosts/{id}/")
    suspend fun getRemoteHost(@Path("id") id: Int): RemoteHost

    @POST("api/remote_hosts/")
    suspend fun createRemoteHost(@Body host: RemoteHostRequest): RemoteHost

    @PUT("api/remote_hosts/{id}/")
    suspend fun updateRemoteHost(@Path("id") id: Int, @Body host: RemoteHostRequest): RemoteHost

    @DELETE("api/remote_hosts/{id}/")
    suspend fun deleteRemoteHost(@Path("id") id: Int)

    @POST("api/remote_hosts/{id}/test_connection/")
    suspend fun testConnection(@Path("id") id: Int): ConnectionTestResult
}

interface SiteSettingsService {
    suspend fun getCurrentSettings(): Result<SiteSettings>
    suspend fun createSettings(siteName: String?, siteTagline: String?, logo: String?, favicon: String?, bannerEnabled: Boolean?, bannerText: String?, bannerColor: String?, bannerTextColor: String?, bannerDismissible: Boolean?, primaryColor: String?, secondaryColor: String?, footerText: String?, allowImportProtocols: Boolean?, allowImportReagents: Boolean?, allowImportStorageObjects: Boolean?, allowImportInstruments: Boolean?, allowImportUsers: Boolean?, allowImportLabGroups: Boolean?, allowImportSessions: Boolean?, allowImportProjects: Boolean?, allowImportAnnotations: Boolean?, allowImportMetadata: Boolean?, staffOnlyImportOverride: Boolean?, importArchiveSizeLimitMb: Int?): Result<SiteSettings>
    suspend fun updateSettings(id: Int, siteName: String?, siteTagline: String?, logo: String?, favicon: String?, bannerEnabled: Boolean?, bannerText: String?, bannerColor: String?, bannerTextColor: String?, bannerDismissible: Boolean?, primaryColor: String?, secondaryColor: String?, footerText: String?, allowImportProtocols: Boolean?, allowImportReagents: Boolean?, allowImportStorageObjects: Boolean?, allowImportInstruments: Boolean?, allowImportUsers: Boolean?, allowImportLabGroups: Boolean?, allowImportSessions: Boolean?, allowImportProjects: Boolean?, allowImportAnnotations: Boolean?, allowImportMetadata: Boolean?, staffOnlyImportOverride: Boolean?, importArchiveSizeLimitMb: Int?): Result<SiteSettings>
    suspend fun patchSettings(id: Int, siteName: String?, siteTagline: String?, logo: String?, favicon: String?, bannerEnabled: Boolean?, bannerText: String?, bannerColor: String?, bannerTextColor: String?, bannerDismissible: Boolean?, primaryColor: String?, secondaryColor: String?, footerText: String?, allowImportProtocols: Boolean?, allowImportReagents: Boolean?, allowImportStorageObjects: Boolean?, allowImportInstruments: Boolean?, allowImportUsers: Boolean?, allowImportLabGroups: Boolean?, allowImportSessions: Boolean?, allowImportProjects: Boolean?, allowImportAnnotations: Boolean?, allowImportMetadata: Boolean?, staffOnlyImportOverride: Boolean?, importArchiveSizeLimitMb: Int?): Result<SiteSettings>
}

interface BackupLogService {
    suspend fun getBackupLogs(backupType: String? = null, status: String? = null, search: String? = null, ordering: String? = null, limit: Int? = null, offset: Int? = null): Result<LimitOffsetResponse<BackupLog>>
    suspend fun getBackupLog(id: Int): Result<BackupLog>
    suspend fun getBackupStatus(): Result<BackupStatus>
}

interface DocumentPermissionService {
    suspend fun getDocumentPermissions(annotationId: Int? = null, userId: Int? = null, labGroupId: Int? = null, limit: Int? = null, offset: Int? = null): Result<LimitOffsetResponse<DocumentPermission>>
    suspend fun getDocumentPermission(id: Int): Result<DocumentPermission>
    suspend fun createPermission(annotationId: Int?, folderId: Int?, userId: Int?, labGroupId: Int?, canView: Boolean, canDownload: Boolean, canComment: Boolean, canEdit: Boolean, canShare: Boolean, canDelete: Boolean, expiresAt: String?): Result<DocumentPermission>
    suspend fun updatePermission(id: Int, annotationId: Int?, folderId: Int?, userId: Int?, labGroupId: Int?, canView: Boolean, canDownload: Boolean, canComment: Boolean, canEdit: Boolean, canShare: Boolean, canDelete: Boolean, expiresAt: String?): Result<DocumentPermission>
    suspend fun deletePermission(id: Int): Result<Unit>
}

interface RemoteHostService {
    suspend fun getRemoteHosts(): Result<List<RemoteHost>>
    suspend fun getRemoteHost(id: Int): Result<RemoteHost>
    suspend fun createRemoteHost(hostName: String, hostUrl: String, hostToken: String?): Result<RemoteHost>
    suspend fun updateRemoteHost(id: Int, hostName: String, hostUrl: String, hostToken: String?): Result<RemoteHost>
    suspend fun deleteRemoteHost(id: Int): Result<Unit>
    suspend fun testConnection(id: Int): Result<ConnectionTestResult>
}

@Singleton
class SiteSettingsServiceImpl @Inject constructor(
    private val apiService: SiteSettingsApiService,
    private val siteSettingsDao: SiteSettingsDao
) : SiteSettingsService {

    override suspend fun getCurrentSettings(): Result<SiteSettings> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getCurrentSettings()
                cacheSiteSettings(response)
                Result.success(response)
            } catch (e: Exception) {
                try {
                    val cachedSettings = siteSettingsDao.getActiveSiteSettings()
                    if (cachedSettings != null) {
                        Result.success(loadSiteSettings(cachedSettings))
                    } else {
                        Result.failure(e)
                    }
                } catch (cacheException: Exception) {
                    Result.failure(e)
                }
            }
        }
    }

    override suspend fun createSettings(siteName: String?, siteTagline: String?, logo: String?, favicon: String?, bannerEnabled: Boolean?, bannerText: String?, bannerColor: String?, bannerTextColor: String?, bannerDismissible: Boolean?, primaryColor: String?, secondaryColor: String?, footerText: String?, allowImportProtocols: Boolean?, allowImportReagents: Boolean?, allowImportStorageObjects: Boolean?, allowImportInstruments: Boolean?, allowImportUsers: Boolean?, allowImportLabGroups: Boolean?, allowImportSessions: Boolean?, allowImportProjects: Boolean?, allowImportAnnotations: Boolean?, allowImportMetadata: Boolean?, staffOnlyImportOverride: Boolean?, importArchiveSizeLimitMb: Int?): Result<SiteSettings> {
        return withContext(Dispatchers.IO) {
            try {
                val request = SiteSettingsRequest(siteName, siteTagline, logo, favicon, bannerEnabled, bannerText, bannerColor, bannerTextColor, bannerDismissible, primaryColor, secondaryColor, footerText, allowImportProtocols, allowImportReagents, allowImportStorageObjects, allowImportInstruments, allowImportUsers, allowImportLabGroups, allowImportSessions, allowImportProjects, allowImportAnnotations, allowImportMetadata, staffOnlyImportOverride, importArchiveSizeLimitMb)
                val response = apiService.createSettings(request)
                cacheSiteSettings(response)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateSettings(id: Int, siteName: String?, siteTagline: String?, logo: String?, favicon: String?, bannerEnabled: Boolean?, bannerText: String?, bannerColor: String?, bannerTextColor: String?, bannerDismissible: Boolean?, primaryColor: String?, secondaryColor: String?, footerText: String?, allowImportProtocols: Boolean?, allowImportReagents: Boolean?, allowImportStorageObjects: Boolean?, allowImportInstruments: Boolean?, allowImportUsers: Boolean?, allowImportLabGroups: Boolean?, allowImportSessions: Boolean?, allowImportProjects: Boolean?, allowImportAnnotations: Boolean?, allowImportMetadata: Boolean?, staffOnlyImportOverride: Boolean?, importArchiveSizeLimitMb: Int?): Result<SiteSettings> {
        return withContext(Dispatchers.IO) {
            try {
                val request = SiteSettingsRequest(siteName, siteTagline, logo, favicon, bannerEnabled, bannerText, bannerColor, bannerTextColor, bannerDismissible, primaryColor, secondaryColor, footerText, allowImportProtocols, allowImportReagents, allowImportStorageObjects, allowImportInstruments, allowImportUsers, allowImportLabGroups, allowImportSessions, allowImportProjects, allowImportAnnotations, allowImportMetadata, staffOnlyImportOverride, importArchiveSizeLimitMb)
                val response = apiService.updateSettings(id, request)
                cacheSiteSettings(response)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun patchSettings(id: Int, siteName: String?, siteTagline: String?, logo: String?, favicon: String?, bannerEnabled: Boolean?, bannerText: String?, bannerColor: String?, bannerTextColor: String?, bannerDismissible: Boolean?, primaryColor: String?, secondaryColor: String?, footerText: String?, allowImportProtocols: Boolean?, allowImportReagents: Boolean?, allowImportStorageObjects: Boolean?, allowImportInstruments: Boolean?, allowImportUsers: Boolean?, allowImportLabGroups: Boolean?, allowImportSessions: Boolean?, allowImportProjects: Boolean?, allowImportAnnotations: Boolean?, allowImportMetadata: Boolean?, staffOnlyImportOverride: Boolean?, importArchiveSizeLimitMb: Int?): Result<SiteSettings> {
        return withContext(Dispatchers.IO) {
            try {
                val request = SiteSettingsRequest(siteName, siteTagline, logo, favicon, bannerEnabled, bannerText, bannerColor, bannerTextColor, bannerDismissible, primaryColor, secondaryColor, footerText, allowImportProtocols, allowImportReagents, allowImportStorageObjects, allowImportInstruments, allowImportUsers, allowImportLabGroups, allowImportSessions, allowImportProjects, allowImportAnnotations, allowImportMetadata, staffOnlyImportOverride, importArchiveSizeLimitMb)
                val response = apiService.patchSettings(id, request)
                cacheSiteSettings(response)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun cacheSiteSettings(siteSettings: SiteSettings) {
        val entity = SiteSettingsEntity(
            id = siteSettings.id,
            isActive = siteSettings.isActive,
            siteName = siteSettings.siteName,
            siteTagline = siteSettings.siteTagline,
            logo = siteSettings.logo,
            favicon = siteSettings.favicon,
            bannerEnabled = siteSettings.bannerEnabled,
            bannerText = siteSettings.bannerText,
            bannerColor = siteSettings.bannerColor,
            bannerTextColor = siteSettings.bannerTextColor,
            bannerDismissible = siteSettings.bannerDismissible,
            primaryColor = siteSettings.primaryColor,
            secondaryColor = siteSettings.secondaryColor,
            footerText = siteSettings.footerText,
            createdAt = siteSettings.createdAt,
            updatedAt = siteSettings.updatedAt,
            updatedBy = siteSettings.updatedBy?.id,
            allowImportProtocols = siteSettings.allowImportProtocols,
            allowImportReagents = siteSettings.allowImportReagents,
            allowImportStorageObjects = siteSettings.allowImportStorageObjects,
            allowImportInstruments = siteSettings.allowImportInstruments,
            allowImportUsers = siteSettings.allowImportUsers,
            allowImportLabGroups = siteSettings.allowImportLabGroups,
            allowImportSessions = siteSettings.allowImportSessions,
            allowImportProjects = siteSettings.allowImportProjects,
            allowImportAnnotations = siteSettings.allowImportAnnotations,
            allowImportMetadata = siteSettings.allowImportMetadata,
            staffOnlyImportOverride = siteSettings.staffOnlyImportOverride,
            importArchiveSizeLimitMb = siteSettings.importArchiveSizeLimitMb
        )
        siteSettingsDao.insert(entity)
    }

    private fun loadSiteSettings(entity: SiteSettingsEntity): SiteSettings {
        return SiteSettings(
            id = entity.id,
            isActive = entity.isActive,
            siteName = entity.siteName,
            siteTagline = entity.siteTagline,
            logo = entity.logo,
            favicon = entity.favicon,
            bannerEnabled = entity.bannerEnabled,
            bannerText = entity.bannerText,
            bannerColor = entity.bannerColor,
            bannerTextColor = entity.bannerTextColor,
            bannerDismissible = entity.bannerDismissible,
            primaryColor = entity.primaryColor,
            secondaryColor = entity.secondaryColor,
            footerText = entity.footerText,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            updatedBy = entity.updatedBy?.let { UserBasic(it, "", null, null) },
            allowImportProtocols = entity.allowImportProtocols,
            allowImportReagents = entity.allowImportReagents,
            allowImportStorageObjects = entity.allowImportStorageObjects,
            allowImportInstruments = entity.allowImportInstruments,
            allowImportUsers = entity.allowImportUsers,
            allowImportLabGroups = entity.allowImportLabGroups,
            allowImportSessions = entity.allowImportSessions,
            allowImportProjects = entity.allowImportProjects,
            allowImportAnnotations = entity.allowImportAnnotations,
            allowImportMetadata = entity.allowImportMetadata,
            staffOnlyImportOverride = entity.staffOnlyImportOverride,
            importArchiveSizeLimitMb = entity.importArchiveSizeLimitMb,
            availableImportOptions = null // This is a computed field from the backend
        )
    }
}

@Singleton
class BackupLogServiceImpl @Inject constructor(
    private val apiService: BackupLogApiService,
    private val backupLogDao: BackupLogDao
) : BackupLogService {

    override suspend fun getBackupLogs(backupType: String?, status: String?, search: String?, ordering: String?, limit: Int?, offset: Int?): Result<LimitOffsetResponse<BackupLog>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getBackupLogs(backupType, status, search, ordering, limit, offset)
                Log.d("BackupLogService", "Fetched ${response.results.size} backup logs from API")
                response.results.forEach { backupLog ->
                    cacheBackupLog(backupLog)
                }
                Result.success(response)
            } catch (e: Exception) {
                try {
                    val cachedLogs = backupLogDao.getAll().first()
                    val logs = cachedLogs.map { loadBackupLog(it) }
                    val limitOffsetResponse = LimitOffsetResponse(
                        count = logs.size,
                        next = null,
                        previous = null,
                        results = logs
                    )
                    Result.success(limitOffsetResponse)
                } catch (cacheException: Exception) {
                    Result.failure(e)
                }
            }
        }
    }

    override suspend fun getBackupLog(id: Int): Result<BackupLog> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getBackupLog(id)
                cacheBackupLog(response)
                Result.success(response)
            } catch (e: Exception) {
                try {
                    val cachedLog = backupLogDao.getById(id)
                    if (cachedLog != null) {
                        Result.success(loadBackupLog(cachedLog))
                    } else {
                        Result.failure(e)
                    }
                } catch (cacheException: Exception) {
                    Result.failure(e)
                }
            }
        }
    }

    override suspend fun getBackupStatus(): Result<BackupStatus> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getBackupStatus()
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun cacheBackupLog(backupLog: BackupLog) {
        val entity = BackupLogEntity(
            id = backupLog.id,
            backupType = backupLog.backupType,
            status = backupLog.status,
            startedAt = backupLog.startedAt,
            completedAt = backupLog.completedAt,
            durationSeconds = backupLog.durationSeconds,
            backupFilePath = backupLog.backupFilePath,
            fileSizeBytes = backupLog.fileSizeBytes,
            errorMessage = backupLog.errorMessage,
            successMessage = backupLog.successMessage,
            triggeredBy = backupLog.triggeredBy,
            containerId = backupLog.containerId
        )
        backupLogDao.insert(entity)
    }

    private fun loadBackupLog(entity: BackupLogEntity): BackupLog {
        return BackupLog(
            id = entity.id,
            backupType = entity.backupType,
            backupTypeDisplay = null, // Display field not stored in entity
            status = entity.status,
            statusDisplay = null, // Display field not stored in entity
            startedAt = entity.startedAt,
            completedAt = entity.completedAt,
            durationSeconds = entity.durationSeconds,
            backupFilePath = entity.backupFilePath,
            fileSizeBytes = entity.fileSizeBytes,
            fileSizeMb = entity.fileSizeBytes?.let { it / (1024f * 1024f) }, // Convert bytes to MB
            errorMessage = entity.errorMessage,
            successMessage = entity.successMessage,
            triggeredBy = entity.triggeredBy,
            containerId = entity.containerId
        )
    }
}

@Singleton
class DocumentPermissionServiceImpl @Inject constructor(
    private val apiService: DocumentPermissionApiService,
    private val documentPermissionDao: DocumentPermissionDao
) : DocumentPermissionService {

    override suspend fun getDocumentPermissions(annotationId: Int?, userId: Int?, labGroupId: Int?, limit: Int?, offset: Int?): Result<LimitOffsetResponse<DocumentPermission>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getDocumentPermissions(annotationId, userId, labGroupId, limit, offset)
                Log.d("DocumentPermissionService", "Fetched ${response.results.size} permissions from API")
                response.results.forEach { permission ->
                    cacheDocumentPermission(permission)
                }
                Result.success(response)
            } catch (e: Exception) {
                try {
                    val cachedPermissions = documentPermissionDao.getByUserId(userId ?: 0).first()
                    val permissions = cachedPermissions.map { loadDocumentPermission(it) }
                    val limitOffsetResponse = LimitOffsetResponse(
                        count = permissions.size,
                        next = null,
                        previous = null,
                        results = permissions
                    )
                    Result.success(limitOffsetResponse)
                } catch (cacheException: Exception) {
                    Result.failure(e)
                }
            }
        }
    }

    override suspend fun getDocumentPermission(id: Int): Result<DocumentPermission> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getDocumentPermission(id)
                cacheDocumentPermission(response)
                Result.success(response)
            } catch (e: Exception) {
                try {
                    val cachedPermission = null // DocumentPermissionDao doesn't have getById method
                    if (cachedPermission != null) {
                        Result.success(loadDocumentPermission(cachedPermission))
                    } else {
                        Result.failure(e)
                    }
                } catch (cacheException: Exception) {
                    Result.failure(e)
                }
            }
        }
    }

    override suspend fun createPermission(annotationId: Int?, folderId: Int?, userId: Int?, labGroupId: Int?, canView: Boolean, canDownload: Boolean, canComment: Boolean, canEdit: Boolean, canShare: Boolean, canDelete: Boolean, expiresAt: String?): Result<DocumentPermission> {
        return withContext(Dispatchers.IO) {
            try {
                val request = DocumentPermissionRequest(annotationId, folderId, userId, labGroupId, canView, canDownload, canComment, canEdit, canShare, canDelete, expiresAt)
                val response = apiService.createPermission(request)
                cacheDocumentPermission(response)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun updatePermission(id: Int, annotationId: Int?, folderId: Int?, userId: Int?, labGroupId: Int?, canView: Boolean, canDownload: Boolean, canComment: Boolean, canEdit: Boolean, canShare: Boolean, canDelete: Boolean, expiresAt: String?): Result<DocumentPermission> {
        return withContext(Dispatchers.IO) {
            try {
                val request = DocumentPermissionRequest(annotationId, folderId, userId, labGroupId, canView, canDownload, canComment, canEdit, canShare, canDelete, expiresAt)
                val response = apiService.updatePermission(id, request)
                cacheDocumentPermission(response)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun deletePermission(id: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.deletePermission(id)
                // Cannot delete by ID since DocumentPermissionDao doesn't have getById method
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun cacheDocumentPermission(permission: DocumentPermission) {
        val entity = DocumentPermissionEntity(
            id = permission.id,
            annotationId = permission.annotation,
            folderId = permission.folder,
            userId = permission.userId,
            labGroupId = permission.labGroupId,
            canView = permission.canView,
            canDownload = permission.canDownload,
            canComment = permission.canComment,
            canEdit = permission.canEdit,
            canShare = permission.canShare,
            canDelete = permission.canDelete,
            sharedBy = permission.sharedBy?.id,
            sharedAt = permission.sharedAt,
            expiresAt = permission.expiresAt,
            lastAccessed = permission.lastAccessed,
            accessCount = permission.accessCount
        )
        documentPermissionDao.insert(entity)
    }

    private fun loadDocumentPermission(entity: DocumentPermissionEntity): DocumentPermission {
        return DocumentPermission(
            id = entity.id,
            annotation = entity.annotationId,
            folder = entity.folderId,
            user = entity.userId?.let { UserBasic(it, "", null, null) },
            labGroup = entity.labGroupId?.let { LabGroup(it, "", null, null, null, null, false, null) },
            userId = entity.userId,
            labGroupId = entity.labGroupId,
            canView = entity.canView,
            canDownload = entity.canDownload,
            canComment = entity.canComment,
            canEdit = entity.canEdit,
            canShare = entity.canShare,
            canDelete = entity.canDelete,
            sharedBy = entity.sharedBy?.let { UserBasic(it, "", null, null) },
            sharedAt = entity.sharedAt,
            expiresAt = entity.expiresAt,
            lastAccessed = entity.lastAccessed,
            accessCount = entity.accessCount,
            isExpired = null // Entity doesn't store this calculated field
        )
    }
}

@Singleton
class RemoteHostServiceImpl @Inject constructor(
    private val apiService: RemoteHostApiService,
    private val remoteHostDao: RemoteHostDao
) : RemoteHostService {

    override suspend fun getRemoteHosts(): Result<List<RemoteHost>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getRemoteHosts()
                Log.d("RemoteHostService", "Fetched ${response.size} remote hosts from API")
                response.forEach { remoteHost ->
                    cacheRemoteHost(remoteHost)
                }
                Result.success(response)
            } catch (e: Exception) {
                try {
                    val cachedHosts = remoteHostDao.getAll().first()
                    val hosts = cachedHosts.map { loadRemoteHost(it) }
                    Result.success(hosts)
                } catch (cacheException: Exception) {
                    Result.failure(e)
                }
            }
        }
    }

    override suspend fun getRemoteHost(id: Int): Result<RemoteHost> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getRemoteHost(id)
                cacheRemoteHost(response)
                Result.success(response)
            } catch (e: Exception) {
                try {
                    val cachedHost = remoteHostDao.getById(id)
                    if (cachedHost != null) {
                        Result.success(loadRemoteHost(cachedHost))
                    } else {
                        Result.failure(e)
                    }
                } catch (cacheException: Exception) {
                    Result.failure(e)
                }
            }
        }
    }

    override suspend fun createRemoteHost(hostName: String, hostUrl: String, hostToken: String?): Result<RemoteHost> {
        return withContext(Dispatchers.IO) {
            try {
                val request = RemoteHostRequest(hostName, hostUrl, hostToken)
                val response = apiService.createRemoteHost(request)
                cacheRemoteHost(response)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateRemoteHost(id: Int, hostName: String, hostUrl: String, hostToken: String?): Result<RemoteHost> {
        return withContext(Dispatchers.IO) {
            try {
                val request = RemoteHostRequest(hostName, hostUrl, hostToken)
                val response = apiService.updateRemoteHost(id, request)
                cacheRemoteHost(response)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteRemoteHost(id: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.deleteRemoteHost(id)
                remoteHostDao.getById(id)?.let { remoteHostDao.delete(it) }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun testConnection(id: Int): Result<ConnectionTestResult> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.testConnection(id)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun cacheRemoteHost(remoteHost: RemoteHost) {
        val entity = RemoteHostEntity(
            id = remoteHost.id,
            hostName = remoteHost.hostName,
            hostUrl = remoteHost.hostUrl,
            hostToken = remoteHost.hostToken,
            createdAt = null, // RemoteHost model doesn't have createdAt
            updatedAt = null  // RemoteHost model doesn't have updatedAt
        )
        remoteHostDao.insert(entity)
    }

    private fun loadRemoteHost(entity: RemoteHostEntity): RemoteHost {
        return RemoteHost(
            id = entity.id,
            hostName = entity.hostName,
            hostUrl = entity.hostUrl,
            hostToken = entity.hostToken
            // RemoteHost model doesn't have createdAt and updatedAt fields
        )
    }
}