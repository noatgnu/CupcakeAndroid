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

    @Query("SELECT * FROM time_keeper WHERE session = :sessionId ORDER BY start_time DESC LIMIT :limit OFFSET :offset")
    fun getBySession(sessionId: Int, limit: Int, offset: Int): Flow<List<TimeKeeperEntity>>

    @Query("SELECT * FROM time_keeper WHERE step = :stepId ORDER BY start_time DESC LIMIT :limit OFFSET :offset")
    fun getByStep(stepId: Int, limit: Int, offset: Int): Flow<List<TimeKeeperEntity>>

    @Query("SELECT * FROM time_keeper ORDER BY start_time DESC LIMIT :limit OFFSET :offset")
    fun getAll(limit: Int, offset: Int): Flow<List<TimeKeeperEntity>>

    @Query("DELETE FROM time_keeper")
    suspend fun deleteAll()

    @Query("SELECT * FROM time_keeper WHERE started = 1 ORDER BY start_time DESC LIMIT :limit OFFSET :offset")
    fun getActiveTimeKeepers(limit: Int, offset: Int): Flow<List<TimeKeeperEntity>>

    @Query("SELECT * FROM time_keeper WHERE session = :sessionId AND user_id = :userId ORDER BY start_time DESC LIMIT :limit OFFSET :offset")
    fun getBySessionAndUser(sessionId: Int, userId: Int, limit: Int, offset: Int): Flow<List<TimeKeeperEntity>>

    @Query("SELECT * FROM time_keeper WHERE step = :stepId AND user_id = :userId ORDER BY start_time DESC LIMIT :limit OFFSET :offset")
    fun getByStepAndUser(stepId: Int, userId: Int, limit: Int, offset: Int): Flow<List<TimeKeeperEntity>>

    @Query("SELECT * FROM time_keeper WHERE user_id = :userId ORDER BY start_time DESC LIMIT :limit OFFSET :offset")
    fun getAllByUser(userId: Int, limit: Int, offset: Int): Flow<List<TimeKeeperEntity>>

    @Query("SELECT * FROM time_keeper WHERE started = :started AND user_id = :userId ORDER BY start_time DESC LIMIT :limit OFFSET :offset")
    fun getByStartedAndUser(started: Boolean, userId: Int, limit: Int, offset: Int): Flow<List<TimeKeeperEntity>>

    @Query("SELECT * FROM time_keeper WHERE session = :sessionId AND started = :started AND user_id = :userId ORDER BY start_time DESC LIMIT :limit OFFSET :offset")
    fun getBySessionStartedAndUser(sessionId: Int, started: Boolean, userId: Int, limit: Int, offset: Int): Flow<List<TimeKeeperEntity>>

    @Query("SELECT * FROM time_keeper WHERE step = :stepId AND started = :started AND user_id = :userId ORDER BY start_time DESC LIMIT :limit OFFSET :offset")
    fun getByStepStartedAndUser(stepId: Int, started: Boolean, userId: Int, limit: Int, offset: Int): Flow<List<TimeKeeperEntity>>



}