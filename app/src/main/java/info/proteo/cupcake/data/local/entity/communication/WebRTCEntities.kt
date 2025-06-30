package info.proteo.cupcake.data.local.entity.communication

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "webrtc_session")
data class WebRTCSessionEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "session_id") val sessionId: String,
    @ColumnInfo(name = "is_active") val isActive: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?
)

@Entity(tableName = "webrtc_user_channel")
data class WebRTCUserChannelEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "user_id") val userId: Int,
    @ColumnInfo(name = "channel_name") val channelName: String,
    @ColumnInfo(name = "channel_type") val channelType: String,
    @ColumnInfo(name = "created_at") val createdAt: String?
)

@Entity(tableName = "webrtc_user_offer")
data class WebRTCUserOfferEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "user_id") val userId: Int,
    @ColumnInfo(name = "offer_data") val offerData: String,
    @ColumnInfo(name = "id_type") val idType: String,
    @ColumnInfo(name = "created_at") val createdAt: String?
)