package info.proteo.cupcake.shared.data.model.maintenance

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import info.proteo.cupcake.shared.data.model.annotation.Annotation
import info.proteo.cupcake.shared.data.model.user.UserBasic

@JsonClass(generateAdapter = true)
data class MaintenanceLog(
    val id: Long,
    @Json(name = "instrument") val instrumentId: Long,
    @Json(name = "maintenance_date") val maintenanceDate: String,
    @Json(name = "maintenance_type") val maintenanceType: String,
    @Json(name = "maintenance_description") val maintenanceDescription: String?,
    @Json(name = "maintenance_notes") val maintenanceNotes: String?,
    val status: String,
    @Json(name = "is_template") val isTemplate: Boolean,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?,
    @Json(name = "created_by") val createdBy: Long?,
    @Json(name = "created_by_user") val createdByUser: UserBasic?,
    @Json(name = "annotation_folder") val annotationFolder: Long?,
    @Json(name = "annotation_folder_details") val annotationFolderDetails: AnnotationFolderDetails?,
    val annotations: List<Annotation> = emptyList()
)

@JsonClass(generateAdapter = true)
data class AnnotationFolderDetails(
    val id: Long,
    @Json(name = "folder_name") val folderName: String
)

@JsonClass(generateAdapter = true)
data class MaintenanceLogRequest(
    @Json(name = "instrument") val instrumentId: Long,
    @Json(name = "maintenance_date") val maintenanceDate: String,
    @Json(name = "maintenance_type") val maintenanceType: String,
    @Json(name = "maintenance_description") val maintenanceDescription: String?,
    @Json(name = "maintenance_notes") val maintenanceNotes: String?,
    val status: String? = null,
    @Json(name = "is_template") val isTemplate: Boolean = false
)

@JsonClass(generateAdapter = true)
data class MaintenanceLogStatusUpdate(
    val status: String
)

@JsonClass(generateAdapter = true)
data class MaintenanceType(
    val value: String,
    val label: String
)

@JsonClass(generateAdapter = true)
data class MaintenanceStatus(
    val value: String,
    val label: String
)

@JsonClass(generateAdapter = true)
data class MaintenanceTypesResponse(
    val types: List<MaintenanceType>
)

@JsonClass(generateAdapter = true)
data class MaintenanceStatusesResponse(
    val statuses: List<MaintenanceStatus>
)

// Constants for maintenance types and statuses
object MaintenanceConstants {
    object Types {
        const val ROUTINE = "routine"
        const val EMERGENCY = "emergency"
        const val OTHER = "other"
    }
    
    object Statuses {
        const val PENDING = "pending"
        const val IN_PROGRESS = "in_progress"
        const val COMPLETED = "completed"
        const val REQUESTED = "requested"
        const val CANCELLED = "cancelled"
    }
}