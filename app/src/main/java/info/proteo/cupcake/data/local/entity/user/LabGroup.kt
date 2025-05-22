package info.proteo.cupcake.data.local.entity.user

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "lab_group_basic")
data class LabGroupBasicEntity(
    @PrimaryKey val id: Int,
    val name: String
)

@Entity(tableName = "lab_group")
data class LabGroupEntity(
    @PrimaryKey val id: Int,
    val name: String,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    val description: String?,
    @ColumnInfo(name = "default_storage") val defaultStorage: Int?,
    @ColumnInfo(name = "is_professional") val isProfessional: Boolean,
    @ColumnInfo(name = "service_storage") val serviceStorage: Int?
)