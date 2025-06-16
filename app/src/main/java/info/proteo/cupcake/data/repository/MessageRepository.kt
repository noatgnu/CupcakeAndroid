package info.proteo.cupcake.data.repository

import info.proteo.cupcake.shared.data.model.LimitOffsetResponse
import info.proteo.cupcake.shared.data.model.message.Message
import info.proteo.cupcake.data.remote.service.MessageService
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface MessageRepository {
    fun getMessages(
        offset: Int,
        limit: Int,
        threadId: Int? = null
    ): Flow<Result<LimitOffsetResponse<Message>>>

    fun getMessage(id: Int): Flow<Result<Message>>

    suspend fun createMessage(
        threadId: Int,
        content: String,
        messageType: String? = "user_message",
        priority: String? = "normal",
        attachments: List<File>? = null
    ): Result<Message>

    suspend fun updateMessage(id: Int, content: String): Result<Message>
    suspend fun deleteMessage(id: Int): Result<Unit>
    suspend fun markAsRead(messageId: Int, userId: Int): Result<Map<String, String>>
    suspend fun markAsUnread(messageId: Int, userId: Int): Result<Map<String, String>>
}

@Singleton
class MessageRepositoryImpl @Inject constructor(
    private val messageService: MessageService
) : MessageRepository {

    override fun getMessages(
        offset: Int,
        limit: Int,
        threadId: Int?
    ): Flow<Result<LimitOffsetResponse<Message>>> = flow {
        emit(messageService.getMessages(offset, limit, threadId))
    }

    override fun getMessage(id: Int): Flow<Result<Message>> = flow {
        emit(messageService.getMessage(id))
    }

    override suspend fun createMessage(
        threadId: Int,
        content: String,
        messageType: String?,
        priority: String?,
        attachments: List<File>?
    ): Result<Message> {
        return messageService.createMessage(threadId, content, messageType, priority, attachments)
    }

    override suspend fun updateMessage(id: Int, content: String): Result<Message> {
        return messageService.updateMessage(id, mapOf("content" to content))
    }

    override suspend fun deleteMessage(id: Int): Result<Unit> {
        return messageService.deleteMessage(id)
    }

    override suspend fun markAsRead(messageId: Int, userId: Int): Result<Map<String, String>> {
        return messageService.markAsRead(messageId, userId)
    }

    override suspend fun markAsUnread(messageId: Int, userId: Int): Result<Map<String, String>> {
        return messageService.markAsUnread(messageId, userId)
    }
}