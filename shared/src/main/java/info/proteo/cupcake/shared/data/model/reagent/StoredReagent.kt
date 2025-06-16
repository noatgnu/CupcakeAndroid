package info.proteo.cupcake.shared.data.model.reagent

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import info.proteo.cupcake.shared.data.model.metadatacolumn.MetadataColumn
import info.proteo.cupcake.shared.data.model.protocol.ProtocolStep
import info.proteo.cupcake.shared.data.model.storage.StorageObjectBasic
import info.proteo.cupcake.shared.data.model.user.UserBasic


@JsonClass(generateAdapter = true)
data class StoredReagent(
    val id: Int,
    val reagent: Reagent,
    @Json(name = "reagent_id") val reagentId: Int?,
    @Json(name = "storage_object") val storageObject: StorageObjectBasic,
    @Json(name = "storage_object_id") val storageObjectId: Int?,
    val quantity: Float,
    val notes: String?,
    val user: UserBasic,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?,
    @Json(name = "current_quantity") val currentQuantity: Float,
    @Json(name = "png_base64") val pngBase64: String?,
    val barcode: String?,
    val shareable: Boolean,
    @Json(name = "expiration_date") val expirationDate: String?,
    @Json(name = "created_by_session") val createdBySession: String?,
    @Json(name = "created_by_step") val createdByStep: ProtocolStep?,
    @Json(name = "metadata_columns") val metadataColumns: List<MetadataColumn>?,
    @Json(name = "notify_on_low_stock") val notifyOnLowStock: Boolean,
    @Json(name = "last_notification_sent") val lastNotificationSent: String?,
    @Json(name = "low_stock_threshold") val lowStockThreshold: Float?,
    @Json(name = "notify_days_before_expiry") val notifyDaysBeforeExpiry: Int?,
    @Json(name = "notify_on_expiry") val notifyOnExpiry: Boolean,
    @Json(name = "last_expiry_notification_sent") val lastExpiryNotificationSent: String?,
    @Json(name = "is_subscribed") val isSubscribed: Boolean,
    val subscription: ReagentSubscriptionInfo?,
    @Json(name = "subscriber_count") val subscriberCount: Int

)
fun StoredReagent.barcodeContent(): String {
    return barcode?: buildString {
        append("ID:${id}")
        append("|NAME:${reagent.name}")
    }
}