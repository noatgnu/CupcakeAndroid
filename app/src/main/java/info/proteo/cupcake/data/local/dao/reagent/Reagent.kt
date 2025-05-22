package info.proteo.cupcake.data.local.dao.reagent

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import info.proteo.cupcake.data.local.entity.reagent.ReagentActionEntity
import info.proteo.cupcake.data.local.entity.reagent.ReagentEntity
import info.proteo.cupcake.data.local.entity.reagent.ReagentSubscriptionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReagentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(reagent: ReagentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(reagents: List<ReagentEntity>)

    @Update
    suspend fun update(reagent: ReagentEntity)

    @Delete
    suspend fun delete(reagent: ReagentEntity)

    @Query("SELECT * FROM reagent WHERE id = :id")
    suspend fun getById(id: Int): ReagentEntity?

    @Query("SELECT * FROM reagent")
    fun getAllReagents(): Flow<List<ReagentEntity>>

    @Query("SELECT * FROM reagent WHERE name LIKE :searchQuery")
    fun searchReagents(searchQuery: String): Flow<List<ReagentEntity>>

    @Query("DELETE FROM reagent")
    suspend fun deleteAll()
}

@Dao
interface ReagentSubscriptionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(subscription: ReagentSubscriptionEntity): Long

    @Update
    suspend fun update(subscription: ReagentSubscriptionEntity)

    @Delete
    suspend fun delete(subscription: ReagentSubscriptionEntity)

    @Query("SELECT * FROM reagent_subscription WHERE id = :id")
    suspend fun getById(id: Int): ReagentSubscriptionEntity?

    @Query("SELECT * FROM reagent_subscription WHERE user_id = :userId")
    fun getByUser(userId: Int): Flow<List<ReagentSubscriptionEntity>>

    @Query("SELECT * FROM reagent_subscription WHERE stored_reagent = :reagentId")
    fun getByReagent(reagentId: Int): Flow<List<ReagentSubscriptionEntity>>

    @Query("SELECT * FROM reagent_subscription WHERE user_id = :userId AND stored_reagent = :reagentId")
    suspend fun getUserSubscriptionForReagent(userId: Int, reagentId: Int): ReagentSubscriptionEntity?

    @Query("DELETE FROM reagent_subscription")
    suspend fun deleteAll()
}

@Dao
interface ReagentActionDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(action: ReagentActionEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(actions: List<ReagentActionEntity>)

    @Update
    suspend fun update(action: ReagentActionEntity)

    @Delete
    suspend fun delete(action: ReagentActionEntity)

    @Query("SELECT * FROM reagent_action WHERE id = :id")
    suspend fun getById(id: Int): ReagentActionEntity?

    @Query("SELECT * FROM reagent_action WHERE reagent = :reagentId")
    fun getByReagent(reagentId: Int): Flow<List<ReagentActionEntity>>

    @Query("SELECT * FROM reagent_action WHERE user = :username")
    fun getByUser(username: String): Flow<List<ReagentActionEntity>>

    @Query("DELETE FROM reagent_action")
    suspend fun deleteAll()
}

