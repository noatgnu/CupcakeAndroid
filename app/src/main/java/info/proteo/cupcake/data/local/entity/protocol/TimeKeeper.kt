package info.proteo.cupcake.data.local.entity.protocol

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "time_keeper")
data class TimeKeeperEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "start_time") val startTime: String?,
    val session: Int?,
    val step: Int?,
    val started: Boolean,
    @ColumnInfo(name = "current_duration") val currentDuration: Float?,
    @ColumnInfo(name = "user_id") val userId: Int? = null,
)