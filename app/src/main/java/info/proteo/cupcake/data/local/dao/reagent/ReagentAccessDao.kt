package info.proteo.cupcake.data.local.dao.reagent

import androidx.room.*
import info.proteo.cupcake.data.local.entity.reagent.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ReagentAccessDao {
    // Stored Reagent Access User operations
    @Query("SELECT userId FROM stored_reagent_access_user WHERE storedReagentId = :storedReagentId")
    fun getUsersForStoredReagent(storedReagentId: Int): Flow<List<Int>>

    @Query("SELECT storedReagentId FROM stored_reagent_access_user WHERE userId = :userId")
    fun getStoredReagentsForUser(userId: Int): Flow<List<Int>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStoredReagentAccessUser(crossRef: StoredReagentAccessUserCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStoredReagentAccessUsers(crossRefs: List<StoredReagentAccessUserCrossRef>)

    @Delete
    suspend fun deleteStoredReagentAccessUser(crossRef: StoredReagentAccessUserCrossRef)

    @Query("DELETE FROM stored_reagent_access_user WHERE storedReagentId = :storedReagentId")
    suspend fun deleteAllUsersForStoredReagent(storedReagentId: Int)

    // Stored Reagent Access Lab Group operations
    @Query("SELECT labGroupId FROM stored_reagent_access_lab_group WHERE storedReagentId = :storedReagentId")
    fun getLabGroupsForStoredReagent(storedReagentId: Int): Flow<List<Int>>

    @Query("SELECT storedReagentId FROM stored_reagent_access_lab_group WHERE labGroupId = :labGroupId")
    fun getStoredReagentsForLabGroup(labGroupId: Int): Flow<List<Int>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStoredReagentAccessLabGroup(crossRef: StoredReagentAccessLabGroupCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertStoredReagentAccessLabGroups(crossRefs: List<StoredReagentAccessLabGroupCrossRef>)

    @Delete
    suspend fun deleteStoredReagentAccessLabGroup(crossRef: StoredReagentAccessLabGroupCrossRef)

    @Query("DELETE FROM stored_reagent_access_lab_group WHERE storedReagentId = :storedReagentId")
    suspend fun deleteAllLabGroupsForStoredReagent(storedReagentId: Int)

    @Query("DELETE FROM stored_reagent_access_lab_group WHERE labGroupId = :labGroupId")
    suspend fun deleteAllStoredReagentsForLabGroup(labGroupId: Int)
}