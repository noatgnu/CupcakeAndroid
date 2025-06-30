package info.proteo.cupcake.data.local.dao.system

import androidx.room.*
import info.proteo.cupcake.data.local.entity.system.RemoteHostEntity
import info.proteo.cupcake.data.local.entity.system.SiteSettingsEntity
import info.proteo.cupcake.data.local.entity.system.BackupLogEntity
import info.proteo.cupcake.data.local.entity.system.DocumentPermissionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RemoteHostDao {
    @Query("SELECT * FROM remote_host")
    fun getAll(): Flow<List<RemoteHostEntity>>

    @Query("SELECT * FROM remote_host WHERE id = :id")
    suspend fun getById(id: Int): RemoteHostEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(remoteHost: RemoteHostEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(remoteHosts: List<RemoteHostEntity>)

    @Update
    suspend fun update(remoteHost: RemoteHostEntity)

    @Delete
    suspend fun delete(remoteHost: RemoteHostEntity)

    @Query("DELETE FROM remote_host")
    suspend fun deleteAll()
}

@Dao
interface SiteSettingsDao {
    @Query("SELECT * FROM site_settings WHERE is_active = 1 LIMIT 1")
    suspend fun getActiveSiteSettings(): SiteSettingsEntity?

    @Query("SELECT * FROM site_settings")
    fun getAll(): Flow<List<SiteSettingsEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(siteSettings: SiteSettingsEntity)

    @Update
    suspend fun update(siteSettings: SiteSettingsEntity)

    @Delete
    suspend fun delete(siteSettings: SiteSettingsEntity)
}

@Dao
interface BackupLogDao {
    @Query("SELECT * FROM backup_log ORDER BY started_at DESC")
    fun getAll(): Flow<List<BackupLogEntity>>

    @Query("SELECT * FROM backup_log WHERE id = :id")
    suspend fun getById(id: Int): BackupLogEntity?

    @Query("SELECT * FROM backup_log WHERE status = :status ORDER BY started_at DESC")
    fun getByStatus(status: String): Flow<List<BackupLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(backupLog: BackupLogEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(backupLogs: List<BackupLogEntity>)

    @Update
    suspend fun update(backupLog: BackupLogEntity)

    @Delete
    suspend fun delete(backupLog: BackupLogEntity)

    @Query("DELETE FROM backup_log")
    suspend fun deleteAll()
}

@Dao
interface DocumentPermissionDao {
    @Query("SELECT * FROM document_permission WHERE annotation_id = :annotationId")
    fun getByAnnotationId(annotationId: Int): Flow<List<DocumentPermissionEntity>>

    @Query("SELECT * FROM document_permission WHERE folder_id = :folderId")
    fun getByFolderId(folderId: Int): Flow<List<DocumentPermissionEntity>>

    @Query("SELECT * FROM document_permission WHERE user_id = :userId")
    fun getByUserId(userId: Int): Flow<List<DocumentPermissionEntity>>

    @Query("SELECT * FROM document_permission WHERE lab_group_id = :labGroupId")
    fun getByLabGroupId(labGroupId: Int): Flow<List<DocumentPermissionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(permission: DocumentPermissionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(permissions: List<DocumentPermissionEntity>)

    @Update
    suspend fun update(permission: DocumentPermissionEntity)

    @Delete
    suspend fun delete(permission: DocumentPermissionEntity)

    @Query("DELETE FROM document_permission WHERE annotation_id = :annotationId")
    suspend fun deleteByAnnotationId(annotationId: Int)

    @Query("DELETE FROM document_permission WHERE folder_id = :folderId")
    suspend fun deleteByFolderId(folderId: Int)
}