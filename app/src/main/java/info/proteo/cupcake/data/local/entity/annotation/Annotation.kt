package info.proteo.cupcake.data.local.entity.annotation

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey


@Entity(tableName = "annotation")
data class AnnotationEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "step") val step: Int?,
    @ColumnInfo(name = "session") val session: Int?,
    @ColumnInfo(name = "annotation") val annotation: String?,
    @ColumnInfo(name = "file") val file: String?,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "annotation_type") val annotationType: String?,
    @ColumnInfo(name = "transcribed") val transcribed: Boolean?,
    @ColumnInfo(name = "transcription") val transcription: String?,
    @ColumnInfo(name = "language") val language: String?,
    @ColumnInfo(name = "translation") val translation: String?,
    @ColumnInfo(name = "scratched") val scratched: Boolean?,
    @ColumnInfo(name = "annotation_name") val annotationName: String?,
    @ColumnInfo(name = "summary") val summary: String?,
    @ColumnInfo(name = "fixed") val fixed: Boolean?,
    @ColumnInfo(name = "user_id") val userId: Int?,
    @ColumnInfo(name = "stored_reagent") val storedReagent: Int?
)

@Entity(tableName = "annotation_folder_path")
data class AnnotationFolderPathEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "folder_name") val folderName: String
)

@Entity(tableName = "annotation_folder")
data class AnnotationFolderEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "folder_name") val folderName: String,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "parent_folder") val parentFolder: Int?,
    val session: Int?,
    val instrument: Int?,
    @ColumnInfo(name = "stored_reagent") val storedReagent: Int?
)