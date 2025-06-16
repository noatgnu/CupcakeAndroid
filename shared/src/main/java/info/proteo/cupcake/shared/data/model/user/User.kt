package info.proteo.cupcake.shared.data.model.user

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass


@JsonClass(generateAdapter = true)
data class User(
    val id: Int,
    val username: String,
    @Json(name = "email") val email: String?,
    @Json(name = "first_name") val firstName: String?,
    @Json(name = "last_name") val lastName: String?,
    @Json(name = "is_staff") val isStaff: Boolean,
    @Json(name = "lab_groups") val labGroups: List<LabGroup>,
    @Json(name = "managed_lab_groups") val managedLabGroups: List<LabGroup>

)

@JsonClass(generateAdapter = true)
data class UserBasic(
    val id: Int,
    val username: String,
    @Json(name = "first_name") val firstName: String?,
    @Json(name = "last_name") val lastName: String?,
)