package info.proteo.cupcake.data.local.dao.instrument

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import info.proteo.cupcake.data.local.entity.instrument.SupportInformationEntity
import info.proteo.cupcake.data.local.entity.instrument.SupportInformationManufacturerContactCrossRef
import info.proteo.cupcake.data.local.entity.instrument.SupportInformationVendorContactCrossRef
import info.proteo.cupcake.data.local.entity.instrument.SupportInformationWithContacts

@Dao
interface SupportInformationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(info: SupportInformationEntity): Long

    @Update
    suspend fun update(info: SupportInformationEntity)

    @Delete
    suspend fun delete(info: SupportInformationEntity)

    @Query("SELECT * FROM support_information WHERE id = :id")
    suspend fun getById(id: Int): SupportInformationEntity?

    @Query("DELETE FROM support_information")
    suspend fun deleteAll()

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVendorContactRef(crossRef: SupportInformationVendorContactCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertManufacturerContactRef(crossRef: SupportInformationManufacturerContactCrossRef)

    @Delete
    suspend fun deleteVendorContactRef(crossRef: SupportInformationVendorContactCrossRef)

    @Delete
    suspend fun deleteManufacturerContactRef(crossRef: SupportInformationManufacturerContactCrossRef)

    @Transaction
    @Query("SELECT * FROM support_information WHERE id = :id")
    suspend fun getSupportInformationWithContacts(id: Int): SupportInformationWithContacts?

    @Query("DELETE FROM support_information_vendor_contact WHERE support_information_id = :supportInfoId")
    suspend fun deleteAllVendorContactsForSupportInfo(supportInfoId: Int)

    @Query("DELETE FROM support_information_manufacturer_contact WHERE support_information_id = :supportInfoId")
    suspend fun deleteAllManufacturerContactsForSupportInfo(supportInfoId: Int)

}
