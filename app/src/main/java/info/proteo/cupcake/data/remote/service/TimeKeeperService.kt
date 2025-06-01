package info.proteo.cupcake.data.remote.service

import info.proteo.cupcake.data.local.dao.protocol.TimeKeeperDao
import info.proteo.cupcake.data.local.dao.user.UserPreferencesDao
import info.proteo.cupcake.data.local.entity.protocol.TimeKeeperEntity
import info.proteo.cupcake.data.remote.model.LimitOffsetResponse
import info.proteo.cupcake.data.remote.model.protocol.TimeKeeper
import info.proteo.cupcake.data.repository.UserRepository
import kotlinx.coroutines.flow.first
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

interface TimeKeeperApiService {
    @GET("api/timekeeper/")
    suspend fun getTimeKeepers(
        @Query("offset") offset: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("session") sessionId: Int? = null,
        @Query("step") stepId: Int? = null,
        @Query("started") started: Boolean? = null,
        @Query("ordering") ordering: String = "-start_time"
    ): LimitOffsetResponse<TimeKeeper>

    // Other methods remain unchanged
    @GET("api/timekeeper/{id}/")
    suspend fun getTimeKeeperById(@Path("id") id: Int): TimeKeeper

    @POST("api/timekeeper/")
    suspend fun createTimeKeeper(@Body timeKeeper: TimeKeeper): TimeKeeper

    @PATCH("api/timekeeper/{id}/")
    suspend fun updateTimeKeeper(@Path("id") id: Int, @Body timeKeeper: TimeKeeper): TimeKeeper

    @DELETE("api/timekeeper/{id}/")
    suspend fun deleteTimeKeeper(@Path("id") id: Int): Response<Unit>
}

interface TimeKeeperService {
    suspend fun getTimeKeepers(
        offset: Int? = null,
        limit: Int? = null,
        sessionId: Int? = null,
        stepId: Int? = null,
        started: Boolean? = null,
        ordering: String = "-start_time"
    ): Result<LimitOffsetResponse<TimeKeeper>>

    suspend fun getTimeKeeperById(id: Int): Result<TimeKeeper>
    suspend fun createTimeKeeper(timeKeeper: TimeKeeper): Result<TimeKeeper>
    suspend fun updateTimeKeeper(id: Int, timeKeeper: TimeKeeper): Result<TimeKeeper>
    suspend fun deleteTimeKeeper(id: Int): Result<Unit>
}

@Singleton
class TimeKeeperServiceImpl @Inject constructor(
    private val apiService: TimeKeeperApiService,
    private val timeKeeperDao: TimeKeeperDao,
    private val userRepository: UserRepository,
    private val userPreferencesDao: UserPreferencesDao
) : TimeKeeperService {

    override suspend fun getTimeKeepers(
        offset: Int?,
        limit: Int?,
        sessionId: Int?,
        stepId: Int?,
        started: Boolean?,
        ordering: String
    ): Result<LimitOffsetResponse<TimeKeeper>> {
        val currentUserId = getCurrentUserId()
        if (currentUserId == null) {
            return Result.failure(Exception("Unable to retrieve current user ID"))
        }

        return try {
            val response = apiService.getTimeKeepers(offset, limit, sessionId, stepId, started, ordering)
            response.results.forEach {
                cacheTimeKeeper(it, currentUserId)
            }
            Result.success(response)
        } catch (e: Exception) {
            try {
                val effectiveLimit = limit ?: 10
                val effectiveOffset = offset ?: 0

                val cachedItems = when {
                    started != null && sessionId != null ->
                        timeKeeperDao.getBySessionStartedAndUser(sessionId, started, currentUserId, effectiveLimit, effectiveOffset).first()
                    started != null && stepId != null ->
                        timeKeeperDao.getByStepStartedAndUser(stepId, started, currentUserId, effectiveLimit, effectiveOffset).first()
                    started != null ->
                        timeKeeperDao.getByStartedAndUser(started, currentUserId, effectiveLimit, effectiveOffset).first()

                    sessionId != null ->
                        timeKeeperDao.getBySessionAndUser(sessionId, currentUserId, effectiveLimit, effectiveOffset).first()
                    stepId != null ->
                        timeKeeperDao.getByStepAndUser(stepId, currentUserId, effectiveLimit, effectiveOffset).first()
                    else ->
                        timeKeeperDao.getAllByUser(currentUserId, effectiveLimit, effectiveOffset).first()
                }

                val totalCount = cachedItems.size
                val domainObjects = cachedItems.map { it.toDomainModel() }

                val response = LimitOffsetResponse(
                    count = totalCount,
                    next = null,
                    previous = null,
                    results = domainObjects
                )
                Result.success(response)
            } catch (cacheException: Exception) {
                Result.failure(e)
            }
        }
    }

    private suspend fun getCurrentUserId(): Int? {
        val repoUser = userRepository.getCurrentUser()

        if (repoUser.isSuccess) {
            return repoUser.getOrNull()?.id
        }

        val preferences = userPreferencesDao.getCurrentlyActivePreference()
        if (preferences != null) {
            val userFromDb = userRepository.getUserFromActivePreference()
            return userFromDb?.id
        }

        return null
    }

    private suspend fun cacheTimeKeeper(timeKeeper: TimeKeeper, userId: Int) {
        timeKeeperDao.insert(TimeKeeperEntity(
            id = timeKeeper.id,
            startTime = timeKeeper.startTime,
            session = timeKeeper.session,
            step = timeKeeper.step,
            started = timeKeeper.started,
            currentDuration = timeKeeper.currentDuration,
            userId = userId
        ))
    }

    private fun TimeKeeperEntity.toDomainModel(): TimeKeeper {
        return TimeKeeper(
            id = id,
            startTime = startTime,
            session = session,
            step = step,
            started = started,
            currentDuration = currentDuration
        )
    }

    // Update other methods to use the getCurrentUserId helper
    override suspend fun getTimeKeeperById(id: Int): Result<TimeKeeper> {
        return try {
            val response = apiService.getTimeKeeperById(id)
            val userId = getCurrentUserId()
            if (userId != null) {
                cacheTimeKeeper(response, userId)
            }
            Result.success(response)
        } catch (e: Exception) {
            val cachedObject = timeKeeperDao.getById(id)
            if (cachedObject != null) {
                Result.success(cachedObject.toDomainModel())
            } else {
                Result.failure(e)
            }
        }
    }

    override suspend fun createTimeKeeper(timeKeeper: TimeKeeper): Result<TimeKeeper> {
        return try {
            val response = apiService.createTimeKeeper(timeKeeper)
            val userId = getCurrentUserId()
            if (userId != null) {
                cacheTimeKeeper(response, userId)
            }
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateTimeKeeper(id: Int, timeKeeper: TimeKeeper): Result<TimeKeeper> {
        return try {
            val response = apiService.updateTimeKeeper(id, timeKeeper)
            val userId = getCurrentUserId()
            if (userId != null) {
                cacheTimeKeeper(response, userId)
            }
            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun deleteTimeKeeper(id: Int): Result<Unit> {
        return try {
            apiService.deleteTimeKeeper(id)
            timeKeeperDao.getById(id)?.let { timeKeeperDao.delete(it) }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}