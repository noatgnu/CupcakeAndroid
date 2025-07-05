package info.proteo.cupcake.data.remote.service

import android.util.Log
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import info.proteo.cupcake.data.local.dao.annotation.AnnotationDao
import info.proteo.cupcake.data.local.dao.protocol.ProtocolModelDao
import info.proteo.cupcake.data.local.dao.protocol.ProtocolReagentDao
import info.proteo.cupcake.data.local.dao.protocol.ProtocolSectionDao
import info.proteo.cupcake.data.local.dao.protocol.ProtocolStepDao
import info.proteo.cupcake.data.local.dao.protocol.ProtocolTagDao
import info.proteo.cupcake.data.local.dao.reagent.ReagentDao
import info.proteo.cupcake.data.local.dao.tag.TagDao
import info.proteo.cupcake.data.local.entity.protocol.ProtocolEditorCrossRef
import info.proteo.cupcake.data.local.entity.protocol.ProtocolModelEntity
import info.proteo.cupcake.data.local.entity.protocol.ProtocolReagentEntity
import info.proteo.cupcake.data.local.entity.protocol.ProtocolSectionEntity
import info.proteo.cupcake.data.local.entity.protocol.ProtocolStepEntity
import info.proteo.cupcake.data.local.entity.protocol.ProtocolStepNextRelation
import info.proteo.cupcake.data.local.entity.protocol.ProtocolTagEntity
import info.proteo.cupcake.data.local.entity.protocol.ProtocolViewerCrossRef
import info.proteo.cupcake.data.local.entity.reagent.ReagentEntity
import info.proteo.cupcake.data.local.entity.tag.TagEntity
import info.proteo.cupcake.shared.data.model.annotation.Annotation
import info.proteo.cupcake.shared.data.model.reagent.Reagent
import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import info.proteo.cupcake.shared.data.model.protocol.ProtocolModel
import info.proteo.cupcake.shared.data.model.protocol.ProtocolReagent
import info.proteo.cupcake.shared.data.model.protocol.ProtocolSection
import info.proteo.cupcake.shared.data.model.protocol.ProtocolStep
import info.proteo.cupcake.shared.data.model.protocol.ProtocolTag
import info.proteo.cupcake.shared.data.model.protocol.Session
import info.proteo.cupcake.shared.data.model.tag.Tag
import info.proteo.cupcake.shared.data.model.user.User
import info.proteo.cupcake.data.repository.TagRepository
import info.proteo.cupcake.data.repository.UserRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import retrofit2.HttpException
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.text.insert
import kotlin.text.toLong

@JsonClass(generateAdapter = true)
data class CreateProtocolRequest(
    val url: String? = null,
    @Json(name = "protocol_title") val protocolTitle: String? = null,
    @Json(name = "protocol_description") val protocolDescription: String? = null
)

@JsonClass(generateAdapter = true)
data class UpdateProtocolRequest(
    @Json(name = "protocol_title") val protocolTitle: String?,
    @Json(name = "protocol_description") val protocolDescription: String?,
    val enabled: Boolean? = null
)

@JsonClass(generateAdapter = true)
data class CreateExportRequest(
    @Json(name = "export_type") val exportType: String,
    val session: Int? = null,
    val format: String? = null
)

@JsonClass(generateAdapter = true)
data class AddUserRoleRequest(
    val user: String,
    val role: String
)

@JsonClass(generateAdapter = true)
data class RemoveUserRoleRequest(
    val user: String,
    val role: String
)

@JsonClass(generateAdapter = true)
data class ProtocolAddTagRequest(
    val tag: String
)

@JsonClass(generateAdapter = true)
data class ProtocolRemoveTagRequest(
    @Json(name = "tag") val tagId: Int
)

@JsonClass(generateAdapter = true)
data class AddMetadataColumnsRequest(
    @Json(name = "metadata_columns") val metadataColumns: List<MetadataColumnPayload>
)

@JsonClass(generateAdapter = true)
data class MetadataColumnPayload(
    val name: String,
    val value: String?,
    val type: String
)

@JsonClass(generateAdapter = true)
data class SessionMinimal(
    val id: Int,
    val name: String,
    val enabled: Boolean
)

interface ProtocolApiService {
    @GET("api/protocol/")
    suspend fun getProtocols(
        @Query("offset") offset: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("search") search: String? = null,
        @Query("ordering") ordering: String? = null,
        @Query("protocol_title") protocolTitle: String? = null,
        ): LimitOffsetResponse<ProtocolModel>

    @POST("api/protocol/")
    suspend fun createProtocol(@Body request: CreateProtocolRequest): ProtocolModel

    @GET("api/protocol/{id}/")
    suspend fun getProtocolById(@Path("id") id: Int): ProtocolModel

    @PUT("api/protocol/{id}/")
    suspend fun updateProtocol(@Path("id") id: Int, @Body protocol: UpdateProtocolRequest): ProtocolModel

    @DELETE("api/protocol/{id}/")
    suspend fun deleteProtocol(@Path("id") id: Int): Response<Unit>

    @GET("api/protocol/{id}/get_associated_sessions/")
    suspend fun getAssociatedSessions(@Path("id") id: Int): List<Session>

    @GET("api/protocol/get_user_protocols/")
    suspend fun getUserProtocols(
        @Query("offset") offset: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("search") search: String? = null
    ): LimitOffsetResponse<ProtocolModel>

    @POST("api/protocol/{id}/create_export/")
    suspend fun createExport(@Path("id") id: Int, @Body request: CreateExportRequest): Response<Unit>


    @POST("api/protocol/{id}/clone/")
    suspend fun cloneProtocol(
        @Path("id") id: Int,
        @Body request: Map<String, String>
    ): ProtocolModel

    @POST("api/protocol/check_if_title_exists/")
    suspend fun checkIfTitleExists(@Body request: Map<String, String>): Response<Unit>

    @POST("api/protocol/{id}/add_user_role/")
    suspend fun addUserRole(@Path("id") id: Int, @Body request: AddUserRoleRequest): Response<Unit>

    @GET("api/protocol/{id}/get_editors/")
    suspend fun getEditors(@Path("id") id: Int): List<User> // Assuming User model and UserSerializer

    @GET("api/protocol/{id}/get_viewers/")
    suspend fun getViewers(@Path("id") id: Int): List<User> // Assuming User model and UserSerializer

    @POST("api/protocol/{id}/remove_user_role/")
    suspend fun removeUserRole(@Path("id") id: Int, @Body request: RemoveUserRoleRequest): Response<Unit>

    @GET("api/protocol/{id}/get_reagents/")
    suspend fun getProtocolReagents(@Path("id") id: Int): List<ProtocolReagent>

    @POST("api/protocol/{id}/add_tag/")
    suspend fun addTagToProtocol(@Path("id") id: Int, @Body request: ProtocolAddTagRequest): ProtocolTag

    @POST("api/protocol/{id}/remove_tag/") // Backend uses POST for remove_tag with body
    suspend fun removeTagFromProtocol(@Path("id") id: Int, @Body request: ProtocolRemoveTagRequest): Response<Unit>

    @POST("api/protocol/{id}/add_metadata_columns/")
    suspend fun addMetadataColumns(@Path("id") id: Int, @Body request: AddMetadataColumnsRequest): ProtocolModel
}

interface ProtocolService {
    suspend fun getProtocols(
        offset: Int? = null,
        limit: Int? = null,
        search: String? = null,
        ordering: String? = null,
        protocolTitle: String? = null,
        protocolCreatedOn: String? = null
    ): Result<LimitOffsetResponse<ProtocolModel>>

    suspend fun createProtocol(request: CreateProtocolRequest): Result<ProtocolModel>
    suspend fun getProtocolById(id: Int): Result<ProtocolModel>
    suspend fun updateProtocol(id: Int, request: UpdateProtocolRequest): Result<ProtocolModel>
    suspend fun deleteProtocol(id: Int): Result<Unit>

    fun getProtocolByIdFlow(id: Int): Flow<ProtocolModel?>
    fun getAllProtocolsFlow(): Flow<List<ProtocolModel>>
    fun getEnabledProtocolsFlow(): Flow<List<ProtocolModel>>

    suspend fun getAssociatedSessions(id: Int): Result<List<Session>>
    suspend fun getUserProtocols(
        offset: Int? = null,
        limit: Int? = null,
        search: String? = null
    ): Result<LimitOffsetResponse<ProtocolModel>>
    suspend fun createExport(id: Int, request: CreateExportRequest): Result<Unit>
    suspend fun cloneProtocol(id: Int, newTitle: String?, newDescription: String?): Result<ProtocolModel>
    suspend fun checkIfTitleExists(title: String): Result<Boolean> // True if exists (conflict), false otherwise
    suspend fun addUserRole(id: Int, username: String, role: String): Result<Unit>
    suspend fun getEditors(id: Int): Result<List<User>>
    suspend fun getViewers(id: Int): Result<List<User>>
    suspend fun removeUserRole(id: Int, username: String, role: String): Result<Unit>
    suspend fun getProtocolReagents(id: Int): Result<List<ProtocolReagent>>
    suspend fun addTagToProtocol(id: Int, tagName: String): Result<ProtocolTag>
    suspend fun removeTagFromProtocol(id: Int, tagId: Int): Result<Unit>
    suspend fun addMetadataColumns(id: Int, metadataColumns: List<MetadataColumnPayload>): Result<ProtocolModel>
}

@Singleton
class ProtocolServiceImpl @Inject constructor(
    private val apiService: ProtocolApiService,
    private val protocolModelDao: ProtocolModelDao,
    private val protocolStepDao: ProtocolStepDao,
    private val protocolSectionDao: ProtocolSectionDao,
    private val protocolReagentDao: ProtocolReagentDao,
    private val protocolTagDao: ProtocolTagDao,
    private val reagentDao: ReagentDao,
    private val annotationDao: AnnotationDao,
    private val userRepository: UserRepository,
    private val tagDao: TagDao
) : ProtocolService {

    override suspend fun getProtocols(
        offset: Int?,
        limit: Int?,
        search: String?,
        ordering: String?,
        protocolTitle: String?,
        protocolCreatedOn: String?
    ): Result<LimitOffsetResponse<ProtocolModel>> {
        return try {
            Log.d("ProtocolService", "Fetching protocols with params: offset=$offset, limit=$limit, search=$search")
            val response = apiService.getProtocols(offset, limit, search, ordering, protocolTitle)
            Log.d("ProtocolService", "Successfully received response with ${response.results.size} protocols")

            response.results.forEachIndexed { index, protocol ->
                try {
                    Log.d("ProtocolService", "Caching protocol $index: id=${protocol.id}, title=${protocol.protocolTitle}")
                    cacheProtocolModel(protocol)
                    Log.d("ProtocolService", "Successfully cached protocol ${protocol.id}")
                } catch (e: Exception) {
                    Log.e("ProtocolService", "Failed to cache protocol ${protocol.id}", e)
                }
            }

            Result.success(response)
        } catch (e: Exception) {
            Log.e("ProtocolService", "Failed to fetch protocols from API", e)
            Log.e("ProtocolService", "Exception type: ${e.javaClass.simpleName}")
            Log.e("ProtocolService", "Exception message: ${e.message}")
            e.printStackTrace()

            try {
                Log.d("ProtocolService", "Attempting to load protocols from cache")
                val cachedItems = protocolModelDao.getAllProtocols(limit?:10, offset?:0).firstOrNull() ?: emptyList()
                val domainObjects = cachedItems.map { it.toDomainModel() }
                Log.d("ProtocolService", "Successfully loaded ${domainObjects.size} protocols from cache")
                Result.success(LimitOffsetResponse(cachedItems.size, null, null, domainObjects))
            } catch (cacheEx: Exception) {
                Log.e("ProtocolService", "Failed to load protocols from cache", cacheEx)
                Result.failure(e)
            }
        }
    }

    override suspend fun createProtocol(request: CreateProtocolRequest): Result<ProtocolModel> {
        return try {
            val protocolModel = apiService.createProtocol(request)
            cacheProtocolModel(protocolModel)
            Result.success(protocolModel)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProtocolById(id: Int): Result<ProtocolModel> {
        return try {
            val protocolModel = apiService.getProtocolById(id)
            //Log.d("ProtocolService", "Fetched protocol: ${protocolModel.reagents}")
            cacheProtocolModel(protocolModel)
            //Log.d("ProtocolService", "Cached reagents: ${protocolModel.reagents}")
            Result.success(protocolModel)
        } catch (e: Exception) {
            val cached = protocolModelDao.getById(id).firstOrNull()
            if (cached != null) {
                Result.success(cached.toDomainModel())
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateProtocol(id: Int, request: UpdateProtocolRequest): Result<ProtocolModel> {
        return try {
            val protocolModel = apiService.updateProtocol(id, request)
            cacheProtocolModel(protocolModel)
            Result.success(protocolModel)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteProtocol(id: Int): Result<Unit> {
        return try {
            apiService.deleteProtocol(id)
            protocolModelDao.getById(id).firstOrNull()?.let { protocolModelDao.delete(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getProtocolByIdFlow(id: Int): Flow<ProtocolModel?> {
        val protocolModel = protocolModelDao.getById(id)
        return protocolModel.map { entity ->
            entity?.toDomainModel()
        }
    }

    override fun getAllProtocolsFlow(): Flow<List<ProtocolModel>> {
        return protocolModelDao.getAllProtocols()
            .map { list ->
                list.map { it.toDomainModel() }
            }
    }

    override fun getEnabledProtocolsFlow(): Flow<List<ProtocolModel>> {
        return protocolModelDao.getEnabledProtocols().map { list ->
            list.map { it.toDomainModel() }
        }
    }

    override suspend fun getAssociatedSessions(id: Int): Result<List<Session>> {
        return try {
            Result.success(apiService.getAssociatedSessions(id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getUserProtocols(
        offset: Int?,
        limit: Int?,
        search: String?
    ): Result<LimitOffsetResponse<ProtocolModel>> = withContext(Dispatchers.IO) {

        try {
            val response = apiService.getUserProtocols(offset, limit, search)
            response.results.forEach {
                Log.d("ProtocolService", "$it")
                cacheProtocolModel(it)
            }
            Result.success(response)
        } catch (e: Exception) {
            Log.e("ProtocolService", "Failed to fetch user protocols: ${e.message}")
            val user = userRepository.getUserFromActivePreference()
            if (user == null) {
                return@withContext Result.failure(IllegalStateException("User not found in preferences"))
            }
            val items = protocolModelDao
                .getUserProtocols(user.id, limit ?: 10, offset ?: 0)
                .firstOrNull()
                .orEmpty()
                .map { it.toDomainModel() }
            val total = protocolModelDao.countUserProtocols(user.id).firstOrNull() ?: 0
            Result.success(LimitOffsetResponse(total, null, null, items))
        }
    }

    override suspend fun createExport(id: Int, request: CreateExportRequest): Result<Unit> {
        return try {
            apiService.createExport(id, request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun cloneProtocol(id: Int, newTitle: String?, newDescription: String?): Result<ProtocolModel> {
        return try {
            val body = mutableMapOf<String, String>()
            newTitle?.let { body["protocol_title"] = it }
            newDescription?.let { body["protocol_description"] = it }
            val clonedProtocol = apiService.cloneProtocol(id, body)
            cacheProtocolModel(clonedProtocol)
            Result.success(clonedProtocol)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun checkIfTitleExists(title: String): Result<Boolean> {
        return try {
            apiService.checkIfTitleExists(mapOf("protocol_title" to title))
            Result.success(false)
        } catch (e: HttpException) {
            if (e.code() == 409) Result.success(true)
            else Result.failure(e)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addUserRole(id: Int, username: String, role: String): Result<Unit> {
        return try {
            apiService.addUserRole(id, AddUserRoleRequest(username, role))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getEditors(id: Int): Result<List<User>> {
        return try {
            Result.success(apiService.getEditors(id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getViewers(id: Int): Result<List<User>> {
        return try {
            Result.success(apiService.getViewers(id))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeUserRole(id: Int, username: String, role: String): Result<Unit> {
        return try {
            apiService.removeUserRole(id, RemoveUserRoleRequest(username, role))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProtocolReagents(id: Int): Result<List<ProtocolReagent>> {
        return try {
            val reagents = apiService.getProtocolReagents(id)

            // Optionally cache them if not already handled by cacheProtocolModel
            Result.success(reagents)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addTagToProtocol(id: Int, tagName: String): Result<ProtocolTag> {
        return try {
            val newTag = apiService.addTagToProtocol(id, ProtocolAddTagRequest(tagName))
            getProtocolById(id)
            Result.success(newTag)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeTagFromProtocol(id: Int, tagId: Int): Result<Unit> {
        return try {
            apiService.removeTagFromProtocol(id, ProtocolRemoveTagRequest(tagId))
            getProtocolById(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addMetadataColumns(id: Int, metadataColumns: List<MetadataColumnPayload>): Result<ProtocolModel> {
        return try {
            val updatedProtocol = apiService.addMetadataColumns(id, AddMetadataColumnsRequest(metadataColumns))
            cacheProtocolModel(updatedProtocol)
            Result.success(updatedProtocol)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun cacheProtocolModel(protocol: ProtocolModel) {
        val entity = protocol.toEntity()
        protocolModelDao.insert(entity)

        // Process sections
        protocolSectionDao.deleteByProtocol(protocol.id)
        val sectionEntities = protocol.sections?.map { it.toEntity() } ?: emptyList()
        protocolSectionDao.insertAll(sectionEntities)

        // Process steps
        protocol.steps?.forEach { step ->
            protocolStepDao.insertStep(ProtocolStepEntity(
                id = step.id,
                protocol = step.protocol,
                stepId = step.stepId?.toLong(),
                description = step.stepDescription ?: "",
                stepSection = step.stepSection,
                duration = step.stepDuration,
                previousStep = step.previousStep,
                original = true,
                branchFrom = null,
                remoteId = null,
                createdAt = step.createdAt,
                updatedAt = step.updatedAt,
                remoteHost = null
            ))

            protocolStepDao.clearNextStepsForStep(step.id)

        }

        protocol.steps?.forEach { step ->
            step.nextStep?.forEach { nextStepId ->
                protocolStepDao.addNextStepRelation(
                    ProtocolStepNextRelation(
                        fromStep = step.id,
                        toStep = nextStepId
                    )
                )
            }
        }
        protocol.reagents?.forEach { protocolReagent ->
            val reagent = ReagentEntity(
                id = protocolReagent.reagent.id,
                name = protocolReagent.reagent.name,
                unit = protocolReagent.reagent.unit,
                createdAt = protocolReagent.reagent.createdAt,
                updatedAt = protocolReagent.reagent.updatedAt
            )
            reagentDao.insert(reagent)
        }

        //protocolReagentDao.deleteByProtocol(protocol.id)
        val reagentEntities = protocol.reagents?.map { it.toEntity() } ?: emptyList()
        protocolReagentDao.insertAll(reagentEntities)


        protocol.tags?.forEach { protocolTag ->
            val tag = TagEntity(
                id = protocolTag.tag.id,
                tag = protocolTag.tag.tag ?: "Unknown Tag",
                createdAt = protocolTag.tag.createdAt,
                updatedAt = protocolTag.tag.updatedAt
            )
            tagDao.insert(tag)
        }

        //Log.d("ProtocolService", "Caching tags for protocol ${protocol.id}: ${protocol.tags?.map { it.tag.tag }}")

        val tagEntities = protocol.tags?.map { it.toEntity() } ?: emptyList()
        protocolTagDao.insertAll(tagEntities)

        //Log.d("ProtocolService", "Cached tags: ${tagEntities.map { it.tag }}")

    }

    fun ProtocolModel.toEntity(): ProtocolModelEntity {
        return ProtocolModelEntity(
            id = id,
            protocolId = protocolId,
            protocolCreatedOn = protocolCreatedOn,
            protocolDoi = protocolDoi,
            protocolTitle = protocolTitle,
            protocolDescription = protocolDescription,
            protocolUrl = protocolUrl,
            protocolVersionUri = protocolVersionUri,
            enabled = enabled,
            complexityRating = complexityRating,
            durationRating = durationRating,
            user = null,
            remoteId = null,
            modelHash = null,
            remoteHost = null,
            createdAt = null,
            updatedAt = null
        )
    }

    suspend fun ProtocolModelEntity.toDomainModel(): ProtocolModel {
        val steps = protocolStepDao.getStepsByProtocol(id, 1000, 0).firstOrNull()?.map { it.toDomainModel() } ?: emptyList()
        val sections = protocolSectionDao.getSectionsByProtocol(id).firstOrNull()?.map { it.toDomainModel() } ?: emptyList()
        val reagents = protocolReagentDao.getByProtocol(id).firstOrNull()?.map { it.toDomainModel(/*need Reagent object here*/) } ?: emptyList()
        val tags = protocolTagDao.getByProtocol(id).firstOrNull()?.map { it.toDomainModel(/*need Tag object here*/) } ?: emptyList()


        return ProtocolModel(
            id = id,
            protocolId = protocolId,
            protocolCreatedOn = protocolCreatedOn,
            protocolDoi = protocolDoi,
            protocolTitle = protocolTitle,
            protocolDescription = protocolDescription,
            protocolUrl = protocolUrl,
            protocolVersionUri = protocolVersionUri,
            steps = steps,
            sections = sections,
            enabled = enabled,
            complexityRating = complexityRating,
            durationRating = durationRating,
            reagents = reagents,
            tags = tags,
            metadataColumns = emptyList(),
        )
    }

    private suspend fun cacheProtocolStep(protocolStep: ProtocolStep) {
        protocolStepDao.insertStep(ProtocolStepEntity(
            id = protocolStep.id,
            protocol = protocolStep.protocol,
            stepId = protocolStep.stepId?.toLong(),
            description = protocolStep.stepDescription ?: "",
            stepSection = protocolStep.stepSection,
            duration = protocolStep.stepDuration,
            previousStep = protocolStep.previousStep,
            original = true,
            branchFrom = null,
            remoteId = null,
            createdAt = protocolStep.createdAt,
            updatedAt = protocolStep.updatedAt,
            remoteHost = null
        ))

        protocolStepDao.clearNextStepsForStep(protocolStep.id)

        protocolStep.nextStep?.forEach { nextStepId ->
            protocolStepDao.addNextStepRelation(
                ProtocolStepNextRelation(
                    fromStep = protocolStep.id,
                    toStep = nextStepId
                )
            )
        }
    }

    suspend fun ProtocolStepEntity.toDomainModel(): ProtocolStep {
        val annotationEntities = annotationDao.getByStep(id).firstOrNull() ?: emptyList()
        val annotations = annotationEntities.map { entity ->
            Annotation(
                id = entity.id,
                step = entity.step,
                session = entity.session,
                annotation = entity.annotation,
                file = entity.file,
                createdAt = entity.createdAt,
                updatedAt = entity.updatedAt,
                annotationType = entity.annotationType,
                transcribed = entity.transcribed,
                transcription = entity.transcription,
                language = entity.language,
                translation = entity.translation,
                scratched = entity.scratched,
                annotationName = entity.annotationName,
                folder = emptyList(),
                summary = entity.summary,
                instrumentUsage = null,
                metadataColumns = null,
                fixed = entity.fixed,
                user = null,
                storedReagent = entity.storedReagent
            )
        }

        val stepWithNextSteps = protocolStepDao.getStepWithNextSteps(id)
        val nextStepIds = stepWithNextSteps?.nextSteps?.map { it.id } ?: emptyList()

        return ProtocolStep(
            id = id,
            protocol = protocol,
            stepDescription = description,
            stepSection = stepSection,
            stepDuration = duration,
            nextStep = nextStepIds,
            previousStep = previousStep,
            createdAt = createdAt,
            updatedAt = updatedAt,
            reagents = emptyList(),
            variations = emptyList(),
            tags = emptyList(),
            annotations = annotations,
            stepId = stepId?.toInt(),
        )
    }

    fun ProtocolStep.toEntity(): ProtocolStepEntity {
        return ProtocolStepEntity(
            id = id,
            protocol = protocol,
            stepId = stepId?.toLong(),
            description = stepDescription ?: "",
            stepSection = stepSection,
            duration = stepDuration,
            previousStep = previousStep,
            original = true, // Default value
            branchFrom = null, // Default value
            remoteId = null, // Default value
            createdAt = createdAt,
            updatedAt = updatedAt,
            remoteHost = null // Default value
        )
    }

    fun ProtocolStep.toNextStepRelations(): List<ProtocolStepNextRelation> {
        return nextStep?.map { nextStepId ->
            ProtocolStepNextRelation(
                fromStep = id,
                toStep = nextStepId
            )
        } ?: emptyList()
    }

    fun ProtocolSection.toEntity(): ProtocolSectionEntity {
        return ProtocolSectionEntity(
            id = id,
            protocol = protocol,
            sectionDescription = sectionDescription,
            sectionDuration = sectionDuration,
            createdAt = createdAt,
            updatedAt = updatedAt,
            remoteHost = null,
            remoteId = null
        )
    }

    fun ProtocolSectionEntity.toDomainModel(): ProtocolSection {
        return ProtocolSection(
            id = id,
            protocol = protocol,
            sectionDescription = sectionDescription,
            sectionDuration = sectionDuration,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    fun ProtocolReagent.toEntity(): ProtocolReagentEntity {
        return ProtocolReagentEntity(
            id = id,
            protocol = protocol,
            reagent = reagent.id,
            quantity = quantity,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    fun ProtocolReagentEntity.toDomainModel(): ProtocolReagent {
        val placeholderReagent = Reagent(id = reagent, name = "Unknown Reagent", unit = "", "", "")
        return ProtocolReagent(
            id = id,
            protocol = protocol,
            reagent = placeholderReagent,
            quantity = quantity,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }


    fun ProtocolTag.toEntity(): ProtocolTagEntity {
        return ProtocolTagEntity(
            id = id,
            protocol = protocol,
            tag = tag.id,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }

    fun ProtocolTagEntity.toDomainModel(): ProtocolTag {
        val placeholderTag = Tag(id = tag, tag = "Unknown Tag", "", "")
        return ProtocolTag(
            id = id,
            protocol = protocol,
            tag = placeholderTag,
            createdAt = createdAt,
            updatedAt = updatedAt
        )
    }
}

