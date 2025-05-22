package info.proteo.cupcake.data.local.dao.storage

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import info.proteo.cupcake.data.local.entity.storage.StorageObjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StorageObjectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(storageObject: StorageObjectEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(storageObjects: List<StorageObjectEntity>)

    @Update
    suspend fun update(storageObject: StorageObjectEntity)

    @Delete
    suspend fun delete(storageObject: StorageObjectEntity)

    @Query("SELECT * FROM storage_object WHERE id = :id")
    suspend fun getById(id: Int): StorageObjectEntity?

    @Query("SELECT * FROM storage_object WHERE stored_at = :parentId ORDER BY object_name LIMIT :limit OFFSET :offset")
    fun getByParentPaginated(parentId: Int, limit: Int, offset: Int): Flow<List<StorageObjectEntity>>

    @Query("SELECT * FROM storage_object WHERE stored_at = :parentId")
    fun getByParent(parentId: Int): Flow<List<StorageObjectEntity>>

    @Query("SELECT * FROM storage_object WHERE stored_at IS NULL ORDER BY object_name LIMIT :limit OFFSET :offset")
    fun getRootObjectsPaginated(limit: Int, offset: Int): Flow<List<StorageObjectEntity>>

    @Query("SELECT * FROM storage_object WHERE stored_at IS NULL")
    fun getRootObjects(): Flow<List<StorageObjectEntity>>

    @Query("SELECT COUNT(*) FROM storage_object WHERE stored_at = :parentId")
    suspend fun countByParent(parentId: Int): Int

    @Query("SELECT COUNT(*) FROM storage_object WHERE stored_at IS NULL")
    suspend fun countRootObjects(): Int

    @Query("SELECT * FROM storage_object WHERE id NOT IN (:excludeIds) AND stored_at = :parentId ORDER BY object_name LIMIT :limit OFFSET :offset")
    fun getByParentExcludingIds(parentId: Int, excludeIds: List<Int>, limit: Int, offset: Int): Flow<List<StorageObjectEntity>>

    @Query("SELECT * FROM storage_object WHERE id NOT IN (:excludeIds) AND stored_at IS NULL ORDER BY object_name LIMIT :limit OFFSET :offset")
    fun getRootObjectsExcludingIds(excludeIds: List<Int>, limit: Int, offset: Int): Flow<List<StorageObjectEntity>>

    @Query("SELECT * FROM storage_object WHERE (object_name LIKE :query OR object_description LIKE :query) ORDER BY object_name LIMIT :limit OFFSET :offset")
    fun searchStorageObjects(query: String, limit: Int, offset: Int): Flow<List<StorageObjectEntity>>

    @Query("SELECT * FROM storage_object WHERE (object_name LIKE :query OR object_description LIKE :query) AND id NOT IN (:excludeIds) ORDER BY object_name LIMIT :limit OFFSET :offset")
    fun searchStorageObjectsExcludingIds(query: String, excludeIds: List<Int>, limit: Int, offset: Int): Flow<List<StorageObjectEntity>>

    @Query("SELECT COUNT(*) FROM storage_object WHERE (object_name LIKE :query OR object_description LIKE :query)")
    suspend fun countSearchResults(query: String): Int

    @Query("DELETE FROM storage_object")
    suspend fun deleteAll()

    @Query("UPDATE storage_object SET object_name = :name WHERE id = :id")
    suspend fun updateObjectName(id: Int, name: String)

    @Query("UPDATE storage_object SET object_description = :description WHERE id = :id")
    suspend fun updateObjectDescription(id: Int, description: String)

    @Query("UPDATE storage_object SET object_type = :type WHERE id = :id")
    suspend fun updateObjectType(id: Int, type: String?)

    @Query("UPDATE storage_object SET stored_at = :parentId WHERE id = :id")
    suspend fun updateParent(id: Int, parentId: Int?)

    @Query("UPDATE storage_object SET png_base64 = :pngBase64 WHERE id = :id")
    suspend fun updatePngBase64(id: Int, pngBase64: String?)

    @Query("UPDATE storage_object SET stored_at = :newParentId WHERE stored_at = :oldParentId")
    suspend fun moveAllChildren(oldParentId: Int, newParentId: Int)

    @Query("UPDATE storage_object SET can_delete = :canDelete WHERE id IN (:ids)")
    suspend fun updateCanDeleteForIds(ids: List<Int>, canDelete: Boolean)

    @Query("UPDATE storage_object SET object_name = :name, object_description = :description WHERE id = :id")
    suspend fun updateNameAndDescription(id: Int, name: String, description: String)

    @Query("SELECT EXISTS(SELECT 1 FROM storage_object WHERE id = :id)")
    suspend fun exists(id: Int): Boolean
}
