package info.proteo.cupcake.data.cache

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.ResponseBody
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.security.MessageDigest
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages local file caching for annotations, media, and documents
 * Implements LRU cache with automatic cleanup and size management
 */
@Singleton
class FileCacheManager @Inject constructor(
    private val context: Context
) {
    
    companion object {
        private const val TAG = "FileCacheManager"
        private const val CACHE_DIR_NAME = "cupcake_cache"
        private const val IMAGES_DIR = "images"
        private const val AUDIO_DIR = "audio"
        private const val DOCUMENTS_DIR = "documents"
        private const val TEMP_DIR = "temp"
        private const val THUMBNAILS_DIR = "thumbnails"
        
        // Cache size limits (in bytes)
        private const val MAX_CACHE_SIZE = 500L * 1024 * 1024 // 500MB
        private const val MAX_SINGLE_FILE_SIZE = 50L * 1024 * 1024 // 50MB
        private const val CLEANUP_THRESHOLD = 0.8 // Start cleanup at 80% capacity
        
        // File extensions
        private val IMAGE_EXTENSIONS = setOf("jpg", "jpeg", "png", "gif", "webp", "bmp")
        private val AUDIO_EXTENSIONS = setOf("mp3", "wav", "aac", "3gp", "m4a", "ogg")
        private val DOCUMENT_EXTENSIONS = setOf("pdf", "doc", "docx", "txt", "rtf")
    }
    
    private val cacheDir: File by lazy {
        File(context.cacheDir, CACHE_DIR_NAME).apply {
            if (!exists()) mkdirs()
        }
    }
    
    private val imagesDir: File by lazy {
        File(cacheDir, IMAGES_DIR).apply { if (!exists()) mkdirs() }
    }
    
    private val audioDir: File by lazy {
        File(cacheDir, AUDIO_DIR).apply { if (!exists()) mkdirs() }
    }
    
    private val documentsDir: File by lazy {
        File(cacheDir, DOCUMENTS_DIR).apply { if (!exists()) mkdirs() }
    }
    
    private val tempDir: File by lazy {
        File(cacheDir, TEMP_DIR).apply { if (!exists()) mkdirs() }
    }
    
    val thumbnailsDir: File by lazy {
        File(cacheDir, THUMBNAILS_DIR).apply { if (!exists()) mkdirs() }
    }
    
    /**
     * Cache a file from ResponseBody with automatic type detection
     */
    suspend fun cacheFile(
        annotationId: Int,
        fileName: String?,
        responseBody: ResponseBody
    ): Result<CachedFile> = withContext(Dispatchers.IO) {
        try {
            val extension = fileName?.substringAfterLast('.', "")?.lowercase() ?: ""
            val fileType = detectFileType(extension)
            val targetDir = getDirectoryForType(fileType)
            
            // Generate unique filename
            val cacheKey = generateCacheKey(annotationId, fileName ?: "unknown")
            val cachedFile = File(targetDir, "$cacheKey.$extension")
            
            // Check file size limit
            val contentLength = responseBody.contentLength()
            if (contentLength > MAX_SINGLE_FILE_SIZE) {
                return@withContext Result.failure(
                    IOException("File too large: ${contentLength}bytes > ${MAX_SINGLE_FILE_SIZE}bytes")
                )
            }
            
            // Ensure cache has space
            ensureCacheSpace(contentLength)
            
            // Write file
            responseBody.byteStream().use { inputStream ->
                FileOutputStream(cachedFile).use { outputStream ->
                    inputStream.copyTo(outputStream)
                }
            }
            
            // Update access time for LRU
            updateAccessTime(cachedFile)
            
            val result = CachedFile(
                annotationId = annotationId,
                fileName = fileName,
                localPath = cachedFile.absolutePath,
                fileType = fileType,
                sizeBytes = cachedFile.length(),
                cachedAt = System.currentTimeMillis()
            )
            
            Log.d(TAG, "Cached file: $fileName for annotation $annotationId")
            Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error caching file for annotation $annotationId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get cached file if it exists
     */
    fun getCachedFile(annotationId: Int, fileName: String?): CachedFile? {
        return try {
            val extension = fileName?.substringAfterLast('.', "")?.lowercase() ?: ""
            val fileType = detectFileType(extension)
            val targetDir = getDirectoryForType(fileType)
            val cacheKey = generateCacheKey(annotationId, fileName ?: "unknown")
            val cachedFile = File(targetDir, "$cacheKey.$extension")
            
            if (cachedFile.exists()) {
                updateAccessTime(cachedFile)
                CachedFile(
                    annotationId = annotationId,
                    fileName = fileName,
                    localPath = cachedFile.absolutePath,
                    fileType = fileType,
                    sizeBytes = cachedFile.length(),
                    cachedAt = cachedFile.lastModified()
                )
            } else null
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cached file for annotation $annotationId", e)
            null
        }
    }
    
    /**
     * Check if file is cached
     */
    fun isFileCached(annotationId: Int, fileName: String?): Boolean {
        return getCachedFile(annotationId, fileName) != null
    }
    
    /**
     * Get File object for cached file
     */
    fun getCachedFileObject(annotationId: Int, fileName: String?): File? {
        return getCachedFile(annotationId, fileName)?.let { File(it.localPath) }
    }
    
    /**
     * Delete specific cached file
     */
    fun deleteCachedFile(annotationId: Int, fileName: String?): Boolean {
        return try {
            getCachedFileObject(annotationId, fileName)?.let { file ->
                val deleted = file.delete()
                if (deleted) {
                    Log.d(TAG, "Deleted cached file for annotation $annotationId")
                }
                deleted
            } ?: false
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting cached file for annotation $annotationId", e)
            false
        }
    }
    
    /**
     * Delete all cached files for an annotation
     */
    fun deleteCachedFilesForAnnotation(annotationId: Int): Int {
        var deletedCount = 0
        try {
            listOf(imagesDir, audioDir, documentsDir, thumbnailsDir).forEach { dir ->
                dir.listFiles()?.forEach { file ->
                    if (file.name.startsWith("${annotationId}_")) {
                        if (file.delete()) {
                            deletedCount++
                        }
                    }
                }
            }
            Log.d(TAG, "Deleted $deletedCount cached files for annotation $annotationId")
        } catch (e: Exception) {
            Log.e(TAG, "Error deleting cached files for annotation $annotationId", e)
        }
        return deletedCount
    }
    
    /**
     * Get current cache size in bytes
     */
    fun getCurrentCacheSize(): Long {
        return try {
            cacheDir.walkTopDown()
                .filter { it.isFile }
                .sumOf { it.length() }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating cache size", e)
            0L
        }
    }
    
    /**
     * Clean cache to free up space
     */
    suspend fun cleanCache(targetFreeBytes: Long = MAX_CACHE_SIZE / 4): Long = withContext(Dispatchers.IO) {
        var freedBytes = 0L
        try {
            val allFiles = cacheDir.walkTopDown()
                .filter { it.isFile }
                .sortedBy { it.lastModified() } // LRU: oldest first
                .toList()
            
            for (file in allFiles) {
                if (freedBytes >= targetFreeBytes) break
                
                val fileSize = file.length()
                if (file.delete()) {
                    freedBytes += fileSize
                    Log.d(TAG, "Deleted old cache file: ${file.name}")
                }
            }
            
            Log.d(TAG, "Cache cleanup freed ${freedBytes}bytes")
        } catch (e: Exception) {
            Log.e(TAG, "Error during cache cleanup", e)
        }
        freedBytes
    }
    
    /**
     * Clear entire cache
     */
    suspend fun clearCache(): Boolean = withContext(Dispatchers.IO) {
        try {
            cacheDir.deleteRecursively()
            cacheDir.mkdirs()
            imagesDir.mkdirs()
            audioDir.mkdirs()
            documentsDir.mkdirs()
            tempDir.mkdirs()
            thumbnailsDir.mkdirs()
            Log.d(TAG, "Cache cleared successfully")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error clearing cache", e)
            false
        }
    }
    
    /**
     * Get cache statistics
     */
    fun getCacheStats(): CacheStats {
        var totalFiles = 0
        var totalSize = 0L
        var imageFiles = 0
        var audioFiles = 0
        var documentFiles = 0
        
        try {
            cacheDir.walkTopDown().filter { it.isFile }.forEach { file ->
                totalFiles++
                totalSize += file.length()
                
                when (file.parentFile?.name) {
                    IMAGES_DIR, THUMBNAILS_DIR -> imageFiles++
                    AUDIO_DIR -> audioFiles++
                    DOCUMENTS_DIR -> documentFiles++
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error calculating cache stats", e)
        }
        
        return CacheStats(
            totalFiles = totalFiles,
            totalSizeBytes = totalSize,
            imageFiles = imageFiles,
            audioFiles = audioFiles,
            documentFiles = documentFiles,
            maxSizeBytes = MAX_CACHE_SIZE
        )
    }
    
    private fun detectFileType(extension: String): FileType {
        return when {
            extension in IMAGE_EXTENSIONS -> FileType.IMAGE
            extension in AUDIO_EXTENSIONS -> FileType.AUDIO
            extension in DOCUMENT_EXTENSIONS -> FileType.DOCUMENT
            else -> FileType.OTHER
        }
    }
    
    private fun getDirectoryForType(fileType: FileType): File {
        return when (fileType) {
            FileType.IMAGE -> imagesDir
            FileType.AUDIO -> audioDir
            FileType.DOCUMENT -> documentsDir
            FileType.OTHER -> tempDir
        }
    }
    
    fun generateCacheKey(annotationId: Int, fileName: String): String {
        val input = "${annotationId}_${fileName}"
        val md5 = MessageDigest.getInstance("MD5")
        val hashBytes = md5.digest(input.toByteArray())
        return hashBytes.joinToString("") { "%02x".format(it) }
    }
    
    private fun updateAccessTime(file: File) {
        try {
            file.setLastModified(System.currentTimeMillis())
        } catch (e: Exception) {
            Log.w(TAG, "Could not update access time for ${file.name}", e)
        }
    }
    
    private suspend fun ensureCacheSpace(requiredBytes: Long) {
        val currentSize = getCurrentCacheSize()
        val availableSpace = MAX_CACHE_SIZE - currentSize
        
        if (availableSpace < requiredBytes || 
            currentSize > (MAX_CACHE_SIZE * CLEANUP_THRESHOLD)) {
            val targetFreeBytes = requiredBytes + (MAX_CACHE_SIZE / 4)
            cleanCache(targetFreeBytes)
        }
    }
}

/**
 * Represents a cached file
 */
data class CachedFile(
    val annotationId: Int,
    val fileName: String?,
    val localPath: String,
    val fileType: FileType,
    val sizeBytes: Long,
    val cachedAt: Long
)

/**
 * File type enumeration
 */
enum class FileType {
    IMAGE, AUDIO, DOCUMENT, OTHER
}

/**
 * Cache statistics
 */
data class CacheStats(
    val totalFiles: Int,
    val totalSizeBytes: Long,
    val imageFiles: Int,
    val audioFiles: Int,
    val documentFiles: Int,
    val maxSizeBytes: Long
) {
    val usagePercentage: Float
        get() = if (maxSizeBytes > 0) (totalSizeBytes.toFloat() / maxSizeBytes) * 100 else 0f
    
    val availableBytes: Long
        get() = maxSizeBytes - totalSizeBytes
}