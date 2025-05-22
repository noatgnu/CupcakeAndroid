package info.proteo.cupcake.data.model.api.reagent

import com.squareup.moshi.Json
import info.proteo.cupcake.data.model.api.user.UserBasic

data class Reagent(
    val id: Int,
    val name: String,
    val unit: String,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?
)

data class ReagentSubscription(
    val id: Int,
    val user: UserBasic,
    @Json(name = "stored_reagent") val storedReagent: Int,
    @Json(name = "notify_on_low_stock") val notifyOnLowStock: Boolean,
    @Json(name = "notify_on_expiry") val notifyOnExpiry: Boolean,
    @Json(name = "created_at") val createdAt: String
)

data class ReagentSubscriptionInfo(
    val id: Int,
    @Json(name = "notify_on_low_stock") val notifyOnLowStock: Boolean,
    @Json(name = "notify_on_expiry") val notifyOnExpiry: Boolean,
    @Json(name = "created_at") val createdAt: String
)

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