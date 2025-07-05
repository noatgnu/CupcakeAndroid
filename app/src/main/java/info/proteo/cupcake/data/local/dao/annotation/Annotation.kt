package info.proteo.cupcake.data.local.dao.annotation

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import info.proteo.cupcake.data.local.entity.annotation.AnnotationEntity
import info.proteo.cupcake.data.local.entity.annotation.AnnotationFolderEntity
import info.proteo.cupcake.data.local.entity.annotation.AnnotationFolderPathEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AnnotationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(annotation: AnnotationEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(annotations: List<AnnotationEntity>)

    @Update
    suspend fun update(annotation: AnnotationEntity)

    @Delete
    suspend fun delete(annotation: AnnotationEntity)

    @Query("SELECT * FROM annotation WHERE id = :id")
    suspend fun getById(id: Int): AnnotationEntity?

    @Query("SELECT * FROM annotation WHERE step = :stepId")
    fun getByStep(stepId: Int): Flow<List<AnnotationEntity>>

    @Query("SELECT * FROM annotation WHERE session = :sessionId")
    fun getBySession(sessionId: Int): Flow<List<AnnotationEntity>>

    @Query("SELECT * FROM annotation WHERE stored_reagent = :reagentId")
    fun getByReagent(reagentId: Int): Flow<List<AnnotationEntity>>

    @Query("DELETE FROM annotation")
    suspend fun deleteAll()

    @Query("SELECT COUNT(*) FROM annotation")
    fun countAll(): Flow<Int>

    @Query("SELECT * FROM annotation LIMIT :limit OFFSET :offset")
    fun getAllPaginated(limit: Int, offset: Int): Flow<List<AnnotationEntity>>

    @Query("SELECT * FROM annotation WHERE folder_id = :folderId ORDER BY created_at DESC LIMIT :limit OFFSET :offset")
    fun getByFolderIdPaginated(folderId: Int, limit: Int, offset: Int): Flow<List<AnnotationEntity>>

    @Query("SELECT COUNT(*) FROM annotation WHERE folder_id = :folderId")
    fun countByFolderId(folderId: Int): Flow<Int>

    @Query("SELECT * FROM annotation ORDER BY created_at DESC")
    fun getAllAnnotations(): Flow<List<AnnotationEntity>>
}

@Dao
interface AnnotationFolderPathDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(path: AnnotationFolderPathEntity): Long

    @Query("SELECT * FROM annotation_folder_path WHERE id = :id")
    suspend fun getById(id: Int): AnnotationFolderPathEntity?

    @Query("DELETE FROM annotation_folder_path")
    suspend fun deleteAll()
}

@Dao
interface AnnotationFolderDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(folder: AnnotationFolderEntity): Long

    @Update
    suspend fun update(folder: AnnotationFolderEntity)

    @Delete
    suspend fun delete(folder: AnnotationFolderEntity)

    @Query("SELECT * FROM annotation_folder WHERE id = :id")
    suspend fun getById(id: Int): AnnotationFolderEntity?

    @Query("SELECT * FROM annotation_folder WHERE parent_folder = :parentId")
    fun getByParent(parentId: Int): Flow<List<AnnotationFolderEntity>>

    @Query("SELECT * FROM annotation_folder WHERE session = :sessionId")
    fun getBySession(sessionId: Int): Flow<List<AnnotationFolderEntity>>

    @Query("SELECT * FROM annotation_folder WHERE instrument = :instrumentId")
    fun getByInstrument(instrumentId: Int): Flow<List<AnnotationFolderEntity>>

    @Query("SELECT * FROM annotation_folder WHERE stored_reagent = :reagentId")
    fun getByReagent(reagentId: Int): Flow<List<AnnotationFolderEntity>>

    @Query("DELETE FROM annotation_folder")
    suspend fun deleteAll()
}
