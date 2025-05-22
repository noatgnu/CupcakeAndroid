package info.proteo.cupcake.data.local.entity.reagent

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "reagent")
data class ReagentEntity(
    @PrimaryKey val id: Int,
    val name: String,
    val unit: String,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?
)

@Entity(tableName = "reagent_subscription")
data class ReagentSubscriptionEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "user_id") val userId: Int,
    @ColumnInfo(name = "stored_reagent") val storedReagent: Int,
    @ColumnInfo(name = "notify_on_low_stock") val notifyOnLowStock: Boolean,
    @ColumnInfo(name = "notify_on_expiry") val notifyOnExpiry: Boolean,
    @ColumnInfo(name = "created_at") val createdAt: String
)

@Entity(tableName = "reagent_action")
data class ReagentActionEntity(
    @PrimaryKey val id: Int,
    val reagent: Int,
    @ColumnInfo(name = "action_type") val actionType: String?,
    val notes: String?,
    val quantity: Float,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    val user: String?,
    @ColumnInfo(name = "step_reagent") val stepReagent: Int?
)