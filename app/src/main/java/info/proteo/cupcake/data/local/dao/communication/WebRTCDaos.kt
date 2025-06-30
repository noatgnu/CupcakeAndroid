package info.proteo.cupcake.data.local.dao.communication

import androidx.room.*
import info.proteo.cupcake.data.local.entity.communication.WebRTCSessionEntity
import info.proteo.cupcake.data.local.entity.communication.WebRTCUserChannelEntity
import info.proteo.cupcake.data.local.entity.communication.WebRTCUserOfferEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface WebRTCSessionDao {
    @Query("SELECT * FROM webrtc_session WHERE is_active = 1")
    fun getActiveSessions(): Flow<List<WebRTCSessionEntity>>

    @Query("SELECT * FROM webrtc_session WHERE id = :id")
    suspend fun getById(id: Int): WebRTCSessionEntity?

    @Query("SELECT * FROM webrtc_session WHERE session_id = :sessionId")
    suspend fun getBySessionId(sessionId: String): WebRTCSessionEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(session: WebRTCSessionEntity)

    @Update
    suspend fun update(session: WebRTCSessionEntity)

    @Delete
    suspend fun delete(session: WebRTCSessionEntity)

    @Query("UPDATE webrtc_session SET is_active = 0 WHERE id = :id")
    suspend fun deactivateSession(id: Int)

    @Query("DELETE FROM webrtc_session WHERE is_active = 0")
    suspend fun deleteInactiveSessions()
}

@Dao
interface WebRTCUserChannelDao {
    @Query("SELECT * FROM webrtc_user_channel WHERE user_id = :userId")
    fun getByUserId(userId: Int): Flow<List<WebRTCUserChannelEntity>>

    @Query("SELECT * FROM webrtc_user_channel WHERE channel_name = :channelName")
    suspend fun getByChannelName(channelName: String): WebRTCUserChannelEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(channel: WebRTCUserChannelEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(channels: List<WebRTCUserChannelEntity>)

    @Update
    suspend fun update(channel: WebRTCUserChannelEntity)

    @Delete
    suspend fun delete(channel: WebRTCUserChannelEntity)

    @Query("DELETE FROM webrtc_user_channel WHERE user_id = :userId")
    suspend fun deleteByUserId(userId: Int)
}

@Dao
interface WebRTCUserOfferDao {
    @Query("SELECT * FROM webrtc_user_offer WHERE user_id = :userId")
    fun getByUserId(userId: Int): Flow<List<WebRTCUserOfferEntity>>

    @Query("SELECT * FROM webrtc_user_offer WHERE id = :id")
    suspend fun getById(id: Int): WebRTCUserOfferEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(offer: WebRTCUserOfferEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(offers: List<WebRTCUserOfferEntity>)

    @Update
    suspend fun update(offer: WebRTCUserOfferEntity)

    @Delete
    suspend fun delete(offer: WebRTCUserOfferEntity)

    @Query("DELETE FROM webrtc_user_offer WHERE user_id = :userId")
    suspend fun deleteByUserId(userId: Int)
}