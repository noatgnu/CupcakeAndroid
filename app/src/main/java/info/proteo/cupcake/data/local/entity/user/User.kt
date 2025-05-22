package info.proteo.cupcake.data.local.entity.user

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_basic")
data class UserBasicEntity(
    @PrimaryKey val id: Int,
    val username: String,
    @ColumnInfo(name = "first_name") val firstName: String?,
    @ColumnInfo(name = "last_name") val lastName: String?
)

@Entity(tableName = "user")
data class UserEntity(
    @PrimaryKey val id: Int,
    val username: String,
    val email: String?,
    @ColumnInfo(name = "first_name") val firstName: String?,
    @ColumnInfo(name = "last_name") val lastName: String?,
    @ColumnInfo(name = "is_staff") val isStaff: Boolean
)