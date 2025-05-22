package info.proteo.cupcake.data.local.dao.user

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import info.proteo.cupcake.data.local.entity.user.LabGroupBasicEntity
import info.proteo.cupcake.data.local.entity.user.LabGroupEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface LabGroupBasicDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(labGroup: LabGroupBasicEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(labGroups: List<LabGroupBasicEntity>)

    @Update
    suspend fun update(labGroup: LabGroupBasicEntity)

    @Delete
    suspend fun delete(labGroup: LabGroupBasicEntity)

    @Query("SELECT * FROM lab_group_basic WHERE id = :id")
    suspend fun getById(id: Int): LabGroupBasicEntity?

    @Query("SELECT * FROM lab_group_basic")
    fun getAllLabGroups(): Flow<List<LabGroupBasicEntity>>

    @Query("DELETE FROM lab_group_basic")
    suspend fun deleteAll()
}

@Dao
interface LabGroupDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(labGroup: LabGroupEntity): Long

    @Update
    suspend fun update(labGroup: LabGroupEntity)

    @Delete
    suspend fun delete(labGroup: LabGroupEntity)

    @Query("SELECT * FROM lab_group WHERE id = :id")
    suspend fun getById(id: Int): LabGroupEntity?

    @Query("SELECT * FROM lab_group")
    fun getAllLabGroups(): Flow<List<LabGroupEntity>>

    @Query("DELETE FROM lab_group")
    suspend fun deleteAll()

    @Query("SELECT * FROM lab_group ORDER BY name ASC LIMIT :limit")
    suspend fun getLabGroups(limit: Int): List<LabGroupEntity>

    @Query("DELETE FROM lab_group WHERE id = :id")
    suspend fun deleteById(id: Int)
}
