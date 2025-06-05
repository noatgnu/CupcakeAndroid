package info.proteo.cupcake.data.remote.model.project

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class Project(
    val id: Int,
    @Json(name = "project_name") val projectName: String,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?,
    @Json(name = "project_description") val projectDescription: String?,
    val owner: String,
    val sessions: List<ProjectSession>
)

@JsonClass(generateAdapter = true)
data class ProjectSession(
    @Json(name = "unique_id") val uniqueId: String,
    val name: String?,
    val protocol: List<Int>
)