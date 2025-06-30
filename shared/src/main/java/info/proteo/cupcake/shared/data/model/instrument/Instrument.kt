package info.proteo.cupcake.shared.data.model.instrument

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import info.proteo.cupcake.shared.data.model.annotation.AnnotationFolder
import info.proteo.cupcake.shared.data.model.metadatacolumn.MetadataColumn

@JsonClass(generateAdapter = true)
data class Instrument(
    val id: Int,
    @Json(name = "max_days_ahead_pre_approval") val maxDaysAheadPreApproval: Int?,
    @Json(name = "max_days_within_usage_pre_approval") val maxDaysWithinUsagePreApproval: Int?,
    @Json(name = "instrument_name") val instrumentName: String?,
    @Json(name = "instrument_description") val instrumentDescription: String?,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?,
    val enabled: Boolean,
    @Json(name = "metadata_columns") val metadataColumns: List<MetadataColumn>?,
    @Json(name = "annotation_folders") val annotationFolders: List<AnnotationFolder>?,
    val image: String?,
    @Json(name = "support_information") val supportInformation: List<SupportInformation>?,
    @Json(name = "days_before_maintenance_notification") val daysBeforeMaintenanceNotification: Int?,
    @Json(name = "days_before_warranty_notification") val daysBeforeWarrantyNotification: Int?,
    @Json(name = "last_maintenance_notification_sent") val lastMaintenanceNotificationSent: String?,
    @Json(name = "last_warranty_notification_sent") val lastWarrantyNotificationSent: String?,
    @Json(name = "accepts_bookings") val acceptsBookings: Boolean?
)