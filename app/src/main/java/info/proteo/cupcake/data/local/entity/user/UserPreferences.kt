package info.proteo.cupcake.data.local.entity.user

import androidx.room.ColumnInfo
import androidx.room.Entity

@Entity(
    tableName = "user_preferences",
    primaryKeys = ["user_id", "hostname"]
)
data class UserPreferencesEntity(
    @ColumnInfo(name = "user_id") val userId: String,
    val hostname: String,
    @ColumnInfo(name = "auth_token") val authToken: String? = null,
    @ColumnInfo(name = "session_token") val sessionToken: String? = null,
    @ColumnInfo(name = "last_login_timestamp") val lastLoginTimestamp: Long = 0,
    @ColumnInfo(name = "remember_credentials") val rememberCredentials: Boolean = false,
    val theme: String = "system",
    @ColumnInfo(name = "notifications_enabled") val notificationsEnabled: Boolean = true,
    @ColumnInfo(name = "sync_frequency") val syncFrequency: Int = 15,
    @ColumnInfo(name = "sync_on_wifi_only") val syncOnWifiOnly: Boolean = true,
    @ColumnInfo(name = "last_sync_timestamp") val lastSyncTimestamp: Long = 0,
    @ColumnInfo(name = "is_active") val isActive: Boolean = false
)