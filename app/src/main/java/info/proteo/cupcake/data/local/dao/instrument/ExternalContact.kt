package info.proteo.cupcake.data.local.dao.instrument

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import info.proteo.cupcake.data.local.entity.instrument.ExternalContactDetailsEntity
import info.proteo.cupcake.data.local.entity.instrument.ExternalContactEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ExternalContactDetailsDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(detail: ExternalContactDetailsEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(details: List<ExternalContactDetailsEntity>)

    @Update
    suspend fun update(detail: ExternalContactDetailsEntity)

    @Delete
    suspend fun delete(detail: ExternalContactDetailsEntity)

    @Query("SELECT * FROM external_contact_details WHERE id = :id")
    suspend fun getById(id: Int): ExternalContactDetailsEntity?

    @Query("DELETE FROM external_contact_details")
    suspend fun deleteAll()
}

@Dao
interface ExternalContactDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(contact: ExternalContactEntity): Long

    @Update
    suspend fun update(contact: ExternalContactEntity)

    @Delete
    suspend fun delete(contact: ExternalContactEntity)

    @Query("SELECT * FROM external_contact WHERE id = :id")
    suspend fun getById(id: Int): ExternalContactEntity?

    @Query("SELECT * FROM external_contact WHERE user = :userId")
    fun getByUserId(userId: Int): Flow<List<ExternalContactEntity>>

    @Query("DELETE FROM external_contact")
    suspend fun deleteAll()
}
