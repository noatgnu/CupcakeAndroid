package info.proteo.cupcake.shared.data.model.metadatacolumn

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class MetadataTableTemplate(
    val id: Int,
    val name: String?,
    val user: Int?,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?,
    @Json(name = "user_columns") val userColumns: List<MetadataColumn>?,
    @Json(name = "staff_columns") val staffColumns: List<MetadataColumn>?,
    @Json(name = "service_lab_group") val serviceLabGroup: Int?,
    @Json(name = "lab_group") val labGroup: Int?,
    val enabled: Boolean,
    @Json(name = "field_mask_mapping") val fieldMaskMapping: String?, // JSON string
    @Json(name = "hidden_user_columns_count") val hiddenUserColumnsCount: Int?,
    @Json(name = "hidden_staff_columns_count") val hiddenStaffColumnsCount: Int?
)

@JsonClass(generateAdapter = true)
data class MetadataTableTemplateBasic(
    val id: Int,
    val name: String?,
    val enabled: Boolean,
    @Json(name = "service_lab_group") val serviceLabGroup: Int?,
    @Json(name = "lab_group") val labGroup: Int?
)