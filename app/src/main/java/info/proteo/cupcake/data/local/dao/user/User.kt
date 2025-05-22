package info.proteo.cupcake.data.local.dao.user

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import info.proteo.cupcake.data.local.entity.user.UserBasicEntity
import info.proteo.cupcake.data.local.entity.user.UserEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserBasicDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserBasicEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<UserBasicEntity>)

    @Update
    suspend fun update(user: UserBasicEntity)

    @Delete
    suspend fun delete(user: UserBasicEntity)

    @Query("SELECT * FROM user_basic WHERE id = :id")
    suspend fun getById(id: Int): UserBasicEntity?

    @Query("SELECT * FROM user_basic")
    fun getAllUsers(): Flow<List<UserBasicEntity>>

    @Query("DELETE FROM user_basic")
    suspend fun deleteAll()
}

@Dao
interface UserDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(user: UserEntity): Long

    @Update
    suspend fun update(user: UserEntity)

    @Delete
    suspend fun delete(user: UserEntity)

    @Query("SELECT * FROM user WHERE id = :id")
    suspend fun getById(id: Int): UserEntity?

    @Query("SELECT * FROM user WHERE username = :username")
    suspend fun getByUsername(username: String): UserEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateUser(user: UserEntity)

    @Query("DELETE FROM user")
    suspend fun deleteAll()
}