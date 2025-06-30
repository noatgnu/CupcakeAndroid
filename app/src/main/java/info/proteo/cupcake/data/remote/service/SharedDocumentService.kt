package info.proteo.cupcake.data.remote.service

import android.util.Log
import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import info.proteo.cupcake.shared.data.model.shareddocuments.ChunkedUploadBinding
import info.proteo.cupcake.shared.data.model.shareddocuments.DocumentShare
import info.proteo.cupcake.shared.data.model.shareddocuments.DownloadResponse
import info.proteo.cupcake.shared.data.model.shareddocuments.FolderShare
import info.proteo.cupcake.shared.data.model.shareddocuments.SharedDocument
import info.proteo.cupcake.shared.data.model.shareddocuments.SharedDocumentCreate
import info.proteo.cupcake.shared.data.model.shareddocuments.SharedDocumentUpdate
import info.proteo.cupcake.shared.data.model.shareddocuments.SharedWithMeDocument
import info.proteo.cupcake.shared.data.model.shareddocuments.UnshareRequest
import kotlinx.coroutines.Dispatchers
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

interface SharedDocumentApiService {
    @GET("api/shared_documents/")
    suspend fun getSharedDocuments(
        @Query("annotation_type") annotationType: String? = null,
        @Query("created_at") createdAt: String? = null,
        @Query("updated_at") updatedAt: String? = null,
        @Query("user") user: Int? = null,
        @Query("folder") folder: Int? = null,
        @Query("session") session: Int? = null,
        @Query("search") search: String? = null,
        @Query("ordering") ordering: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): LimitOffsetResponse<SharedDocument>

    @GET("api/shared_documents/{id}/")
    suspend fun getSharedDocument(@Path("id") id: Int): SharedDocument

    @POST("api/shared_documents/")
    suspend fun createSharedDocument(@Body document: SharedDocumentCreate): SharedDocument

    @PUT("api/shared_documents/{id}/")
    suspend fun updateSharedDocument(
        @Path("id") id: Int,
        @Body document: SharedDocumentUpdate
    ): SharedDocument

    @PATCH("api/shared_documents/{id}/")
    suspend fun patchSharedDocument(
        @Path("id") id: Int,
        @Body document: SharedDocumentUpdate
    ): SharedDocument

    @DELETE("api/shared_documents/{id}/")
    suspend fun deleteSharedDocument(@Path("id") id: Int)

    @POST("api/shared_documents/{id}/share/")
    suspend fun shareDocument(
        @Path("id") id: Int,
        @Body shareRequest: DocumentShare
    ): SharedDocument

    @DELETE("api/shared_documents/{id}/unshare/")
    suspend fun unshareDocument(
        @Path("id") id: Int,
        @Body unshareRequest: UnshareRequest
    )

    @GET("api/shared_documents/{id}/download/")
    suspend fun getDownloadUrl(@Path("id") id: Int): DownloadResponse

    @GET("api/shared_documents/shared_with_me/")
    suspend fun getSharedWithMe(
        @Query("annotation_type") annotationType: String? = null,
        @Query("created_at") createdAt: String? = null,
        @Query("updated_at") updatedAt: String? = null,
        @Query("user") user: Int? = null,
        @Query("folder") folder: Int? = null,
        @Query("session") session: Int? = null,
        @Query("search") search: String? = null,
        @Query("ordering") ordering: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): LimitOffsetResponse<SharedWithMeDocument>

    @POST("api/shared_documents/bind_chunked_file/")
    suspend fun bindChunkedFile(@Body binding: ChunkedUploadBinding): SharedDocument

    @POST("api/shared_documents/share_folder/")
    suspend fun shareFolder(@Body folderShare: FolderShare)
}

interface SharedDocumentService {
    suspend fun getSharedDocuments(
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
    ): Result<LimitOffsetResponse<SharedDocument>>

    suspend fun getSharedDocument(id: Int): Result<SharedDocument>

    suspend fun createSharedDocument(
        annotation: String,
        annotationName: String?,
        file: String?,
        folderId: Int? = null,
        summary: String? = null
    ): Result<SharedDocument>

    suspend fun updateSharedDocument(
        id: Int,
        annotation: String? = null,
        annotationName: String? = null,
        summary: String? = null,
        fixed: Boolean? = null,
        scratched: Boolean? = null
    ): Result<SharedDocument>

    suspend fun patchSharedDocument(
        id: Int,
        annotation: String? = null,
        annotationName: String? = null,
        summary: String? = null,
        fixed: Boolean? = null,
        scratched: Boolean? = null
    ): Result<SharedDocument>

    suspend fun deleteSharedDocument(id: Int): Result<Unit>

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
    ): Result<SharedDocument>

    suspend fun unshareDocument(
        id: Int,
        userId: Int? = null,
        labGroupId: Int? = null
    ): Result<Unit>

    suspend fun getDownloadUrl(id: Int): Result<DownloadResponse>

    suspend fun getSharedWithMe(
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
    ): Result<LimitOffsetResponse<SharedWithMeDocument>>

    suspend fun bindChunkedFile(
        chunkedUploadId: String,
        annotationName: String,
        folderId: Int? = null
    ): Result<SharedDocument>

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
    ): Result<Unit>
}

@Singleton
class SharedDocumentServiceImpl @Inject constructor(
    private val apiService: SharedDocumentApiService
) : SharedDocumentService {

    override suspend fun getSharedDocuments(
        annotationType: String?,
        createdAt: String?,
        updatedAt: String?,
        user: Int?,
        folder: Int?,
        session: Int?,
        search: String?,
        ordering: String?,
        limit: Int?,
        offset: Int?
    ): Result<LimitOffsetResponse<SharedDocument>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getSharedDocuments(
                    annotationType, createdAt, updatedAt, user, folder, session,
                    search, ordering, limit, offset
                )
                Log.d("SharedDocumentService", "Fetched ${response.results.size} shared documents from API")
                Result.success(response)
            } catch (e: Exception) {
                Log.e("SharedDocumentService", "Error fetching shared documents", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun getSharedDocument(id: Int): Result<SharedDocument> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getSharedDocument(id)
                Result.success(response)
            } catch (e: Exception) {
                Log.e("SharedDocumentService", "Error fetching shared document $id", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun createSharedDocument(
        annotation: String,
        annotationName: String?,
        file: String?,
        folderId: Int?,
        summary: String?
    ): Result<SharedDocument> {
        return withContext(Dispatchers.IO) {
            try {
                val request = SharedDocumentCreate(
                    annotation = annotation,
                    annotationName = annotationName,
                    file = file,
                    folderId = folderId,
                    summary = summary
                )
                val response = apiService.createSharedDocument(request)
                Log.d("SharedDocumentService", "Created shared document: ${response.id}")
                Result.success(response)
            } catch (e: Exception) {
                Log.e("SharedDocumentService", "Error creating shared document", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun updateSharedDocument(
        id: Int,
        annotation: String?,
        annotationName: String?,
        summary: String?,
        fixed: Boolean?,
        scratched: Boolean?
    ): Result<SharedDocument> {
        return withContext(Dispatchers.IO) {
            try {
                val request = SharedDocumentUpdate(
                    annotation = annotation,
                    annotationName = annotationName,
                    summary = summary,
                    fixed = fixed,
                    scratched = scratched
                )
                val response = apiService.updateSharedDocument(id, request)
                Log.d("SharedDocumentService", "Updated shared document: $id")
                Result.success(response)
            } catch (e: Exception) {
                Log.e("SharedDocumentService", "Error updating shared document $id", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun patchSharedDocument(
        id: Int,
        annotation: String?,
        annotationName: String?,
        summary: String?,
        fixed: Boolean?,
        scratched: Boolean?
    ): Result<SharedDocument> {
        return withContext(Dispatchers.IO) {
            try {
                val request = SharedDocumentUpdate(
                    annotation = annotation,
                    annotationName = annotationName,
                    summary = summary,
                    fixed = fixed,
                    scratched = scratched
                )
                val response = apiService.patchSharedDocument(id, request)
                Log.d("SharedDocumentService", "Patched shared document: $id")
                Result.success(response)
            } catch (e: Exception) {
                Log.e("SharedDocumentService", "Error patching shared document $id", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteSharedDocument(id: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.deleteSharedDocument(id)
                Log.d("SharedDocumentService", "Deleted shared document: $id")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("SharedDocumentService", "Error deleting shared document $id", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun shareDocument(
        id: Int,
        users: List<Int>?,
        labGroups: List<Int>?,
        canView: Boolean,
        canDownload: Boolean,
        canComment: Boolean,
        canEdit: Boolean,
        canShare: Boolean,
        canDelete: Boolean,
        expiresAt: String?
    ): Result<SharedDocument> {
        return withContext(Dispatchers.IO) {
            try {
                val request = DocumentShare(
                    users = users,
                    labGroups = labGroups,
                    permissions = info.proteo.cupcake.shared.data.model.shareddocuments.SharePermissions(
                        canView = canView,
                        canDownload = canDownload,
                        canComment = canComment,
                        canEdit = canEdit,
                        canShare = canShare,
                        canDelete = canDelete,
                        expiresAt = expiresAt
                    )
                )
                val response = apiService.shareDocument(id, request)
                Log.d("SharedDocumentService", "Shared document $id with ${users?.size ?: 0} users and ${labGroups?.size ?: 0} groups")
                Result.success(response)
            } catch (e: Exception) {
                Log.e("SharedDocumentService", "Error sharing document $id", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun unshareDocument(id: Int, userId: Int?, labGroupId: Int?): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val request = UnshareRequest(userId = userId, labGroupId = labGroupId)
                apiService.unshareDocument(id, request)
                Log.d("SharedDocumentService", "Unshared document $id from user: $userId, group: $labGroupId")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("SharedDocumentService", "Error unsharing document $id", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun getDownloadUrl(id: Int): Result<DownloadResponse> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getDownloadUrl(id)
                Log.d("SharedDocumentService", "Generated download URL for document: $id")
                Result.success(response)
            } catch (e: Exception) {
                Log.e("SharedDocumentService", "Error getting download URL for document $id", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun getSharedWithMe(
        annotationType: String?,
        createdAt: String?,
        updatedAt: String?,
        user: Int?,
        folder: Int?,
        session: Int?,
        search: String?,
        ordering: String?,
        limit: Int?,
        offset: Int?
    ): Result<LimitOffsetResponse<SharedWithMeDocument>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getSharedWithMe(
                    annotationType, createdAt, updatedAt, user, folder, session,
                    search, ordering, limit, offset
                )
                Log.d("SharedDocumentService", "Fetched ${response.results.size} documents shared with me")
                Result.success(response)
            } catch (e: Exception) {
                Log.e("SharedDocumentService", "Error fetching documents shared with me", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun bindChunkedFile(
        chunkedUploadId: String,
        annotationName: String,
        folderId: Int?
    ): Result<SharedDocument> {
        return withContext(Dispatchers.IO) {
            try {
                val request = ChunkedUploadBinding(
                    chunkedUploadId = chunkedUploadId,
                    annotationName = annotationName,
                    folderId = folderId
                )
                val response = apiService.bindChunkedFile(request)
                Log.d("SharedDocumentService", "Bound chunked file to shared document: ${response.id}")
                Result.success(response)
            } catch (e: Exception) {
                Log.e("SharedDocumentService", "Error binding chunked file: $chunkedUploadId", e)
                Result.failure(e)
            }
        }
    }

    override suspend fun shareFolder(
        folderId: Int,
        users: List<Int>?,
        labGroups: List<Int>?,
        canView: Boolean,
        canDownload: Boolean,
        canComment: Boolean,
        canEdit: Boolean,
        canShare: Boolean,
        canDelete: Boolean,
        expiresAt: String?
    ): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                val request = FolderShare(
                    folderId = folderId,
                    users = users,
                    labGroups = labGroups,
                    permissions = info.proteo.cupcake.shared.data.model.shareddocuments.SharePermissions(
                        canView = canView,
                        canDownload = canDownload,
                        canComment = canComment,
                        canEdit = canEdit,
                        canShare = canShare,
                        canDelete = canDelete,
                        expiresAt = expiresAt
                    )
                )
                apiService.shareFolder(request)
                Log.d("SharedDocumentService", "Shared folder $folderId with ${users?.size ?: 0} users and ${labGroups?.size ?: 0} groups")
                Result.success(Unit)
            } catch (e: Exception) {
                Log.e("SharedDocumentService", "Error sharing folder $folderId", e)
                Result.failure(e)
            }
        }
    }
}