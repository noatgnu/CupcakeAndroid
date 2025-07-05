package info.proteo.cupcake.data.local.dao.system

import androidx.room.*
import info.proteo.cupcake.data.local.entity.system.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ImportTrackerDao {
    @Query("SELECT * FROM import_tracker ORDER BY created_at DESC")
    fun getAllImportTrackers(): Flow<List<ImportTrackerEntity>>

    @Query("SELECT * FROM import_tracker WHERE id = :id")
    suspend fun getImportTrackerById(id: Int): ImportTrackerEntity?

    @Query("SELECT * FROM import_tracker WHERE import_status = :status")
    fun getImportTrackersByStatus(status: String): Flow<List<ImportTrackerEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImportTracker(importTracker: ImportTrackerEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImportTrackers(importTrackers: List<ImportTrackerEntity>)

    @Update
    suspend fun updateImportTracker(importTracker: ImportTrackerEntity)

    @Delete
    suspend fun deleteImportTracker(importTracker: ImportTrackerEntity)

    @Query("DELETE FROM import_tracker WHERE id = :id")
    suspend fun deleteImportTrackerById(id: Int)

    @Query("DELETE FROM import_tracker")
    suspend fun deleteAllImportTrackers()
}

@Dao
interface ImportedObjectDao {
    @Query("SELECT * FROM imported_object WHERE import_tracker = :importTrackerId")
    fun getImportedObjectsByTracker(importTrackerId: Int): Flow<List<ImportedObjectEntity>>

    @Query("SELECT * FROM imported_object WHERE id = :id")
    suspend fun getImportedObjectById(id: Int): ImportedObjectEntity?

    @Query("SELECT * FROM imported_object WHERE object_type = :objectType AND object_id = :objectId")
    suspend fun getImportedObject(objectType: String, objectId: Int): ImportedObjectEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImportedObject(importedObject: ImportedObjectEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImportedObjects(importedObjects: List<ImportedObjectEntity>)

    @Delete
    suspend fun deleteImportedObject(importedObject: ImportedObjectEntity)

    @Query("DELETE FROM imported_object WHERE import_tracker = :importTrackerId")
    suspend fun deleteImportedObjectsByTracker(importTrackerId: Int)
}

@Dao
interface ImportedFileDao {
    @Query("SELECT * FROM imported_file WHERE import_tracker = :importTrackerId")
    fun getImportedFilesByTracker(importTrackerId: Int): Flow<List<ImportedFileEntity>>

    @Query("SELECT * FROM imported_file WHERE id = :id")
    suspend fun getImportedFileById(id: Int): ImportedFileEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImportedFile(importedFile: ImportedFileEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImportedFiles(importedFiles: List<ImportedFileEntity>)

    @Delete
    suspend fun deleteImportedFile(importedFile: ImportedFileEntity)

    @Query("DELETE FROM imported_file WHERE import_tracker = :importTrackerId")
    suspend fun deleteImportedFilesByTracker(importTrackerId: Int)
}

@Dao
interface ImportedRelationshipDao {
    @Query("SELECT * FROM imported_relationship WHERE import_tracker = :importTrackerId")
    fun getImportedRelationshipsByTracker(importTrackerId: Int): Flow<List<ImportedRelationshipEntity>>

    @Query("SELECT * FROM imported_relationship WHERE id = :id")
    suspend fun getImportedRelationshipById(id: Int): ImportedRelationshipEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImportedRelationship(importedRelationship: ImportedRelationshipEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertImportedRelationships(importedRelationships: List<ImportedRelationshipEntity>)

    @Delete
    suspend fun deleteImportedRelationship(importedRelationship: ImportedRelationshipEntity)

    @Query("DELETE FROM imported_relationship WHERE import_tracker = :importTrackerId")
    suspend fun deleteImportedRelationshipsByTracker(importTrackerId: Int)
}