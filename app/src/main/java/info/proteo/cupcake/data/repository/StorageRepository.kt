package info.proteo.cupcake.data.repository

import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import info.proteo.cupcake.shared.data.model.storage.StorageObject
import info.proteo.cupcake.shared.data.model.storage.StorageObjectBasic
import info.proteo.cupcake.shared.data.model.storage.StoragePathItem
import info.proteo.cupcake.data.remote.service.StorageObjectService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class StorageRepository @Inject constructor(
    private val storageObjectService: StorageObjectService
) {
    suspend fun getStorageObjects(offset: Int = 0, limit: Int = 20,
                                  excludeObjects: List<Int>? = null,
                                  labGroupId: Int? = null): Result<LimitOffsetResponse<StorageObject>> {
        return storageObjectService.getStorageObjects(offset, limit, excludeObjects, labGroupId)
    }

    suspend fun getStorageObjectById(id: Int): Result<StorageObject> {
        return storageObjectService.getStorageObjectById(id)
    }

    suspend fun searchStorageObjects(query: String, offset: Int = 0, limit: Int = 20,
                                     excludeObjects: List<Int>? = null,
                                     labGroupId: Int? = null): Result<LimitOffsetResponse<StorageObject>> {
        return storageObjectService.searchStorageObjects(query, offset, limit, excludeObjects, labGroupId)
    }

    suspend fun getChildStorageObjects(parentId: Int? = null, offset: Int = 0, limit: Int = 20,
                                       excludeObjects: List<Int>? = null,
                                       labGroupId: Int? = null): Result<LimitOffsetResponse<StorageObject>> {
        return storageObjectService.getChildStorageObjects(parentId, offset, limit, excludeObjects, labGroupId)
    }

    suspend fun createStorageObject(storageObject: StorageObject): Result<StorageObject> {
        return storageObjectService.createStorageObject(storageObject)
    }

    suspend fun updateStorageObject(id: Int, storageObject: StorageObject): Result<StorageObject> {
        return storageObjectService.updateStorageObject(id, storageObject)
    }

    suspend fun deleteStorageObject(id: Int): Result<Unit> {
        return storageObjectService.deleteStorageObject(id)
    }

    suspend fun addAccessGroup(storageObjectId: Int, labGroupId: Int): Result<StorageObject> {
        return storageObjectService.addAccessGroup(storageObjectId, labGroupId)
    }

    suspend fun removeAccessGroup(storageObjectId: Int, labGroupId: Int): Result<StorageObject> {
        return storageObjectService.removeAccessGroup(storageObjectId, labGroupId)
    }

    suspend fun getPathToRoot(id: Int): Result<List<StoragePathItem>> {
        return storageObjectService.getPathToRoot(id)
    }

    suspend fun getRootStorageObjects(
        offset: Int = 0,
        limit: Int = 20,
        excludeObjects: List<Int>? = null,
        labGroupId: Int? = null
    ): Result<LimitOffsetResponse<StorageObject>> {
        return storageObjectService.getRootStorageObjects(offset, limit, excludeObjects, labGroupId)
    }
}

