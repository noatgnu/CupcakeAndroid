package info.proteo.cupcake.shared.data.model.storage

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import info.proteo.cupcake.shared.data.model.reagent.StoredReagent

@JsonClass(generateAdapter = true)
data class StoragePathItem(
    val id: Int,
    val name: String
)

@JsonClass(generateAdapter = true)
data class StorageObject(
    val id: Int,
    @Json(name = "object_name") val objectName: String,
    @Json(name = "object_type") val objectType: String?,
    @Json(name = "object_description") val objectDescription: String?,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?,
    @Json(name = "can_delete") val canDelete: Boolean,
    @Json(name = "stored_at") val storedAt: Int?,
    @Json(name = "stored_reagents") val storedReagents: List<StoredReagent>?,
    @Json(name = "png_base64") val pngBase64: String?,
    val user: String?,
    @Json(name = "access_lab_groups") val accessLabGroups: List<Int>?,
    @Json(name = "path_to_root") val pathToRoot: List<StoragePathItem>?,
    @Json(name = "child_count") val childCount: Int,
    @Json(name = "remote_id") val remoteId: Long?,
    @Json(name = "remote_host") val remoteHost: Int?
)

@JsonClass(generateAdapter = true)
data class StorageObjectBasic(
    val id: Int,
    @Json(name = "object_name") val objectName: String,
    @Json(name = "object_description") val objectDescription: String?,
    @Json(name = "object_type") val objectType: String?
)

