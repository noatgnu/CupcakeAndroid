package info.proteo.cupcake.data.remote.model.instrument

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import info.proteo.cupcake.data.remote.model.annotation.AnnotationFolderDetails
import info.proteo.cupcake.data.remote.model.user.UserBasic

@JsonClass(generateAdapter = true)
data class MaintenanceLog(
    val id: Int,
    @Json(name = "maintenance_date") val maintenanceDate: String?,
    @Json(name = "maintenance_notes") val maintenanceNotes: String?,
    @Json(name = "maintenance_type") val maintenanceType: String?,
    @Json(name = "maintenance_description") val maintenanceDescription: String?,
    val instrument: Int?,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?,
    @Json(name = "created_by") val createdBy: Int?,
    @Json(name = "created_by_user") val createdByUser: UserBasic?,
    val status: String?,
    @Json(name = "is_template") val isTemplate: Boolean,
    @Json(name = "annotation_folder") val annotationFolder: Int?,
    @Json(name = "annotation_folder_details") val annotationFolderDetails: AnnotationFolderDetails?,
    val annotations: List<Annotation>
)