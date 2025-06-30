package info.proteo.cupcake.shared.data.model.communication

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class WebRTCSession(
    val id: Int,
    @Json(name = "session_id") val sessionId: String,
    @Json(name = "is_active") val isActive: Boolean,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?
)

@JsonClass(generateAdapter = true)
data class WebRTCUserChannel(
    val id: Int,
    @Json(name = "user_id") val userId: Int,
    @Json(name = "channel_name") val channelName: String,
    @Json(name = "channel_type") val channelType: String,
    @Json(name = "created_at") val createdAt: String?
)

@JsonClass(generateAdapter = true)
data class WebRTCUserOffer(
    val id: Int,
    @Json(name = "user_id") val userId: Int,
    @Json(name = "offer_data") val offerData: String,
    @Json(name = "id_type") val idType: String,
    @Json(name = "created_at") val createdAt: String?
)