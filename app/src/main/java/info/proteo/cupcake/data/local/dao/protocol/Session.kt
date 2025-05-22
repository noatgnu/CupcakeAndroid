package info.proteo.cupcake.data.local.dao.protocol

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import info.proteo.cupcake.data.local.entity.protocol.SessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface SessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: SessionEntity): Long

    @Update
    suspend fun update(session: SessionEntity)

    @Delete
    suspend fun delete(session: SessionEntity)

    @Query("SELECT * FROM session WHERE id = :id")
    suspend fun getById(id: Int): SessionEntity?

    @Query("SELECT * FROM session WHERE unique_id = :uniqueId")
    suspend fun getByUniqueId(uniqueId: String): SessionEntity?

    @Query("SELECT * FROM session WHERE user = :userId")
    fun getByUser(userId: Int): Flow<List<SessionEntity>>

    @Query("DELETE FROM session")
    suspend fun deleteAll()
}