package info.proteo.cupcake.data.repository

import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import info.proteo.cupcake.shared.data.model.protocol.ProtocolTag
import info.proteo.cupcake.shared.data.model.protocol.StepTag
import info.proteo.cupcake.shared.data.model.tag.Tag
import info.proteo.cupcake.data.remote.service.TagService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TagRepository @Inject constructor(
    private val tagService: TagService
) {
    suspend fun getTags(
        offset: Int? = null,
        limit: Int? = null,
        search: String? = null,
        ordering: String? = null,
        tagName: String? = null
    ): Result<LimitOffsetResponse<Tag>> {
        return tagService.getTags(offset, limit, search, ordering, tagName)
    }

    suspend fun createTag(tagValue: String): Result<Tag> {
        return tagService.createTag(tagValue)
    }

    suspend fun getTagById(id: Int): Result<Tag> {
        return tagService.getTagById(id)
    }

    fun getTagByIdFlow(id: Int): Flow<Tag?> {
        return tagService.getTagByIdFlow(id)
    }

    fun getAllTagsFlow(): Flow<List<Tag>> {
        return tagService.getAllTagsFlow()
    }

    suspend fun updateTag(id: Int, tagValue: String): Result<Tag> {
        return tagService.updateTag(id, tagValue)
    }

    suspend fun deleteTag(id: Int): Result<Unit> {
        return tagService.deleteTag(id)
    }

    suspend fun getTagByName(tagName: String): Result<Tag?> {
        return tagService.getTagByName(tagName)
    }

    suspend fun getProtocolTags(
        offset: Int? = null,
        limit: Int? = null,
        protocolId: Int? = null,
        tagId: Int? = null,
        search: String? = null,
        ordering: String? = null
    ): Result<LimitOffsetResponse<ProtocolTag>> {
        return tagService.getProtocolTags(offset, limit, protocolId, tagId, search, ordering)
    }

    suspend fun createProtocolTag(protocolId: Int, tagId: Int): Result<ProtocolTag> {
        return tagService.createProtocolTag(protocolId, tagId)
    }

    suspend fun getProtocolTagById(id: Int): Result<ProtocolTag> {
        return tagService.getProtocolTagById(id)
    }

    suspend fun updateProtocolTag(id: Int, newTagId: Int): Result<ProtocolTag> {
        return tagService.updateProtocolTag(id, newTagId)
    }

    suspend fun deleteProtocolTag(id: Int): Result<Unit> {
        return tagService.deleteProtocolTag(id)
    }

    fun getTagsByProtocolFlow(protocolId: Int): Flow<List<ProtocolTag>> {
        return tagService.getTagsByProtocolFlow(protocolId)
    }

    suspend fun getStepTags(
        offset: Int? = null,
        limit: Int? = null,
        stepId: Int? = null,
        tagId: Int? = null,
        search: String? = null,
        ordering: String? = null
    ): Result<LimitOffsetResponse<StepTag>> {
        return tagService.getStepTags(offset, limit, stepId, tagId, search, ordering)
    }

    suspend fun createStepTag(stepId: Int, tagId: Int): Result<StepTag> {
        return tagService.createStepTag(stepId, tagId)
    }

    suspend fun getStepTagById(id: Int): Result<StepTag> {
        return tagService.getStepTagById(id)
    }

    suspend fun updateStepTag(id: Int, newTagId: Int): Result<StepTag> {
        return tagService.updateStepTag(id, newTagId)
    }

    suspend fun deleteStepTag(id: Int): Result<Unit> {
        return tagService.deleteStepTag(id)
    }

    fun getTagsByStepFlow(stepId: Int): Flow<List<StepTag>> {
        return tagService.getTagsByStepFlow(stepId)
    }
}