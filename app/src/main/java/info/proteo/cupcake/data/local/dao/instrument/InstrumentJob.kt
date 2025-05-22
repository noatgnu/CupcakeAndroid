package info.proteo.cupcake.data.local.dao.instrument

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import info.proteo.cupcake.data.local.entity.instrument.InstrumentJobEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InstrumentJobDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(job: InstrumentJobEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(jobs: List<InstrumentJobEntity>)

    @Update
    suspend fun update(job: InstrumentJobEntity)

    @Delete
    suspend fun delete(job: InstrumentJobEntity)

    @Query("SELECT * FROM instrument_job WHERE id = :id")
    suspend fun getById(id: Int): InstrumentJobEntity?

    @Query("SELECT * FROM instrument_job WHERE instrument = :instrumentId")
    fun getByInstrument(instrumentId: Int): Flow<List<InstrumentJobEntity>>

    @Query("SELECT * FROM instrument_job WHERE user_id = :userId")
    fun getByUserId(userId: Int): Flow<List<InstrumentJobEntity>>

    @Query("SELECT * FROM instrument_job WHERE status = :status")
    fun getByStatus(status: String): Flow<List<InstrumentJobEntity>>

    @Query("DELETE FROM instrument_job")
    suspend fun deleteAll()
}