package info.proteo.cupcake.data.local.entity.instrument

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "maintenance_log")
data class MaintenanceLogEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "maintenance_date") val maintenanceDate: String?,
    @ColumnInfo(name = "maintenance_notes") val maintenanceNotes: String?,
    @ColumnInfo(name = "maintenance_type") val maintenanceType: String?,
    @ColumnInfo(name = "maintenance_description") val maintenanceDescription: String?,
    val instrument: Int?,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "created_by") val createdBy: Int?,
    val status: String?,
    @ColumnInfo(name = "is_template") val isTemplate: Boolean,
    @ColumnInfo(name = "annotation_folder") val annotationFolder: Int?
)