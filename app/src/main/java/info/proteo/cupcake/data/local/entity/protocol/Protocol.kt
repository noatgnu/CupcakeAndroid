package info.proteo.cupcake.data.local.entity.protocol

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "protocol_model")
data class ProtocolModelEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "protocol_id") val protocolId: String?,
    @ColumnInfo(name = "protocol_created_on") val protocolCreatedOn: String?,
    @ColumnInfo(name = "protocol_doi") val protocolDoi: String?,
    @ColumnInfo(name = "protocol_title") val protocolTitle: String?,
    @ColumnInfo(name = "protocol_description") val protocolDescription: String?,
    @ColumnInfo(name = "protocol_url") val protocolUrl: String?,
    @ColumnInfo(name = "protocol_version_uri") val protocolVersionUri: String?,
    val enabled: Boolean,
    @ColumnInfo(name = "complexity_rating") val complexityRating: Float,
    @ColumnInfo(name = "duration_rating") val durationRating: Float
)

@Entity(tableName = "protocol_step")
data class ProtocolStepEntity(
    @PrimaryKey val id: Int,
    val protocol: Int,
    @ColumnInfo(name = "step_id") val stepId: String?,
    @ColumnInfo(name = "step_description") val stepDescription: String?,
    @ColumnInfo(name = "step_section") val stepSection: String?,
    @ColumnInfo(name = "step_duration") val stepDuration: String?,
    @ColumnInfo(name = "next_step") val nextStep: Int?,
    @ColumnInfo(name = "previous_step") val previousStep: Int?,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?
)

@Entity(tableName = "protocol_section")
data class ProtocolSectionEntity(
    @PrimaryKey val id: Int,
    val protocol: Int,
    @ColumnInfo(name = "section_description") val sectionDescription: String?,
    @ColumnInfo(name = "section_duration") val sectionDuration: String?,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?
)

@Entity(tableName = "protocol_rating")
data class ProtocolRatingEntity(
    @PrimaryKey val id: Int,
    val protocol: Int,
    val user: Int,
    @ColumnInfo(name = "complexity_rating") val complexityRating: Int,
    @ColumnInfo(name = "duration_rating") val durationRating: Int,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?
)

@Entity(tableName = "protocol_reagent")
data class ProtocolReagentEntity(
    @PrimaryKey val id: Int,
    val protocol: Int,
    val reagent: Int,
    val quantity: Float,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?
)

@Entity(tableName = "protocol_tag")
data class ProtocolTagEntity(
    @PrimaryKey val id: Int,
    val protocol: Int,
    val tag: Int,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?
)