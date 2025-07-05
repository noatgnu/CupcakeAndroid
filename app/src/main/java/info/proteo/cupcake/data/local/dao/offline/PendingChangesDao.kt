package info.proteo.cupcake.data.local.dao.offline

import androidx.room.*
import info.proteo.cupcake.data.offline.PendingChange
import info.proteo.cupcake.data.offline.SyncStatus
import info.proteo.cupcake.data.offline.ChangeType
import kotlinx.coroutines.flow.Flow

/**
 * DAO for managing pending changes that need to be synced
 */
@Dao
interface PendingChangesDao {
    
    @Query("SELECT * FROM pending_changes WHERE syncStatus = :status ORDER BY timestamp ASC")
    fun getAllPending(status: SyncStatus = SyncStatus.PENDING): Flow<List<PendingChange>>
    
    @Query("SELECT * FROM pending_changes WHERE annotationId = :annotationId ORDER BY timestamp DESC")
    fun getChangesForAnnotation(annotationId: Int): Flow<List<PendingChange>>
    
    @Query("SELECT * FROM pending_changes WHERE id = :id")
    suspend fun getById(id: Long): PendingChange?
    
    @Query("SELECT COUNT(*) FROM pending_changes WHERE syncStatus = :status")
    fun getPendingCount(status: SyncStatus = SyncStatus.PENDING): Flow<Int>
    
    @Query("SELECT COUNT(*) > 0 FROM pending_changes WHERE annotationId = :annotationId AND syncStatus = :status")
    fun hasPendingChanges(annotationId: Int, status: SyncStatus = SyncStatus.PENDING): Flow<Boolean>
    
    @Query("SELECT * FROM pending_changes WHERE syncStatus = :status AND retryCount < :maxRetries ORDER BY timestamp ASC LIMIT :limit")
    suspend fun getRetryableFailed(
        status: SyncStatus = SyncStatus.FAILED, 
        maxRetries: Int = 3, 
        limit: Int = 50
    ): List<PendingChange>
    
    @Query("SELECT * FROM pending_changes WHERE changeType = :changeType AND syncStatus = :status")
    fun getByChangeType(changeType: ChangeType, status: SyncStatus = SyncStatus.PENDING): Flow<List<PendingChange>>
    
    @Query("DELETE FROM pending_changes WHERE syncStatus = :status")
    suspend fun deleteSynced(status: SyncStatus = SyncStatus.SYNCED): Int
    
    @Query("DELETE FROM pending_changes WHERE annotationId = :annotationId")
    suspend fun deleteForAnnotation(annotationId: Int): Int
    
    @Query("DELETE FROM pending_changes WHERE timestamp < :cutoffTime AND syncStatus = :status")
    suspend fun deleteOldSynced(cutoffTime: Long, status: SyncStatus = SyncStatus.SYNCED): Int
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(pendingChange: PendingChange): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(pendingChanges: List<PendingChange>): List<Long>
    
    @Update
    suspend fun update(pendingChange: PendingChange)
    
    @Update
    suspend fun updateAll(pendingChanges: List<PendingChange>)
    
    @Delete
    suspend fun delete(pendingChange: PendingChange)
    
    @Query("UPDATE pending_changes SET syncStatus = :newStatus WHERE id = :id")
    suspend fun updateSyncStatus(id: Long, newStatus: SyncStatus)
    
    @Query("UPDATE pending_changes SET syncStatus = :newStatus, retryCount = retryCount + 1, lastError = :error WHERE id = :id")
    suspend fun markFailed(id: Long, newStatus: SyncStatus = SyncStatus.FAILED, error: String?)
    
    @Query("UPDATE pending_changes SET syncStatus = :newStatus WHERE annotationId = :annotationId")
    suspend fun updateStatusForAnnotation(annotationId: Int, newStatus: SyncStatus)
    
    // Batch operations
    @Query("SELECT * FROM pending_changes WHERE syncStatus IN (:statuses) ORDER BY timestamp ASC LIMIT :batchSize")
    suspend fun getBatchForSync(statuses: List<SyncStatus>, batchSize: Int = 20): List<PendingChange>
    
    @Query("UPDATE pending_changes SET syncStatus = :newStatus WHERE id IN (:ids)")
    suspend fun updateBatchStatus(ids: List<Long>, newStatus: SyncStatus)
    
    // Statistics
    @Query("""
        SELECT 
            changeType,
            syncStatus,
            COUNT(*) as count
        FROM pending_changes 
        GROUP BY changeType, syncStatus
    """)
    suspend fun getStatistics(): List<ChangeStatistic>
    
    @Query("SELECT MIN(timestamp) FROM pending_changes WHERE syncStatus = :status")
    suspend fun getOldestPendingTimestamp(status: SyncStatus = SyncStatus.PENDING): Long?
    
    @Query("SELECT MAX(timestamp) FROM pending_changes WHERE syncStatus = :status")
    suspend fun getNewestSyncedTimestamp(status: SyncStatus = SyncStatus.SYNCED): Long?
}

/**
 * Statistics data class for Room query results
 */
data class ChangeStatistic(
    val changeType: ChangeType,
    val syncStatus: SyncStatus,
    val count: Int
)