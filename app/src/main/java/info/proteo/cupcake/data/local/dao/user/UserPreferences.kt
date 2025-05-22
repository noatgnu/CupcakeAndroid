package info.proteo.cupcake.data.local.dao.user

import androidx.room.*
import info.proteo.cupcake.data.local.entity.user.UserPreferencesEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface UserPreferencesDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(preferences: UserPreferencesEntity)

    @Query("SELECT * FROM user_preferences WHERE user_id = :userId AND hostname = :hostname")
    suspend fun getPreferences(userId: String, hostname: String): UserPreferencesEntity?

    @Query("SELECT * FROM user_preferences WHERE user_id = :userId AND hostname = :hostname")
    fun getPreferencesFlow(userId: String, hostname: String): Flow<UserPreferencesEntity?>

    @Query("UPDATE user_preferences SET auth_token = :token WHERE user_id = :userId AND hostname = :hostname")
    suspend fun updateAuthToken(userId: String, hostname: String, token: String?)

    @Query("UPDATE user_preferences SET session_token = :token WHERE user_id = :userId AND hostname = :hostname")
    suspend fun updateSessionToken(userId: String, hostname: String, token: String?)

    @Query("DELETE FROM user_preferences WHERE user_id = :userId AND hostname = :hostname")
    suspend fun deletePreferences(userId: String, hostname: String)

    @Query("SELECT DISTINCT hostname FROM user_preferences")
    suspend fun getAllHostnames(): List<String>

    @Query("SELECT * FROM user_preferences WHERE is_active = 1 AND hostname = :hostname LIMIT 1")
    suspend fun getCurrentlyActivePreferences(hostname: String): UserPreferencesEntity?

    @Query("SELECT * FROM user_preferences WHERE is_active = 1 LIMIT 1")
    suspend fun getCurrentlyActivePreference(): UserPreferencesEntity?

    @Query("SELECT * FROM user_preferences WHERE is_active = 1 AND hostname = :hostname")
    fun getCurrentlyActivePreferencesFlow(hostname: String): Flow<UserPreferencesEntity?>

    @Query("UPDATE user_preferences SET is_active = 0 WHERE hostname = :hostname")
    suspend fun deactivateAllPreferencesForHostname(hostname: String)

    @Query("UPDATE user_preferences SET is_active = 1 WHERE user_id = :userId AND hostname = :hostname")
    suspend fun setPreferenceActive(userId: String, hostname: String)

    @Transaction
    suspend fun switchActivePreference(userId: String, hostname: String) {
        deactivateAllPreferencesForHostname(hostname)
        setPreferenceActive(userId, hostname)
    }

    @Query("SELECT * FROM user_preferences")
    suspend fun getAllUserPreferences(): List<UserPreferencesEntity>
}