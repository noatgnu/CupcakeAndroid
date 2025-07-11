package info.proteo.cupcake.shared.data.model.user

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import info.proteo.cupcake.shared.data.model.storage.StorageObjectBasic

@JsonClass(generateAdapter = true)
data class LabGroupBasic(
    val id: Int,
    val name: String
)

@JsonClass(generateAdapter = true)
data class LabGroup(
    val id: Int,
    val name: String,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?,
    val description: String?,
    @Json(name = "default_storage") val defaultStorage: StorageObjectBasic?,
    @Json(name = "is_professional") val isProfessional: Boolean,
    @Json(name = "service_storage") val serviceStorage: StorageObjectBasic?,
    val managers: List<UserBasic>? = null
)