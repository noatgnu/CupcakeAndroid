package info.proteo.cupcake.shared.data.model.reagent

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import info.proteo.cupcake.shared.data.model.DataPermission
import info.proteo.cupcake.shared.data.model.user.UserBasic

@JsonClass(generateAdapter = true)
data class StoredReagentPermission(
    val permission: DataPermission,
    @Json(name = "stored_reagent") val storedReagent: Int,
)

@JsonClass(generateAdapter = true)
data class Reagent(
    val id: Int,
    val name: String,
    val unit: String,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?
)

@JsonClass(generateAdapter = true)
data class ReagentSubscription(
    val id: Int,
    val user: UserBasic,
    @Json(name = "stored_reagent") val storedReagent: Int,
    @Json(name = "notify_on_low_stock") val notifyOnLowStock: Boolean,
    @Json(name = "notify_on_expiry") val notifyOnExpiry: Boolean,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class ReagentSubscriptionInfo(
    val id: Int,
    @Json(name = "notify_on_low_stock") val notifyOnLowStock: Boolean,
    @Json(name = "notify_on_expiry") val notifyOnExpiry: Boolean,
    @Json(name = "created_at") val createdAt: String
)

@JsonClass(generateAdapter = true)
data class ReagentAction(
    val id: Int,
    val reagent: Int,
    @Json(name = "action_type") val actionType: String?,
    val notes: String?,
    val quantity: Float,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?,
    val user: String?,
    @Json(name = "step_reagent") val stepReagent: Int?
)

@JsonClass(generateAdapter = true)
data class StoredReagentCreateRequest(
    @Json(name = "created_by_project") val createdByProject: Int? = null,
    @Json(name = "created_by_protocol") val createdByProtocol: Int? = null,
    @Json(name = "storage_object_id") val storageObjectId: Int? = null,
    @Json(name = "name") val name: String,
    @Json(name = "unit") val unit: String,
    @Json(name = "quantity") val quantity: Float,
    @Json(name = "notes") val notes: String? = null,
    @Json(name = "shareable") val shareable: Boolean = false,
    @Json(name = "access_all") val accessAll: Boolean = false,
    @Json(name = "barcode") val barcode: String? = null,
    @Json(name = "created_by_step") val createdByStep: Int? = null,
    @Json(name = "created_by_session") val createdBySession: String? = null,
)