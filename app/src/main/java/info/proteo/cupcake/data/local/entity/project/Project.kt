package info.proteo.cupcake.data.local.entity.project

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "project")
data class ProjectEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "project_name") val projectName: String,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "project_description") val projectDescription: String?,
    val owner: String
)