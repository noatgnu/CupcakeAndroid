package info.proteo.cupcake.data.remote.model.instrument

import com.squareup.moshi.Json
import info.proteo.cupcake.data.remote.model.storage.StorageObjectBasic

data class SupportInformation(
    val id: Int,
    @Json(name = "vendor_name") val vendorName: String?,
    @Json(name = "vendor_contacts") val vendorContacts: List<ExternalContact>?,
    @Json(name = "manufacturer_name") val manufacturerName: String?,
    @Json(name = "manufacturer_contacts") val manufacturerContacts: List<ExternalContact>?,
    @Json(name = "serial_number") val serialNumber: String?,
    @Json(name = "maintenance_frequency_days") val maintenanceFrequencyDays: Int?,
    val location: StorageObjectBasic?,
    @Json(name = "location_id") val locationId: Int?,
    @Json(name = "warranty_start_date") val warrantyStartDate: String?,
    @Json(name = "warranty_end_date") val warrantyEndDate: String?,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?
)