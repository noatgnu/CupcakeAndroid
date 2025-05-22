package info.proteo.cupcake.data.remote.service

import info.proteo.cupcake.data.local.dao.storage.StorageObjectDao
import info.proteo.cupcake.data.local.dao.user.LabGroupDao
import info.proteo.cupcake.data.local.dao.user.UserDao
import info.proteo.cupcake.data.local.entity.storage.StorageObjectEntity
import info.proteo.cupcake.data.local.entity.user.LabGroupEntity
import info.proteo.cupcake.data.local.entity.user.UserEntity
import info.proteo.cupcake.data.remote.model.LimitOffsetResponse
import info.proteo.cupcake.data.remote.model.storage.StorageObjectBasic
import info.proteo.cupcake.data.remote.model.user.LabGroup
import info.proteo.cupcake.data.remote.model.user.User
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.http.*
import javax.inject.Inject
import javax.inject.Singleton

interface LabGroupApiService {
    @GET("api/lab_group/")
    suspend fun getLabGroups(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @Query("search") search: String? = null,
        @Query("ordering") ordering: String? = null,
        @Query("stored_reagent") storedReagentId: Int? = null,
        @Query("storage_object") storageObjectId: Int? = null,
        @Query("is_professional") isProfessional: Boolean? = null
    ): LimitOffsetResponse<LabGroup>

    @GET("api/lab_groups/{id}/")
    suspend fun getLabGroupById(@Path("id") id: Int): LabGroup

    @POST("api/lab_groups/")
    suspend fun createLabGroup(@Body labGroup: Map<String, Any>): LabGroup

    @PATCH("api/lab_groups/{id}/")
    suspend fun updateLabGroup(@Path("id") id: Int, @Body labGroup: Map<String, Any>): LabGroup

    @DELETE("api/lab_groups/{id}/")
    suspend fun deleteLabGroup(@Path("id") id: Int)

    @POST("api/lab_groups/{id}/remove_user/")
    suspend fun removeUser(@Path("id") id: Int, @Body user: Map<String, Int>): LabGroup

    @POST("api/lab_groups/{id}/add_user/")
    suspend fun addUser(@Path("id") id: Int, @Body user: Map<String, Int>): LabGroup

    @GET("api/lab_groups/{id}/get_users/")
    suspend fun getUsers(@Path("id") id: Int): List<User>

    @GET("api/lab_groups/{id}/get_managers/")
    suspend fun getManagers(@Path("id") id: Int): List<User>
}

interface LabGroupService {
    suspend fun getLabGroups(
        offset: Int,
        limit: Int,
        search: String? = null,
        ordering: String? = null,
        storedReagentId: Int? = null,
        storageObjectId: Int? = null,
        isProfessional: Boolean? = null
    ): Result<LimitOffsetResponse<LabGroup>>

    suspend fun getLabGroupById(id: Int): Result<LabGroup>
    suspend fun createLabGroup(name: String, description: String, isProfessional: Boolean): Result<LabGroup>
    suspend fun updateLabGroup(id: Int, updates: Map<String, Any>): Result<LabGroup>
    suspend fun deleteLabGroup(id: Int): Result<Unit>
    suspend fun removeUser(labGroupId: Int, userId: Int): Result<LabGroup>
    suspend fun addUser(labGroupId: Int, userId: Int): Result<LabGroup>
    suspend fun getUsers(labGroupId: Int): Result<List<User>>
    suspend fun getManagers(labGroupId: Int): Result<List<User>>
}

@Singleton
class LabGroupServiceImpl @Inject constructor(
    private val apiService: LabGroupApiService,
    private val labGroupDao: LabGroupDao,
    private val storageObjectDao: StorageObjectDao,
    private val userDao: UserDao
) : LabGroupService {

    override suspend fun getLabGroups(
        offset: Int,
        limit: Int,
        search: String?,
        ordering: String?,
        storedReagentId: Int?,
        storageObjectId: Int?,
        isProfessional: Boolean?
    ): Result<LimitOffsetResponse<LabGroup>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getLabGroups(
                offset = offset,
                limit = limit,
                search = search,
                ordering = ordering,
                storedReagentId = storedReagentId,
                storageObjectId = storageObjectId,
                isProfessional = isProfessional
            )

            response.results.forEach { labGroup ->
                cacheLabGroupWithRelations(labGroup)
            }

            Result.success(response)
        } catch (e: Exception) {
            // Try to load from cache if network request fails
            if (offset == 0 && search.isNullOrEmpty() &&
                storedReagentId == null && storageObjectId == null && isProfessional == null) {

                val cachedLabGroups = labGroupDao.getLabGroups(limit)
                if (cachedLabGroups.isNotEmpty()) {
                    val labGroups = cachedLabGroups.map { entity ->
                        loadLabGroupWithRelations(entity)
                    }

                    Result.success(
                        LimitOffsetResponse(
                            count = labGroups.size,
                            next = null,
                            previous = null,
                            results = labGroups
                        )
                    )
                } else {
                    Result.failure(e)
                }
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun getLabGroupById(id: Int): Result<LabGroup> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getLabGroupById(id)

            // Cache lab group
            cacheLabGroupWithRelations(response)

            Result.success(response)
        } catch (e: Exception) {
            // Try to load from cache if network request fails
            val cachedLabGroup = labGroupDao.getById(id)
            if (cachedLabGroup != null) {
                Result.success(loadLabGroupWithRelations(cachedLabGroup))
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun createLabGroup(
        name: String,
        description: String,
        isProfessional: Boolean
    ): Result<LabGroup> = withContext(Dispatchers.IO) {
        try {
            val labGroupData = mapOf(
                "name" to name,
                "description" to description,
                "is_professional" to isProfessional
            )

            val response = apiService.createLabGroup(labGroupData)

            // Cache lab group
            cacheLabGroupWithRelations(response)

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateLabGroup(id: Int, updates: Map<String, Any>): Result<LabGroup> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.updateLabGroup(id, updates)

                // Cache lab group
                cacheLabGroupWithRelations(response)

                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun deleteLabGroup(id: Int): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            apiService.deleteLabGroup(id)

            // Remove from cache
            labGroupDao.deleteById(id)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeUser(labGroupId: Int, userId: Int): Result<LabGroup> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.removeUser(labGroupId, mapOf("user" to userId))

                // Update cache
                cacheLabGroupWithRelations(response)

                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun addUser(labGroupId: Int, userId: Int): Result<LabGroup> =
        withContext(Dispatchers.IO) {
            try {
                val response = apiService.addUser(labGroupId, mapOf("user" to userId))

                // Update cache
                cacheLabGroupWithRelations(response)

                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun getUsers(labGroupId: Int): Result<List<User>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getUsers(labGroupId)

            response.forEach { user ->
                userDao.insert(
                    UserEntity(
                        id = user.id,
                        username = user.username,
                        email = user.email,
                        firstName = user.firstName,
                        lastName = user.lastName,
                        isStaff = user.isStaff
                    )
                )
            }

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getManagers(labGroupId: Int): Result<List<User>> = withContext(Dispatchers.IO) {
        try {
            val response = apiService.getManagers(labGroupId)

            response.forEach { user ->
                userDao.insert(
                    UserEntity(
                        id = user.id,
                        username = user.username,
                        email = user.email,
                        firstName = user.firstName,
                        lastName = user.lastName,
                        isStaff = user.isStaff
                    )
                )
            }

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun cacheLabGroupWithRelations(labGroup: LabGroup) {
        // Store the lab group entity
        labGroupDao.insert(
            LabGroupEntity(
                id = labGroup.id,
                name = labGroup.name,
                createdAt = labGroup.createdAt,
                updatedAt = labGroup.updatedAt,
                description = labGroup.description,
                defaultStorage = labGroup.defaultStorage?.id,
                isProfessional = labGroup.isProfessional,
                serviceStorage = labGroup.serviceStorage?.id
            )
        )

        // Cache storage objects if present
        labGroup.defaultStorage?.let { storageObject ->
            val existingStorageObject = storageObjectDao.getById(storageObject.id)
            if (existingStorageObject == null) {

                storageObjectDao.insert(
                    StorageObjectEntity(
                        id = storageObject.id,
                        objectName = storageObject.objectName,
                        objectType = storageObject.objectType,
                        objectDescription = storageObject.objectDescription,
                        createdAt = null,
                        updatedAt = null,
                        canDelete = false,
                        storedAt = null,
                        pngBase64 = null,
                        user = null,
                    )
                )
            }
        }

        labGroup.serviceStorage?.let { storageObject ->
            val existingStorageObject = storageObjectDao.getById(storageObject.id)
            if (existingStorageObject == null) {
                storageObjectDao.insert(
                    StorageObjectEntity(
                        id = storageObject.id,
                        objectName = storageObject.objectName,
                        objectType = storageObject.objectType,
                        objectDescription = storageObject.objectDescription,
                        createdAt = null,
                        updatedAt = null,
                        canDelete = false,
                        storedAt = null,
                        pngBase64 = null,
                        user = null,
                    )
                )
            }
        }
    }

    private suspend fun loadLabGroupWithRelations(entity: LabGroupEntity): LabGroup {
        // Load related storage objects from cache
        val defaultStorage = entity.defaultStorage?.let { id ->
            val storageEntity = storageObjectDao.getById(id)
            if (storageEntity != null) {
                StorageObjectBasic(
                    id = storageEntity.id,
                    objectName = storageEntity.objectName,
                    objectType = storageEntity.objectType ?: "",
                    objectDescription = storageEntity.objectDescription ?: ""
                )
            } else null
        }

        val serviceStorage = entity.serviceStorage?.let { id ->
            val storageEntity = storageObjectDao.getById(id)
            if (storageEntity != null) {
                StorageObjectBasic(
                    id = storageEntity.id,
                    objectName = storageEntity.objectName,
                    objectType = storageEntity.objectType ?: "",
                    objectDescription = storageEntity.objectDescription ?: ""
                )
            } else null
        }

        return LabGroup(
            id = entity.id,
            name = entity.name,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            description = entity.description,
            defaultStorage = defaultStorage,
            isProfessional = entity.isProfessional,
            serviceStorage = serviceStorage
        )
    }
}