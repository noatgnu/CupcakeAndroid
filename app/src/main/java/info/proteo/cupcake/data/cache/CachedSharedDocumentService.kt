package info.proteo.cupcake.data.cache

import android.util.Log
import info.proteo.cupcake.data.remote.service.SharedDocumentService
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
import okhttp3.OkHttpClient
import okhttp3.Request
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced SharedDocument service with local file caching capabilities
 * Wraps the original SharedDocumentService to add caching functionality
 */
@Singleton
class CachedSharedDocumentService @Inject constructor(
    private val sharedDocumentService: SharedDocumentService,
    private val fileCacheManager: FileCacheManager,
    private val okHttpClient: OkHttpClient
) : SharedDocumentService {
    
    companion object {
        private const val TAG = "CachedSharedDocumentService"
    }
    
    // Delegate all non-download operations to the original service
    
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
        return sharedDocumentService.getSharedDocuments(
            annotationType, createdAt, updatedAt, user, folder, session,
            search, ordering, limit, offset
        )
    }
    
    override suspend fun getSharedDocument(id: Int): Result<SharedDocument> {
        return sharedDocumentService.getSharedDocument(id)
    }
    
    override suspend fun createSharedDocument(
        annotation: String,
        annotationName: String?,
        file: String?,
        folderId: Int?,
        summary: String?
    ): Result<SharedDocument> {
        return sharedDocumentService.createSharedDocument(
            annotation, annotationName, file, folderId, summary
        )
    }
    
    override suspend fun updateSharedDocument(
        id: Int,
        annotation: String?,
        annotationName: String?,
        summary: String?,
        fixed: Boolean?,
        scratched: Boolean?
    ): Result<SharedDocument> {
        return sharedDocumentService.updateSharedDocument(
            id, annotation, annotationName, summary, fixed, scratched
        )
    }
    
    override suspend fun patchSharedDocument(
        id: Int,
        annotation: String?,
        annotationName: String?,
        summary: String?,
        fixed: Boolean?,
        scratched: Boolean?
    ): Result<SharedDocument> {
        return sharedDocumentService.patchSharedDocument(
            id, annotation, annotationName, summary, fixed, scratched
        )
    }
    
    override suspend fun deleteSharedDocument(id: Int): Result<Unit> {
        // Delete cached files when shared document is deleted
        fileCacheManager.deleteCachedFilesForAnnotation(id)
        return sharedDocumentService.deleteSharedDocument(id)
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
        return sharedDocumentService.shareDocument(
            id, users, labGroups, canView, canDownload, canComment,
            canEdit, canShare, canDelete, expiresAt
        )
    }
    
    override suspend fun unshareDocument(
        id: Int,
        userId: Int?,
        labGroupId: Int?
    ): Result<Unit> {
        return sharedDocumentService.unshareDocument(id, userId, labGroupId)
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
        return sharedDocumentService.getSharedWithMe(
            annotationType, createdAt, updatedAt, user, folder, session,
            search, ordering, limit, offset
        )
    }
    
    override suspend fun bindChunkedFile(
        chunkedUploadId: String,
        annotationName: String,
        folderId: Int?
    ): Result<SharedDocument> {
        return sharedDocumentService.bindChunkedFile(chunkedUploadId, annotationName, folderId)
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
        return sharedDocumentService.shareFolder(
            folderId, users, labGroups, canView, canDownload, canComment,
            canEdit, canShare, canDelete, expiresAt
        )
    }
    
    // Enhanced download operations with caching
    
    override suspend fun getDownloadUrl(id: Int): Result<DownloadResponse> {
        return sharedDocumentService.getDownloadUrl(id)
    }
    
    /**
     * Download shared document with caching
     * Returns cached file if available, otherwise downloads and caches
     */
    suspend fun downloadSharedDocumentWithCache(
        documentId: Int,
        fileName: String?
    ): Result<CachedFile> {
        // Check if file is already cached
        fileCacheManager.getCachedFile(documentId, fileName)?.let { cachedFile ->
            Log.d(TAG, "Returning cached shared document $documentId: $fileName")
            return Result.success(cachedFile)
        }
        
        return try {
            Log.d(TAG, "Downloading and caching shared document $documentId: $fileName")
            
            // Get download URL
            val downloadUrlResult = sharedDocumentService.getDownloadUrl(documentId)
            if (downloadUrlResult.isFailure) {
                return Result.failure(
                    downloadUrlResult.exceptionOrNull() ?: Exception("Failed to get download URL")
                )
            }
            
            val downloadResponse = downloadUrlResult.getOrThrow()
            
            // Download file using the URL
            val request = Request.Builder()
                .url(downloadResponse.downloadUrl)
                .build()
            
            val response = okHttpClient.newCall(request).execute()
            if (!response.isSuccessful) {
                return Result.failure(Exception("Download failed: ${response.code}"))
            }
            
            val responseBody = response.body
                ?: return Result.failure(Exception("Empty response body"))
            
            // Cache the file
            fileCacheManager.cacheFile(documentId, fileName, responseBody)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading shared document $documentId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Bulk download shared documents with caching
     */
    suspend fun bulkDownloadWithCache(
        documents: List<SharedDocumentDownloadRequest>
    ): Result<List<CachedFile>> {
        val results = mutableListOf<CachedFile>()
        val errors = mutableListOf<Exception>()
        
        try {
            documents.forEach { request ->
                val downloadResult = downloadSharedDocumentWithCache(
                    request.documentId,
                    request.fileName
                )
                
                downloadResult.fold(
                    onSuccess = { cachedFile -> results.add(cachedFile) },
                    onFailure = { error -> errors.add(error as Exception) }
                )
            }
            
            Log.d(TAG, "Bulk download completed: ${results.size} successful, ${errors.size} failed")
            
            return if (errors.isEmpty()) {
                Result.success(results)
            } else {
                Result.failure(Exception("Bulk download partially failed: ${errors.size} errors"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in bulk download", e)
            return Result.failure(e)
        }
    }
    
    /**
     * Check if shared document is cached
     */
    fun isSharedDocumentCached(documentId: Int, fileName: String?): Boolean {
        return fileCacheManager.isFileCached(documentId, fileName)
    }
    
    /**
     * Get cached shared document path
     */
    fun getCachedSharedDocumentPath(documentId: Int, fileName: String?): String? {
        return fileCacheManager.getCachedFile(documentId, fileName)?.localPath
    }
    
    /**
     * Delete cached shared document
     */
    fun deleteCachedSharedDocument(documentId: Int, fileName: String?): Boolean {
        return fileCacheManager.deleteCachedFile(documentId, fileName)
    }
    
    /**
     * Preload shared documents for offline access
     */
    suspend fun preloadSharedDocuments(
        documents: List<SharedDocumentDownloadRequest>
    ): Result<Int> {
        return bulkDownloadWithCache(documents).map { cachedFiles ->
            cachedFiles.size
        }
    }
}

/**
 * Request for downloading shared document
 */
data class SharedDocumentDownloadRequest(
    val documentId: Int,
    val fileName: String?
)