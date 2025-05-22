package info.proteo.cupcake.data.local.dao.message

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import info.proteo.cupcake.data.local.entity.message.MessageAttachmentEntity
import info.proteo.cupcake.data.local.entity.message.MessageEntity
import info.proteo.cupcake.data.local.entity.message.MessageRecipientEntity
import info.proteo.cupcake.data.local.entity.message.MessageThreadEntity
import kotlinx.coroutines.flow.Flow


@Dao
interface MessageAttachmentDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attachment: MessageAttachmentEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(attachments: List<MessageAttachmentEntity>)

    @Update
    suspend fun update(attachment: MessageAttachmentEntity)

    @Delete
    suspend fun delete(attachment: MessageAttachmentEntity)

    @Query("SELECT * FROM message_attachment WHERE id = :id")
    suspend fun getById(id: Int): MessageAttachmentEntity?

    @Query("SELECT * FROM message_attachment WHERE message_id = :messageId")
    fun getByMessage(messageId: Int): Flow<List<MessageAttachmentEntity>>

    @Query("DELETE FROM message_attachment WHERE message_id = :messageId")
    suspend fun deleteByMessageId(messageId: Int)

    @Query("DELETE FROM message_attachment")
    suspend fun deleteAll()
}

@Dao
interface MessageRecipientDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recipient: MessageRecipientEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(recipients: List<MessageRecipientEntity>)

    @Update
    suspend fun update(recipient: MessageRecipientEntity)

    @Delete
    suspend fun delete(recipient: MessageRecipientEntity)

    @Query("SELECT * FROM message_recipient WHERE id = :id")
    suspend fun getById(id: Int): MessageRecipientEntity?

    @Query("SELECT * FROM message_recipient WHERE message_id = :messageId")
    fun getByMessage(messageId: Int): Flow<List<MessageRecipientEntity>>

    @Query("SELECT * FROM message_recipient WHERE user_id = :userId")
    fun getByUser(userId: Int): Flow<List<MessageRecipientEntity>>

    @Query("SELECT * FROM message_recipient WHERE user_id = :userId AND is_read = 0")
    fun getUnreadByUser(userId: Int): Flow<List<MessageRecipientEntity>>

    @Query("DELETE FROM message_recipient")
    suspend fun deleteAll()
}

@Dao
interface MessageDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(message: MessageEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(messages: List<MessageEntity>)

    @Update
    suspend fun update(message: MessageEntity)

    @Delete
    suspend fun delete(message: MessageEntity)

    @Query("SELECT * FROM message WHERE id = :id")
    suspend fun getById(id: Int): MessageEntity?

    @Query("SELECT * FROM message WHERE sender_id = :senderId")
    fun getBySender(senderId: Int): Flow<List<MessageEntity>>

    @Query("SELECT * FROM message WHERE thread_id = :threadId ORDER BY created_at DESC")
    fun getByThread(threadId: Int): Flow<List<MessageEntity>>

    @Query("SELECT * FROM message WHERE message_type = :type")
    fun getByType(type: String): Flow<List<MessageEntity>>

    @Query("DELETE FROM message WHERE thread_id = :threadId")
    suspend fun deleteByThreadId(threadId: Int)

    @Query("DELETE FROM message")
    suspend fun deleteAll()
}

@Dao
interface MessageThreadDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(thread: MessageThreadEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(threads: List<MessageThreadEntity>)

    @Update
    suspend fun update(thread: MessageThreadEntity)

    @Delete
    suspend fun delete(thread: MessageThreadEntity)

    @Query("SELECT * FROM message_thread WHERE id = :id")
    suspend fun getById(id: Int): MessageThreadEntity?

    @Query("SELECT * FROM message_thread WHERE creator_id = :creatorId")
    fun getByCreator(creatorId: Int): Flow<List<MessageThreadEntity>>

    @Query("SELECT * FROM message_thread WHERE lab_group_id = :labGroupId")
    fun getByLabGroup(labGroupId: Int): Flow<List<MessageThreadEntity>>

    @Query("SELECT * FROM message_thread WHERE is_system_thread = 1")
    fun getSystemThreads(): Flow<List<MessageThreadEntity>>

    @Query("SELECT * FROM message_thread ORDER BY updated_at DESC")
    fun getAllThreads(): Flow<List<MessageThreadEntity>>

    @Query("DELETE FROM message_thread")
    suspend fun deleteAll()
}
