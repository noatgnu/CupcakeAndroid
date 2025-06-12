package info.proteo.cupcake.data.local.dao.protocol

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import info.proteo.cupcake.data.local.entity.protocol.RecentSessionEntity
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

    @Query("SELECT * FROM session WHERE started_at >= :startDate AND started_at <= :endDate")
    fun getSessionsByDateRange(startDate: String, endDate: String): Flow<List<SessionEntity>>

    @Query("SELECT * FROM session WHERE name LIKE '%' || :search || '%' LIMIT :limit OFFSET :offset")
    fun searchSessions(search: String?, limit: Int, offset: Int): Flow<List<SessionEntity>>

    @Query("SELECT COUNT(*) FROM session WHERE name LIKE '%' || :search || '%'")
    fun countSearchSessions(search: String?): Flow<Int>
}

@Dao
interface RecentSessionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recentSession: RecentSessionEntity): Long

    @Update
    suspend fun update(recentSession: RecentSessionEntity)

    @Delete
    suspend fun delete(recentSession: RecentSessionEntity)

    @Query("SELECT * FROM recent_session WHERE user_id = :userId ORDER BY last_accessed DESC LIMIT :limit")
    fun getRecentSessionsByUser(userId: Int, limit: Int): Flow<List<RecentSessionEntity>>

    @Query("DELETE FROM recent_session WHERE session_id = :sessionId AND user_id = :userId")
    suspend fun deleteRecentSession(sessionId: Int, userId: Int)

    @Query("SELECT * FROM recent_session WHERE user_id = :userId ORDER BY last_accessed DESC LIMIT 1")
    suspend fun getMostRecentSession(userId: Int): RecentSessionEntity?
}