package info.proteo.cupcake.data.repository

import info.proteo.cupcake.data.remote.service.BackupLogService
import info.proteo.cupcake.data.remote.service.BackupStatus
import info.proteo.cupcake.data.remote.service.ConnectionTestResult
import info.proteo.cupcake.data.remote.service.DocumentPermissionService
import info.proteo.cupcake.data.remote.service.RemoteHostService
import info.proteo.cupcake.data.remote.service.SiteSettingsService
import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import info.proteo.cupcake.shared.data.model.system.BackupLog
import info.proteo.cupcake.shared.data.model.system.DocumentPermission
import info.proteo.cupcake.shared.data.model.system.RemoteHost
import info.proteo.cupcake.shared.data.model.system.SiteSettings
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SystemRepository @Inject constructor(
    private val siteSettingsService: SiteSettingsService,
    private val backupLogService: BackupLogService,
    private val documentPermissionService: DocumentPermissionService,
    private val remoteHostService: RemoteHostService
) {

    // Site Settings Operations
    suspend fun getCurrentSiteSettings(): Result<SiteSettings> {
        return siteSettingsService.getCurrentSettings()
    }

    suspend fun createSiteSettings(
        siteName: String? = null,
        siteTagline: String? = null,
        logo: String? = null,
        favicon: String? = null,
        bannerEnabled: Boolean? = null,
        bannerText: String? = null,
        bannerColor: String? = null,
        bannerTextColor: String? = null,
        bannerDismissible: Boolean? = null,
        primaryColor: String? = null,
        secondaryColor: String? = null,
        footerText: String? = null,
        allowImportProtocols: Boolean? = null,
        allowImportReagents: Boolean? = null,
        allowImportStorageObjects: Boolean? = null,
        allowImportInstruments: Boolean? = null,
        allowImportUsers: Boolean? = null,
        allowImportLabGroups: Boolean? = null,
        allowImportSessions: Boolean? = null,
        allowImportProjects: Boolean? = null,
        allowImportAnnotations: Boolean? = null,
        allowImportMetadata: Boolean? = null,
        staffOnlyImportOverride: Boolean? = null,
        importArchiveSizeLimitMb: Int? = null
    ): Result<SiteSettings> {
        return siteSettingsService.createSettings(
            siteName, siteTagline, logo, favicon, bannerEnabled, bannerText,
            bannerColor, bannerTextColor, bannerDismissible, primaryColor,
            secondaryColor, footerText, allowImportProtocols, allowImportReagents,
            allowImportStorageObjects, allowImportInstruments, allowImportUsers,
            allowImportLabGroups, allowImportSessions, allowImportProjects,
            allowImportAnnotations, allowImportMetadata, staffOnlyImportOverride,
            importArchiveSizeLimitMb
        )
    }

    suspend fun updateSiteSettings(
        id: Int,
        siteName: String? = null,
        siteTagline: String? = null,
        logo: String? = null,
        favicon: String? = null,
        bannerEnabled: Boolean? = null,
        bannerText: String? = null,
        bannerColor: String? = null,
        bannerTextColor: String? = null,
        bannerDismissible: Boolean? = null,
        primaryColor: String? = null,
        secondaryColor: String? = null,
        footerText: String? = null,
        allowImportProtocols: Boolean? = null,
        allowImportReagents: Boolean? = null,
        allowImportStorageObjects: Boolean? = null,
        allowImportInstruments: Boolean? = null,
        allowImportUsers: Boolean? = null,
        allowImportLabGroups: Boolean? = null,
        allowImportSessions: Boolean? = null,
        allowImportProjects: Boolean? = null,
        allowImportAnnotations: Boolean? = null,
        allowImportMetadata: Boolean? = null,
        staffOnlyImportOverride: Boolean? = null,
        importArchiveSizeLimitMb: Int? = null
    ): Result<SiteSettings> {
        return siteSettingsService.updateSettings(
            id, siteName, siteTagline, logo, favicon, bannerEnabled, bannerText,
            bannerColor, bannerTextColor, bannerDismissible, primaryColor,
            secondaryColor, footerText, allowImportProtocols, allowImportReagents,
            allowImportStorageObjects, allowImportInstruments, allowImportUsers,
            allowImportLabGroups, allowImportSessions, allowImportProjects,
            allowImportAnnotations, allowImportMetadata, staffOnlyImportOverride,
            importArchiveSizeLimitMb
        )
    }

    suspend fun patchSiteSettings(
        id: Int,
        siteName: String? = null,
        siteTagline: String? = null,
        logo: String? = null,
        favicon: String? = null,
        bannerEnabled: Boolean? = null,
        bannerText: String? = null,
        bannerColor: String? = null,
        bannerTextColor: String? = null,
        bannerDismissible: Boolean? = null,
        primaryColor: String? = null,
        secondaryColor: String? = null,
        footerText: String? = null,
        allowImportProtocols: Boolean? = null,
        allowImportReagents: Boolean? = null,
        allowImportStorageObjects: Boolean? = null,
        allowImportInstruments: Boolean? = null,
        allowImportUsers: Boolean? = null,
        allowImportLabGroups: Boolean? = null,
        allowImportSessions: Boolean? = null,
        allowImportProjects: Boolean? = null,
        allowImportAnnotations: Boolean? = null,
        allowImportMetadata: Boolean? = null,
        staffOnlyImportOverride: Boolean? = null,
        importArchiveSizeLimitMb: Int? = null
    ): Result<SiteSettings> {
        return siteSettingsService.patchSettings(
            id, siteName, siteTagline, logo, favicon, bannerEnabled, bannerText,
            bannerColor, bannerTextColor, bannerDismissible, primaryColor,
            secondaryColor, footerText, allowImportProtocols, allowImportReagents,
            allowImportStorageObjects, allowImportInstruments, allowImportUsers,
            allowImportLabGroups, allowImportSessions, allowImportProjects,
            allowImportAnnotations, allowImportMetadata, staffOnlyImportOverride,
            importArchiveSizeLimitMb
        )
    }

    // Backup Log Operations
    suspend fun getBackupLogs(
        backupType: String? = null,
        status: String? = null,
        search: String? = null,
        ordering: String? = null,
        limit: Int? = null,
        offset: Int? = null
    ): Result<LimitOffsetResponse<BackupLog>> {
        return backupLogService.getBackupLogs(backupType, status, search, ordering, limit, offset)
    }

    suspend fun getBackupLog(id: Int): Result<BackupLog> {
        return backupLogService.getBackupLog(id)
    }

    suspend fun getBackupStatus(): Result<BackupStatus> {
        return backupLogService.getBackupStatus()
    }

    // Document Permission Operations
    suspend fun getDocumentPermissions(
        annotationId: Int? = null,
        userId: Int? = null,
        labGroupId: Int? = null,
        limit: Int? = null,
        offset: Int? = null
    ): Result<LimitOffsetResponse<DocumentPermission>> {
        return documentPermissionService.getDocumentPermissions(annotationId, userId, labGroupId, limit, offset)
    }

    suspend fun getDocumentPermission(id: Int): Result<DocumentPermission> {
        return documentPermissionService.getDocumentPermission(id)
    }

    suspend fun createDocumentPermission(
        annotationId: Int? = null,
        folderId: Int? = null,
        userId: Int? = null,
        labGroupId: Int? = null,
        canView: Boolean = false,
        canDownload: Boolean = false,
        canComment: Boolean = false,
        canEdit: Boolean = false,
        canShare: Boolean = false,
        canDelete: Boolean = false,
        expiresAt: String? = null
    ): Result<DocumentPermission> {
        return documentPermissionService.createPermission(
            annotationId, folderId, userId, labGroupId, canView, canDownload,
            canComment, canEdit, canShare, canDelete, expiresAt
        )
    }

    suspend fun updateDocumentPermission(
        id: Int,
        annotationId: Int? = null,
        folderId: Int? = null,
        userId: Int? = null,
        labGroupId: Int? = null,
        canView: Boolean = false,
        canDownload: Boolean = false,
        canComment: Boolean = false,
        canEdit: Boolean = false,
        canShare: Boolean = false,
        canDelete: Boolean = false,
        expiresAt: String? = null
    ): Result<DocumentPermission> {
        return documentPermissionService.updatePermission(
            id, annotationId, folderId, userId, labGroupId, canView, canDownload,
            canComment, canEdit, canShare, canDelete, expiresAt
        )
    }

    suspend fun deleteDocumentPermission(id: Int): Result<Unit> {
        return documentPermissionService.deletePermission(id)
    }

    // Remote Host Operations
    suspend fun getRemoteHosts(): Result<List<RemoteHost>> {
        return remoteHostService.getRemoteHosts()
    }

    suspend fun getRemoteHost(id: Int): Result<RemoteHost> {
        return remoteHostService.getRemoteHost(id)
    }

    suspend fun createRemoteHost(
        hostName: String,
        hostUrl: String,
        hostToken: String? = null
    ): Result<RemoteHost> {
        return remoteHostService.createRemoteHost(hostName, hostUrl, hostToken)
    }

    suspend fun updateRemoteHost(
        id: Int,
        hostName: String,
        hostUrl: String,
        hostToken: String? = null
    ): Result<RemoteHost> {
        return remoteHostService.updateRemoteHost(id, hostName, hostUrl, hostToken)
    }

    suspend fun deleteRemoteHost(id: Int): Result<Unit> {
        return remoteHostService.deleteRemoteHost(id)
    }

    suspend fun testRemoteHostConnection(id: Int): Result<ConnectionTestResult> {
        return remoteHostService.testConnection(id)
    }
}