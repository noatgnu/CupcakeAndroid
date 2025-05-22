package info.proteo.cupcake.data.model.api.user

import com.squareup.moshi.Json

data class UserBasic(
    val id: Int,
    val username: String,
    @Json(name = "first_name") val firstName: String?,
    @Json(name = "last_name") val lastName: String?
)

data class User(
    val id: Int,
    val username: String,
    @Json(name = "lab_groups") val labGroups: List<LabGroup>,
    @Json(name = "managed_lab_groups") val managedLabGroups: List<LabGroup>,
    val email: String?,
    @Json(name = "first_name") val firstName: String?,
    @Json(name = "last_name") val lastName: String?,
    @Json(name = "is_staff") val isStaff: Boolean
)