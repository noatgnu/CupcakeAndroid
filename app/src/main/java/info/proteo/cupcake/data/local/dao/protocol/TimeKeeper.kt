package info.proteo.cupcake.data.local.dao.protocol

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import info.proteo.cupcake.data.local.entity.protocol.TimeKeeperEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface TimeKeeperDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(timeKeeper: TimeKeeperEntity): Long

    @Update
    suspend fun update(timeKeeper: TimeKeeperEntity)

    @Delete
    suspend fun delete(timeKeeper: TimeKeeperEntity)

    @Query("SELECT * FROM time_keeper WHERE id = :id")
    suspend fun getById(id: Int): TimeKeeperEntity?

    @Query("SELECT * FROM time_keeper WHERE session = :sessionId")
    fun getBySession(sessionId: Int): Flow<List<TimeKeeperEntity>>

    @Query("SELECT * FROM time_keeper WHERE step = :stepId")
    fun getByStep(stepId: Int): Flow<List<TimeKeeperEntity>>

    @Query("DELETE FROM time_keeper")
    suspend fun deleteAll()
}