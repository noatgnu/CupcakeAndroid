package info.proteo.cupcake.data.local.entity.storage

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "storage_object")
data class StorageObjectEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "object_name") val objectName: String,
    @ColumnInfo(name = "object_type") val objectType: String?,
    @ColumnInfo(name = "object_description") val objectDescription: String?,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "can_delete") val canDelete: Boolean,
    @ColumnInfo(name = "stored_at") val storedAt: Int?,
    @ColumnInfo(name = "png_base64") val pngBase64: String?,
    val user: String?,
    @ColumnInfo(name = "remote_id") val remoteId: Long?,
    @ColumnInfo(name = "remote_host") val remoteHost: Int?
)

@Entity(
    tableName = "storage_object_access_lab_group",
    primaryKeys = ["storageObjectId", "labGroupId"]
)
data class StorageObjectAccessLabGroupCrossRef(
    val storageObjectId: Int,
    val labGroupId: Int
)

