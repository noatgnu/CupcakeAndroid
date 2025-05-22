package info.proteo.cupcake.data.local.dao.project

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import info.proteo.cupcake.data.local.entity.project.ProjectEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ProjectDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(project: ProjectEntity): Long

    @Update
    suspend fun update(project: ProjectEntity)

    @Delete
    suspend fun delete(project: ProjectEntity)

    @Query("SELECT * FROM project WHERE id = :id")
    suspend fun getById(id: Int): ProjectEntity?

    @Query("SELECT * FROM project WHERE owner = :ownerUsername")
    fun getByOwner(ownerUsername: String): Flow<List<ProjectEntity>>

    @Query("DELETE FROM project")
    suspend fun deleteAll()
}