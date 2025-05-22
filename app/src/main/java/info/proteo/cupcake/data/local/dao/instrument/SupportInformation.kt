package info.proteo.cupcake.data.local.dao.instrument

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import info.proteo.cupcake.data.local.entity.instrument.SupportInformationEntity

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
}
