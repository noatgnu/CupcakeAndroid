package info.proteo.cupcake.data.local.entity.reagent

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "stored_reagent")
data class StoredReagentEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "reagent_id") val reagentId: Int,
    @ColumnInfo(name = "storage_object_id") val storageObjectId: Int,
    val quantity: Float,
    val notes: String?,
    @ColumnInfo(name = "user_id") val userId: Int,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "current_quantity") val currentQuantity: Float,
    @ColumnInfo(name = "png_base64") val pngBase64: String?,
    val barcode: String?,
    val shareable: Boolean,
    @ColumnInfo(name = "expiration_date") val expirationDate: String?,
    @ColumnInfo(name = "created_by_session") val createdBySession: String?,
    @ColumnInfo(name = "notify_on_low_stock") val notifyOnLowStock: Boolean,
    @ColumnInfo(name = "last_notification_sent") val lastNotificationSent: String?,
    @ColumnInfo(name = "low_stock_threshold") val lowStockThreshold: Float?,
    @ColumnInfo(name = "notify_days_before_expiry") val notifyDaysBeforeExpiry: Int?,
    @ColumnInfo(name = "notify_on_expiry") val notifyOnExpiry: Boolean,
    @ColumnInfo(name = "last_expiry_notification_sent") val lastExpiryNotificationSent: String?,
    @ColumnInfo(name = "subscriber_count") val subscriberCount: Int
)