package info.proteo.cupcake.data.remote.model.instrument

import com.squareup.moshi.Json
import info.proteo.cupcake.data.remote.model.annotation.AnnotationFolder
import info.proteo.cupcake.data.remote.model.metadatacolumn.MetadataColumn

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
    @Json(name = "support_information") val supportInformation: List<SupportInformation>?
)