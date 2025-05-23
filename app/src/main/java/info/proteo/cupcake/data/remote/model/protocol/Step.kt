package info.proteo.cupcake.data.remote.model.protocol

import com.squareup.moshi.Json
import info.proteo.cupcake.data.remote.model.reagent.Reagent
import info.proteo.cupcake.data.remote.model.tag.Tag

data class StepVariation(
    val id: Int,
    val step: Int,
    @Json(name = "variation_description") val variationDescription: String?,
    @Json(name = "variation_duration") val variationDuration: String?
)

data class StepReagent(
    val id: Int,
    val step: Int,
    val reagent: Reagent,
    val quantity: Float,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?,
    val scalable: Boolean,
    @Json(name = "scalable_factor") val scalableFactor: Float?
)

data class StepTag(
    val id: Int,
    val step: Int,
    val tag: Tag,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?
)