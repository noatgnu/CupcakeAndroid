package info.proteo.cupcake.data.local.entity.protocol

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "step_variation")
data class StepVariationEntity(
    @PrimaryKey val id: Int,
    val step: Int,
    @ColumnInfo(name = "variation_description") val variationDescription: String?,
    @ColumnInfo(name = "variation_duration") val variationDuration: String?
)

@Entity(tableName = "step_reagent")
data class StepReagentEntity(
    @PrimaryKey val id: Int,
    val step: Int,
    val reagent: Int,
    val quantity: Float,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    val scalable: Boolean,
    @ColumnInfo(name = "scalable_factor") val scalableFactor: Float?
)

@Entity(tableName = "step_tag")
data class StepTagEntity(
    @PrimaryKey val id: Int,
    val step: Int,
    val tag: Int,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?
)