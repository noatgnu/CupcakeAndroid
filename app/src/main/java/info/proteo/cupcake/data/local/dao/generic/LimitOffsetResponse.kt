package info.proteo.cupcake.data.local.dao.generic

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import info.proteo.cupcake.data.local.entity.generic.LimitOffsetCacheEntity

@Dao
interface LimitOffsetCacheDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(cache: LimitOffsetCacheEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(caches: List<LimitOffsetCacheEntity>)

    @Update
    suspend fun update(cache: LimitOffsetCacheEntity)

    @Delete
    suspend fun delete(cache: LimitOffsetCacheEntity)

    @Query("SELECT * FROM limit_offset_cache WHERE cacheKey = :key")
    suspend fun getByKey(key: String): LimitOffsetCacheEntity?

    @Query("DELETE FROM limit_offset_cache WHERE lastUpdated < :timestamp")
    suspend fun deleteOlderThan(timestamp: Long)

    @Query("DELETE FROM limit_offset_cache")
    suspend fun deleteAll()
}