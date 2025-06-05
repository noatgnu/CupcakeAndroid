package info.proteo.cupcake.data.local.dao.protocol

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import info.proteo.cupcake.data.local.entity.protocol.ProtocolEditorCrossRef
import info.proteo.cupcake.data.local.entity.protocol.ProtocolModelEntity
import info.proteo.cupcake.data.local.entity.protocol.ProtocolModelWithAccess
import info.proteo.cupcake.data.local.entity.protocol.ProtocolRatingEntity
import info.proteo.cupcake.data.local.entity.protocol.ProtocolReagentEntity
import info.proteo.cupcake.data.local.entity.protocol.ProtocolSectionEntity
import info.proteo.cupcake.data.local.entity.protocol.ProtocolStepEntity
import info.proteo.cupcake.data.local.entity.protocol.ProtocolStepNextRelation
import info.proteo.cupcake.data.local.entity.protocol.ProtocolStepWithNextSteps
import info.proteo.cupcake.data.local.entity.protocol.ProtocolTagEntity
import info.proteo.cupcake.data.local.entity.protocol.ProtocolViewerCrossRef
import kotlinx.coroutines.flow.Flow

@Dao
interface ProtocolModelDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(protocol: ProtocolModelEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(protocols: List<ProtocolModelEntity>)

    @Update
    suspend fun update(protocol: ProtocolModelEntity)

    @Delete
    suspend fun delete(protocol: ProtocolModelEntity)

    @Query("SELECT * FROM protocol_model WHERE id = :id")
    fun getById(id: Int): Flow<ProtocolModelEntity?>

    @Query("SELECT * FROM protocol_model LIMIT :limit OFFSET :offset")
    fun getAllProtocols(limit: Int, offset: Int): Flow<List<ProtocolModelEntity>>

    @Query("SELECT * FROM protocol_model")
    fun getAllProtocols(): Flow<List<ProtocolModelEntity>>

    @Query("SELECT * FROM protocol_model WHERE enabled = 1")
    fun getEnabledProtocols(): Flow<List<ProtocolModelEntity>>

    @Query("DELETE FROM protocol_model")
    suspend fun deleteAll()

    @Query("SELECT * FROM protocol_model WHERE user = :userId LIMIT :limit OFFSET :offset")
    fun getUserProtocols(userId: Int, limit: Int, offset: Int): Flow<List<ProtocolModelEntity>>

    @Query("SELECT COUNT(*) FROM protocol_model WHERE user = :userId")
    fun countUserProtocols(userId: Int): Flow<Int>

    @Query("DELETE FROM protocol_editor_cross_ref WHERE protocolId = :protocolId")
    suspend fun clearEditorsByProtocol(protocolId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProtocolEditorCrossRef(crossRef: ProtocolEditorCrossRef)

    @Query("DELETE FROM protocol_viewer_cross_ref WHERE protocolId = :protocolId")
    suspend fun clearViewersByProtocol(protocolId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProtocolViewerCrossRef(crossRef: ProtocolViewerCrossRef)

    @Transaction
    @Query("SELECT * FROM protocol_model WHERE id = :protocolId")
    suspend fun getProtocolWithAccess(protocolId: Int): ProtocolModelWithAccess?
}

@Dao
interface ProtocolStepDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(step: ProtocolStepEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(steps: List<ProtocolStepEntity>)

    @Update
    suspend fun update(step: ProtocolStepEntity)

    @Delete
    suspend fun delete(step: ProtocolStepEntity)

    @Query("SELECT * FROM protocol_step WHERE id = :id")
    suspend fun getById(id: Int): ProtocolStepEntity?

    @Query("SELECT * FROM protocol_step WHERE protocol = :protocolId ORDER BY id LIMIT :limit OFFSET :offset")
    fun getStepsByProtocol(protocolId: Int, limit: Int, offset: Int): Flow<List<ProtocolStepEntity>>

    @Query("SELECT COUNT(*) FROM protocol_step WHERE protocol = :protocolId")
    fun countStepsByProtocol(protocolId: Int): Flow<Int>

    @Query("DELETE FROM protocol_step WHERE protocol = :protocolId")
    suspend fun deleteByProtocol(protocolId: Int)

    @Query("DELETE FROM protocol_step")
    suspend fun deleteAll()

    @Transaction
    @Query("SELECT * FROM protocol_step WHERE id = :stepId")
    suspend fun getStepWithNextSteps(stepId: Int): ProtocolStepWithNextSteps?

    @Transaction
    @Query("SELECT * FROM protocol_step WHERE protocol = :protocolId")
    suspend fun getProtocolStepsWithNextSteps(protocolId: Int): List<ProtocolStepWithNextSteps>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStep(step: ProtocolStepEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addNextStepRelation(relation: ProtocolStepNextRelation)

    @Delete
    suspend fun removeNextStepRelation(relation: ProtocolStepNextRelation)

    @Query("DELETE FROM protocol_step_next_relation WHERE from_step = :stepId")
    suspend fun clearNextStepsForStep(stepId: Int)
}

@Dao
interface ProtocolSectionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(section: ProtocolSectionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(sections: List<ProtocolSectionEntity>)

    @Update
    suspend fun update(section: ProtocolSectionEntity)

    @Delete
    suspend fun delete(section: ProtocolSectionEntity)

    @Query("SELECT * FROM protocol_section WHERE id = :id")
    suspend fun getById(id: Int): ProtocolSectionEntity?

    @Query("SELECT * FROM protocol_section WHERE id = :id")
    fun getByIdFlow(id: Int): Flow<ProtocolSectionEntity?>

    @Query("SELECT * FROM protocol_section WHERE protocol = :protocolId LIMIT :limit OFFSET :offset")
    fun getSectionsByProtocol(protocolId: Int, limit: Int, offset: Int): Flow<List<ProtocolSectionEntity>>

    @Query("SELECT * FROM protocol_section WHERE protocol = :protocolId")
    fun getSectionsByProtocol(protocolId: Int): Flow<List<ProtocolSectionEntity>>


    @Query("DELETE FROM protocol_section")
    suspend fun deleteAll()

    @Query("DELETE FROM protocol_section WHERE protocol = :protocolId")
    suspend fun deleteByProtocol(protocolId: Int)
}

@Dao
interface ProtocolRatingDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(rating: ProtocolRatingEntity): Long

    @Update
    suspend fun update(rating: ProtocolRatingEntity)

    @Delete
    suspend fun delete(rating: ProtocolRatingEntity)

    @Query("SELECT * FROM protocol_rating WHERE id = :id")
    suspend fun getById(id: Int): ProtocolRatingEntity?

    @Query("SELECT * FROM protocol_rating WHERE protocol = :protocolId")
    fun getByProtocol(protocolId: Int): Flow<List<ProtocolRatingEntity>>

    @Query("SELECT * FROM protocol_rating WHERE user = :userId")
    fun getByUser(userId: Int): Flow<List<ProtocolRatingEntity>>

    @Query("DELETE FROM protocol_rating")
    suspend fun deleteAll()
}

@Dao
interface ProtocolReagentDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reagent: ProtocolReagentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reagents: List<ProtocolReagentEntity>)

    @Update
    suspend fun update(reagent: ProtocolReagentEntity)

    @Delete
    suspend fun delete(reagent: ProtocolReagentEntity)

    @Query("SELECT * FROM protocol_reagent WHERE id = :id")
    suspend fun getById(id: Int): ProtocolReagentEntity?

    @Query("SELECT * FROM protocol_reagent WHERE protocol = :protocolId")
    fun getByProtocol(protocolId: Int): Flow<List<ProtocolReagentEntity>>

    @Query("DELETE FROM protocol_reagent")
    suspend fun deleteAll()

    @Query("DELETE FROM protocol_reagent WHERE protocol = :i")
    fun deleteByProtocol(i: Int)
}

@Dao
interface ProtocolTagDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(tag: ProtocolTagEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tags: List<ProtocolTagEntity>)

    @Update
    suspend fun update(tag: ProtocolTagEntity)

    @Delete
    suspend fun delete(tag: ProtocolTagEntity)

    @Query("SELECT * FROM protocol_tag WHERE id = :id")
    suspend fun getById(id: Int): ProtocolTagEntity?

    @Query("SELECT * FROM protocol_tag WHERE protocol = :protocolId")
    fun getByProtocol(protocolId: Int): Flow<List<ProtocolTagEntity>>

    @Query("DELETE FROM protocol_tag")
    suspend fun deleteAll()

    @Query("DELETE FROM protocol_tag WHERE protocol = :i")
    fun deleteByProtocol(i: Int)

    @Query("SELECT * FROM protocol_tag WHERE tag = :i")
    fun getByTag(i: Int) : Flow<List<ProtocolTagEntity>?>
}
