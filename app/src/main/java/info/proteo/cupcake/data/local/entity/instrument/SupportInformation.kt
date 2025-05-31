package info.proteo.cupcake.data.local.entity.instrument

import androidx.room.ColumnInfo
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Junction
import androidx.room.PrimaryKey
import androidx.room.Relation

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

@Entity(
    tableName = "support_information_vendor_contact",
    primaryKeys = ["support_information_id", "contact_id"],
    foreignKeys = [
        ForeignKey(
            entity = SupportInformationEntity::class,
            parentColumns = ["id"],
            childColumns = ["support_information_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExternalContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contact_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SupportInformationVendorContactCrossRef(
    @ColumnInfo(name = "support_information_id") val supportInformationId: Int,
    @ColumnInfo(name = "contact_id") val contactId: Int
)

@Entity(
    tableName = "support_information_manufacturer_contact",
    primaryKeys = ["support_information_id", "contact_id"],
    foreignKeys = [
        ForeignKey(
            entity = SupportInformationEntity::class,
            parentColumns = ["id"],
            childColumns = ["support_information_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = ExternalContactEntity::class,
            parentColumns = ["id"],
            childColumns = ["contact_id"],
            onDelete = ForeignKey.CASCADE
        )
    ]
)
data class SupportInformationManufacturerContactCrossRef(
    @ColumnInfo(name = "support_information_id") val supportInformationId: Int,
    @ColumnInfo(name = "contact_id") val contactId: Int
)

data class SupportInformationWithContacts(
    @Embedded val supportInformation: SupportInformationEntity,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = SupportInformationVendorContactCrossRef::class,
            parentColumn = "support_information_id",
            entityColumn = "contact_id"
        )
    )
    val vendorContacts: List<ExternalContactEntity>,

    @Relation(
        parentColumn = "id",
        entityColumn = "id",
        associateBy = Junction(
            value = SupportInformationManufacturerContactCrossRef::class,
            parentColumn = "support_information_id",
            entityColumn = "contact_id"
        )
    )
    val manufacturerContacts: List<ExternalContactEntity>
)