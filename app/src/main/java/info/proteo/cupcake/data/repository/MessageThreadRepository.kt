package info.proteo.cupcake.data.repository

import info.proteo.cupcake.data.remote.model.LimitOffsetResponse
import info.proteo.cupcake.data.remote.model.message.MessageThread
import info.proteo.cupcake.data.remote.model.message.MessageThreadDetail
import info.proteo.cupcake.data.remote.service.MessageThreadService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

interface MessageThreadRepository {
    fun getMessageThreads(
        offset: Int,
        limit: Int,
        messageType: String? = null,
        unread: Boolean? = null
    ): Flow<Result<LimitOffsetResponse<MessageThread>>>

    fun getMessageThread(threadId: Int): Flow<Result<MessageThreadDetail>>

    suspend fun createMessageThread(
        title: String,
        participants: List<Int>,
        labGroupId: Int? = null,
        isSystemThread: Boolean = false
    ): Result<MessageThread>

    suspend fun addParticipant(threadId: Int, userId: Int): Result<Map<String, String>>
    suspend fun removeParticipant(threadId: Int, userId: Int): Result<Map<String, String>>
    suspend fun markAllAsRead(threadId: Int, userId: Int): Result<Map<String, String>>
}

@Singleton
class MessageThreadRepositoryImpl @Inject constructor(
    private val messageThreadService: MessageThreadService
) : MessageThreadRepository {

    override fun getMessageThreads(
        offset: Int,
        limit: Int,
        messageType: String?,
        unread: Boolean?
    ): Flow<Result<LimitOffsetResponse<MessageThread>>> = flow {
        emit(messageThreadService.getMessageThreads(offset, limit, messageType, unread))
    }

    override fun getMessageThread(threadId: Int): Flow<Result<MessageThreadDetail>> = flow {
        emit(messageThreadService.getMessageThread(threadId))
    }

    override suspend fun createMessageThread(
        title: String,
        participants: List<Int>,
        labGroupId: Int?,
        isSystemThread: Boolean
    ): Result<MessageThread> {
        return messageThreadService.createMessageThread(title, participants, labGroupId, isSystemThread)
    }

    override suspend fun addParticipant(threadId: Int, userId: Int): Result<Map<String, String>> {
        return messageThreadService.addParticipant(threadId, userId)
    }

    override suspend fun removeParticipant(threadId: Int, userId: Int): Result<Map<String, String>> {
        return messageThreadService.removeParticipant(threadId, userId)
    }

    override suspend fun markAllAsRead(threadId: Int, userId: Int): Result<Map<String, String>> {
        return messageThreadService.markAllAsRead(threadId, userId)
    }
}