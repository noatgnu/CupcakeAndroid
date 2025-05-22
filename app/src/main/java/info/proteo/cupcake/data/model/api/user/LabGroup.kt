package info.proteo.cupcake.data.model.api.user

import com.squareup.moshi.Json
import info.proteo.cupcake.data.model.api.storage.StorageObjectBasic

data class LabGroupBasic(
    val id: Int,
    val name: String
)

data class LabGroup(
    val id: Int,
    val name: String,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?,
    val description: String?,
    @Json(name = "default_storage") val defaultStorage: StorageObjectBasic?,
    @Json(name = "is_professional") val isProfessional: Boolean,
    @Json(name = "service_storage") val serviceStorage: StorageObjectBasic?
)