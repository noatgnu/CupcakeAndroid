package info.proteo.cupcake.data.local.dao.tag

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import info.proteo.cupcake.data.local.entity.tag.TagEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: TagEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tags: List<TagEntity>)

    @Update
    suspend fun update(tag: TagEntity)

    @Delete
    suspend fun delete(tag: TagEntity)

    @Query("SELECT * FROM tag WHERE id = :id")
    fun getById(id: Int): Flow<TagEntity?>

    @Query("SELECT * FROM tag")
    fun getAllTags(): Flow<List<TagEntity>>

    @Query("SELECT * FROM tag LIMIT :limit OFFSET :offset")
    fun getAllTags(limit: Int, offset: Int): Flow<List<TagEntity>>

    @Query("SELECT * FROM tag WHERE tag = :name")
    suspend fun getByName(name: String): TagEntity?

    @Query("SELECT * FROM tag WHERE tag LIKE '%' || :name || '%'")
    fun searchByName(name: String): Flow<List<TagEntity>>


    @Query("SELECT * FROM tag WHERE tag LIKE '%' || :name || '%' LIMIT :limit OFFSET :offset")
    fun searchByName(name: String, limit: Int, offset: Int): Flow<List<TagEntity>>

    @Query("DELETE FROM tag")
    suspend fun deleteAll()
}