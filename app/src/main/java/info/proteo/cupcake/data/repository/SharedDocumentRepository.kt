package info.proteo.cupcake.data.repository

import info.proteo.cupcake.data.remote.service.SharedDocumentService
import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import info.proteo.cupcake.shared.data.model.shareddocuments.DownloadResponse
import info.proteo.cupcake.shared.data.model.shareddocuments.SharedDocument
import info.proteo.cupcake.shared.data.model.shareddocuments.SharedWithMeDocument
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SharedDocumentRepository @Inject constructor(
    private val sharedDocumentService: SharedDocumentService
) {

    // Shared Documents Operations
    fun getSharedDocuments(
        annotationType: String? = null,
        createdAt: String? = null,
        updatedAt: String? = null,
        user: Int? = null,
        folder: Int? = null,
        session: Int? = null,
        search: String? = null,
        ordering: String? = null,
        limit: Int? = null,
        offset: Int? = null
    ): Flow<Result<LimitOffsetResponse<SharedDocument>>> = flow {
        emit(
            sharedDocumentService.getSharedDocuments(
                annotationType, createdAt, updatedAt, user, folder, session,
                search, ordering, limit, offset
            )
        )
    }

    fun getSharedDocument(id: Int): Flow<Result<SharedDocument>> = flow {
        emit(sharedDocumentService.getSharedDocument(id))
    }

    suspend fun createSharedDocument(
        annotation: String,
        annotationName: String? = null,
        file: String? = null,
        folderId: Int? = null,
        summary: String? = null
    ): Result<SharedDocument> {
        return sharedDocumentService.createSharedDocument(
            annotation, annotationName, file, folderId, summary
        )
    }

    suspend fun updateSharedDocument(
        id: Int,
        annotation: String? = null,
        annotationName: String? = null,
        summary: String? = null,
        fixed: Boolean? = null,
        scratched: Boolean? = null
    ): Result<SharedDocument> {
        return sharedDocumentService.updateSharedDocument(
            id, annotation, annotationName, summary, fixed, scratched
        )
    }

    suspend fun patchSharedDocument(
        id: Int,
        annotation: String? = null,
        annotationName: String? = null,
        summary: String? = null,
        fixed: Boolean? = null,
        scratched: Boolean? = null
    ): Result<SharedDocument> {
        return sharedDocumentService.patchSharedDocument(
            id, annotation, annotationName, summary, fixed, scratched
        )
    }

    suspend fun deleteSharedDocument(id: Int): Result<Unit> {
        return sharedDocumentService.deleteSharedDocument(id)
    }

    // Document Sharing Operations
    suspend fun shareDocument(
        id: Int,
        users: List<Int>? = null,
        labGroups: List<Int>? = null,
        canView: Boolean = true,
        canDownload: Boolean = true,
        canComment: Boolean = false,
        canEdit: Boolean = false,
        canShare: Boolean = false,
        canDelete: Boolean = false,
        expiresAt: String? = null
    ): Result<SharedDocument> {
        return sharedDocumentService.shareDocument(
            id, users, labGroups, canView, canDownload, canComment,
            canEdit, canShare, canDelete, expiresAt
        )
    }

    suspend fun shareDocumentWithUser(
        id: Int,
        userId: Int,
        canView: Boolean = true,
        canDownload: Boolean = true,
        canComment: Boolean = false,
        canEdit: Boolean = false,
        canShare: Boolean = false,
        canDelete: Boolean = false,
        expiresAt: String? = null
    ): Result<SharedDocument> {
        return shareDocument(
            id, listOf(userId), null, canView, canDownload,
            canComment, canEdit, canShare, canDelete, expiresAt
        )
    }

    suspend fun shareDocumentWithGroup(
        id: Int,
        labGroupId: Int,
        canView: Boolean = true,
        canDownload: Boolean = true,
        canComment: Boolean = false,
        canEdit: Boolean = false,
        canShare: Boolean = false,
        canDelete: Boolean = false,
        expiresAt: String? = null
    ): Result<SharedDocument> {
        return shareDocument(
            id, null, listOf(labGroupId), canView, canDownload,
            canComment, canEdit, canShare, canDelete, expiresAt
        )
    }

    suspend fun unshareDocumentFromUser(id: Int, userId: Int): Result<Unit> {
        return sharedDocumentService.unshareDocument(id, userId, null)
    }

    suspend fun unshareDocumentFromGroup(id: Int, labGroupId: Int): Result<Unit> {
        return sharedDocumentService.unshareDocument(id, null, labGroupId)
    }

    // Download Operations
    fun getDownloadUrl(id: Int): Flow<Result<DownloadResponse>> = flow {
        emit(sharedDocumentService.getDownloadUrl(id))
    }

    // Shared With Me Operations
    fun getSharedWithMe(
        annotationType: String? = null,
        createdAt: String? = null,
        updatedAt: String? = null,
        user: Int? = null,
        folder: Int? = null,
        session: Int? = null,
        search: String? = null,
        ordering: String? = null,
        limit: Int? = null,
        offset: Int? = null
    ): Flow<Result<LimitOffsetResponse<SharedWithMeDocument>>> = flow {
        emit(
            sharedDocumentService.getSharedWithMe(
                annotationType, createdAt, updatedAt, user, folder, session,
                search, ordering, limit, offset
            )
        )
    }

    // File Upload Operations
    suspend fun bindChunkedFile(
        chunkedUploadId: String,
        annotationName: String,
        folderId: Int? = null
    ): Result<SharedDocument> {
        return sharedDocumentService.bindChunkedFile(chunkedUploadId, annotationName, folderId)
    }

    // Folder Sharing Operations
    suspend fun shareFolder(
        folderId: Int,
        users: List<Int>? = null,
        labGroups: List<Int>? = null,
        canView: Boolean = true,
        canDownload: Boolean = true,
        canComment: Boolean = false,
        canEdit: Boolean = false,
        canShare: Boolean = false,
        canDelete: Boolean = false,
        expiresAt: String? = null
    ): Result<Unit> {
        return sharedDocumentService.shareFolder(
            folderId, users, labGroups, canView, canDownload,
            canComment, canEdit, canShare, canDelete, expiresAt
        )
    }

    suspend fun shareFolderWithUser(
        folderId: Int,
        userId: Int,
        canView: Boolean = true,
        canDownload: Boolean = true,
        canComment: Boolean = false,
        canEdit: Boolean = false,
        canShare: Boolean = false,
        canDelete: Boolean = false,
        expiresAt: String? = null
    ): Result<Unit> {
        return shareFolder(
            folderId, listOf(userId), null, canView, canDownload,
            canComment, canEdit, canShare, canDelete, expiresAt
        )
    }

    suspend fun shareFolderWithGroup(
        folderId: Int,
        labGroupId: Int,
        canView: Boolean = true,
        canDownload: Boolean = true,
        canComment: Boolean = false,
        canEdit: Boolean = false,
        canShare: Boolean = false,
        canDelete: Boolean = false,
        expiresAt: String? = null
    ): Result<Unit> {
        return shareFolder(
            folderId, null, listOf(labGroupId), canView, canDownload,
            canComment, canEdit, canShare, canDelete, expiresAt
        )
    }

    // Convenience Methods
    suspend fun createAndShareDocument(
        annotation: String,
        annotationName: String?,
        file: String?,
        folderId: Int? = null,
        summary: String? = null,
        shareWithUsers: List<Int>? = null,
        shareWithGroups: List<Int>? = null,
        sharePermissions: DocumentSharePermissions = DocumentSharePermissions()
    ): Result<SharedDocument> {
        val createResult = createSharedDocument(annotation, annotationName, file, folderId, summary)
        
        return if (createResult.isSuccess && (!shareWithUsers.isNullOrEmpty() || !shareWithGroups.isNullOrEmpty())) {
            val document = createResult.getOrNull()!!
            shareDocument(
                document.id,
                shareWithUsers,
                shareWithGroups,
                sharePermissions.canView,
                sharePermissions.canDownload,
                sharePermissions.canComment,
                sharePermissions.canEdit,
                sharePermissions.canShare,
                sharePermissions.canDelete,
                sharePermissions.expiresAt
            )
        } else {
            createResult
        }
    }

    suspend fun saveSharedDocument(document: SharedDocument): Result<SharedDocument> {
        return updateSharedDocument(
            id = document.id,
            annotation = document.annotation,
            annotationName = document.annotationName,
            summary = document.summary,
            fixed = document.fixed,
            scratched = document.scratched
        )
    }
}

data class DocumentSharePermissions(
    val canView: Boolean = true,
    val canDownload: Boolean = true,
    val canComment: Boolean = false,
    val canEdit: Boolean = false,
    val canShare: Boolean = false,
    val canDelete: Boolean = false,
    val expiresAt: String? = null
)