package info.proteo.cupcake.data.cache

import android.util.Log
import info.proteo.cupcake.data.remote.service.AnnotationService
import info.proteo.cupcake.data.remote.service.SignedTokenResponse
import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import info.proteo.cupcake.shared.data.model.annotation.Annotation
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.ResponseBody
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Enhanced annotation service with local file caching capabilities
 * Wraps the original AnnotationService to add caching functionality
 */
@Singleton
class CachedAnnotationService @Inject constructor(
    private val annotationService: AnnotationService,
    private val fileCacheManager: FileCacheManager
) : AnnotationService {
    
    companion object {
        private const val TAG = "CachedAnnotationService"
    }
    
    // Delegate all non-file operations to the original service
    
    override suspend fun getAnnotationsInFolder(
        folderId: Int,
        searchTerm: String?,
        limit: Int,
        offset: Int
    ): Result<LimitOffsetResponse<Annotation>> {
        return annotationService.getAnnotationsInFolder(folderId, searchTerm, limit, offset)
    }
    
    override suspend fun getAnnotations(
        stepId: Int?,
        sessionUniqueId: String?,
        search: String?,
        ordering: String?,
        limit: Int?,
        offset: Int?,
        folderId: Int?
    ): Result<LimitOffsetResponse<Annotation>> {
        return annotationService.getAnnotations(
            stepId, sessionUniqueId, search, ordering, limit, offset, folderId
        )
    }
    
    override suspend fun getAnnotationById(id: Int): Result<Annotation> {
        return annotationService.getAnnotationById(id)
    }
    
    override suspend fun createAnnotation(
        partMap: Map<String, RequestBody>,
        file: MultipartBody.Part?
    ): Result<Annotation> {
        return annotationService.createAnnotation(partMap, file)
    }
    
    override suspend fun updateAnnotation(
        id: Int,
        partMap: Map<String, RequestBody>,
        file: MultipartBody.Part?
    ): Result<Annotation> {
        return annotationService.updateAnnotation(id, partMap, file)
    }
    
    override suspend fun deleteAnnotation(id: Int): Result<Unit> {
        // Delete cached files when annotation is deleted
        fileCacheManager.deleteCachedFilesForAnnotation(id)
        return annotationService.deleteAnnotation(id)
    }
    
    override suspend fun retranscribe(id: Int, language: String?): Result<Unit> {
        return annotationService.retranscribe(id, language)
    }
    
    override suspend fun ocr(id: Int): Result<Unit> {
        return annotationService.ocr(id)
    }
    
    override suspend fun scratch(id: Int): Result<Annotation> {
        return annotationService.scratch(id)
    }
    
    override suspend fun renameAnnotation(id: Int, newName: String): Result<Annotation> {
        return annotationService.renameAnnotation(id, newName)
    }
    
    override suspend fun moveToFolder(id: Int, folderId: Int): Result<Annotation> {
        return annotationService.moveToFolder(id, folderId)
    }
    
    override suspend fun bindUploadedFile(request: info.proteo.cupcake.data.remote.service.BindUploadedFileRequest): Result<Annotation> {
        return annotationService.bindUploadedFile(request)
    }
    
    // Enhanced file operations with caching
    
    /**
     * Download file with automatic caching
     * Returns cached file if available, otherwise downloads and caches
     */
    override suspend fun downloadFile(id: Int): Result<ResponseBody> {
        return annotationService.downloadFile(id)
    }
    
    /**
     * Download file with caching using annotation context
     */
    suspend fun downloadFileWithCache(
        annotationId: Int, 
        fileName: String?
    ): Result<CachedFile> {
        // Check if file is already cached
        fileCacheManager.getCachedFile(annotationId, fileName)?.let { cachedFile ->
            Log.d(TAG, "Returning cached file for annotation $annotationId: $fileName")
            return Result.success(cachedFile)
        }
        
        // Download and cache the file
        return try {
            Log.d(TAG, "Downloading and caching file for annotation $annotationId: $fileName")
            
            val downloadResult = annotationService.downloadFile(annotationId)
            if (downloadResult.isFailure) {
                return Result.failure(downloadResult.exceptionOrNull() ?: Exception("Download failed"))
            }
            
            val responseBody = downloadResult.getOrThrow()
            fileCacheManager.cacheFile(annotationId, fileName, responseBody)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading file for annotation $annotationId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Download file using signed URL with caching
     */
    suspend fun downloadSignedFileWithCache(
        annotationId: Int,
        fileName: String?,
        token: String
    ): Result<CachedFile> {
        // Check if file is already cached
        fileCacheManager.getCachedFile(annotationId, fileName)?.let { cachedFile ->
            Log.d(TAG, "Returning cached signed file for annotation $annotationId: $fileName")
            return Result.success(cachedFile)
        }
        
        // Download and cache the file
        return try {
            Log.d(TAG, "Downloading and caching signed file for annotation $annotationId: $fileName")
            
            val downloadResult = annotationService.downloadSignedFile(token)
            if (downloadResult.isFailure) {
                return Result.failure(downloadResult.exceptionOrNull() ?: Exception("Signed download failed"))
            }
            
            val responseBody = downloadResult.getOrThrow()
            fileCacheManager.cacheFile(annotationId, fileName, responseBody)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error downloading signed file for annotation $annotationId", e)
            Result.failure(e)
        }
    }
    
    override suspend fun getSignedUrl(id: Int): Result<SignedTokenResponse> {
        return annotationService.getSignedUrl(id)
    }
    
    override suspend fun downloadSignedFile(token: String): Result<ResponseBody> {
        return annotationService.downloadSignedFile(token)
    }
    
    /**
     * Get signed URL and download file with caching in one operation
     */
    suspend fun getSignedUrlAndDownloadWithCache(
        annotationId: Int,
        fileName: String?
    ): Result<CachedFile> {
        // Check if file is already cached
        fileCacheManager.getCachedFile(annotationId, fileName)?.let { cachedFile ->
            Log.d(TAG, "Returning cached file for signed download annotation $annotationId: $fileName")
            return Result.success(cachedFile)
        }
        
        return try {
            // Get signed URL
            val signedUrlResult = annotationService.getSignedUrl(annotationId)
            if (signedUrlResult.isFailure) {
                return Result.failure(signedUrlResult.exceptionOrNull() ?: Exception("Failed to get signed URL"))
            }
            
            val signedToken = signedUrlResult.getOrThrow().signedToken
            
            // Download with cache using the signed token
            downloadSignedFileWithCache(annotationId, fileName, signedToken)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in signed URL download with cache for annotation $annotationId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Check if annotation file is cached
     */
    fun isAnnotationFileCached(annotationId: Int, fileName: String?): Boolean {
        return fileCacheManager.isFileCached(annotationId, fileName)
    }
    
    /**
     * Get cached file path if exists
     */
    fun getCachedFilePath(annotationId: Int, fileName: String?): String? {
        return fileCacheManager.getCachedFile(annotationId, fileName)?.localPath
    }
    
    /**
     * Delete cached file for annotation
     */
    fun deleteCachedFile(annotationId: Int, fileName: String?): Boolean {
        return fileCacheManager.deleteCachedFile(annotationId, fileName)
    }
    
    /**
     * Get cache statistics
     */
    fun getCacheStats(): CacheStats {
        return fileCacheManager.getCacheStats()
    }
    
    /**
     * Clear all cached files
     */
    suspend fun clearCache(): Boolean {
        return fileCacheManager.clearCache()
    }
}