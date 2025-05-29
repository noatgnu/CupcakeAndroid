package info.proteo.cupcake.data.remote.model.annotation

import com.squareup.moshi.Json
import info.proteo.cupcake.data.remote.model.instrument.InstrumentUsage
import info.proteo.cupcake.data.remote.model.metadatacolumn.MetadataColumn
import info.proteo.cupcake.data.remote.model.user.UserBasic

data class Annotation(
    val id: Int,
    val step: Int?,
    val session: Int?,
    val annotation: String?,
    val file: String?,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?,
    @Json(name = "annotation_type") val annotationType: String?,
    val transcribed: Boolean?,
    val transcription: String?,
    val language: String?,
    val translation: String?,
    val scratched: Boolean?,
    @Json(name = "annotation_name") val annotationName: String?,
    val folder: List<AnnotationFolderPath>,
    val summary: String?,
    @Json(name = "instrument_usage") val instrumentUsage: List<InstrumentUsage>?,
    @Json(name = "metadata_columns") val metadataColumns: List<MetadataColumn>?,
    val fixed: Boolean?,
    val user: UserBasic?,
    @Json(name = "stored_reagent") val storedReagent: Int?
)

data class AnnotationFolderPath(
    val id: Int,
    @Json(name = "folder_name") val folderName: String
)

data class AnnotationFolder(
    val id: Int,
    @Json(name = "folder_name") val folderName: String,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?,
    @Json(name = "parent_folder") val parentFolder: Int?,
    val session: Int?,
    val instrument: Int?,
    @Json(name = "stored_reagent") val storedReagent: Int?
)

data class AnnotationFolderDetails(
    val id: Int,
    @Json(name = "folder_name") val folderName: String
)