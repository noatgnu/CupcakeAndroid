package info.proteo.cupcake.data.local.dao.protocol

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import info.proteo.cupcake.data.local.entity.protocol.StepReagentEntity
import info.proteo.cupcake.data.local.entity.protocol.StepTagEntity
import info.proteo.cupcake.data.local.entity.protocol.StepVariationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface StepVariationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(variation: StepVariationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(variations: List<StepVariationEntity>)

    @Update
    suspend fun update(variation: StepVariationEntity)

    @Delete
    suspend fun delete(variation: StepVariationEntity)

    @Query("SELECT * FROM step_variation WHERE id = :id")
    suspend fun getById(id: Int): StepVariationEntity?

    @Query("SELECT * FROM step_variation WHERE step = :stepId")
    fun getByStep(stepId: Int): Flow<List<StepVariationEntity>>

    @Query("DELETE FROM step_variation")
    suspend fun deleteAll()
}

@Dao
interface StepReagentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reagent: StepReagentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reagents: List<StepReagentEntity>)

    @Update
    suspend fun update(reagent: StepReagentEntity)

    @Delete
    suspend fun delete(reagent: StepReagentEntity)

    @Query("SELECT * FROM step_reagent WHERE id = :id")
    suspend fun getById(id: Int): StepReagentEntity?

    @Query("SELECT * FROM step_reagent WHERE step = :stepId")
    fun getByStep(stepId: Int): Flow<List<StepReagentEntity>>

    @Query("DELETE FROM step_reagent")
    suspend fun deleteAll()
}

@Dao
interface StepTagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: StepTagEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tags: List<StepTagEntity>)

    @Update
    suspend fun update(tag: StepTagEntity)

    @Delete
    suspend fun delete(tag: StepTagEntity)

    @Query("SELECT * FROM step_tag WHERE id = :id")
    suspend fun getById(id: Int): StepTagEntity?

    @Query("SELECT * FROM step_tag WHERE step = :stepId")
    fun getByStep(stepId: Int): Flow<List<StepTagEntity>>

    @Query("DELETE FROM step_tag")
    suspend fun deleteAll()
}
