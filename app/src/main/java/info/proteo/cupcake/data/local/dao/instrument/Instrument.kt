package info.proteo.cupcake.data.local.dao.instrument

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import info.proteo.cupcake.data.local.entity.instrument.InstrumentEntity
import info.proteo.cupcake.data.local.entity.instrument.InstrumentUsageEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface InstrumentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(instrument: InstrumentEntity): Long

    @Update
    suspend fun update(instrument: InstrumentEntity)

    @Delete
    suspend fun delete(instrument: InstrumentEntity)

    @Query("SELECT * FROM instrument WHERE id = :id")
    suspend fun getById(id: Int): InstrumentEntity?

    @Query("SELECT * FROM instrument WHERE enabled = 1")
    fun getEnabledInstruments(): Flow<List<InstrumentEntity>>

    @Query("SELECT * FROM instrument")
    fun getAllInstruments(): Flow<List<InstrumentEntity>>

    @Query("DELETE FROM instrument")
    suspend fun deleteAll()
}

@Dao
interface InstrumentUsageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(usage: InstrumentUsageEntity): Long

    @Update
    suspend fun update(usage: InstrumentUsageEntity)

    @Delete
    suspend fun delete(usage: InstrumentUsageEntity)

    @Query("SELECT * FROM instrument_usage WHERE id = :id")
    suspend fun getById(id: Int): InstrumentUsageEntity?

    @Query("SELECT * FROM instrument_usage WHERE instrument = :instrumentId")
    fun getByInstrument(instrumentId: Int): Flow<List<InstrumentUsageEntity>>

    @Query("SELECT * FROM instrument_usage WHERE user = :username")
    fun getByUser(username: String): Flow<List<InstrumentUsageEntity>>

    @Query("DELETE FROM instrument_usage")
    suspend fun deleteAll()

    suspend fun insertAll(usages: List<InstrumentUsageEntity>) {
        usages.forEach { insert(it) }
    }
}