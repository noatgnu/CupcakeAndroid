package info.proteo.cupcake.data.remote.service

import info.proteo.cupcake.data.local.dao.reagent.ReagentDao
import info.proteo.cupcake.data.local.dao.reagent.StoredReagentDao
import info.proteo.cupcake.data.local.dao.storage.StorageObjectDao
import info.proteo.cupcake.data.local.dao.user.UserDao
import info.proteo.cupcake.data.local.entity.reagent.ReagentEntity
import info.proteo.cupcake.data.local.entity.reagent.StoredReagentEntity
import info.proteo.cupcake.data.local.entity.storage.StorageObjectEntity
import info.proteo.cupcake.data.local.entity.user.UserEntity
import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import info.proteo.cupcake.shared.data.model.reagent.Reagent
import info.proteo.cupcake.shared.data.model.reagent.ReagentAction
import info.proteo.cupcake.shared.data.model.storage.StorageObjectBasic
import info.proteo.cupcake.shared.data.model.reagent.StoredReagent
import info.proteo.cupcake.shared.data.model.reagent.StoredReagentCreateRequest
import info.proteo.cupcake.shared.data.model.user.UserBasic
import kotlinx.coroutines.flow.first
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

interface StoredReagentApiService {
    @GET("api/stored_reagent/")
    suspend fun getStoredReagents(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @Query("storage_object") storageObjectId: Int? = null,
        @Query("lab_group") labGroupId: Int? = null,
        @Query("barcode") barcode: String? = null,
        @Query("search") search: String? = null
    ): LimitOffsetResponse<StoredReagent>

    @GET("api/stored_reagent/{id}/")
    suspend fun getStoredReagentById(@Path("id") id: Int): StoredReagent

    @POST("api/stored_reagent/")
    suspend fun createStoredReagent(@Body request: StoredReagentCreateRequest): StoredReagent

    @PATCH("api/stored_reagent/{id}/")
    suspend fun updateStoredReagent(@Path("id") id: Int, @Body storedReagent: StoredReagent): StoredReagent

    @DELETE("api/stored_reagent/{id}/")
    suspend fun deleteStoredReagent(@Path("id") id: Int)

    @GET("api/stored_reagent/{id}/get_reagent_actions/")
    suspend fun getReagentActions(
        @Path("id") id: Int,
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): List<ReagentAction>

    @POST("api/stored_reagent/{id}/add_access_group/")
    suspend fun addAccessGroup(@Path("id") id: Int, @Body body: Map<String, Int>): StoredReagent

    @POST("api/stored_reagent/{id}/remove_access_group/")
    suspend fun removeAccessGroup(@Path("id") id: Int, @Body body: Map<String, Int>): StoredReagent

    @POST("api/stored_reagent/{id}/subscribe/")
    suspend fun subscribe(@Path("id") id: Int, @Body body: Map<String, Boolean>): Map<String, Any>

    @POST("api/stored_reagent/{id}/unsubscribe/")
    suspend fun unsubscribe(@Path("id") id: Int, @Body body: Map<String, Boolean>): Map<String, Any>
}

interface StoredReagentService {
    suspend fun getStoredReagents(offset: Int, limit: Int, storageObjectId: Int? = null, labGroupId: Int? = null, barcode: String? = null, search: String? = null): Result<LimitOffsetResponse<StoredReagent>>
    suspend fun getStoredReagentById(id: Int): Result<StoredReagent>
    suspend fun createStoredReagent(request: StoredReagentCreateRequest): Result<StoredReagent>
    suspend fun updateStoredReagent(id: Int, storedReagent: StoredReagent): Result<StoredReagent>
    suspend fun deleteStoredReagent(id: Int): Result<Unit>
    suspend fun getReagentActions(id: Int, startDate: String? = null, endDate: String? = null): Result<List<ReagentAction>>
    suspend fun addAccessGroup(id: Int, labGroupId: Int): Result<StoredReagent>
    suspend fun removeAccessGroup(id: Int, labGroupId: Int): Result<StoredReagent>
    suspend fun subscribe(id: Int, notifyLowStock: Boolean, notifyExpiry: Boolean): Result<Map<String, Any>>
    suspend fun unsubscribe(id: Int, notifyLowStock: Boolean, notifyExpiry: Boolean): Result<Map<String, Any>>
}

@Singleton
class StoredReagentServiceImpl @Inject constructor(
    private val apiService: StoredReagentApiService,
    private val storedReagentDao: StoredReagentDao,
    private val reagentDao: ReagentDao,
    private val storageObjectDao: StorageObjectDao,
    private val userDao: UserDao
) : StoredReagentService {

    override suspend fun getStoredReagents(
        offset: Int,
        limit: Int,
        storageObjectId: Int?,
        labGroupId: Int?,
        barcode: String?,
        search: String?
    ): Result<LimitOffsetResponse<StoredReagent>> {
        return try {
            val response = apiService.getStoredReagents(offset, limit, storageObjectId, labGroupId, barcode, search)
            response.results.forEach {
                cacheStoredReagentWithRelations(it)
            }
            Result.success(response)
        } catch (e: Exception) {
            try {
                val count = if (storageObjectId != null) {
                    storedReagentDao.countByStorageObject(storageObjectId)
                } else {
                    storedReagentDao.countAll()
                }

                val cachedItems = if (storageObjectId != null) {
                    storedReagentDao.getByStorageObjectPaginated(storageObjectId, limit, offset).first()
                } else {
                    storedReagentDao.getAllPaginated(limit, offset).first()
                }

                val domainObjects = cachedItems.map { loadStoredReagentWithRelations(it) }
                Result.success(LimitOffsetResponse(count, null, null, domainObjects))
            } catch (cacheException: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getStoredReagentById(id: Int): Result<StoredReagent> {
        return try {
            val response = apiService.getStoredReagentById(id)
            cacheStoredReagentWithRelations(response)
            Result.success(response)
        } catch (e: Exception) {
            val cachedObject = storedReagentDao.getById(id)
            if (cachedObject != null) {
                Result.success(loadStoredReagentWithRelations(cachedObject))
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun createStoredReagent(request: StoredReagentCreateRequest): Result<StoredReagent> {
        return try {
            val response = apiService.createStoredReagent(request)
            // Cache created object with reagent data
            cacheStoredReagentWithRelations(response)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateStoredReagent(id: Int, storedReagent: StoredReagent): Result<StoredReagent> {
        return try {
            val response = apiService.updateStoredReagent(id, storedReagent)
            cacheStoredReagentWithRelations(response)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteStoredReagent(id: Int): Result<Unit> {
        return try {
            apiService.deleteStoredReagent(id)
            storedReagentDao.getById(id)?.let { storedReagentDao.delete(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getReagentActions(id: Int, startDate: String?, endDate: String?): Result<List<ReagentAction>> {
        return try {
            val actions = apiService.getReagentActions(id, startDate, endDate)
            Result.success(actions)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addAccessGroup(id: Int, labGroupId: Int): Result<StoredReagent> {
        return try {
            val response = apiService.addAccessGroup(id, mapOf("lab_group" to labGroupId))
            storedReagentDao.insert(response.toEntity())
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeAccessGroup(id: Int, labGroupId: Int): Result<StoredReagent> {
        return try {
            val response = apiService.removeAccessGroup(id, mapOf("lab_group" to labGroupId))
            storedReagentDao.insert(response.toEntity())
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun subscribe(id: Int, notifyLowStock: Boolean, notifyExpiry: Boolean): Result<Map<String, Any>> {
        return try {
            val response = apiService.subscribe(id, mapOf(
                "notify_on_low_stock" to notifyLowStock,
                "notify_on_expiry" to notifyExpiry
            ))
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun unsubscribe(id: Int, notifyLowStock: Boolean, notifyExpiry: Boolean): Result<Map<String, Any>> {
        return try {
            val response = apiService.unsubscribe(id, mapOf(
                "notify_on_low_stock" to notifyLowStock,
                "notify_on_expiry" to notifyExpiry
            ))
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun cacheStoredReagentWithRelations(storedReagent: StoredReagent) {
        storedReagentDao.insert(storedReagent.toEntity())

        storedReagent.reagent.let { reagent ->
            reagentDao.insert(ReagentEntity(
                id = reagent.id,
                name = reagent.name,
                unit = reagent.unit,
                createdAt = reagent.createdAt,
                updatedAt = reagent.updatedAt
            ))
        }

        storedReagent.storageObject.let { storageObject ->
            val existingStorageObject = storageObjectDao.getById(storageObject!!.id)

            if (existingStorageObject == null) {
                storageObjectDao.insert(StorageObjectEntity(
                    id = storageObject!!.id,
                    objectName = storageObject!!.objectName ?: "",
                    objectType = storageObject!!.objectType ?: "",
                    objectDescription = storageObject.objectDescription ?: "",
                    createdAt = null,
                    updatedAt = null,
                    canDelete = false,
                    storedAt = null,
                    pngBase64 = null,
                    user = null,
                    remoteId = null,
                    remoteHost = null
                ))
            } else {
                val updatedName = if (storageObject.objectName.isNotEmpty())
                    storageObject.objectName
                else
                    existingStorageObject.objectName


                if (updatedName != existingStorageObject.objectName) {
                    val updatedStorageObject = existingStorageObject.copy(
                        objectName = updatedName,
                    )
                    storageObjectDao.insert(updatedStorageObject)
                }
            }
        }
        storedReagent.user.let { user ->
            val existingUser = userDao.getById(user!!.id)
            if (existingUser == null) {
                userDao.insert(UserEntity(
                    id = user!!.id,
                    username = user!!.username,
                    email = null,
                    firstName = null,
                    lastName = null,
                    isStaff = false,
                ))
            } else if (user!!.username.isNotEmpty() && user!!.username != existingUser.username) {
                val updatedUser = existingUser.copy(username = user!!.username)
                userDao.insert(updatedUser)
            }
        }
    }

    private suspend fun loadStoredReagentWithRelations(entity: StoredReagentEntity): StoredReagent {
        val reagentEntity = reagentDao.getById(entity.reagentId)

        val storageEntity = storageObjectDao.getById(entity.storageObjectId)

        val userEntity = userDao.getById(entity.userId)

        return StoredReagent(
            id = entity.id,
            reagent = Reagent(
                id = entity.reagentId,
                name = reagentEntity?.name ?: "",
                unit = reagentEntity?.unit ?: "",
                createdAt = reagentEntity?.createdAt,
                updatedAt = reagentEntity?.updatedAt
            ),
            reagentId = entity.reagentId,
            storageObject = StorageObjectBasic(
                id = entity.storageObjectId,
                objectName = storageEntity?.objectName ?: "",
                objectType = storageEntity?.objectType ?: "",
                objectDescription = storageEntity?.objectDescription ?: "",
            ),
            storageObjectId = entity.storageObjectId,
            quantity = entity.quantity,
            notes = entity.notes,
            user = UserBasic(
                id = entity.userId,
                username = userEntity?.username ?: "",
                firstName = userEntity?.firstName,
                lastName = userEntity?.lastName
            ),
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            currentQuantity = entity.currentQuantity,
            pngBase64 = entity.pngBase64,
            barcode = entity.barcode,
            shareable = entity.shareable,
            expirationDate = entity.expirationDate,
            createdBySession = entity.createdBySession,
            createdByStep = null,
            metadataColumns = null,
            notifyOnLowStock = entity.notifyOnLowStock,
            lastNotificationSent = entity.lastNotificationSent,
            lowStockThreshold = entity.lowStockThreshold,
            notifyDaysBeforeExpiry = entity.notifyDaysBeforeExpiry,
            notifyOnExpiry = entity.notifyOnExpiry,
            lastExpiryNotificationSent = entity.lastExpiryNotificationSent,
            isSubscribed = false,
            subscription = null,
            subscriberCount = entity.subscriberCount,
            accessUsers = null,
            accessLabGroups = null,
            accessAll = entity.accessAll,
            createdByProject = entity.createdByProject,
            createdByProtocol = entity.createdByProtocol,
            remoteId = entity.remoteId,
            remoteHost = entity.remoteHost
        )
    }

    private fun StoredReagent.toEntity(): StoredReagentEntity {
        return StoredReagentEntity(
            id = id,
            reagentId = reagentId ?: reagent.id,
            storageObjectId = storageObjectId ?: storageObject!!.id,
            quantity = quantity,
            notes = notes,
            userId = user!!.id,
            createdAt = createdAt,
            updatedAt = updatedAt,
            currentQuantity = currentQuantity,
            pngBase64 = pngBase64,
            barcode = barcode,
            shareable = shareable,
            expirationDate = expirationDate,
            createdBySession = createdBySession,
            notifyOnLowStock = notifyOnLowStock,
            lastNotificationSent = lastNotificationSent,
            lowStockThreshold = lowStockThreshold,
            notifyDaysBeforeExpiry = notifyDaysBeforeExpiry,
            notifyOnExpiry = notifyOnExpiry,
            lastExpiryNotificationSent = lastExpiryNotificationSent,
            subscriberCount = subscriberCount,
            accessAll = accessAll ?: false,
            createdByProject = createdByProject,
            createdByProtocol = createdByProtocol,
            createdByStep = createdByStep,
            remoteId = null,
            remoteHost = null
        )
    }
}