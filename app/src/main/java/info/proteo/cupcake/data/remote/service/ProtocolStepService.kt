package info.proteo.cupcake.data.remote.service

import com.squareup.moshi.Json
import info.proteo.cupcake.data.local.dao.annotation.AnnotationDao
import info.proteo.cupcake.data.local.dao.protocol.ProtocolStepDao
import info.proteo.cupcake.data.local.dao.user.UserPreferencesDao
import info.proteo.cupcake.data.local.entity.protocol.ProtocolStepEntity
import info.proteo.cupcake.data.local.entity.protocol.ProtocolStepNextRelation
import info.proteo.cupcake.data.remote.model.LimitOffsetResponse
import info.proteo.cupcake.data.remote.model.protocol.ProtocolStep
import info.proteo.cupcake.data.remote.model.protocol.StepReagent
import info.proteo.cupcake.data.remote.model.protocol.StepTag
import info.proteo.cupcake.data.remote.model.protocol.TimeKeeper
import info.proteo.cupcake.data.remote.model.annotation.Annotation
import info.proteo.cupcake.data.remote.model.reagent.ReagentAction
import info.proteo.cupcake.data.repository.UserRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

data class StepTimeKeeperRequest(
    @Json(name = "session") val session: String? = null,
    @Json(name = "started") val started: Boolean? = null,
    @Json(name = "start_time") val startTime: String? = null,
)

data class StepReagentRequest(
    val name: String,
    val unit: String,
    val quantity: Float,
    val scalable: Boolean = false,
    @Json(name = "scalable_factor") val scalableFactor: Float? = null,
)

data class StepReagentRemoveRequest(
    val reagent: Int
)

data class StepReagentUpdateRequest(
    val reagent: Int,
    val quantity: Float,
    val scalable: Boolean = false,
    @Json(name = "scalable_factor") val scalableFactor: Float? = null,
)

data class StepTagRequest(
    val tag: String
)

data class StepTagRemoveRequest(
    val tag: Int
)

data class ExportAssociatedMetadataResponse(
    val name: String,
    val type: String,
    val value: String? = null,
)

data class SDRFConversionRequest(
    @Json(name = "column_position") val columnPosition: Int? = null,
    val name: String,
    val type: String,
    val value: String? = null,
    @Json(name = "not_applicable") val notApplicable: Boolean = false,
)

interface ProtocolStepApiService {
    @GET("api/step/")
    suspend fun getProtocolSteps(
        @Query("offset") offset: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("protocol") protocolId: Int? = null,
        @Query("step_section") sectionId: Int? = null,
        @Query("ordering") ordering: String = "id"
    ): LimitOffsetResponse<ProtocolStep>

    @GET("api/step/{id}/")
    suspend fun getProtocolStepById(@Path("id") id: Int): ProtocolStep

    @POST("api/step/")
    suspend fun createProtocolStep(@Body protocolStep: ProtocolStep): ProtocolStep

    @PATCH("api/step/{id}/")
    suspend fun updateProtocolStep(@Path("id") id: Int, @Body protocolStep: ProtocolStep): ProtocolStep

    @DELETE("api/step/{id}/")
    suspend fun deleteProtocolStep(@Path("id") id: Int): Response<Unit>

    @PATCH("api/step/{id}/move_up/")
    suspend fun moveStepUp(@Path("id") id: Int): ProtocolStep

    @PATCH("api/step/{id}/move_down/")
    suspend fun moveStepDown(@Path("id") id: Int): ProtocolStep

    @POST("api/step/{id}/get_timekeeper/")
    suspend fun getTimeKeeper(@Path("id") id: Int, @Body request: StepTimeKeeperRequest): TimeKeeper

    @POST("api/step/{id}/add_protocol_reagent/")
    suspend fun addProtocolReagent(@Path("id") id: Int, @Body request: StepReagentRequest): StepReagent

    @POST("api/step/{id}/remove_protocol_reagent/")
    suspend fun removeProtocolReagent(@Path("id") id: Int, @Body request: StepReagentRemoveRequest): StepReagent

    @POST("api/step/{id}/update_protocol_reagent/")
    suspend fun updateProtocolReagent(@Path("id") id: Int, @Body request: StepReagentUpdateRequest): StepReagent

    @POST("api/step/{id}/add_tag/")
    suspend fun addTagToStep(@Path("id") id: Int, @Body tag: StepTagRequest): StepTag

    @DELETE("api/step/{id}/remove_tag/")
    suspend fun removeTagFromStep(@Path("id") id: Int, @Body tag: StepTagRemoveRequest): Response<Unit>

    @GET("api/step/{id}/get_associated_reagent_actions/")
    suspend fun getAssociatedReagentActions(
        @Path("id") id: Int,
        @Query("session") session: String,
        ): List<ReagentAction>

    @GET("api/step/{id}/export_associated_metadata/")
    suspend fun exportAssociatedMetadata(
        @Path("id") id: Int,
        @Query("session") session: String
    ): List<ExportAssociatedMetadataResponse>

    @POST("api/step/{id}/convert_metadata_to_sdrf_txt/")
    suspend fun convertMetadataToSdrfTxt(
        @Path("id") id: Int,
        @Body request: SDRFConversionRequest
    ): List<List<String>>
}

interface ProtocolStepService {
    suspend fun getProtocolSteps(
        offset: Int? = null,
        limit: Int? = null,
        protocolId: Int? = null,
        sectionId: Int? = null,
        ordering: String = "id"
    ): Result<LimitOffsetResponse<ProtocolStep>>

    suspend fun getProtocolStepById(id: Int): Result<ProtocolStep>
    suspend fun createProtocolStep(protocolStep: ProtocolStep): Result<ProtocolStep>
    suspend fun updateProtocolStep(id: Int, protocolStep: ProtocolStep): Result<ProtocolStep>
    suspend fun deleteProtocolStep(id: Int): Result<Unit>
    suspend fun moveStepUp(id: Int): Result<ProtocolStep>
    suspend fun moveStepDown(id: Int): Result<ProtocolStep>
    suspend fun getTimeKeeper(id: Int, session: String, started: Boolean? = null, startTime: String? = null): Result<TimeKeeper>
    suspend fun addProtocolReagent(id: Int, name: String, unit: String, quantity: Float, scalable: Boolean = false, scalableFactor: Float? = null): Result<StepReagent>
    suspend fun removeProtocolReagent(id: Int, reagentId: Int): Result<StepReagent>
    suspend fun updateProtocolReagent(id: Int, reagentId: Int, quantity: Float, scalable: Boolean = false, scalableFactor: Float? = null): Result<StepReagent>
    suspend fun addTagToStep(id: Int, tag: String): Result<StepTag>
    suspend fun removeTagFromStep(id: Int, tagId: Int): Result<Unit>
    suspend fun getAssociatedReagentActions(id: Int, session: String): Result<List<ReagentAction>>
    suspend fun exportAssociatedMetadata(id: Int, session: String): Result<List<ExportAssociatedMetadataResponse>>
    suspend fun convertMetadataToSdrfTxt(id: Int, request: SDRFConversionRequest): Result<List<List<String>>>
}

@Singleton
class ProtocolStepServiceImpl @Inject constructor(
    private val apiService: ProtocolStepApiService,
    private val protocolStepDao: ProtocolStepDao,
    private val userRepository: UserRepository,
    private val userPreferencesDao: UserPreferencesDao,
    private val annotationDao: AnnotationDao
) : ProtocolStepService {

    override suspend fun getProtocolSteps(
        offset: Int?,
        limit: Int?,
        protocolId: Int?,
        sectionId: Int?,
        ordering: String
    ): Result<LimitOffsetResponse<ProtocolStep>> {
        return try {
            val response = apiService.getProtocolSteps(offset, limit, protocolId, sectionId, ordering)
            // Cache results
            response.results.forEach {
                cacheProtocolStep(it)
            }
            Result.success(response)
        } catch (e: Exception) {
            try {
                val cachedItems = when {
                    protocolId != null -> protocolStepDao.getStepsByProtocol(protocolId, limit?:10, offset?:0)
                        .first()
                    else -> emptyList()
                }
                val totalNumber = when {
                    protocolId != null -> protocolStepDao.countStepsByProtocol(protocolId).first()
                    else -> 0
                }

                val domainObjects = cachedItems.map { it.toDomainModel() }

                val response = LimitOffsetResponse(
                    count = totalNumber,
                    next = null,
                    previous = null,
                    results = domainObjects
                )
                Result.success(response)
            } catch (cacheException: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getProtocolStepById(id: Int): Result<ProtocolStep> {
        return try {
            val response = apiService.getProtocolStepById(id)
            cacheProtocolStep(response)
            Result.success(response)
        } catch (e: Exception) {
            val cachedStep = protocolStepDao.getById(id)
            if (cachedStep != null) {
                Result.success(cachedStep.toDomainModel())
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun createProtocolStep(protocolStep: ProtocolStep): Result<ProtocolStep> {
        return try {
            val response = apiService.createProtocolStep(protocolStep)
            cacheProtocolStep(response)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProtocolStep(id: Int, protocolStep: ProtocolStep): Result<ProtocolStep> {
        return try {
            val response = apiService.updateProtocolStep(id, protocolStep)
            cacheProtocolStep(response)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteProtocolStep(id: Int): Result<Unit> {
        return try {
            apiService.deleteProtocolStep(id)
            protocolStepDao.getById(id)?.let { protocolStepDao.delete(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun moveStepUp(id: Int): Result<ProtocolStep> {
        return try {
            val response = apiService.moveStepUp(id)
            cacheProtocolStep(response)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun moveStepDown(id: Int): Result<ProtocolStep> {
        return try {
            val response = apiService.moveStepDown(id)
            cacheProtocolStep(response)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTimeKeeper(id: Int, session: String, started: Boolean?, startTime: String?): Result<TimeKeeper> {
        return try {
            val request = StepTimeKeeperRequest(session, started, startTime)
            val response = apiService.getTimeKeeper(id, request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addProtocolReagent(id: Int, name: String, unit: String, quantity: Float, scalable: Boolean, scalableFactor: Float?): Result<StepReagent> {
        return try {
            val request = StepReagentRequest(name, unit, quantity, scalable, scalableFactor)
            val response = apiService.addProtocolReagent(id, request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeProtocolReagent(id: Int, reagentId: Int): Result<StepReagent> {
        return try {
            val request = StepReagentRemoveRequest(reagentId)
            val response = apiService.removeProtocolReagent(id, request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateProtocolReagent(id: Int, reagentId: Int, quantity: Float, scalable: Boolean, scalableFactor: Float?): Result<StepReagent> {
        return try {
            val request = StepReagentUpdateRequest(reagentId, quantity, scalable, scalableFactor)
            val response = apiService.updateProtocolReagent(id, request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addTagToStep(id: Int, tag: String): Result<StepTag> {
        return try {
            val request = StepTagRequest(tag)
            val response = apiService.addTagToStep(id, request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeTagFromStep(id: Int, tagId: Int): Result<Unit> {
        return try {
            val request = StepTagRemoveRequest(tagId)
            apiService.removeTagFromStep(id, request)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAssociatedReagentActions(id: Int, session: String): Result<List<ReagentAction>> {
        return try {
            val response = apiService.getAssociatedReagentActions(id, session)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun exportAssociatedMetadata(id: Int, session: String): Result<List<ExportAssociatedMetadataResponse>> {
        return try {
            val response = apiService.exportAssociatedMetadata(id, session)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun convertMetadataToSdrfTxt(id: Int, request: SDRFConversionRequest): Result<List<List<String>>> {
        return try {

            val response = apiService.convertMetadataToSdrfTxt(id, request)
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun cacheProtocolStep(protocolStep: ProtocolStep) {
        // First insert/update the protocol step entity
        protocolStepDao.insertStep(ProtocolStepEntity(
            id = protocolStep.id,
            protocol = protocolStep.protocol,
            stepId = protocolStep.stepId?.toLong(),
            description = protocolStep.stepDescription ?: "",
            stepSection = protocolStep.stepSection,
            duration = protocolStep.stepDuration,
            previousStep = protocolStep.previousStep,
            original = true, // Default value, adjust as needed
            branchFrom = null, // Default value, adjust as needed
            remoteId = null, // Default value, adjust as needed
            createdAt = protocolStep.createdAt,
            updatedAt = protocolStep.updatedAt,
            remoteHost = null // Default value, adjust as needed
        ))

        // Clear existing next step relations for this step
        protocolStepDao.clearNextStepsForStep(protocolStep.id)

        // Add new next step relations
        protocolStep.nextStep?.forEach { nextStepId ->
            protocolStepDao.addNextStepRelation(
                ProtocolStepNextRelation(
                    fromStep = protocolStep.id,
                    toStep = nextStepId
                )
            )
        }
    }

    private suspend fun ProtocolStepEntity.toDomainModel(): ProtocolStep {
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
            original = true,
            branchFrom = null,
            remoteId = null,
            createdAt = createdAt,
            updatedAt = updatedAt,
            remoteHost = null
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
}