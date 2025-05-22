package info.proteo.cupcake.data.local.entity.message

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "message_attachment")
data class MessageAttachmentEntity(
    @PrimaryKey val id: Int,
    val file: String?,
    @ColumnInfo(name = "file_name") val fileName: String?,
    @ColumnInfo(name = "file_size") val fileSize: Int?,
    @ColumnInfo(name = "content_type") val contentType: String?,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "message_id") val messageId: Int
)

@Entity(tableName = "message_recipient")
data class MessageRecipientEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "user_id") val userId: Int,
    @ColumnInfo(name = "message_id") val messageId: Int,
    @ColumnInfo(name = "is_read") val isRead: Boolean,
    @ColumnInfo(name = "read_at") val readAt: String?,
    @ColumnInfo(name = "is_archived") val isArchived: Boolean,
    @ColumnInfo(name = "is_deleted") val isDeleted: Boolean
)

@Entity(tableName = "message")
data class MessageEntity(
    @PrimaryKey val id: Int,
    @ColumnInfo(name = "sender_id") val senderId: Int,
    val content: String?,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "message_type") val messageType: String?,
    val priority: String?,
    @ColumnInfo(name = "expires_at") val expiresAt: String?,
    val project: Int?,
    val protocol: Int?,
    val session: Int?,
    val instrument: Int?,
    @ColumnInfo(name = "instrument_job") val instrumentJob: Int?,
    @ColumnInfo(name = "stored_reagent") val storedReagent: Int?,
    @ColumnInfo(name = "thread_id") val threadId: Int
)

@Entity(tableName = "message_thread")
data class MessageThreadEntity(
    @PrimaryKey val id: Int,
    val title: String?,
    @ColumnInfo(name = "created_at") val createdAt: String?,
    @ColumnInfo(name = "updated_at") val updatedAt: String?,
    @ColumnInfo(name = "is_system_thread") val isSystemThread: Boolean,
    @ColumnInfo(name = "lab_group_id") val labGroupId: Int?,
    @ColumnInfo(name = "creator_id") val creatorId: Int
)