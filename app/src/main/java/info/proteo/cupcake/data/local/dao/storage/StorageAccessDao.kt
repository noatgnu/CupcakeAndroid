package info.proteo.cupcake.data.local.dao.storage

import androidx.room.*
import info.proteo.cupcake.data.local.entity.storage.*
import kotlinx.coroutines.flow.Flow

@Dao
interface StorageAccessDao {
    // Storage Object Access Lab Group operations
    @Query("SELECT labGroupId FROM storage_object_access_lab_group WHERE storageObjectId = :storageObjectId")
    fun getLabGroupsForStorageObject(storageObjectId: Int): Flow<List<Int>>

    @Query("SELECT storageObjectId FROM storage_object_access_lab_group WHERE labGroupId = :labGroupId")
    fun getStorageObjectsForLabGroup(labGroupId: Int): Flow<List<Int>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStorageObjectAccessLabGroup(crossRef: StorageObjectAccessLabGroupCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStorageObjectAccessLabGroups(crossRefs: List<StorageObjectAccessLabGroupCrossRef>)

    @Delete
    suspend fun deleteStorageObjectAccessLabGroup(crossRef: StorageObjectAccessLabGroupCrossRef)

    @Query("DELETE FROM storage_object_access_lab_group WHERE storageObjectId = :storageObjectId")
    suspend fun deleteAllLabGroupsForStorageObject(storageObjectId: Int)

    @Query("DELETE FROM storage_object_access_lab_group WHERE labGroupId = :labGroupId")
    suspend fun deleteAllStorageObjectsForLabGroup(labGroupId: Int)
}