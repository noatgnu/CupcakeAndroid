package info.proteo.cupcake.data.remote.service

import android.util.Log
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import info.proteo.cupcake.data.local.dao.system.ImportTrackerDao
import info.proteo.cupcake.data.local.dao.system.ImportedObjectDao
import info.proteo.cupcake.data.local.dao.system.ImportedFileDao
import info.proteo.cupcake.data.local.dao.system.ImportedRelationshipDao
import info.proteo.cupcake.data.local.entity.system.ImportTrackerEntity
import info.proteo.cupcake.data.local.entity.system.ImportedObjectEntity
import info.proteo.cupcake.data.local.entity.system.ImportedFileEntity
import info.proteo.cupcake.data.local.entity.system.ImportedRelationshipEntity
import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import info.proteo.cupcake.shared.data.model.system.ImportTracker
import info.proteo.cupcake.shared.data.model.system.ImportedObject
import info.proteo.cupcake.shared.data.model.system.ImportedFile
import info.proteo.cupcake.shared.data.model.system.ImportedRelationship
import info.proteo.cupcake.shared.data.model.user.UserBasic
import info.proteo.cupcake.shared.data.model.user.LabGroup
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

@JsonClass(generateAdapter = true)
data class ImportTrackerRequest(
    @Json(name = "import_type") val importType: String?,
    @Json(name = "import_name") val importName: String?,
    @Json(name = "import_description") val importDescription: String?,
    @Json(name = "lab_group") val labGroup: Int?
)

@JsonClass(generateAdapter = true)
data class ImportedObjectRequest(
    @Json(name = "import_tracker") val importTracker: Int,
    @Json(name = "object_type") val objectType: String,
    @Json(name = "object_id") val objectId: Int,
    @Json(name = "action_type") val actionType: String,
    @Json(name = "object_data") val objectData: String?
)

@JsonClass(generateAdapter = true)
data class ImportedFileRequest(
    @Json(name = "import_tracker") val importTracker: Int,
    @Json(name = "file_path") val filePath: String,
    @Json(name = "original_filename") val originalFilename: String?,
    @Json(name = "file_size_bytes") val fileSizeBytes: Int?,
    @Json(name = "file_hash") val fileHash: String?
)

@JsonClass(generateAdapter = true)
data class ImportedRelationshipRequest(
    @Json(name = "import_tracker") val importTracker: Int,
    @Json(name = "relationship_type") val relationshipType: String,
    @Json(name = "parent_model") val parentModel: String,
    @Json(name = "parent_id") val parentId: Int,
    @Json(name = "child_model") val childModel: String,
    @Json(name = "child_id") val childId: Int
)

interface ImportTrackerApiService {
    @GET("api/import_tracker/")
    suspend fun getImportTrackers(
        @Query("import_type") importType: String? = null,
        @Query("import_status") importStatus: String? = null,
        @Query("created_by") createdBy: Int? = null,
        @Query("lab_group") labGroup: Int? = null,
        @Query("search") search: String? = null,
        @Query("ordering") ordering: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): LimitOffsetResponse<ImportTracker>

    @GET("api/import_tracker/{id}/")
    suspend fun getImportTracker(@Path("id") id: Int): ImportTracker

    @POST("api/import_tracker/")
    suspend fun createImportTracker(@Body tracker: ImportTrackerRequest): ImportTracker

    @PUT("api/import_tracker/{id}/")
    suspend fun updateImportTracker(@Path("id") id: Int, @Body tracker: ImportTrackerRequest): ImportTracker

    @DELETE("api/import_tracker/{id}/")
    suspend fun deleteImportTracker(@Path("id") id: Int)

    @POST("api/import_tracker/{id}/rollback/")
    suspend fun rollbackImport(@Path("id") id: Int): ImportTracker
}

interface ImportedObjectApiService {
    @GET("api/imported_object/")
    suspend fun getImportedObjects(
        @Query("import_tracker") importTracker: Int? = null,
        @Query("object_type") objectType: String? = null,
        @Query("action_type") actionType: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): LimitOffsetResponse<ImportedObject>

    @GET("api/imported_object/{id}/")
    suspend fun getImportedObject(@Path("id") id: Int): ImportedObject

    @POST("api/imported_object/")
    suspend fun createImportedObject(@Body obj: ImportedObjectRequest): ImportedObject

    @DELETE("api/imported_object/{id}/")
    suspend fun deleteImportedObject(@Path("id") id: Int)
}

interface ImportedFileApiService {
    @GET("api/imported_file/")
    suspend fun getImportedFiles(
        @Query("import_tracker") importTracker: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): LimitOffsetResponse<ImportedFile>

    @GET("api/imported_file/{id}/")
    suspend fun getImportedFile(@Path("id") id: Int): ImportedFile

    @POST("api/imported_file/")
    suspend fun createImportedFile(@Body file: ImportedFileRequest): ImportedFile

    @DELETE("api/imported_file/{id}/")
    suspend fun deleteImportedFile(@Path("id") id: Int)
}

interface ImportedRelationshipApiService {
    @GET("api/imported_relationship/")
    suspend fun getImportedRelationships(
        @Query("import_tracker") importTracker: Int? = null,
        @Query("relationship_type") relationshipType: String? = null,
        @Query("parent_model") parentModel: String? = null,
        @Query("child_model") childModel: String? = null,
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null
    ): LimitOffsetResponse<ImportedRelationship>

    @GET("api/imported_relationship/{id}/")
    suspend fun getImportedRelationship(@Path("id") id: Int): ImportedRelationship

    @POST("api/imported_relationship/")
    suspend fun createImportedRelationship(@Body relationship: ImportedRelationshipRequest): ImportedRelationship

    @DELETE("api/imported_relationship/{id}/")
    suspend fun deleteImportedRelationship(@Path("id") id: Int)
}

interface ImportTrackerService {
    suspend fun getImportTrackers(importType: String? = null, importStatus: String? = null, createdBy: Int? = null, labGroup: Int? = null, search: String? = null, ordering: String? = null, limit: Int? = null, offset: Int? = null): Result<LimitOffsetResponse<ImportTracker>>
    suspend fun getImportTracker(id: Int): Result<ImportTracker>
    suspend fun createImportTracker(importType: String?, importName: String?, importDescription: String?, labGroup: Int?): Result<ImportTracker>
    suspend fun updateImportTracker(id: Int, importType: String?, importName: String?, importDescription: String?, labGroup: Int?): Result<ImportTracker>
    suspend fun deleteImportTracker(id: Int): Result<Unit>
    suspend fun rollbackImport(id: Int): Result<ImportTracker>
}

interface ImportedObjectService {
    suspend fun getImportedObjects(importTracker: Int? = null, objectType: String? = null, actionType: String? = null, limit: Int? = null, offset: Int? = null): Result<LimitOffsetResponse<ImportedObject>>
    suspend fun getImportedObject(id: Int): Result<ImportedObject>
    suspend fun createImportedObject(importTracker: Int, objectType: String, objectId: Int, actionType: String, objectData: String?): Result<ImportedObject>
    suspend fun deleteImportedObject(id: Int): Result<Unit>
}

interface ImportedFileService {
    suspend fun getImportedFiles(importTracker: Int? = null, limit: Int? = null, offset: Int? = null): Result<LimitOffsetResponse<ImportedFile>>
    suspend fun getImportedFile(id: Int): Result<ImportedFile>
    suspend fun createImportedFile(importTracker: Int, filePath: String, originalFilename: String?, fileSizeBytes: Int?, fileHash: String?): Result<ImportedFile>
    suspend fun deleteImportedFile(id: Int): Result<Unit>
}

interface ImportedRelationshipService {
    suspend fun getImportedRelationships(importTracker: Int? = null, relationshipType: String? = null, parentModel: String? = null, childModel: String? = null, limit: Int? = null, offset: Int? = null): Result<LimitOffsetResponse<ImportedRelationship>>
    suspend fun getImportedRelationship(id: Int): Result<ImportedRelationship>
    suspend fun createImportedRelationship(importTracker: Int, relationshipType: String, parentModel: String, parentId: Int, childModel: String, childId: Int): Result<ImportedRelationship>
    suspend fun deleteImportedRelationship(id: Int): Result<Unit>
}

@Singleton
class ImportTrackerServiceImpl @Inject constructor(
    private val apiService: ImportTrackerApiService,
    private val importTrackerDao: ImportTrackerDao
) : ImportTrackerService {

    override suspend fun getImportTrackers(importType: String?, importStatus: String?, createdBy: Int?, labGroup: Int?, search: String?, ordering: String?, limit: Int?, offset: Int?): Result<LimitOffsetResponse<ImportTracker>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getImportTrackers(importType, importStatus, createdBy, labGroup, search, ordering, limit, offset)
                Log.d("ImportTrackerService", "Fetched ${response.results.size} import trackers from API")
                response.results.forEach { tracker ->
                    cacheImportTracker(tracker)
                }
                Result.success(response)
            } catch (e: Exception) {
                try {
                    val cachedTrackers = importTrackerDao.getAll().first()
                    val trackers = cachedTrackers.map { loadImportTracker(it) }
                    val limitOffsetResponse = LimitOffsetResponse(
                        count = trackers.size,
                        next = null,
                        previous = null,
                        results = trackers
                    )
                    Result.success(limitOffsetResponse)
                } catch (cacheException: Exception) {
                    Result.failure(e)
                }
            }
        }
    }

    override suspend fun getImportTracker(id: Int): Result<ImportTracker> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getImportTracker(id)
                cacheImportTracker(response)
                Result.success(response)
            } catch (e: Exception) {
                try {
                    val cachedTracker = importTrackerDao.getById(id)
                    if (cachedTracker != null) {
                        Result.success(loadImportTracker(cachedTracker))
                    } else {
                        Result.failure(e)
                    }
                } catch (cacheException: Exception) {
                    Result.failure(e)
                }
            }
        }
    }

    override suspend fun createImportTracker(importType: String?, importName: String?, importDescription: String?, labGroup: Int?): Result<ImportTracker> {
        return withContext(Dispatchers.IO) {
            try {
                val request = ImportTrackerRequest(importType, importName, importDescription, labGroup)
                val response = apiService.createImportTracker(request)
                cacheImportTracker(response)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateImportTracker(id: Int, importType: String?, importName: String?, importDescription: String?, labGroup: Int?): Result<ImportTracker> {
        return withContext(Dispatchers.IO) {
            try {
                val request = ImportTrackerRequest(importType, importName, importDescription, labGroup)
                val response = apiService.updateImportTracker(id, request)
                cacheImportTracker(response)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteImportTracker(id: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.deleteImportTracker(id)
                importTrackerDao.getById(id)?.let { importTrackerDao.delete(it) }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun rollbackImport(id: Int): Result<ImportTracker> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.rollbackImport(id)
                cacheImportTracker(response)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun cacheImportTracker(tracker: ImportTracker) {
        val entity = ImportTrackerEntity(
            id = tracker.id,
            importType = tracker.importType,
            importStatus = tracker.importStatus,
            createdAt = tracker.createdAt,
            updatedAt = tracker.updatedAt,
            createdBy = tracker.createdBy?.id,
            importName = tracker.importName,
            importDescription = tracker.importDescription,
            totalObjects = tracker.totalObjects,
            processedObjects = tracker.processedObjects,
            createdObjects = tracker.createdObjects,
            updatedObjects = tracker.updatedObjects,
            failedObjects = tracker.failedObjects,
            errorLog = tracker.errorLog,
            importMetadata = tracker.importMetadata,
            fileSizeBytes = tracker.fileSizeBytes,
            labGroup = tracker.labGroup?.id
        )
        importTrackerDao.insert(entity)
    }

    private fun loadImportTracker(entity: ImportTrackerEntity): ImportTracker {
        return ImportTracker(
            id = entity.id,
            importType = entity.importType,
            importStatus = entity.importStatus,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            createdBy = entity.createdBy?.let { UserBasic(it, "", null, null) },
            importName = entity.importName,
            importDescription = entity.importDescription,
            totalObjects = entity.totalObjects,
            processedObjects = entity.processedObjects,
            createdObjects = entity.createdObjects,
            updatedObjects = entity.updatedObjects,
            failedObjects = entity.failedObjects,
            errorLog = entity.errorLog,
            importMetadata = entity.importMetadata,
            fileSizeBytes = entity.fileSizeBytes,
            labGroup = entity.labGroup?.let { LabGroup(it, "", null, null, null, null, false, null) },
            importedObjects = null,
            importedFiles = null,
            importedRelationships = null
        )
    }
}

@Singleton
class ImportedObjectServiceImpl @Inject constructor(
    private val apiService: ImportedObjectApiService,
    private val importedObjectDao: ImportedObjectDao
) : ImportedObjectService {

    override suspend fun getImportedObjects(importTracker: Int?, objectType: String?, actionType: String?, limit: Int?, offset: Int?): Result<LimitOffsetResponse<ImportedObject>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getImportedObjects(importTracker, objectType, actionType, limit, offset)
                response.results.forEach { obj ->
                    cacheImportedObject(obj)
                }
                Result.success(response)
            } catch (e: Exception) {
                try {
                    val cachedObjects = if (importTracker != null) {
                        importedObjectDao.getByImportTracker(importTracker).first()
                    } else {
                        importedObjectDao.getAll().first()
                    }
                    val objects = cachedObjects.map { loadImportedObject(it) }
                    val limitOffsetResponse = LimitOffsetResponse(
                        count = objects.size,
                        next = null,
                        previous = null,
                        results = objects
                    )
                    Result.success(limitOffsetResponse)
                } catch (cacheException: Exception) {
                    Result.failure(e)
                }
            }
        }
    }

    override suspend fun getImportedObject(id: Int): Result<ImportedObject> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getImportedObject(id)
                cacheImportedObject(response)
                Result.success(response)
            } catch (e: Exception) {
                try {
                    val cachedObject = importedObjectDao.getById(id)
                    if (cachedObject != null) {
                        Result.success(loadImportedObject(cachedObject))
                    } else {
                        Result.failure(e)
                    }
                } catch (cacheException: Exception) {
                    Result.failure(e)
                }
            }
        }
    }

    override suspend fun createImportedObject(importTracker: Int, objectType: String, objectId: Int, actionType: String, objectData: String?): Result<ImportedObject> {
        return withContext(Dispatchers.IO) {
            try {
                val request = ImportedObjectRequest(importTracker, objectType, objectId, actionType, objectData)
                val response = apiService.createImportedObject(request)
                cacheImportedObject(response)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteImportedObject(id: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.deleteImportedObject(id)
                importedObjectDao.getById(id)?.let { importedObjectDao.delete(it) }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun cacheImportedObject(obj: ImportedObject) {
        val entity = ImportedObjectEntity(
            id = obj.id,
            importTracker = obj.importTracker,
            objectType = obj.objectType,
            objectId = obj.objectId,
            actionType = obj.actionType,
            createdAt = obj.createdAt,
            objectData = obj.objectData
        )
        importedObjectDao.insert(entity)
    }

    private fun loadImportedObject(entity: ImportedObjectEntity): ImportedObject {
        return ImportedObject(
            id = entity.id,
            importTracker = entity.importTracker,
            objectType = entity.objectType,
            objectId = entity.objectId,
            actionType = entity.actionType,
            createdAt = entity.createdAt,
            objectData = entity.objectData
        )
    }
}

@Singleton
class ImportedFileServiceImpl @Inject constructor(
    private val apiService: ImportedFileApiService,
    private val importedFileDao: ImportedFileDao
) : ImportedFileService {

    override suspend fun getImportedFiles(importTracker: Int?, limit: Int?, offset: Int?): Result<LimitOffsetResponse<ImportedFile>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getImportedFiles(importTracker, limit, offset)
                response.results.forEach { file ->
                    cacheImportedFile(file)
                }
                Result.success(response)
            } catch (e: Exception) {
                try {
                    val cachedFiles = if (importTracker != null) {
                        importedFileDao.getByImportTracker(importTracker).first()
                    } else {
                        importedFileDao.getAll().first()
                    }
                    val files = cachedFiles.map { loadImportedFile(it) }
                    val limitOffsetResponse = LimitOffsetResponse(
                        count = files.size,
                        next = null,
                        previous = null,
                        results = files
                    )
                    Result.success(limitOffsetResponse)
                } catch (cacheException: Exception) {
                    Result.failure(e)
                }
            }
        }
    }

    override suspend fun getImportedFile(id: Int): Result<ImportedFile> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getImportedFile(id)
                cacheImportedFile(response)
                Result.success(response)
            } catch (e: Exception) {
                try {
                    val cachedFile = importedFileDao.getById(id)
                    if (cachedFile != null) {
                        Result.success(loadImportedFile(cachedFile))
                    } else {
                        Result.failure(e)
                    }
                } catch (cacheException: Exception) {
                    Result.failure(e)
                }
            }
        }
    }

    override suspend fun createImportedFile(importTracker: Int, filePath: String, originalFilename: String?, fileSizeBytes: Int?, fileHash: String?): Result<ImportedFile> {
        return withContext(Dispatchers.IO) {
            try {
                val request = ImportedFileRequest(importTracker, filePath, originalFilename, fileSizeBytes, fileHash)
                val response = apiService.createImportedFile(request)
                cacheImportedFile(response)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteImportedFile(id: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.deleteImportedFile(id)
                importedFileDao.getById(id)?.let { importedFileDao.delete(it) }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun cacheImportedFile(file: ImportedFile) {
        val entity = ImportedFileEntity(
            id = file.id,
            importTracker = file.importTracker,
            filePath = file.filePath,
            originalFilename = file.originalFilename,
            fileSizeBytes = file.fileSizeBytes,
            fileHash = file.fileHash,
            createdAt = file.createdAt
        )
        importedFileDao.insert(entity)
    }

    private fun loadImportedFile(entity: ImportedFileEntity): ImportedFile {
        return ImportedFile(
            id = entity.id,
            importTracker = entity.importTracker,
            filePath = entity.filePath,
            originalFilename = entity.originalFilename,
            fileSizeBytes = entity.fileSizeBytes,
            fileHash = entity.fileHash,
            createdAt = entity.createdAt
        )
    }
}

@Singleton
class ImportedRelationshipServiceImpl @Inject constructor(
    private val apiService: ImportedRelationshipApiService,
    private val importedRelationshipDao: ImportedRelationshipDao
) : ImportedRelationshipService {

    override suspend fun getImportedRelationships(importTracker: Int?, relationshipType: String?, parentModel: String?, childModel: String?, limit: Int?, offset: Int?): Result<LimitOffsetResponse<ImportedRelationship>> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getImportedRelationships(importTracker, relationshipType, parentModel, childModel, limit, offset)
                response.results.forEach { relationship ->
                    cacheImportedRelationship(relationship)
                }
                Result.success(response)
            } catch (e: Exception) {
                try {
                    val cachedRelationships = if (importTracker != null) {
                        importedRelationshipDao.getByImportTracker(importTracker).first()
                    } else {
                        importedRelationshipDao.getAll().first()
                    }
                    val relationships = cachedRelationships.map { loadImportedRelationship(it) }
                    val limitOffsetResponse = LimitOffsetResponse(
                        count = relationships.size,
                        next = null,
                        previous = null,
                        results = relationships
                    )
                    Result.success(limitOffsetResponse)
                } catch (cacheException: Exception) {
                    Result.failure(e)
                }
            }
        }
    }

    override suspend fun getImportedRelationship(id: Int): Result<ImportedRelationship> {
        return withContext(Dispatchers.IO) {
            try {
                val response = apiService.getImportedRelationship(id)
                cacheImportedRelationship(response)
                Result.success(response)
            } catch (e: Exception) {
                try {
                    val cachedRelationship = importedRelationshipDao.getById(id)
                    if (cachedRelationship != null) {
                        Result.success(loadImportedRelationship(cachedRelationship))
                    } else {
                        Result.failure(e)
                    }
                } catch (cacheException: Exception) {
                    Result.failure(e)
                }
            }
        }
    }

    override suspend fun createImportedRelationship(importTracker: Int, relationshipType: String, parentModel: String, parentId: Int, childModel: String, childId: Int): Result<ImportedRelationship> {
        return withContext(Dispatchers.IO) {
            try {
                val request = ImportedRelationshipRequest(importTracker, relationshipType, parentModel, parentId, childModel, childId)
                val response = apiService.createImportedRelationship(request)
                cacheImportedRelationship(response)
                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun deleteImportedRelationship(id: Int): Result<Unit> {
        return withContext(Dispatchers.IO) {
            try {
                apiService.deleteImportedRelationship(id)
                importedRelationshipDao.getById(id)?.let { importedRelationshipDao.delete(it) }
                Result.success(Unit)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun cacheImportedRelationship(relationship: ImportedRelationship) {
        val entity = ImportedRelationshipEntity(
            id = relationship.id,
            importTracker = relationship.importTracker,
            relationshipType = relationship.relationshipType,
            parentModel = relationship.parentModel,
            parentId = relationship.parentId,
            childModel = relationship.childModel,
            childId = relationship.childId,
            createdAt = relationship.createdAt
        )
        importedRelationshipDao.insert(entity)
    }

    private fun loadImportedRelationship(entity: ImportedRelationshipEntity): ImportedRelationship {
        return ImportedRelationship(
            id = entity.id,
            importTracker = entity.importTracker,
            relationshipType = entity.relationshipType,
            parentModel = entity.parentModel,
            parentId = entity.parentId,
            childModel = entity.childModel,
            childId = entity.childId,
            createdAt = entity.createdAt
        )
    }
}