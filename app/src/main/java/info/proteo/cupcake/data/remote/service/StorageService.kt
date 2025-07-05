package info.proteo.cupcake.data.remote.service

import info.proteo.cupcake.data.local.dao.storage.StorageObjectDao
import info.proteo.cupcake.data.local.entity.storage.StorageObjectEntity
import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import info.proteo.cupcake.shared.data.model.storage.StorageObject
import info.proteo.cupcake.shared.data.model.storage.StorageObjectBasic
import info.proteo.cupcake.shared.data.model.storage.StoragePathItem
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.flow.first

interface StorageObjectApiService {
    @GET("api/storage_object/")
    suspend fun getStorageObjects(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @Query("exclude_objects") excludeObjects: String? = null,
        @Query("lab_group") labGroupId: Int? = null
    ): LimitOffsetResponse<StorageObject>


    @GET("api/storage_object/")
    suspend fun getChildStorageObjects(
        @Query("stored_at") parentId: Int,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @Query("exclude_objects") excludeObjects: String? = null,
        @Query("lab_group") labGroupId: Int? = null
    ): LimitOffsetResponse<StorageObject>

    @GET("api/storage_object/")
    suspend fun getRootStorageObjects(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @Query("root") root: String = "true",
        @Query("exclude_objects") excludeObjects: String? = null,
        @Query("lab_group") labGroupId: Int? = null
    ): LimitOffsetResponse<StorageObject>


    @GET("api/storage_object/{id}/")
    suspend fun getStorageObjectById(
        @Path("id") id: Int
    ): StorageObject

    @GET("api/storage_object/")
    suspend fun searchStorageObjects(
        @Query("search") search: String,
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @Query("exclude_objects") excludeObjects: String? = null,
        @Query("lab_group") labGroupId: Int? = null
    ): LimitOffsetResponse<StorageObject>

    @POST("api/storage_object/")
    suspend fun createStorageObject(
        @Body storageObject: StorageObject
    ): StorageObject

    @PUT("api/storage_object/{id}/")
    suspend fun updateStorageObject(
        @Path("id") id: Int,
        @Body storageObject: StorageObject
    ): StorageObject

    @DELETE("api/storage_object/{id}/")
    suspend fun deleteStorageObject(
        @Path("id") id: Int
    )

    @POST("api/storage_object/{id}/add_access_group/")
    suspend fun addAccessGroup(
        @Path("id") id: Int,
        @Body body: Map<String, Int>
    ): StorageObject

    @POST("api/storage_object/{id}/remove_access_group/")
    suspend fun removeAccessGroup(
        @Path("id") id: Int,
        @Body body: Map<String, Int>
    ): StorageObject

    @GET("api/storage_object/{id}/get_path_to_root/")
    suspend fun getPathToRoot(@Path("id") id: Int): List<StoragePathItem>
}

interface StorageObjectService {
    suspend fun getStorageObjects(offset: Int = 0, limit: Int = 20, excludeObjects: List<Int>? = null, labGroupId: Int? = null): Result<LimitOffsetResponse<StorageObject>>
    suspend fun getStorageObjectById(id: Int): Result<StorageObject>
    suspend fun searchStorageObjects(query: String, offset: Int = 0, limit: Int = 20, excludeObjects: List<Int>? = null, labGroupId: Int? = null): Result<LimitOffsetResponse<StorageObject>>
    suspend fun createStorageObject(storageObject: StorageObject): Result<StorageObject>
    suspend fun updateStorageObject(id: Int, storageObject: StorageObject): Result<StorageObject>
    suspend fun deleteStorageObject(id: Int): Result<Unit>
    suspend fun getChildStorageObjects(parentId: Int?, offset: Int = 0, limit: Int = 20, excludeObjects: List<Int>? = null, labGroupId: Int? = null): Result<LimitOffsetResponse<StorageObject>>
    suspend fun addAccessGroup(storageObjectId: Int, labGroupId: Int): Result<StorageObject>
    suspend fun removeAccessGroup(storageObjectId: Int, labGroupId: Int): Result<StorageObject>
    suspend fun getPathToRoot(id: Int): Result<List<StoragePathItem>>
    suspend fun getRootStorageObjects(offset: Int = 0, limit: Int = 20, excludeObjects: List<Int>? = null, labGroupId: Int? = null): Result<LimitOffsetResponse<StorageObject>>
}

@Singleton
class StorageObjectServiceImpl @Inject constructor(
    private val apiService: StorageObjectApiService,
    private val storageObjectDao: StorageObjectDao
) : StorageObjectService {

    private fun List<Int>?.toExcludeString(): String? {
        return this?.joinToString(",").takeIf { !it.isNullOrBlank() }
    }

    override suspend fun getStorageObjects(offset: Int, limit: Int, excludeObjects: List<Int>?, labGroupId: Int?): Result<LimitOffsetResponse<StorageObject>> {
        return try {
            val response = apiService.getStorageObjects(offset, limit, excludeObjects.toExcludeString(), labGroupId)
            // Cache all results
            response.results.forEach { storageObjectDao.insert(it.toEntity()) }
            Result.success(response)
        } catch (e: Exception) {
            // Try to fetch from cache on failure
            try {
                if (excludeObjects.isNullOrEmpty()) {
                    val count = storageObjectDao.countRootObjects()
                    val cachedObjects = storageObjectDao.getRootObjectsPaginated(limit, offset).first()
                    val domainObjects = cachedObjects.map { it.toDomain() }
                    Result.success(LimitOffsetResponse(count, null, null, domainObjects))
                } else {
                    val count = storageObjectDao.countRootObjects()
                    val cachedObjects = storageObjectDao.getRootObjectsExcludingIds(excludeObjects, limit, offset).first()
                    val domainObjects = cachedObjects.map { it.toDomain() }
                    Result.success(LimitOffsetResponse(count, null, null, domainObjects))
                }
            } catch (cacheException: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getStorageObjectById(id: Int): Result<StorageObject> {
        return try {
            val response = apiService.getStorageObjectById(id)
            storageObjectDao.insert(response.toEntity())
            Result.success(response)
        } catch (e: Exception) {
            val cachedObject = storageObjectDao.getById(id)
            if (cachedObject != null) {
                Result.success(cachedObject.toDomain())
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun searchStorageObjects(query: String, offset: Int, limit: Int, excludeObjects: List<Int>?, labGroupId: Int?): Result<LimitOffsetResponse<StorageObject>> {
        return try {
            val response = apiService.searchStorageObjects(query, offset, limit, excludeObjects.toExcludeString(), labGroupId)

            response.results.forEach { storageObjectDao.insert(it.toEntity()) }
            Result.success(response)
        } catch (e: Exception) {
            try {
                val searchQuery = "%$query%"
                val count = storageObjectDao.countSearchResults(searchQuery)

                val cachedObjects = if (excludeObjects.isNullOrEmpty()) {
                    storageObjectDao.searchStorageObjects(searchQuery, limit, offset).first()
                } else {
                    storageObjectDao.searchStorageObjectsExcludingIds(searchQuery, excludeObjects, limit, offset).first()
                }

                val domainObjects = cachedObjects.map { it.toDomain() }
                Result.success(LimitOffsetResponse(count, null, null, domainObjects))
            } catch (cacheException: Exception) {
                Result.failure(cacheException)
            }
        }
    }

    override suspend fun getChildStorageObjects(parentId: Int?, offset: Int, limit: Int, excludeObjects: List<Int>?, labGroupId: Int?): Result<LimitOffsetResponse<StorageObject>> {
        return try {
            val response = if (parentId != null) {
                apiService.getChildStorageObjects(parentId, offset, limit, excludeObjects.toExcludeString(), labGroupId)
            } else {
                apiService.getRootStorageObjects(offset, limit, excludeObjects = excludeObjects.toExcludeString(), labGroupId = labGroupId)
            }
            response.results.forEach { storageObjectDao.insert(it.toEntity()) }
            Result.success(response)
        } catch (e: Exception) {
            try {
                if (parentId != null) {
                    val count = storageObjectDao.countByParent(parentId)
                    val cachedObjects = if (excludeObjects.isNullOrEmpty()) {
                        storageObjectDao.getByParentPaginated(parentId, limit, offset).first()
                    } else {
                        storageObjectDao.getByParentExcludingIds(parentId, excludeObjects, limit, offset).first()
                    }
                    val domainObjects = cachedObjects.map { it.toDomain() }
                    Result.success(LimitOffsetResponse(count, null, null, domainObjects))
                } else {
                    val count = storageObjectDao.countRootObjects()
                    val cachedObjects = if (excludeObjects.isNullOrEmpty()) {
                        storageObjectDao.getRootObjectsPaginated(limit, offset).first()
                    } else {
                        storageObjectDao.getRootObjectsExcludingIds(excludeObjects, limit, offset).first()
                    }
                    val domainObjects = cachedObjects.map { it.toDomain() }
                    Result.success(LimitOffsetResponse(count, null, null, domainObjects))
                }
            } catch (cacheException: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun createStorageObject(storageObject: StorageObject): Result<StorageObject> {
        return try {
            val response = apiService.createStorageObject(storageObject)
            storageObjectDao.insert(response.toEntity())
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateStorageObject(id: Int, storageObject: StorageObject): Result<StorageObject> {
        return try {
            val response = apiService.updateStorageObject(id, storageObject)
            storageObjectDao.insert(response.toEntity())
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteStorageObject(id: Int): Result<Unit> {
        return try {
            apiService.deleteStorageObject(id)
            val cachedObject = storageObjectDao.getById(id)
            if (cachedObject != null) {
                storageObjectDao.delete(cachedObject)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addAccessGroup(storageObjectId: Int, labGroupId: Int): Result<StorageObject> {
        return try {
            val response = apiService.addAccessGroup(storageObjectId, mapOf("lab_group" to labGroupId))
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeAccessGroup(storageObjectId: Int, labGroupId: Int): Result<StorageObject> {
        return try {
            val response = apiService.addAccessGroup(storageObjectId, mapOf("lab_group" to labGroupId))
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getPathToRoot(id: Int): Result<List<StoragePathItem>> {
        return try {
            val response = apiService.getPathToRoot(id)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getRootStorageObjects(offset: Int, limit: Int, excludeObjects: List<Int>?, labGroupId: Int?): Result<LimitOffsetResponse<StorageObject>> {
        return try {
            val response = apiService.getRootStorageObjects(offset, limit, excludeObjects = excludeObjects.toExcludeString(), labGroupId = labGroupId)
            // Cache results
            response.results.forEach { storageObjectDao.insert(it.toEntity()) }
            Result.success(response)
        } catch (e: Exception) {
            // Try to fetch from cache on failure
            try {
                val count = storageObjectDao.countRootObjects()
                val cachedObjects = if (excludeObjects.isNullOrEmpty()) {
                    storageObjectDao.getRootObjectsPaginated(limit, offset).first()
                } else {
                    storageObjectDao.getRootObjectsExcludingIds(excludeObjects, limit, offset).first()
                }
                val domainObjects = cachedObjects.map { it.toDomain() }
                Result.success(LimitOffsetResponse(count, null, null, domainObjects))
            } catch (cacheException: Exception) {
                Result.failure(e)
            }
        }
    }

    private fun StorageObject.toEntity(): StorageObjectEntity {
        return StorageObjectEntity(
            id = id,
            objectName = objectName,
            objectType = objectType,
            objectDescription = objectDescription,
            createdAt = createdAt,
            updatedAt = updatedAt,
            canDelete = canDelete,
            storedAt = storedAt,
            pngBase64 = pngBase64,
            user = user,
            remoteId = remoteId,
            remoteHost = remoteHost
        )
    }

    private fun StorageObjectEntity.toDomain(): StorageObject {
        return StorageObject(
            id = id,
            objectName = objectName,
            objectType = objectType,
            objectDescription = objectDescription,
            createdAt = createdAt,
            updatedAt = updatedAt,
            canDelete = canDelete,
            storedAt = storedAt,
            pngBase64 = pngBase64,
            user = user,
            remoteId = remoteId,
            remoteHost = remoteHost,
            storedReagents = null,
            accessLabGroups = null,
            pathToRoot = null,
            childCount = 0
        )
    }

}

