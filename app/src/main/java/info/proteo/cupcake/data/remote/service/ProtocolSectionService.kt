package info.proteo.cupcake.data.remote.service

import com.squareup.moshi.Json
import info.proteo.cupcake.data.local.dao.protocol.ProtocolSectionDao
import info.proteo.cupcake.data.local.dao.protocol.ProtocolStepDao
import info.proteo.cupcake.data.local.entity.protocol.ProtocolSectionEntity
import info.proteo.cupcake.data.local.entity.protocol.ProtocolStepEntity
import info.proteo.cupcake.data.local.entity.protocol.ProtocolStepNextRelation
import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import info.proteo.cupcake.shared.data.model.protocol.ProtocolSection
import info.proteo.cupcake.shared.data.model.protocol.ProtocolStep
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.map
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

data class UpdateSectionStepsRequest(
    @Json(name = "steps") val steps: List<ProtocolStep>
)

interface ProtocolSectionApiService {
    @GET("api/section/")
    suspend fun getProtocolSections(
        @Query("offset") offset: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("protocol") protocolId: Int? = null,
        @Query("ordering") ordering: String? = null
    ): LimitOffsetResponse<ProtocolSection>

    @POST("api/section/")
    suspend fun createProtocolSection(@Body protocolSection: ProtocolSection): ProtocolSection

    @GET("api/section/{id}/")
    suspend fun getProtocolSectionById(@Path("id") id: Int): ProtocolSection

    @PUT("api/section/{id}/")
    suspend fun updateProtocolSection(@Path("id") id: Int, @Body protocolSection: ProtocolSection): ProtocolSection

    @PATCH("api/section/{id}/")
    suspend fun partialUpdateProtocolSection(@Path("id") id: Int, @Body protocolSection: Map<String, @JvmSuppressWildcards Any>): ProtocolSection

    @DELETE("api/section/{id}/")
    suspend fun deleteProtocolSection(@Path("id") id: Int): Response<Unit>

    @GET("api/section/{id}/get_steps/")
    suspend fun getSectionSteps(@Path("id") sectionId: Int): List<ProtocolStep>

    @PATCH("api/section/{id}/update_steps/")
    suspend fun updateSectionSteps(@Path("id") sectionId: Int, @Body request: UpdateSectionStepsRequest): List<ProtocolStep>
}


interface ProtocolSectionService {
    suspend fun getProtocolSections(
        offset: Int? = null,
        limit: Int? = null,
        protocolId: Int? = null,
        ordering: String? = null
    ): Result<LimitOffsetResponse<ProtocolSection>>

    suspend fun createProtocolSection(protocolSection: ProtocolSection): Result<ProtocolSection>
    suspend fun getProtocolSectionById(id: Int): Result<ProtocolSection>
    suspend fun updateProtocolSection(id: Int, protocolSection: ProtocolSection): Result<ProtocolSection>
    suspend fun partialUpdateProtocolSection(id: Int, protocolSectionData: Map<String, Any>): Result<ProtocolSection>
    suspend fun deleteProtocolSection(id: Int): Result<Unit>

    fun getProtocolSectionByIdFlow(id: Int): Flow<ProtocolSection?>
    fun getSectionsByProtocolFlow(protocolId: Int): Flow<List<ProtocolSection>>

    suspend fun getSectionSteps(sectionId: Int): Result<List<ProtocolStep>>
    suspend fun updateSectionSteps(sectionId: Int, steps: List<ProtocolStep>): Result<List<ProtocolStep>>
}

@Singleton
class ProtocolSectionServiceImpl @Inject constructor(
    private val apiService: ProtocolSectionApiService,
    private val protocolSectionDao: ProtocolSectionDao,
    private val protocolStepDao: ProtocolStepDao // For caching steps if needed from getSectionSteps
) : ProtocolSectionService {

    override suspend fun getProtocolSections(
        offset: Int?,
        limit: Int?,
        protocolId: Int?,
        ordering: String?
    ): Result<LimitOffsetResponse<ProtocolSection>> {
        return try {
            val response = apiService.getProtocolSections(offset, limit, protocolId, ordering)
            response.results.forEach { cacheProtocolSection(it) }
            Result.success(response)
        } catch (e: Exception) {
            if (protocolId != null) {
                try {
                    val cachedSections = protocolSectionDao.getSectionsByProtocol(protocolId, limit?:50, offset?:0).firstOrNull() ?: emptyList()
                    val domainObjects = cachedSections.map { it.toDomainModel() }
                    Result.success(LimitOffsetResponse(count = domainObjects.size, next = null, previous = null, results = domainObjects))
                } catch (cacheEx: Exception) {
                    Result.failure(e)
                }
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun createProtocolSection(protocolSection: ProtocolSection): Result<ProtocolSection> {
        return try {
            val newSection = apiService.createProtocolSection(protocolSection)
            cacheProtocolSection(newSection)
            Result.success(newSection)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProtocolSectionById(id: Int): Result<ProtocolSection> {
        return try {
            val section = apiService.getProtocolSectionById(id)
            cacheProtocolSection(section)
            Result.success(section)
        } catch (e: Exception) {
            val cached = protocolSectionDao.getById(id)
            if (cached != null) {
                Result.success(cached.toDomainModel())
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateProtocolSection(id: Int, protocolSection: ProtocolSection): Result<ProtocolSection> {
        return try {
            val updatedSection = apiService.updateProtocolSection(id, protocolSection)
            cacheProtocolSection(updatedSection)
            Result.success(updatedSection)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun partialUpdateProtocolSection(id: Int, protocolSectionData: Map<String, Any>): Result<ProtocolSection> {
        return try {
            val updatedSection = apiService.partialUpdateProtocolSection(id, protocolSectionData)
            cacheProtocolSection(updatedSection)
            Result.success(updatedSection)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteProtocolSection(id: Int): Result<Unit> {
        return try {
            apiService.deleteProtocolSection(id)
            protocolSectionDao.getById(id)?.let { protocolSectionDao.delete(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getProtocolSectionByIdFlow(id: Int): Flow<ProtocolSection?> {
        return protocolSectionDao.getByIdFlow(id).map { it?.toDomainModel() }
    }

    override fun getSectionsByProtocolFlow(protocolId: Int): Flow<List<ProtocolSection>> {
        return protocolSectionDao.getSectionsByProtocol(protocolId).map { list ->
            list.map { it.toDomainModel() }
        }
    }

    override suspend fun getSectionSteps(sectionId: Int): Result<List<ProtocolStep>> {
        return try {
            val steps = apiService.getSectionSteps(sectionId)
            steps.forEach { step ->
                protocolStepDao.insert(step.toEntity())
            }
            Result.success(steps)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateSectionSteps(sectionId: Int, steps: List<ProtocolStep>): Result<List<ProtocolStep>> {
        return try {
            val updatedSteps = apiService.updateSectionSteps(sectionId, UpdateSectionStepsRequest(steps))
            updatedSteps.forEach { step ->
                protocolStepDao.insert(step.toEntity())
            }
            getProtocolSectionById(sectionId)
            Result.success(updatedSteps)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun cacheProtocolSection(section: ProtocolSection) {
        protocolSectionDao.insert(section.toEntity())
    }

    private fun ProtocolSection.toEntity(): ProtocolSectionEntity {
        return ProtocolSectionEntity(
            id = id,
            protocol = protocol,
            sectionDescription = sectionDescription,
            sectionDuration = sectionDuration,
            createdAt = createdAt,
            updatedAt = updatedAt,
            remoteId = null,
            remoteHost = null
        )
    }

    private fun ProtocolSectionEntity.toDomainModel(): ProtocolSection {
        return ProtocolSection(
            id = id,
            protocol = protocol,
            sectionDescription = sectionDescription,
            sectionDuration = sectionDuration,
            createdAt = createdAt,
            updatedAt = updatedAt
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

    // Helper function to create the next step relations
    fun ProtocolStep.toNextStepRelations(): List<ProtocolStepNextRelation> {
        return nextStep?.map { nextStepId ->
            ProtocolStepNextRelation(
                fromStep = id,
                toStep = nextStepId
            )
        } ?: emptyList()
    }
}