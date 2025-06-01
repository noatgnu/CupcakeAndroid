package info.proteo.cupcake.data.local.dao.protocol

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import info.proteo.cupcake.data.local.entity.protocol.ProtocolModelEntity
import info.proteo.cupcake.data.local.entity.protocol.ProtocolRatingEntity
import info.proteo.cupcake.data.local.entity.protocol.ProtocolReagentEntity
import info.proteo.cupcake.data.local.entity.protocol.ProtocolSectionEntity
import info.proteo.cupcake.data.local.entity.protocol.ProtocolStepEntity
import info.proteo.cupcake.data.local.entity.protocol.ProtocolTagEntity
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
    suspend fun getById(id: Int): ProtocolModelEntity?

    @Query("SELECT * FROM protocol_model")
    fun getAllProtocols(): Flow<List<ProtocolModelEntity>>

    @Query("SELECT * FROM protocol_model WHERE enabled = 1")
    fun getEnabledProtocols(): Flow<List<ProtocolModelEntity>>

    @Query("DELETE FROM protocol_model")
    suspend fun deleteAll()
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

    @Query("SELECT * FROM protocol_section WHERE protocol = :protocolId")
    fun getSectionsByProtocol(protocolId: Int): Flow<List<ProtocolSectionEntity>>

    @Query("DELETE FROM protocol_section")
    suspend fun deleteAll()
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
}
