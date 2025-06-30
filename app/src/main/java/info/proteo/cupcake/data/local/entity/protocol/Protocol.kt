package info.proteo.cupcake.data.local.entity.protocol

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation
import info.proteo.cupcake.data.local.entity.user.UserEntity

@Entity(tableName = "protocol_model")
data class ProtocolModelEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "protocol_id") val protocolId: Long?,
    @ColumnInfo(name = "protocol_created_on") val protocolCreatedOn: String?,
    @ColumnInfo(name = "protocol_doi") val protocolDoi: String?,
    @ColumnInfo(name = "protocol_title") val protocolTitle: String?,
    @ColumnInfo(name = "protocol_description") val protocolDescription: String?,
    @ColumnInfo(name = "protocol_url") val protocolUrl: String?,
    @ColumnInfo(name = "protocol_version_uri") val protocolVersionUri: String?,
    val enabled: Boolean,
    @ColumnInfo(name = "complexity_rating") val complexityRating: Float,
    @ColumnInfo(name = "duration_rating") val durationRating: Float,
    val user: Int?,
    @ColumnInfo(name = "remote_id") val remoteId: Long?,
    @ColumnInfo(name = "model_hash") val modelHash: String?,
    @ColumnInfo(name = "remote_host") val remoteHost: Int?,
        @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?
)

@Entity(
    tableName = "protocol_editor_cross_ref",
    primaryKeys = ["protocolId", "userId"],
    indices = [
        Index("protocolId"),
        Index("userId")
    ]
)
data class ProtocolEditorCrossRef(
    val protocolId: Int,
    val userId: Int
)

@Entity(
    tableName = "protocol_viewer_cross_ref",
    primaryKeys = ["protocolId", "userId"],
    indices = [
        Index("protocolId"),
        Index("userId")
    ]
)
data class ProtocolViewerCrossRef(
    val protocolId: Int,
    val userId: Int
)

data class ProtocolModelWithAccess(
    @Embedded
    val protocol: ProtocolModelEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            ProtocolEditorCrossRef::class,
            parentColumn = "protocolId",
            entityColumn = "userId"
        )
    )
    val editors: List<UserEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            ProtocolViewerCrossRef::class,
            parentColumn = "protocolId",
            entityColumn = "userId"
        )
    )
    val viewers: List<UserEntity>
)



@Entity(tableName = "protocol_step")
data class ProtocolStepEntity(
    @PrimaryKey val id: Int,
    val protocol: Int,
    @ColumnInfo(name = "step_id") val stepId: Long?,
    @ColumnInfo(name = "step_description") val description: String,
    @ColumnInfo(name = "step_section") val stepSection: Int?,
    @ColumnInfo(name = "step_duration") val duration: Int?,
    @ColumnInfo(name = "previous_step") val previousStep: Int?,
    val original: Boolean,
    @ColumnInfo(name = "branch_from") val branchFrom: Int?,
    @ColumnInfo(name = "remote_id") val remoteId: Long?,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "remote_host") val remoteHost: Int?
)

@Entity(
    tableName = "protocol_step_next_relation",
    primaryKeys = ["from_step", "to_step"],
    foreignKeys = [
        ForeignKey(
            entity = ProtocolStepEntity::class,
            parentColumns = ["id"],
            childColumns = ["from_step"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ProtocolStepEntity::class,
            parentColumns = ["id"],
            childColumns = ["to_step"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["from_step"]),
        Index(value = ["to_step"])
    ]
)
data class ProtocolStepNextRelation(
    @ColumnInfo(name = "from_step") val fromStep: Int,
    @ColumnInfo(name = "to_step") val toStep: Int
)

data class ProtocolStepWithNextSteps(
    @Embedded val step: ProtocolStepEntity,
    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = ProtocolStepNextRelation::class,
            parentColumn = "from_step",
            entityColumn = "to_step"
        )
    )
    val nextSteps: List<ProtocolStepEntity>
)

@Entity(tableName = "protocol_section")
data class ProtocolSectionEntity(
    @PrimaryKey val id: Int,
    val protocol: Int,
    @ColumnInfo(name = "section_description") val sectionDescription: String?,
    @ColumnInfo(name = "section_duration") val sectionDuration: Long?,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "remote_id") val remoteId: Long?,
    @ColumnInfo(name = "remote_host") val remoteHost: Int?
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