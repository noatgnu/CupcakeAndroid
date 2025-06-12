package info.proteo.cupcake.data.local.entity.protocol

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "session")
data class SessionEntity(
    @PrimaryKey val id: Int,
    val user: Int,
    @ColumnInfo(name = "unique_id") val uniqueId: String,
    val enabled: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    val name: String?,
    @ColumnInfo(name = "started_at") val startedAt: String?,
    @ColumnInfo(name = "ended_at") val endedAt: String?
)

@Entity(tableName = "recent_session")
data class RecentSessionEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "session_id") val sessionId: Int,
    @ColumnInfo(name = "session_unique_id") val sessionUniqueId: String,
    @ColumnInfo(name = "session_name") val sessionName: String?,
    @ColumnInfo(name = "protocol_id") val protocolId: Int,
    @ColumnInfo(name = "protocol_name") val protocolName: String?,
    @ColumnInfo(name = "user_id") val userId: Int,
    @ColumnInfo(name = "last_accessed") val lastAccessed: String,
    @ColumnInfo(name = "step_id") val stepId: Int? = null,
) {
    companion object {
        const val MAX_RECENT_SESSIONS = 5
    }
}