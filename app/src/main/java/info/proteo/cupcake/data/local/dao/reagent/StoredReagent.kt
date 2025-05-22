package info.proteo.cupcake.data.local.dao.reagent

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import info.proteo.cupcake.data.local.entity.reagent.StoredReagentEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StoredReagentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reagent: StoredReagentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reagents: List<StoredReagentEntity>)

    @Update
    suspend fun update(reagent: StoredReagentEntity)

    @Delete
    suspend fun delete(reagent: StoredReagentEntity)

    @Query("SELECT * FROM stored_reagent WHERE id = :id")
    suspend fun getById(id: Int): StoredReagentEntity?

    @Query("SELECT * FROM stored_reagent WHERE reagent_id = :reagentId")
    fun getByReagentId(reagentId: Int): Flow<List<StoredReagentEntity>>

    @Query("SELECT * FROM stored_reagent WHERE storage_object_id = :storageId")
    fun getByStorageId(storageId: Int): Flow<List<StoredReagentEntity>>

    @Query("SELECT * FROM stored_reagent WHERE user_id = :userId")
    fun getByUserId(userId: Int): Flow<List<StoredReagentEntity>>

    @Query("SELECT * FROM stored_reagent WHERE expiration_date < :date AND notify_on_expiry = 1")
    fun getExpiringReagents(date: String): Flow<List<StoredReagentEntity>>

    @Query("SELECT * FROM stored_reagent WHERE current_quantity < low_stock_threshold AND notify_on_low_stock = 1")
    fun getLowStockReagents(): Flow<List<StoredReagentEntity>>

    @Query("DELETE FROM stored_reagent")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM stored_reagent")
    suspend fun countAll(): Int

    @Query("SELECT COUNT(*) FROM stored_reagent WHERE storage_object_id = :storageObjectId")
    suspend fun countByStorageObject(storageObjectId: Int): Int

    @Query("SELECT * FROM stored_reagent ORDER BY id LIMIT :limit OFFSET :offset")
    fun getAllPaginated(limit: Int, offset: Int): Flow<List<StoredReagentEntity>>

    @Query("SELECT * FROM stored_reagent WHERE storage_object_id = :storageObjectId ORDER BY id LIMIT :limit OFFSET :offset")
    fun getByStorageObjectPaginated(storageObjectId: Int, limit: Int, offset: Int): Flow<List<StoredReagentEntity>>

}
