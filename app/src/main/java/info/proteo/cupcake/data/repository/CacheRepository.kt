package info.proteo.cupcake.data.repository

import android.util.Log
import info.proteo.cupcake.data.cache.*
import info.proteo.cupcake.shared.data.model.annotation.Annotation
import info.proteo.cupcake.shared.data.model.shareddocuments.SharedDocument
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository for managing all cache operations across the application
 * Provides unified interface for annotation, media, and document caching
 */
@Singleton
class CacheRepository @Inject constructor(
    private val fileCacheManager: FileCacheManager,
    private val mediaCacheHandler: MediaCacheHandler,
    private val cachedAnnotationService: CachedAnnotationService,
    private val cachedSharedDocumentService: CachedSharedDocumentService
) {
    
    companion object {
        private const val TAG = "CacheRepository"
    }
    
    // Annotation caching operations
    
    /**
     * Download and cache annotation file with metadata
     */
    suspend fun cacheAnnotationFile(
        annotation: Annotation
    ): Result<CachedFile> {
        return try {
            val fileName = annotation.file?.substringAfterLast('/') 
                ?: annotation.annotationName 
                ?: "annotation_${annotation.id}"
            
            cachedAnnotationService.downloadFileWithCache(annotation.id, fileName)
        } catch (e: Exception) {
            Log.e(TAG, "Error caching annotation file for ${annotation.id}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Cache annotation using signed URL
     */
    suspend fun cacheAnnotationWithSignedUrl(
        annotation: Annotation
    ): Result<CachedFile> {
        return try {
            val fileName = annotation.file?.substringAfterLast('/') 
                ?: annotation.annotationName 
                ?: "annotation_${annotation.id}"
            
            cachedAnnotationService.getSignedUrlAndDownloadWithCache(annotation.id, fileName)
        } catch (e: Exception) {
            Log.e(TAG, "Error caching annotation with signed URL for ${annotation.id}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Bulk cache multiple annotations
     */
    suspend fun bulkCacheAnnotations(
        annotations: List<Annotation>
    ): Result<List<CachedFile>> = withContext(Dispatchers.IO) {
        val results = mutableListOf<CachedFile>()
        val errors = mutableListOf<Exception>()
        
        try {
            annotations.forEach { annotation ->
                if (annotation.file != null) {
                    val cacheResult = cacheAnnotationFile(annotation)
                    cacheResult.fold(
                        onSuccess = { cachedFile -> results.add(cachedFile) },
                        onFailure = { error -> errors.add(error as Exception) }
                    )
                }
            }
            
            Log.d(TAG, "Bulk cached ${results.size} annotations, ${errors.size} errors")
            
            return@withContext if (errors.isEmpty()) {
                Result.success(results)
            } else {
                Result.failure(Exception("Bulk cache partially failed: ${errors.size} errors"))
            }
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in bulk annotation caching", e)
            Result.failure(e)
        }
    }
    
    // Media caching operations
    
    /**
     * Cache image annotation with thumbnail generation
     */
    suspend fun cacheImageAnnotation(
        annotation: Annotation
    ): Result<ImageCacheResult> {
        return try {
            if (annotation.annotationType != "image") {
                return Result.failure(Exception("Annotation is not an image type"))
            }
            
            // First cache the file
            val cacheResult = cacheAnnotationFile(annotation)
            if (cacheResult.isFailure) {
                return Result.failure(cacheResult.exceptionOrNull() ?: Exception("Failed to cache image"))
            }
            
            val cachedFile = cacheResult.getOrThrow()
            val imageFile = java.io.File(cachedFile.localPath)
            
            // Generate thumbnail and metadata
            val fileName = annotation.file?.substringAfterLast('/') 
                ?: annotation.annotationName 
                ?: "image_${annotation.id}"
            
            mediaCacheHandler.cacheImageWithThumbnail(annotation.id, fileName, imageFile)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error caching image annotation ${annotation.id}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Cache audio annotation with metadata extraction
     */
    suspend fun cacheAudioAnnotation(
        annotation: Annotation
    ): Result<AudioCacheResult> {
        return try {
            if (!listOf("audio", "voice").contains(annotation.annotationType)) {
                return Result.failure(Exception("Annotation is not an audio type"))
            }
            
            // First cache the file
            val cacheResult = cacheAnnotationFile(annotation)
            if (cacheResult.isFailure) {
                return Result.failure(cacheResult.exceptionOrNull() ?: Exception("Failed to cache audio"))
            }
            
            val cachedFile = cacheResult.getOrThrow()
            val audioFile = java.io.File(cachedFile.localPath)
            
            // Extract metadata
            val fileName = annotation.file?.substringAfterLast('/') 
                ?: annotation.annotationName 
                ?: "audio_${annotation.id}"
            
            mediaCacheHandler.cacheAudioWithMetadata(annotation.id, fileName, audioFile)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error caching audio annotation ${annotation.id}", e)
            Result.failure(e)
        }
    }
    
    // SharedDocument caching operations
    
    /**
     * Cache shared document
     */
    suspend fun cacheSharedDocument(
        document: SharedDocument
    ): Result<CachedFile> {
        return try {
            val fileName = document.annotationName ?: "document_${document.id}"
            cachedSharedDocumentService.downloadSharedDocumentWithCache(document.id, fileName)
        } catch (e: Exception) {
            Log.e(TAG, "Error caching shared document ${document.id}", e)
            Result.failure(e)
        }
    }
    
    /**
     * Bulk cache shared documents
     */
    suspend fun bulkCacheSharedDocuments(
        documents: List<SharedDocument>
    ): Result<List<CachedFile>> {
        return try {
            val downloadRequests = documents.map { document ->
                SharedDocumentDownloadRequest(
                    documentId = document.id,
                    fileName = document.annotationName ?: "document_${document.id}"
                )
            }
            
            cachedSharedDocumentService.bulkDownloadWithCache(downloadRequests)
        } catch (e: Exception) {
            Log.e(TAG, "Error bulk caching shared documents", e)
            Result.failure(e)
        }
    }
    
    // Smart caching operations
    
    /**
     * Smart cache based on annotation type
     */
    suspend fun smartCacheAnnotation(
        annotation: Annotation
    ): Result<Any> {
        return when (annotation.annotationType?.lowercase()) {
            "image" -> cacheImageAnnotation(annotation)
            "audio", "voice" -> cacheAudioAnnotation(annotation)
            else -> cacheAnnotationFile(annotation)
        }
    }
    
    /**
     * Preload frequently accessed content
     */
    suspend fun preloadFrequentContent(
        annotations: List<Annotation>,
        sharedDocuments: List<SharedDocument> = emptyList()
    ): Result<PreloadResult> = withContext(Dispatchers.IO) {
        try {
            val annotationResults = bulkCacheAnnotations(annotations)
            val documentResults = if (sharedDocuments.isNotEmpty()) {
                bulkCacheSharedDocuments(sharedDocuments)
            } else {
                Result.success(emptyList())
            }
            
            val result = PreloadResult(
                cachedAnnotations = annotationResults.getOrElse { emptyList() }.size,
                cachedDocuments = documentResults.getOrElse { emptyList() }.size,
                totalRequested = annotations.size + sharedDocuments.size
            )
            
            Log.d(TAG, "Preload completed: ${result.cachedAnnotations + result.cachedDocuments}/${result.totalRequested}")
            Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error in preload operation", e)
            Result.failure(e)
        }
    }
    
    // Cache status and management
    
    /**
     * Check if annotation is cached
     */
    fun isAnnotationCached(annotation: Annotation): Boolean {
        val fileName = annotation.file?.substringAfterLast('/') 
            ?: annotation.annotationName 
            ?: "annotation_${annotation.id}"
        return cachedAnnotationService.isAnnotationFileCached(annotation.id, fileName)
    }
    
    /**
     * Check if shared document is cached
     */
    fun isSharedDocumentCached(document: SharedDocument): Boolean {
        val fileName = document.annotationName ?: "document_${document.id}"
        return cachedSharedDocumentService.isSharedDocumentCached(document.id, fileName)
    }
    
    /**
     * Get cached file path for annotation
     */
    fun getCachedAnnotationPath(annotation: Annotation): String? {
        val fileName = annotation.file?.substringAfterLast('/') 
            ?: annotation.annotationName 
            ?: "annotation_${annotation.id}"
        return cachedAnnotationService.getCachedFilePath(annotation.id, fileName)
    }
    
    /**
     * Get cached thumbnail path for image annotation
     */
    fun getCachedThumbnailPath(annotation: Annotation): String? {
        if (annotation.annotationType != "image") return null
        val fileName = annotation.file?.substringAfterLast('/') 
            ?: annotation.annotationName 
            ?: "image_${annotation.id}"
        return mediaCacheHandler.getCachedThumbnail(annotation.id, fileName)
    }
    
    /**
     * Get comprehensive cache statistics
     */
    fun getCacheStatistics(): CacheStatistics {
        val cacheStats = fileCacheManager.getCacheStats()
        
        return CacheStatistics(
            totalFiles = cacheStats.totalFiles,
            totalSizeBytes = cacheStats.totalSizeBytes,
            availableBytes = cacheStats.availableBytes,
            usagePercentage = cacheStats.usagePercentage,
            imageFiles = cacheStats.imageFiles,
            audioFiles = cacheStats.audioFiles,
            documentFiles = cacheStats.documentFiles,
            maxSizeBytes = cacheStats.maxSizeBytes
        )
    }
    
    /**
     * Clean cache intelligently
     */
    suspend fun cleanCache(): Result<Long> {
        return try {
            val freedBytes = fileCacheManager.cleanCache()
            Log.d(TAG, "Cache cleaned, freed ${freedBytes}bytes")
            Result.success(freedBytes)
        } catch (e: Exception) {
            Log.e(TAG, "Error cleaning cache", e)
            Result.failure(e)
        }
    }
    
    /**
     * Clear all cache
     */
    suspend fun clearAllCache(): Result<Boolean> {
        return try {
            val success = fileCacheManager.clearCache()
            Log.d(TAG, "All cache cleared: $success")
            Result.success(success)
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing all cache", e)
            Result.failure(e)
        }
    }
}

/**
 * Result of preload operation
 */
data class PreloadResult(
    val cachedAnnotations: Int,
    val cachedDocuments: Int,
    val totalRequested: Int
) {
    val successRate: Float
        get() = if (totalRequested > 0) {
            (cachedAnnotations + cachedDocuments).toFloat() / totalRequested
        } else 0f
}

/**
 * Comprehensive cache statistics
 */
data class CacheStatistics(
    val totalFiles: Int,
    val totalSizeBytes: Long,
    val availableBytes: Long,
    val usagePercentage: Float,
    val imageFiles: Int,
    val audioFiles: Int,
    val documentFiles: Int,
    val maxSizeBytes: Long
)