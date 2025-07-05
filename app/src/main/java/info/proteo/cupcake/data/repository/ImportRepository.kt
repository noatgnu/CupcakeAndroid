package info.proteo.cupcake.data.repository

import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import info.proteo.cupcake.shared.data.model.system.ImportTracker
import info.proteo.cupcake.shared.data.model.system.ImportedObject
import info.proteo.cupcake.shared.data.model.system.ImportedFile
import info.proteo.cupcake.shared.data.model.system.ImportedRelationship
import info.proteo.cupcake.data.remote.service.ImportTrackerService
import info.proteo.cupcake.data.remote.service.ImportedObjectService
import info.proteo.cupcake.data.remote.service.ImportedFileService
import info.proteo.cupcake.data.remote.service.ImportedRelationshipService
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ImportRepository @Inject constructor(
    private val importTrackerService: ImportTrackerService,
    private val importedObjectService: ImportedObjectService,
    private val importedFileService: ImportedFileService,
    private val importedRelationshipService: ImportedRelationshipService
) {

    // Import Tracker Operations
    suspend fun getImportTrackers(
        importType: String? = null,
        importStatus: String? = null,
        createdBy: Int? = null,
        labGroup: Int? = null,
        search: String? = null,
        ordering: String? = null,
        limit: Int? = null,
        offset: Int? = null
    ): Result<LimitOffsetResponse<ImportTracker>> {
        return importTrackerService.getImportTrackers(
            importType, importStatus, createdBy, labGroup, search, ordering, limit, offset
        )
    }

    suspend fun getImportTracker(id: Int): Result<ImportTracker> {
        return importTrackerService.getImportTracker(id)
    }

    suspend fun createImportTracker(
        importType: String?,
        importName: String?,
        importDescription: String?,
        labGroup: Int?
    ): Result<ImportTracker> {
        return importTrackerService.createImportTracker(importType, importName, importDescription, labGroup)
    }

    suspend fun updateImportTracker(
        id: Int,
        importType: String?,
        importName: String?,
        importDescription: String?,
        labGroup: Int?
    ): Result<ImportTracker> {
        return importTrackerService.updateImportTracker(id, importType, importName, importDescription, labGroup)
    }

    suspend fun deleteImportTracker(id: Int): Result<Unit> {
        return importTrackerService.deleteImportTracker(id)
    }

    suspend fun rollbackImport(id: Int): Result<ImportTracker> {
        return importTrackerService.rollbackImport(id)
    }

    // Imported Object Operations
    suspend fun getImportedObjects(
        importTracker: Int? = null,
        objectType: String? = null,
        actionType: String? = null,
        limit: Int? = null,
        offset: Int? = null
    ): Result<LimitOffsetResponse<ImportedObject>> {
        return importedObjectService.getImportedObjects(importTracker, objectType, actionType, limit, offset)
    }

    suspend fun getImportedObject(id: Int): Result<ImportedObject> {
        return importedObjectService.getImportedObject(id)
    }

    suspend fun createImportedObject(
        importTracker: Int,
        objectType: String,
        objectId: Int,
        actionType: String,
        objectData: String?
    ): Result<ImportedObject> {
        return importedObjectService.createImportedObject(importTracker, objectType, objectId, actionType, objectData)
    }

    suspend fun deleteImportedObject(id: Int): Result<Unit> {
        return importedObjectService.deleteImportedObject(id)
    }

    // Imported File Operations
    suspend fun getImportedFiles(
        importTracker: Int? = null,
        limit: Int? = null,
        offset: Int? = null
    ): Result<LimitOffsetResponse<ImportedFile>> {
        return importedFileService.getImportedFiles(importTracker, limit, offset)
    }

    suspend fun getImportedFile(id: Int): Result<ImportedFile> {
        return importedFileService.getImportedFile(id)
    }

    suspend fun createImportedFile(
        importTracker: Int,
        filePath: String,
        originalFilename: String?,
        fileSizeBytes: Int?,
        fileHash: String?
    ): Result<ImportedFile> {
        return importedFileService.createImportedFile(importTracker, filePath, originalFilename, fileSizeBytes, fileHash)
    }

    suspend fun deleteImportedFile(id: Int): Result<Unit> {
        return importedFileService.deleteImportedFile(id)
    }

    // Imported Relationship Operations
    suspend fun getImportedRelationships(
        importTracker: Int? = null,
        relationshipType: String? = null,
        parentModel: String? = null,
        childModel: String? = null,
        limit: Int? = null,
        offset: Int? = null
    ): Result<LimitOffsetResponse<ImportedRelationship>> {
        return importedRelationshipService.getImportedRelationships(
            importTracker, relationshipType, parentModel, childModel, limit, offset
        )
    }

    suspend fun getImportedRelationship(id: Int): Result<ImportedRelationship> {
        return importedRelationshipService.getImportedRelationship(id)
    }

    suspend fun createImportedRelationship(
        importTracker: Int,
        relationshipType: String,
        parentModel: String,
        parentId: Int,
        childModel: String,
        childId: Int
    ): Result<ImportedRelationship> {
        return importedRelationshipService.createImportedRelationship(
            importTracker, relationshipType, parentModel, parentId, childModel, childId
        )
    }

    suspend fun deleteImportedRelationship(id: Int): Result<Unit> {
        return importedRelationshipService.deleteImportedRelationship(id)
    }
}