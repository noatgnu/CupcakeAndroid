package info.proteo.cupcake.data.local.entity.instrument

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "support_information")
data class SupportInformationEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "vendor_name") val vendorName: String?,
    @ColumnInfo(name = "manufacturer_name") val manufacturerName: String?,
    @ColumnInfo(name = "serial_number") val serialNumber: String?,
    @ColumnInfo(name = "maintenance_frequency_days") val maintenanceFrequencyDays: Int?,
    @ColumnInfo(name = "location_id") val locationId: Int?,
    @ColumnInfo(name = "warranty_start_date") val warrantyStartDate: String?,
    @ColumnInfo(name = "warranty_end_date") val warrantyEndDate: String?,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?
)