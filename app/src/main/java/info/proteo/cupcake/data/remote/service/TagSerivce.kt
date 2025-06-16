package info.proteo.cupcake.data.remote.service

import com.squareup.moshi.Json
import info.proteo.cupcake.data.local.dao.protocol.ProtocolTagDao
import info.proteo.cupcake.data.local.dao.protocol.StepTagDao
import info.proteo.cupcake.data.local.dao.tag.TagDao
import info.proteo.cupcake.data.local.entity.protocol.ProtocolTagEntity
import info.proteo.cupcake.data.local.entity.protocol.StepTagEntity
import info.proteo.cupcake.data.local.entity.tag.TagEntity
import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import info.proteo.cupcake.shared.data.model.protocol.ProtocolTag
import info.proteo.cupcake.shared.data.model.protocol.StepTag
import info.proteo.cupcake.shared.data.model.tag.Tag
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
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

// For creating/updating a Tag
data class TagCreateUpdateRequest(
    val tag: String
)

// For creating a ProtocolTag (association between Protocol and Tag)
data class ProtocolTagCreateRequest(
    val protocol: Int, // ID of the protocol
    val tag: Int       // ID of the tag
)

// For updating a ProtocolTag
data class ProtocolTagUpdateRequest(
    val tag: Int       // ID of the new tag to associate
)

// For creating a StepTag (association between ProtocolStep and Tag)
data class StepTagCreateRequest(
    val step: Int,     // ID of the step
    val tag: Int       // ID of the tag
)

// For updating a StepTag
data class StepTagUpdateRequest(
    val tag: Int       // ID of the new tag to associate
)


interface TagApiService {
    // Tag endpoints
    @GET("api/tag/")
    suspend fun getTags(
        @Query("offset") offset: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("search") search: String? = null,
        @Query("ordering") ordering: String? = null,
        @Query("tag") tagName: String? = null // For filterset_fields = ['tag']
    ): LimitOffsetResponse<Tag>

    @POST("api/tag/")
    suspend fun createTag(@Body request: TagCreateUpdateRequest): Tag

    @GET("api/tag/{id}/")
    suspend fun getTagById(@Path("id") id: Int): Tag

    @PUT("api/tag/{id}/")
    suspend fun updateTag(@Path("id") id: Int, @Body request: TagCreateUpdateRequest): Tag

    @DELETE("api/tag/{id}/")
    suspend fun deleteTag(@Path("id") id: Int): Response<Unit>

    // ProtocolTag endpoints
    @GET("api/protocol_tag/")
    suspend fun getProtocolTags(
        @Query("offset") offset: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("protocol") protocolId: Int? = null,
        @Query("tag") tagId: Int? = null, // For filterset_fields = ['tag'] (tag_id)
        @Query("search") search: String? = null, // For search_fields = ['tag'] (tag_name via related tag)
        @Query("ordering") ordering: String? = null
    ): LimitOffsetResponse<ProtocolTag>

    @POST("api/protocol_tag/")
    suspend fun createProtocolTag(@Body request: ProtocolTagCreateRequest): ProtocolTag

    @GET("api/protocol_tag/{id}/")
    suspend fun getProtocolTagById(@Path("id") id: Int): ProtocolTag

    @PUT("api/protocol_tag/{id}/")
    suspend fun updateProtocolTag(@Path("id") id: Int, @Body request: ProtocolTagUpdateRequest): ProtocolTag

    @DELETE("api/protocol_tag/{id}/")
    suspend fun deleteProtocolTag(@Path("id") id: Int): Response<Unit>

    // StepTag endpoints
    @GET("api/step_tag/")
    suspend fun getStepTags(
        @Query("offset") offset: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("step") stepId: Int? = null,
        @Query("tag") tagId: Int? = null, // For filterset_fields = ['tag'] (tag_id)
        @Query("search") search: String? = null, // For search_fields = ['tag'] (tag_name via related tag)
        @Query("ordering") ordering: String? = null
    ): LimitOffsetResponse<StepTag>

    @POST("api/step_tag/")
    suspend fun createStepTag(@Body request: StepTagCreateRequest): StepTag

    @GET("api/step_tag/{id}/")
    suspend fun getStepTagById(@Path("id") id: Int): StepTag

    @PUT("api/step_tag/{id}/")
    suspend fun updateStepTag(@Path("id") id: Int, @Body request: StepTagUpdateRequest): StepTag

    @DELETE("api/step_tag/{id}/")
    suspend fun deleteStepTag(@Path("id") id: Int): Response<Unit>
}

interface TagService {
    // Tag methods
    suspend fun getTags(
        offset: Int? = null, limit: Int? = null, search: String? = null,
        ordering: String? = null, tagName: String? = null
    ): Result<LimitOffsetResponse<Tag>>
    suspend fun createTag(tagValue: String): Result<Tag>
    suspend fun getTagById(id: Int): Result<Tag>
    fun getTagByIdFlow(id: Int): Flow<Tag?>
    fun getAllTagsFlow(): Flow<List<Tag>>
    suspend fun updateTag(id: Int, tagValue: String): Result<Tag>
    suspend fun deleteTag(id: Int): Result<Unit>
    suspend fun getTagByName(tagName: String): Result<Tag?>


    // ProtocolTag methods
    suspend fun getProtocolTags(
        offset: Int? = null, limit: Int? = null, protocolId: Int? = null,
        tagId: Int? = null, search: String? = null, ordering: String? = null
    ): Result<LimitOffsetResponse<ProtocolTag>>
    suspend fun createProtocolTag(protocolId: Int, tagId: Int): Result<ProtocolTag>
    suspend fun getProtocolTagById(id: Int): Result<ProtocolTag>
    suspend fun updateProtocolTag(id: Int, newTagId: Int): Result<ProtocolTag>
    suspend fun deleteProtocolTag(id: Int): Result<Unit>
    fun getTagsByProtocolFlow(protocolId: Int): Flow<List<ProtocolTag>>


    // StepTag methods
    suspend fun getStepTags(
        offset: Int? = null, limit: Int? = null, stepId: Int? = null,
        tagId: Int? = null, search: String? = null, ordering: String? = null
    ): Result<LimitOffsetResponse<StepTag>>
    suspend fun createStepTag(stepId: Int, tagId: Int): Result<StepTag>
    suspend fun getStepTagById(id: Int): Result<StepTag>
    suspend fun updateStepTag(id: Int, newTagId: Int): Result<StepTag>
    suspend fun deleteStepTag(id: Int): Result<Unit>
    fun getTagsByStepFlow(stepId: Int): Flow<List<StepTag>>
}

@Singleton
class TagServiceImpl @Inject constructor(
    private val apiService: TagApiService,
    private val tagDao: TagDao,
    private val protocolTagDao: ProtocolTagDao,
    private val stepTagDao: StepTagDao
) : TagService {

    // Tag methods
    override suspend fun getTags(
        offset: Int?, limit: Int?, search: String?,
        ordering: String?, tagName: String?
    ): Result<LimitOffsetResponse<Tag>> {
        return try {
            val response = apiService.getTags(offset, limit, search, ordering, tagName)
            response.results.forEach { cacheTag(it) }
            Result.success(response)
        } catch (e: Exception) {
            try {
                // Basic offline support: return all cached tags, no pagination/filtering from cache
                if (tagName != null) {
                    val cachedTag = tagDao.getByName(tagName)
                    return if (cachedTag != null) {
                        Result.success(LimitOffsetResponse(count = 1, next = null, previous = null, results = listOf(cachedTag.toDomainModel())))
                    } else {
                        Result.success(LimitOffsetResponse(count = 0, next = null, previous = null, results = emptyList()))
                    }
                } else if (search != null) {
                    val cachedItems = tagDao.searchByName(search).firstOrNull()
                    if (cachedItems != null) {
                        val domainObjects = cachedItems.map { it.toDomainModel() }
                        return Result.success(LimitOffsetResponse(count = domainObjects.size, next = null, previous = null, results = domainObjects))
                    } else {
                        return Result.success(LimitOffsetResponse(count = 0, next = null, previous = null, results = emptyList()))
                    }
                } else {
                    val cachedItems = tagDao.getAllTags().firstOrNull() ?: emptyList()
                    val domainObjects = cachedItems.map { it.toDomainModel() }
                    return Result.success(LimitOffsetResponse(count = domainObjects.size, next = null, previous = null, results = domainObjects))
                }

            } catch (cacheEx: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun createTag(tagValue: String): Result<Tag> {
        return try {
            val newTag = apiService.createTag(TagCreateUpdateRequest(tagValue))
            cacheTag(newTag)
            Result.success(newTag)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTagById(id: Int): Result<Tag> {
        return try {
            val tag = apiService.getTagById(id)
            cacheTag(tag)
            Result.success(tag)
        } catch (e: Exception) {
            tagDao.getById(id).firstOrNull().let {
                if (it != null) {
                    Result.success(it.toDomainModel())
                } else {
                    Result.failure(e)
                }
            }
        }
    }

    override fun getTagByIdFlow(id: Int): Flow<Tag?> {
        return tagDao.getById(id).map { it?.toDomainModel() }
    }

    override fun getAllTagsFlow(): Flow<List<Tag>> {
        return tagDao.getAllTags().map { list -> list.map { it.toDomainModel() } }
    }

    override suspend fun updateTag(id: Int, tagValue: String): Result<Tag> {
        return try {
            val updatedTag = apiService.updateTag(id, TagCreateUpdateRequest(tagValue))
            cacheTag(updatedTag)
            Result.success(updatedTag)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTag(id: Int): Result<Unit> {
        return try {
            apiService.deleteTag(id)
            val tag = tagDao.getById(id).firstOrNull()
            tag?.let { tagDao.delete(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getTagByName(tagName: String): Result<Tag?> {
        return try {
            val apiResult = apiService.getTags(limit = 1, tagName = tagName)
            if (apiResult.results.isNotEmpty()) {
                val tag = apiResult.results.first()
                cacheTag(tag)
                Result.success(tag)
            } else {
                tagDao.getByName(tagName)?.let { Result.success(it.toDomainModel()) }
                    ?: Result.success(null)
            }
        } catch (e: Exception) {
            try {
                tagDao.getByName(tagName)?.let { Result.success(it.toDomainModel()) }
                    ?: Result.failure(e)
            } catch (cacheEx: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getProtocolTags(
        offset: Int?, limit: Int?, protocolId: Int?,
        tagId: Int?, search: String?, ordering: String?
    ): Result<LimitOffsetResponse<ProtocolTag>> {
        return try {
            val response = apiService.getProtocolTags(offset, limit, protocolId, tagId, search, ordering)
            response.results.forEach { protocolTag ->
                tagDao.insert(protocolTag.tag.toEntity())
                protocolTagDao.insert(protocolTag.toEntity())
            }
            Result.success(response)
        } catch (e: Exception) {
            try {
                val cachedProtocolTagEntities: List<ProtocolTagEntity> = when {
                    protocolId != null && search != null -> {
                        val matchingTags = tagDao.searchByName(search).firstOrNull() ?: emptyList()
                        val matchingTagIds = matchingTags.map { it.id }
                        if (matchingTagIds.isNotEmpty()) {
                            val protocolTagsForProtocol = protocolTagDao.getByProtocol(protocolId).firstOrNull() ?: emptyList()
                            protocolTagsForProtocol.filter { ptEntity -> ptEntity.tag in matchingTagIds }
                        } else {
                            emptyList()
                        }
                    }
                    protocolId != null -> {
                        protocolTagDao.getByProtocol(protocolId).firstOrNull() ?: emptyList()
                    }
                    tagId != null -> {
                        protocolTagDao.getByTag(tagId).firstOrNull() ?: emptyList()
                    }
                    search != null -> {
                        val matchingTags = tagDao.searchByName(search).firstOrNull() ?: emptyList()
                        val matchingTagIds = matchingTags.map { it.id }
                        val result = mutableListOf<ProtocolTagEntity>()
                        if (matchingTagIds.isNotEmpty()) {
                            matchingTagIds.forEach { mTagId ->
                                result.addAll(protocolTagDao.getByTag(mTagId).firstOrNull() ?: emptyList())
                            }
                        }
                        result
                    }
                    else -> {
                        emptyList()
                    }
                }

                val domainObjects = mutableListOf<ProtocolTag>()
                for (ptEntity in cachedProtocolTagEntities) {
                    val associatedTagEntity = tagDao.getById(ptEntity.tag).firstOrNull()
                    if (associatedTagEntity != null) {
                        domainObjects.add(
                            ProtocolTag(
                                id = ptEntity.id,
                                protocol = ptEntity.protocol,
                                tag = associatedTagEntity.toDomainModel(),
                                createdAt = ptEntity.createdAt,
                                updatedAt = ptEntity.updatedAt
                            )
                        )
                    }
                }

                val paginatedResults = if (offset != null && limit != null) {
                    domainObjects.drop(offset).take(limit)
                } else if (limit != null) {
                    domainObjects.take(limit)
                } else if (offset != null) { // if only offset is provided, take all after offset
                    domainObjects.drop(offset)
                }
                else {
                    domainObjects
                }

                Result.success(
                    LimitOffsetResponse(
                        count = domainObjects.size,
                        next = null,
                        previous = null,
                        results = paginatedResults
                    )
                )
            } catch (cacheException: Exception) {
                Result.failure(e) // Propagate original network exception
            }
        }
    }

    override suspend fun createProtocolTag(protocolId: Int, tagId: Int): Result<ProtocolTag> {
        return try {
            val request = ProtocolTagCreateRequest(protocolId, tagId)
            val newProtocolTag = apiService.createProtocolTag(request)
            cacheProtocolTag(newProtocolTag)
            Result.success(newProtocolTag)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getProtocolTagById(id: Int): Result<ProtocolTag> {
        return try {
            val protocolTagFromApi = apiService.getProtocolTagById(id)

            val tagToCache = protocolTagFromApi.tag
            tagDao.insert(
                TagEntity(
                    id = tagToCache.id,
                    tag = tagToCache.tag,
                    createdAt = tagToCache.createdAt,
                    updatedAt = tagToCache.updatedAt
                )
            )
            protocolTagDao.insert(
                ProtocolTagEntity(
                    id = protocolTagFromApi.id,
                    protocol = protocolTagFromApi.protocol,
                    tag = protocolTagFromApi.tag.id,
                    createdAt = protocolTagFromApi.createdAt,
                    updatedAt = protocolTagFromApi.updatedAt
                )
            )
            Result.success(protocolTagFromApi)
        } catch (e: Exception) {
            val cachedProtocolTagEntity = protocolTagDao.getById(id)

            if (cachedProtocolTagEntity != null) {
                val associatedTagEntity = tagDao.getById(cachedProtocolTagEntity.tag).firstOrNull() // cachedProtocolTagEntity.tag is the tagId

                if (associatedTagEntity != null) {
                    val domainTag = Tag(
                        id = associatedTagEntity.id,
                        tag = associatedTagEntity.tag,
                        createdAt = associatedTagEntity.createdAt,
                        updatedAt = associatedTagEntity.updatedAt
                    )
                    val domainProtocolTag = ProtocolTag(
                        id = cachedProtocolTagEntity.id,
                        protocol = cachedProtocolTagEntity.protocol,
                        tag = domainTag,
                        createdAt = cachedProtocolTagEntity.createdAt,
                        updatedAt = cachedProtocolTagEntity.updatedAt
                    )
                    Result.success(domainProtocolTag)
                } else {
                    Result.failure(
                        Exception(
                            "Cache inconsistency: Found ProtocolTagEntity (id=${cachedProtocolTagEntity.id}) " +
                                    "but missing associated TagEntity (id=${cachedProtocolTagEntity.tag}).",
                            e
                        )
                    )
                }
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateProtocolTag(id: Int, newTagId: Int): Result<ProtocolTag> {
        return try {
            val request = ProtocolTagUpdateRequest(newTagId)
            val updatedProtocolTag = apiService.updateProtocolTag(id, request)
            cacheProtocolTag(updatedProtocolTag)
            Result.success(updatedProtocolTag)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteProtocolTag(id: Int): Result<Unit> {
        return try {
            apiService.deleteProtocolTag(id)
            protocolTagDao.getById(id)?.let { protocolTagDao.delete(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getTagsByProtocolFlow(protocolId: Int): Flow<List<ProtocolTag>> {
        return protocolTagDao.getByProtocol(protocolId)
            .flatMapLatest { protocolTagEntities ->
                if (protocolTagEntities.isEmpty()) {
                    flowOf(emptyList<ProtocolTag>())
                } else {
                    val tagEntityFlows: List<Flow<TagEntity?>> = protocolTagEntities.map { ptEntity ->
                        tagDao.getById(ptEntity.tag)
                    }

                    combine(tagEntityFlows) { arrayOfTagEntities: Array<TagEntity?> ->
                        protocolTagEntities.mapIndexedNotNull { index, ptEntity ->
                            val tagEntity = arrayOfTagEntities[index]
                            tagEntity?.let {
                                val tagModel = it.toDomainModel()
                                ptEntity.toDomainModel(tagModel)
                            }
                        }
                    }
                }
            }
    }

    override suspend fun getStepTags(
        offset: Int?,
        limit: Int?,
        stepId: Int?,
        tagId: Int?,
        search: String?,
        ordering: String?
    ): Result<LimitOffsetResponse<StepTag>> {
        return try {
            val response = apiService.getStepTags(offset, limit, stepId, tagId, search, ordering)
            response.results.forEach { stepTag ->
                tagDao.insert(stepTag.tag.toEntity())
                stepTagDao.insert(stepTag.toEntity())
            }
            Result.success(response)
        } catch (e: Exception) {
            return try {
                val cachedEntities = stepTagDao.getByStep(stepId ?: 0).first()
                val domainList = cachedEntities.map { stEntity ->
                    val tagEntity = tagDao.getById(stEntity.tag).firstOrNull()
                        ?: throw IllegalStateException("TagEntity missing for id=${stEntity.tag}")
                    val tagModel = tagEntity.toDomainModel()
                    stEntity.toDomainModel(tagModel)
                }
                Result.success(
                    LimitOffsetResponse(
                        count = domainList.size,
                        next = null,
                        previous = null,
                        results = domainList
                    )
                )
            } catch (_: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun createStepTag(stepId: Int, tagId: Int): Result<StepTag> {
        return try {
            val request = StepTagCreateRequest(stepId, tagId)
            val newStepTag = apiService.createStepTag(request)
            cacheStepTag(newStepTag)
            Result.success(newStepTag)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getStepTagById(id: Int): Result<StepTag> {
        return try {
            val stepTagFromApi = apiService.getStepTagById(id)
            cacheStepTag(stepTagFromApi)
            Result.success(stepTagFromApi)
        } catch (e: Exception) {
            val cachedStepTagEntity = stepTagDao.getById(id)
            if (cachedStepTagEntity != null) {
                val tagEntity = tagDao.getById(cachedStepTagEntity.tag).firstOrNull()
                if (tagEntity != null) {
                    Result.success(
                        StepTag(
                            id = cachedStepTagEntity.id,
                            step = cachedStepTagEntity.step,
                            tag = tagEntity.toDomainModel(),
                            createdAt = cachedStepTagEntity.createdAt,
                            updatedAt = cachedStepTagEntity.updatedAt
                        )
                    )
                } else {
                    Result.failure(Exception("Associated Tag (ID: ${cachedStepTagEntity.tag}) not found in cache for StepTag ID: $id", e))
                }
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun updateStepTag(id: Int, newTagId: Int): Result<StepTag> {
        return try {
            val request = StepTagUpdateRequest(newTagId)
            val updatedStepTag = apiService.updateStepTag(id, request)
            cacheStepTag(updatedStepTag)
            Result.success(updatedStepTag)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteStepTag(id: Int): Result<Unit> {
        return try {
            apiService.deleteStepTag(id)
            stepTagDao.getById(id)?.let { stepTagDao.delete(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override fun getTagsByStepFlow(stepId: Int): Flow<List<StepTag>> {
        return stepTagDao.getByStep(stepId) // Flow<List<StepTagEntity>>
            .flatMapLatest { stepTagEntities ->
                if (stepTagEntities.isEmpty()) {
                    flowOf(emptyList<StepTag>())
                } else {
                    val stepTagWithTagDetailFlows: List<Flow<Pair<StepTagEntity, Tag?>>> = stepTagEntities.map { stEntity ->
                        tagDao.getById(stEntity.tag)
                            .map { tagEntity ->
                                stEntity to tagEntity?.toDomainModel()
                            }
                    }

                    combine(stepTagWithTagDetailFlows) { arrayOfPairs ->
                        arrayOfPairs.map { (stepTagEntity, resolvedTagDomainModel) ->
                            val finalTag = resolvedTagDomainModel ?: Tag(
                                id = stepTagEntity.tag,
                                tag = "Tag Data Unavailable",
                                createdAt = null,
                                updatedAt = null
                            )
                            stepTagEntity.toDomainModel(finalTag)
                        }
                    }
                }
            }
    }


    private suspend fun cacheTag(tag: Tag) {
        tagDao.insert(tag.toEntity())
    }

    private suspend fun cacheProtocolTag(protocolTag: ProtocolTag) {
        cacheTag(protocolTag.tag) // Cache the nested Tag object
        protocolTagDao.insert(protocolTag.toEntity())
    }

    private suspend fun cacheStepTag(stepTag: StepTag) {
        cacheTag(stepTag.tag) // Cache the nested Tag object
        stepTagDao.insert(stepTag.toEntity())
    }

    private fun Tag.toEntity(): TagEntity = TagEntity(id, tag, createdAt, updatedAt)
    private fun TagEntity.toDomainModel(): Tag = Tag(id, tag, createdAt, updatedAt)

    private fun ProtocolTag.toEntity(): ProtocolTagEntity = ProtocolTagEntity(id, protocol, tag.id, createdAt, updatedAt)
    private fun ProtocolTagEntity.toDomainModel(resolvedTag: Tag): ProtocolTag = ProtocolTag(id, protocol, resolvedTag, createdAt, updatedAt)

    private fun StepTag.toEntity(): StepTagEntity = StepTagEntity(id, step, tag.id, createdAt, updatedAt)
    private fun StepTagEntity.toDomainModel(resolvedTag: Tag): StepTag = StepTag(id, step, resolvedTag, createdAt, updatedAt)
}