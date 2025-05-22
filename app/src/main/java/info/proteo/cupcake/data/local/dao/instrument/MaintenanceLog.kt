package info.proteo.cupcake.data.local.dao.instrument

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import info.proteo.cupcake.data.local.entity.instrument.MaintenanceLogEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MaintenanceLogDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(log: MaintenanceLogEntity): Long

    @Update
    suspend fun update(log: MaintenanceLogEntity)

    @Delete
    suspend fun delete(log: MaintenanceLogEntity)

    @Query("SELECT * FROM maintenance_log WHERE id = :id")
    suspend fun getById(id: Int): MaintenanceLogEntity?

    @Query("SELECT * FROM maintenance_log WHERE instrument = :instrumentId")
    fun getByInstrument(instrumentId: Int): Flow<List<MaintenanceLogEntity>>

    @Query("SELECT * FROM maintenance_log WHERE created_by = :userId")
    fun getByUser(userId: Int): Flow<List<MaintenanceLogEntity>>

    @Query("SELECT * FROM maintenance_log WHERE is_template = 1")
    fun getTemplates(): Flow<List<MaintenanceLogEntity>>

    @Query("DELETE FROM maintenance_log")
    suspend fun deleteAll()
}
