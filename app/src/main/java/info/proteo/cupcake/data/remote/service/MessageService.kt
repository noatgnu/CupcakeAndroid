package info.proteo.cupcake.data.remote.service

import info.proteo.cupcake.SessionManager
import info.proteo.cupcake.data.local.dao.message.MessageAttachmentDao
import info.proteo.cupcake.data.local.dao.message.MessageDao
import info.proteo.cupcake.data.local.dao.message.MessageRecipientDao
import info.proteo.cupcake.data.local.dao.message.MessageThreadDao
import info.proteo.cupcake.data.local.dao.user.UserDao
import info.proteo.cupcake.data.local.entity.message.MessageAttachmentEntity
import info.proteo.cupcake.data.local.entity.message.MessageEntity
import info.proteo.cupcake.data.local.entity.message.MessageRecipientEntity
import info.proteo.cupcake.data.local.entity.message.MessageThreadEntity
import info.proteo.cupcake.data.local.entity.user.UserEntity
import info.proteo.cupcake.data.remote.model.LimitOffsetResponse
import info.proteo.cupcake.data.remote.model.message.Message
import info.proteo.cupcake.data.remote.model.message.MessageAttachment
import info.proteo.cupcake.data.remote.model.message.MessageRecipient
import info.proteo.cupcake.data.remote.model.message.MessageThread
import info.proteo.cupcake.data.remote.model.message.MessageThreadDetail
import info.proteo.cupcake.data.remote.model.message.ThreadMessage
import info.proteo.cupcake.data.remote.model.user.LabGroupBasic
import info.proteo.cupcake.data.remote.model.user.UserBasic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.flatMap
import kotlinx.coroutines.flow.flatMapConcat
import kotlinx.coroutines.flow.forEach
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.withContext
import okhttp3.MediaType
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody
import okhttp3.RequestBody.Companion.asRequestBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface MessageApiService {
    @GET("api/messages/")
    suspend fun getMessages(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @Query("thread") threadId: Int? = null
    ): LimitOffsetResponse<Message>

    @Multipart
    @POST("api/messages/")
    suspend fun createMessage(
        @Part("thread") threadId: RequestBody,
        @Part("content") content: RequestBody,
        @Part("message_type") messageType: RequestBody?,
        @Part("priority") priority: RequestBody?,
        @Part attachments: List<MultipartBody.Part>?
    ): Message

    @POST("api/messages/{id}/mark_read/")
    suspend fun markAsRead(@Path("id") messageId: Int): Map<String, String>

    @POST("api/messages/{id}/mark_unread/")
    suspend fun markAsUnread(@Path("id") messageId: Int): Map<String, String>

    @DELETE("api/messages/{id}/")
    suspend fun deleteMessage(@Path("id") messageId: Int): Unit

    @GET("api/messages/{id}/")
    suspend fun getMessage(@Path("id") messageId: Int): Message

    @POST("api/messages/{id}/")
    suspend fun updateMessage(
        @Path("id") messageId: Int,
        @Body messageData: Map<String, Any>
    ): Message

}

interface MessageThreadApiService {
    @GET("api/message_threads/")
    suspend fun getMessageThreads(
        @Query("offset") offset: Int,
        @Query("limit") limit: Int,
        @Query("message_type") messageType: String? = null,
        @Query("unread") unread: Boolean? = null
    ): LimitOffsetResponse<MessageThread>

    @GET("api/message_threads/{id}/")
    suspend fun getMessageThread(@Path("id") threadId: Int): MessageThreadDetail

    @POST("api/message_threads/")
    suspend fun createMessageThread(@Body threadData: CreateThreadRequest): MessageThread

    @POST("api/message_threads/{id}/add_participant/")
    suspend fun addParticipant(
        @Path("id") threadId: Int,
        @Body data: Map<String, Int>
    ): Map<String, String>

    @POST("api/message_threads/{id}/remove_participant/")
    suspend fun removeParticipant(
        @Path("id") threadId: Int,
        @Body data: Map<String, Int>
    ): Map<String, String>

    @POST("api/message_threads/{id}/mark_all_read/")
    suspend fun markAllAsRead(@Path("id") threadId: Int): Map<String, String>

    @POST("api/message_threads/{id}/")
    suspend fun updateMessageThread(
        @Path("id") threadId: Int,
        @Body threadData: Map<String, Any>
    ): MessageThread


}

interface MessageService {
    suspend fun getMessages(
        offset: Int,
        limit: Int,
        threadId: Int? = null
    ): Result<LimitOffsetResponse<Message>>

    suspend fun createMessage(
        threadId: Int,
        content: String,
        messageType: String? = null,
        priority: String? = null,
        attachments: List<File>? = null
    ): Result<Message>

    suspend fun markAsRead(messageId: Int, userId: Int): Result<Map<String, String>>
    suspend fun markAsUnread(messageId: Int, userId: Int): Result<Map<String, String>>

    suspend fun getMessage(messageId: Int): Result<Message>
    suspend fun deleteMessage(messageId: Int): Result<Unit>
    suspend fun updateMessage(
        messageId: Int,
        messageData: Map<String, Any>
    ): Result<Message>
}

@Singleton
class MessageServiceImpl @Inject constructor(
    private val messageApiService: MessageApiService,
    private val messageDao: MessageDao,
    private val userDao: UserDao,
    private val attachmentDao: MessageAttachmentDao,
    private val recipientDao: MessageRecipientDao,
    private val userService: UserService,
) : MessageService {

    override suspend fun updateMessage(
        messageId: Int,
        messageData: Map<String, Any>
    ): Result<Message> {
        try {
            val response = messageApiService.updateMessage(messageId, messageData)
            cacheMessageWithRelations(response)
            return Result.success(response)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override suspend fun deleteMessage(messageId: Int): Result<Unit> {
        try {
            messageDao.deleteById(messageId)
            recipientDao.deleteByMessageId(messageId)
            attachmentDao.deleteByMessageId(messageId)
            messageApiService.deleteMessage(messageId)
            return Result.success(Unit)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override suspend fun getMessage(messageId: Int): Result<Message> {
        try {
            val response = messageApiService.getMessage(messageId)
            cacheMessageWithRelations(response)
            return Result.success(response)
        } catch (e: Exception) {
            return Result.failure(e)
        }
    }

    override suspend fun getMessages(
        offset: Int,
        limit: Int,
        threadId: Int?
    ): Result<LimitOffsetResponse<Message>> = withContext(Dispatchers.IO) {
        try {
            val response = messageApiService.getMessages(offset, limit, threadId)

            response.results.forEach { message ->
                cacheMessageWithRelations(message)
            }

            Result.success(response)
        } catch (e: Exception) {
            try {
                val cachedData = getCachedMessages(offset, limit, threadId)
                Result.success(cachedData)
            } catch (cacheError: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun createMessage(
        threadId: Int,
        content: String,
        messageType: String?,
        priority: String?,
        attachments: List<File>?
    ): Result<Message> = withContext(Dispatchers.IO) {
        try {
            val threadIdPart = threadId.toString().toRequestBody("text/plain".toMediaTypeOrNull())
            val contentPart = content.toRequestBody("text/plain".toMediaTypeOrNull())
            val messageTypePart = messageType?.toRequestBody("text/plain".toMediaTypeOrNull())
            val priorityPart = priority?.toRequestBody("text/plain".toMediaTypeOrNull())

            val attachmentParts = attachments?.map { file ->
                val requestFile = file.asRequestBody("multipart/form-data".toMediaTypeOrNull())
                MultipartBody.Part.createFormData("attachments", file.name, requestFile)
            }

            val response = messageApiService.createMessage(
                threadIdPart,
                contentPart,
                messageTypePart,
                priorityPart,
                attachmentParts
            )

            cacheMessageWithRelations(response)

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAsRead(messageId: Int, userId: Int): Result<Map<String, String>> =
        withContext(Dispatchers.IO) {
            val currentUserId = userId

            try {
                val response = messageApiService.markAsRead(messageId)

                val timestamp = System.currentTimeMillis().toString()
                recipientDao.markAsRead(messageId, currentUserId, timestamp)

                Result.success(response)
            } catch (e: Exception) {
                try {
                    val timestamp = System.currentTimeMillis().toString()
                    recipientDao.markAsRead(messageId, currentUserId, timestamp)
                    Result.failure(e)
                } catch (dbError: Exception) {
                    Result.failure(e)
                }
            }
        }

    override suspend fun markAsUnread(messageId: Int, userId: Int): Result<Map<String, String>> =
        withContext(Dispatchers.IO) {
            val currentUserId = userId

            try {
                val response = messageApiService.markAsUnread(messageId)
                recipientDao.markAsUnread(messageId, currentUserId)
                Result.success(response)
            } catch (e: Exception) {
                try {
                    recipientDao.markAsUnread(messageId, currentUserId)
                    Result.failure(e)
                } catch (dbError: Exception) {
                    Result.failure(e)
                }
            }
        }


    private suspend fun cacheMessageWithRelations(message: Message) {
        messageDao.insert(
            MessageEntity(
                id = message.id,
                senderId = message.sender.id,
                content = message.content,
                createdAt = message.createdAt,
                updatedAt = message.updatedAt,
                messageType = message.messageType,
                priority = message.priority,
                expiresAt = message.expiresAt,
                project = message.project,
                protocol = message.protocol,
                session = message.session,
                instrument = message.instrument,
                instrumentJob = message.instrumentJob,
                storedReagent = message.storedReagent,
                threadId = message.senderId ?: 0
            )
        )

        val existingUser = userDao.getById(message.sender.id)
        if (existingUser == null) {
            userDao.insert(
                UserEntity(
                    id = message.sender.id,
                    username = message.sender.username,
                    firstName = message.sender.firstName,
                    lastName = message.sender.lastName,
                    email = null,
                    isStaff = false
                )
            )
        }

        message.attachments?.forEach { attachment ->
            attachmentDao.insert(
                MessageAttachmentEntity(
                    id = attachment.id,
                    file = attachment.file,
                    fileName = attachment.fileName,
                    fileSize = attachment.fileSize,
                    contentType = attachment.contentType,
                    createdAt = attachment.createdAt,
                    messageId = message.id
                )
            )
        }

        message.recipients?.forEach { recipient ->
            recipientDao.insert(
                MessageRecipientEntity(
                    id = recipient.id,
                    userId = recipient.user.id,
                    messageId = message.id,
                    isRead = recipient.isRead,
                    readAt = recipient.readAt,
                    isArchived = recipient.isArchived,
                    isDeleted = recipient.isDeleted
                )
            )

            val existingRecipient = userDao.getById(recipient.user.id)
            if (existingRecipient == null) {
                userDao.insert(
                    UserEntity(
                        id = recipient.user.id,
                        username = recipient.user.username,
                        firstName = recipient.user.firstName,
                        lastName = recipient.user.lastName,
                        email = null,
                        isStaff = false
                    )
                )
            } else {
                userDao.update(
                    UserEntity(
                        id = existingRecipient.id,
                        username = recipient.user.username.ifEmpty { existingRecipient.username },
                        firstName = recipient.user.firstName?.ifEmpty { existingRecipient.firstName },
                        lastName = recipient.user.lastName?.ifEmpty { existingRecipient.lastName },
                        email = existingRecipient.email,
                        isStaff = existingRecipient.isStaff
                    )
                )
            }
        }
    }

    private suspend fun getCachedMessages(offset: Int, limit: Int, threadId: Int?): LimitOffsetResponse<Message> {
        val count = if (threadId != null) {
            messageDao.countByThreadId(threadId)
        } else {
            messageDao.countAll()
        }

        val cachedMessages = if (threadId != null) {
            messageDao.getByThreadPaginated(threadId, offset, limit).firstOrNull() ?: emptyList()
        } else {
            messageDao.getAllMessagesPaginated(offset, limit).firstOrNull() ?: emptyList()
        }
        val domainMessages = cachedMessages.map { loadMessageWithRelations(it) }

        return LimitOffsetResponse(count, null, null, domainMessages)
    }

    private suspend fun loadMessageWithRelations(entity: MessageEntity): Message {
        val sender = userDao.getById(entity.senderId)?.let {
            UserBasic(
                id = it.id,
                username = it.username,
                firstName = it.firstName ?: "",
                lastName = it.lastName ?: ""
            )
        } ?: UserBasic(entity.senderId, "", "", "")

        val recipientEntities = recipientDao.getByMessageSync(entity.id)
        val recipients = recipientEntities.map { recipientEntity ->
            val user = userDao.getById(recipientEntity.userId)
            MessageRecipient(
                id = recipientEntity.id,
                user = UserBasic(
                    id = recipientEntity.userId,
                    username = user?.username ?: "",
                    firstName = user?.firstName ?: "",
                    lastName = user?.lastName ?: ""
                ),
                isRead = recipientEntity.isRead,
                readAt = recipientEntity.readAt,
                isArchived = recipientEntity.isArchived,
                isDeleted = recipientEntity.isDeleted
            )
        }

        val attachmentEntities = attachmentDao.getByMessage(entity.id).firstOrNull() ?: emptyList()
        val attachments = attachmentEntities.map { attachmentEntity ->
            MessageAttachment(
                id = attachmentEntity.id,
                file = attachmentEntity.file,
                fileName = attachmentEntity.fileName,
                fileSize = attachmentEntity.fileSize,
                contentType = attachmentEntity.contentType,
                createdAt = attachmentEntity.createdAt
            )
        }

        val currentUserId = userService.getCurrentUser().id

        val isRead = if (currentUserId > 0) {
            recipientDao.isReadByUser(entity.id, currentUserId)
        } else {
            false
        }

        return Message(
            id = entity.id,
            sender = sender,
            senderId = entity.senderId,
            content = entity.content,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            messageType = entity.messageType,
            priority = entity.priority,
            expiresAt = entity.expiresAt,
            recipients = recipients,
            attachments = attachments,
            project = entity.project,
            protocol = entity.protocol,
            session = entity.session,
            instrument = entity.instrument,
            instrumentJob = entity.instrumentJob,
            storedReagent = entity.storedReagent,
            isRead = isRead,
        )
    }
}

data class CreateThreadRequest(
    val title: String,
    val participants: List<Int>,
    val labGroupId: Int? = null,
    val isSystemThread: Boolean = false
)

interface MessageThreadService {
    suspend fun getMessageThreads(
        offset: Int,
        limit: Int,
        messageType: String? = null,
        unread: Boolean? = null
    ): Result<LimitOffsetResponse<MessageThread>>

    suspend fun getMessageThread(threadId: Int): Result<MessageThreadDetail>

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
class MessageThreadServiceImpl @Inject constructor(
    private val messageThreadApiService: MessageThreadApiService,
    private val messageDao: MessageDao,
    private val threadDao: MessageThreadDao,
    private val recipientDao: MessageRecipientDao,
    private val userDao: UserDao,
    private val userService: UserService,
    private val labGroupService: LabGroupService,
    private val attachmentDao: MessageAttachmentDao
) : MessageThreadService {

    override suspend fun getMessageThreads(
        offset: Int,
        limit: Int,
        messageType: String?,
        unread: Boolean?
    ): Result<LimitOffsetResponse<MessageThread>> = withContext(Dispatchers.IO) {
        try {
            val response = messageThreadApiService.getMessageThreads(offset, limit, messageType, unread)

            // Cache threads
            response.results.forEach { thread ->
                cacheThreadWithRelations(thread)
            }

            Result.success(response)
        } catch (e: Exception) {
            try {
                val cachedData = getCachedThreads(offset, limit)
                Result.success(cachedData)
            } catch (cacheError: Exception) {
                Result.failure(e)
            }
        }
    }

    override suspend fun getMessageThread(threadId: Int): Result<MessageThreadDetail> =
        withContext(Dispatchers.IO) {
            try {
                val response = messageThreadApiService.getMessageThread(threadId)
                cacheThreadDetailWithRelations(response)

                Result.success(response)
            } catch (e: Exception) {
                Result.failure(e)
            }
        }

    override suspend fun createMessageThread(
        title: String,
        participants: List<Int>,
        labGroupId: Int?,
        isSystemThread: Boolean
    ): Result<MessageThread> = withContext(Dispatchers.IO) {
        try {

            val request = CreateThreadRequest(title, participants, labGroupId, isSystemThread)

            val response = messageThreadApiService.createMessageThread(request)

            cacheThreadWithRelations(response)

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addParticipant(
        threadId: Int,
        userId: Int
    ): Result<Map<String, String>> = withContext(Dispatchers.IO) {
        try {
            val response = messageThreadApiService.addParticipant(
                threadId,
                mapOf("user_id" to userId)
            )

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeParticipant(
        threadId: Int,
        userId: Int
    ): Result<Map<String, String>> = withContext(Dispatchers.IO) {
        try {
            val response = messageThreadApiService.removeParticipant(
                threadId,
                mapOf("user_id" to userId)
            )

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun markAllAsRead(threadId: Int, userId: Int): Result<Map<String, String>> =
        withContext(Dispatchers.IO) {
            try {
                val response = messageThreadApiService.markAllAsRead(threadId)

                val timestamp = System.currentTimeMillis().toString()
                recipientDao.markThreadAsRead(threadId, userId, timestamp)

                Result.success(response)
            } catch (e: Exception) {
                try {
                    val timestamp = System.currentTimeMillis().toString()
                    recipientDao.markThreadAsRead(threadId, userId, timestamp)
                    Result.failure(e) // Still return failure since the API call failed
                } catch (dbError: Exception) {
                    Result.failure(e)
                }
            }
        }

    private suspend fun cacheThreadWithRelations(thread: MessageThread) {
        threadDao.insert(
            MessageThreadEntity(
                id = thread.id,
                title = thread.title,
                createdAt = thread.createdAt,
                updatedAt = thread.updatedAt,
                isSystemThread = thread.isSystemThread,
                labGroupId = thread.labGroupId,
                creatorId = thread.creator.id
            )
        )

        val existingCreator = userDao.getById(thread.creator.id)
        if (existingCreator == null) {
            userDao.insert(
                UserEntity(
                    id = thread.creator.id,
                    username = thread.creator.username,
                    firstName = thread.creator.firstName,
                    lastName = thread.creator.lastName,
                    email = null,
                    isStaff = false
                )
            )
        }

        thread.participants?.forEach { participant ->
            val existingParticipant = userDao.getById(participant.id)
            if (existingParticipant == null) {
                userDao.insert(
                    UserEntity(
                        id = participant.id,
                        username = participant.username,
                        firstName = participant.firstName,
                        lastName = participant.lastName,
                        email = null,
                        isStaff = false
                    )
                )
            }
        }

        thread.latestMessage?.let { message ->
            messageDao.insert(
                MessageEntity(
                    id = message.id,
                    senderId = message.sender.id,
                    content = message.content,
                    createdAt = message.createdAt,
                    updatedAt = null,
                    messageType = message.messageType,
                    priority = message.priority,
                    expiresAt = null,
                    project = null,
                    protocol = null,
                    session = null,
                    instrument = null,
                    instrumentJob = null,
                    storedReagent = null,
                    threadId = thread.id
                )
            )
        }
    }

    private suspend fun cacheThreadDetailWithRelations(threadDetail: MessageThreadDetail) {
        threadDao.insert(
            MessageThreadEntity(
                id = threadDetail.id,
                title = threadDetail.title,
                createdAt = threadDetail.createdAt,
                updatedAt = threadDetail.updatedAt,
                isSystemThread = threadDetail.isSystemThread,
                labGroupId = threadDetail.labGroupId,
                creatorId = threadDetail.creator.id
            )
        )

        threadDetail.messages.forEach { threadMessage ->
            messageDao.insert(
                MessageEntity(
                    id = threadMessage.id,
                    senderId = threadMessage.sender.id,
                    content = threadMessage.content,
                    createdAt = threadMessage.createdAt,
                    updatedAt = null,
                    messageType = threadMessage.messageType,
                    priority = threadMessage.priority,
                    expiresAt = null,
                    project = null,
                    protocol = null,
                    session = null,
                    instrument = null,
                    instrumentJob = null,
                    storedReagent = null,
                    threadId = threadDetail.id
                )
            )

            val existingSender = userDao.getById(threadMessage.sender.id)
            if (existingSender == null) {
                userDao.insert(
                    UserEntity(
                        id = threadMessage.sender.id,
                        username = threadMessage.sender.username,
                        firstName = threadMessage.sender.firstName,
                        lastName = threadMessage.sender.lastName,
                        email = null,
                        isStaff = false
                    )
                )
            }
        }

        threadDetail.participants?.forEach { participant ->
            val existingParticipant = userDao.getById(participant.id)
            if (existingParticipant == null) {
                userDao.insert(
                    UserEntity(
                        id = participant.id,
                        username = participant.username,
                        firstName = participant.firstName,
                        lastName = participant.lastName,
                        email = null,
                        isStaff = false
                    )
                )
            }
        }
    }

    private suspend fun getCachedThreads(offset: Int, limit: Int): LimitOffsetResponse<MessageThread> {
        val count = threadDao.countAll().firstOrNull() ?: 0
        val cachedThreads = threadDao.getAllThreads()
            .firstOrNull()
            ?.drop(offset)
            ?.take(limit)
            ?: emptyList()

        val domainThreads = cachedThreads.map { loadThreadWithRelations(it) }
        return LimitOffsetResponse(count, null, null, domainThreads)
    }

    private suspend fun loadThreadWithRelations(entity: MessageThreadEntity): MessageThread {
        // Load creator details
        val creator = userDao.getById(entity.creatorId)?.let {
            UserBasic(
                id = it.id,
                username = it.username,
                firstName = it.firstName ?: "",
                lastName = it.lastName ?: ""
            )
        } ?: UserBasic(entity.creatorId, "", "", "")

        val threadMessages = messageDao.getByThread(entity.id).firstOrNull() ?: emptyList()

        val participantIds = mutableSetOf<Int>()
        threadMessages.forEach { message ->
            val recipients = recipientDao.getByMessageSync(message.id)
            recipients.forEach { recipient ->
                participantIds.add(recipient.userId)
            }
            participantIds.add(message.senderId)
        }

        val participants = participantIds.mapNotNull { participantId ->
            userDao.getById(participantId)?.let {
                UserBasic(
                    id = it.id,
                    username = it.username,
                    firstName = it.firstName ?: "",
                    lastName = it.lastName ?: ""
                )
            }
        }

        // Get latest message by sorting messages by creation time
        val latestMessageEntity = threadMessages.maxByOrNull {
            it.createdAt ?: ""
        }

        // Convert Message to ThreadMessage if we have a latest message
        val latestMessage = latestMessageEntity?.let { messageEntity ->
            val sender = userDao.getById(messageEntity.senderId)?.let { user ->
                UserBasic(
                    id = user.id,
                    username = user.username,
                    firstName = user.firstName ?: "",
                    lastName = user.lastName ?: ""
                )
            } ?: UserBasic(messageEntity.senderId, "", "", "")

            // Get attachment count for this message
            val attachmentCount = attachmentDao.getByMessage(messageEntity.id).firstOrNull()?.size ?: 0

            ThreadMessage(
                id = messageEntity.id,
                sender = sender,
                content = messageEntity.content,
                createdAt = messageEntity.createdAt,
                messageType = messageEntity.messageType,
                priority = messageEntity.priority,
                attachmentCount = attachmentCount,
                isRead = false
            )
        }

        val currentUserId = try {
            userService.getCurrentUser().id
        } catch (e: Exception) {
            -1
        }

        val unreadCount = if (currentUserId > 0) {
            recipientDao.countUnreadByThreadForUser(entity.id, currentUserId)
        } else {
            0
        }

        val labGroup = entity.labGroupId?.let { labGroupId ->
            try {
                labGroupService.getLabGroupById(labGroupId).getOrNull()?.let {
                    LabGroupBasic(
                        id = it.id,
                        name = it.name,
                    )
                }
            } catch (e: Exception) {
                null
            }
        }

        return MessageThread(
            id = entity.id,
            title = entity.title,
            createdAt = entity.createdAt,
            updatedAt = entity.updatedAt,
            participants = participants,
            participantIds = participants.map { it.id },
            isSystemThread = entity.isSystemThread,
            labGroup = labGroup,
            labGroupId = entity.labGroupId,
            latestMessage = latestMessage,
            unreadCount = unreadCount,
            creator = creator
        )
    }
}