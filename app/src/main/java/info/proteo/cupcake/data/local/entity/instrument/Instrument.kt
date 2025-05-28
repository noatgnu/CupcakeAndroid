package info.proteo.cupcake.data.local.entity.instrument

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "instrument")
data class InstrumentEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "max_days_ahead_pre_approval") val maxDaysAheadPreApproval: Int?,
    @ColumnInfo(name = "max_days_within_usage_pre_approval") val maxDaysWithinUsagePreApproval: Int?,
    @ColumnInfo(name = "instrument_name") val instrumentName: String?,
    @ColumnInfo(name = "instrument_description") val instrumentDescription: String?,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    val enabled: Boolean,
    val image: String?,
    @ColumnInfo(name = "last_warranty_notification_sent") val lastWarrantyNotificationSent: String?,
    @ColumnInfo(name = "last_maintenance_notification_sent") val lastMaintenanceNotificationSent: String?,
    @ColumnInfo(name = "days_before_warranty_notification") val daysBeforeWarrantyNotification: Int?,
    @ColumnInfo(name = "days_before_maintenance_notification") val daysBeforeMaintenanceNotification: Int?
)

@Entity(tableName = "instrument_usage")
data class InstrumentUsageEntity(
    @PrimaryKey val id: Int,
    val instrument: Int,
    val annotation: Int?,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "time_started") val timeStarted: String?,
    @ColumnInfo(name = "time_ended") val timeEnded: String?,
    val user: String?,
    val description: String?,
    val approved: Boolean?,
    val maintenance: Boolean?,
    @ColumnInfo(name = "approved_by") val approvedBy: Int?
)