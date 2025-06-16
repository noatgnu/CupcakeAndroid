package info.proteo.cupcake.shared.data.model.message

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import info.proteo.cupcake.shared.data.model.user.LabGroupBasic
import info.proteo.cupcake.shared.data.model.user.UserBasic

@JsonClass(generateAdapter = true)
data class Message(
    val id: Int,
    val sender: UserBasic,
    @Json(name = "sender_id") val senderId: Int?,
    val content: String?,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?,
    @Json(name = "message_type") val messageType: String?,
    val priority: String?,
    @Json(name = "expires_at") val expiresAt: String?,
    val recipients: List<MessageRecipient>?,
    val attachments: List<MessageAttachment>?,
    val project: Int?,
    val protocol: Int?,
    val session: Int?,
    val instrument: Int?,
    @Json(name = "instrument_job") val instrumentJob: Int?,
    @Json(name = "stored_reagent") val storedReagent: Int?,
    @Json(name = "is_read") val isRead: Boolean
)
@JsonClass(generateAdapter = true)
data class MessageAttachment(
    val id: Int,
    val file: String?,
    @Json(name = "file_name") val fileName: String?,
    @Json(name = "file_size") val fileSize: Int?,
    @Json(name = "content_type") val contentType: String?,
    @Json(name = "created_at") val createdAt: String?,
)
@JsonClass(generateAdapter = true)
data class MessageRecipient(
    val id: Int,
    val user: UserBasic,
    @Json(name = "is_read") val isRead: Boolean,
    @Json(name = "read_at") val readAt: String?,
    @Json(name = "is_archived") val isArchived: Boolean,
    @Json(name = "is_deleted") val isDeleted: Boolean
)
@JsonClass(generateAdapter = true)
data class ThreadMessage(
    val id: Int,
    val sender: UserBasic,
    val content: String?,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "message_type") val messageType: String?,
    val priority: String?,
    @Json(name = "attachment_count") val attachmentCount: Int,
    @Json(name = "is_read") val isRead: Boolean
)
@JsonClass(generateAdapter = true)
data class MessageThread(
    val id: Int,
    val title: String?,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?,
    val participants: List<UserBasic>?,
    @Json(name = "participant_ids") val participantIds: List<Int>?,
    @Json(name = "is_system_thread") val isSystemThread: Boolean,
    @Json(name = "lab_group") val labGroup: LabGroupBasic?,
    @Json(name = "lab_group_id") val labGroupId: Int?,
    @Json(name = "latest_message") val latestMessage: ThreadMessage?,
    @Json(name = "unread_count") val unreadCount: Int,
    val creator: UserBasic
)
@JsonClass(generateAdapter = true)
data class MessageThreadDetail(
    val id: Int,
    val title: String?,
    @Json(name = "created_at") val createdAt: String?,
    @Json(name = "updated_at") val updatedAt: String?,
    val participants: List<UserBasic>?,
    @Json(name = "participant_ids") val participantIds: List<Int>?,
    @Json(name = "is_system_thread") val isSystemThread: Boolean,
    @Json(name = "lab_group") val labGroup: LabGroupBasic?,
    @Json(name = "lab_group_id") val labGroupId: Int?,
    @Json(name = "latest_message") val latestMessage: ThreadMessage?,
    @Json(name = "unread_count") val unreadCount: Int,
    val creator: UserBasic,
    val messages: List<ThreadMessage>
)