package info.proteo.cupcake.data.cache

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.MediaMetadataRetriever
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Specialized handler for media file caching with thumbnails and metadata
 */
@Singleton
class MediaCacheHandler @Inject constructor(
    private val context: Context,
    private val fileCacheManager: FileCacheManager
) {
    
    companion object {
        private const val TAG = "MediaCacheHandler"
        private const val THUMBNAIL_SIZE = 300 // px
        private const val THUMBNAIL_QUALITY = 80 // JPEG quality
    }
    
    /**
     * Cache image with automatic thumbnail generation
     */
    suspend fun cacheImageWithThumbnail(
        annotationId: Int,
        fileName: String?,
        imageFile: File
    ): Result<ImageCacheResult> = withContext(Dispatchers.IO) {
        try {
            // Generate thumbnail
            val thumbnailResult = generateImageThumbnail(annotationId, fileName, imageFile)
            
            // Get image metadata
            val metadata = getImageMetadata(imageFile)
            
            val result = ImageCacheResult(
                originalPath = imageFile.absolutePath,
                thumbnailPath = thumbnailResult.getOrNull(),
                metadata = metadata
            )
            
            Log.d(TAG, "Cached image with thumbnail for annotation $annotationId: $fileName")
            Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error caching image for annotation $annotationId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Generate thumbnail for image
     */
    private suspend fun generateImageThumbnail(
        annotationId: Int,
        fileName: String?,
        imageFile: File
    ): Result<String> = withContext(Dispatchers.IO) {
        try {
            val originalBitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
                ?: return@withContext Result.failure(Exception("Could not decode image"))
            
            // Calculate thumbnail dimensions maintaining aspect ratio
            val (thumbnailWidth, thumbnailHeight) = calculateThumbnailSize(
                originalBitmap.width, 
                originalBitmap.height
            )
            
            // Create thumbnail bitmap
            val thumbnailBitmap = Bitmap.createScaledBitmap(
                originalBitmap, 
                thumbnailWidth, 
                thumbnailHeight, 
                true
            )
            
            // Save thumbnail
            val thumbnailFileName = "thumb_${fileName ?: "image"}"
            val thumbnailPath = saveThumbnail(annotationId, thumbnailFileName, thumbnailBitmap)
            
            // Cleanup
            originalBitmap.recycle()
            thumbnailBitmap.recycle()
            
            Result.success(thumbnailPath)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error generating thumbnail for annotation $annotationId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Cache audio file with metadata extraction
     */
    suspend fun cacheAudioWithMetadata(
        annotationId: Int,
        fileName: String?,
        audioFile: File
    ): Result<AudioCacheResult> = withContext(Dispatchers.IO) {
        try {
            // Extract audio metadata
            val metadata = getAudioMetadata(audioFile)
            
            val result = AudioCacheResult(
                filePath = audioFile.absolutePath,
                metadata = metadata
            )
            
            Log.d(TAG, "Cached audio with metadata for annotation $annotationId: $fileName")
            Result.success(result)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error caching audio for annotation $annotationId", e)
            Result.failure(e)
        }
    }
    
    /**
     * Get cached thumbnail path for image
     */
    fun getCachedThumbnail(annotationId: Int, fileName: String?): String? {
        return try {
            val thumbnailFileName = "thumb_${fileName ?: "image"}"
            fileCacheManager.getCachedFile(annotationId, thumbnailFileName)?.localPath
        } catch (e: Exception) {
            Log.e(TAG, "Error getting cached thumbnail for annotation $annotationId", e)
            null
        }
    }
    
    /**
     * Preload media files for offline access
     */
    suspend fun preloadMediaFiles(
        mediaFiles: List<MediaPreloadRequest>
    ): Result<Int> = withContext(Dispatchers.IO) {
        var successCount = 0
        
        try {
            mediaFiles.forEach { request ->
                when (request.fileType) {
                    FileType.IMAGE -> {
                        val cachedFile = fileCacheManager.getCachedFileObject(
                            request.annotationId, 
                            request.fileName
                        )
                        cachedFile?.let { file ->
                            cacheImageWithThumbnail(request.annotationId, request.fileName, file)
                                .onSuccess { successCount++ }
                        }
                    }
                    FileType.AUDIO -> {
                        val cachedFile = fileCacheManager.getCachedFileObject(
                            request.annotationId, 
                            request.fileName
                        )
                        cachedFile?.let { file ->
                            cacheAudioWithMetadata(request.annotationId, request.fileName, file)
                                .onSuccess { successCount++ }
                        }
                    }
                    else -> {
                        // Handle other file types if needed
                    }
                }
            }
            
            Log.d(TAG, "Preloaded $successCount/${mediaFiles.size} media files")
            Result.success(successCount)
            
        } catch (e: Exception) {
            Log.e(TAG, "Error preloading media files", e)
            Result.failure(e)
        }
    }
    
    private fun calculateThumbnailSize(originalWidth: Int, originalHeight: Int): Pair<Int, Int> {
        val aspectRatio = originalWidth.toFloat() / originalHeight.toFloat()
        
        return if (originalWidth > originalHeight) {
            // Landscape
            val width = THUMBNAIL_SIZE
            val height = (THUMBNAIL_SIZE / aspectRatio).toInt()
            Pair(width, height)
        } else {
            // Portrait or square
            val width = (THUMBNAIL_SIZE * aspectRatio).toInt()
            val height = THUMBNAIL_SIZE
            Pair(width, height)
        }
    }
    
    private fun saveThumbnail(
        annotationId: Int, 
        fileName: String, 
        bitmap: Bitmap
    ): String {
        val cacheKey = fileCacheManager.generateCacheKey(annotationId, fileName)
        val thumbnailFile = File(fileCacheManager.thumbnailsDir, "$cacheKey.jpg")
        
        FileOutputStream(thumbnailFile).use { outputStream ->
            bitmap.compress(Bitmap.CompressFormat.JPEG, THUMBNAIL_QUALITY, outputStream)
        }
        
        return thumbnailFile.absolutePath
    }
    
    private fun getImageMetadata(imageFile: File): ImageMetadata {
        return try {
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            BitmapFactory.decodeFile(imageFile.absolutePath, options)
            
            ImageMetadata(
                width = options.outWidth,
                height = options.outHeight,
                mimeType = options.outMimeType ?: "unknown",
                fileSizeBytes = imageFile.length()
            )
        } catch (e: Exception) {
            Log.w(TAG, "Could not extract image metadata", e)
            ImageMetadata(0, 0, "unknown", imageFile.length())
        }
    }
    
    private fun getAudioMetadata(audioFile: File): AudioMetadata {
        return try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(audioFile.absolutePath)
            
            val duration = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 0L
            val bitrate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_BITRATE)?.toIntOrNull() ?: 0
            val sampleRate = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_SAMPLERATE)?.toIntOrNull() ?: 0
            val mimeType = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_MIMETYPE) ?: "unknown"
            
            retriever.release()
            
            AudioMetadata(
                durationMs = duration,
                bitrate = bitrate,
                sampleRate = sampleRate,
                mimeType = mimeType,
                fileSizeBytes = audioFile.length()
            )
        } catch (e: Exception) {
            Log.w(TAG, "Could not extract audio metadata", e)
            AudioMetadata(0L, 0, 0, "unknown", audioFile.length())
        }
    }
}

/**
 * Result for cached image with thumbnail
 */
data class ImageCacheResult(
    val originalPath: String,
    val thumbnailPath: String?,
    val metadata: ImageMetadata
)

/**
 * Result for cached audio with metadata
 */
data class AudioCacheResult(
    val filePath: String,
    val metadata: AudioMetadata
)

/**
 * Image metadata
 */
data class ImageMetadata(
    val width: Int,
    val height: Int,
    val mimeType: String,
    val fileSizeBytes: Long
)

/**
 * Audio metadata
 */
data class AudioMetadata(
    val durationMs: Long,
    val bitrate: Int,
    val sampleRate: Int,
    val mimeType: String,
    val fileSizeBytes: Long
)

/**
 * Media preload request
 */
data class MediaPreloadRequest(
    val annotationId: Int,
    val fileName: String?,
    val fileType: FileType
)