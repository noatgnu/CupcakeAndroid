package info.proteo.cupcake.data.local.dao.offline

import androidx.room.*
import info.proteo.cupcake.data.offline.PendingFileOperation
import info.proteo.cupcake.data.offline.FileOperationType
import info.proteo.cupcake.data.offline.SyncStatus
import kotlinx.coroutines.flow.Flow

/**
 * DAO for managing pending file operations
 */
@Dao
interface PendingFileOperationsDao {
    
    @Query("SELECT * FROM pending_file_operations WHERE syncStatus = :status ORDER BY timestamp ASC")
    fun getAllPending(status: SyncStatus = SyncStatus.PENDING): Flow<List<PendingFileOperation>>
    
    @Query("SELECT * FROM pending_file_operations WHERE annotationId = :annotationId ORDER BY timestamp DESC")
    fun getOperationsForAnnotation(annotationId: Int): Flow<List<PendingFileOperation>>
    
    @Query("SELECT * FROM pending_file_operations WHERE operationType = :type AND syncStatus = :status")
    fun getOperationsByType(type: FileOperationType, status: SyncStatus = SyncStatus.PENDING): Flow<List<PendingFileOperation>>
    
    @Query("SELECT * FROM pending_file_operations WHERE id = :id")
    suspend fun getById(id: Long): PendingFileOperation?
    
    @Query("SELECT COUNT(*) FROM pending_file_operations WHERE syncStatus = :status")
    fun getPendingCount(status: SyncStatus = SyncStatus.PENDING): Flow<Int>
    
    @Query("SELECT SUM(fileSize) FROM pending_file_operations WHERE syncStatus = :status AND operationType = :type")
    suspend fun getTotalSizeForType(status: SyncStatus = SyncStatus.PENDING, type: FileOperationType): Long?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(operation: PendingFileOperation): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(operations: List<PendingFileOperation>): List<Long>
    
    @Update
    suspend fun update(operation: PendingFileOperation)
    
    @Delete
    suspend fun delete(operation: PendingFileOperation)
    
    @Query("DELETE FROM pending_file_operations WHERE syncStatus = :status")
    suspend fun deleteSynced(status: SyncStatus = SyncStatus.SYNCED): Int
    
    @Query("DELETE FROM pending_file_operations WHERE annotationId = :annotationId")
    suspend fun deleteForAnnotation(annotationId: Int): Int
    
    @Query("UPDATE pending_file_operations SET syncStatus = :newStatus WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, newStatus: SyncStatus)
    
    @Query("SELECT * FROM pending_file_operations WHERE syncStatus = :status LIMIT :limit")
    suspend fun getBatchForSync(status: SyncStatus = SyncStatus.PENDING, limit: Int = 10): List<PendingFileOperation>
}