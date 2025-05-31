package info.proteo.cupcake.data.local.entity.instrument

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "external_contact_details")
data class ExternalContactDetailsEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "contact_method_alt_name") val contactMethodAltName: String?,
    @ColumnInfo(name = "contact_type") val contactType: String?,
    @ColumnInfo(name = "contact_value") val contactValue: String?
)

@Entity(tableName = "external_contact")
data class ExternalContactEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "contact_value") val contactName: String?
)


