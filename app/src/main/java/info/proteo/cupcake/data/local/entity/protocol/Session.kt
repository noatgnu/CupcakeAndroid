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