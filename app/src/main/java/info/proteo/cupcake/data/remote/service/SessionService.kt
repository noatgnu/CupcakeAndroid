package info.proteo.cupcake.data.remote.service

import info.proteo.cupcake.data.local.dao.protocol.SessionDao
import info.proteo.cupcake.data.local.entity.protocol.SessionEntity
import info.proteo.cupcake.data.remote.model.LimitOffsetResponse
import info.proteo.cupcake.data.remote.model.protocol.ProtocolModel
import info.proteo.cupcake.data.remote.model.protocol.Session
import info.proteo.cupcake.data.repository.UserRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.withContext
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

interface SessionApiService {
    @GET("sessions/")
    suspend fun getSessions(
        @Query("offset") offset: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("search") search: String? = null
    ): Result<LimitOffsetResponse<Session>>

    @GET("sessions/{unique_id}/")
    suspend fun getSessionByUniqueId(
        @Path("unique_id") uniqueId: String
    ): Result<Session>

    @POST("sessions/")
    suspend fun createSession(
        @Body requestBody: Map<String, List<Int>> = emptyMap()
    ): Result<Session>

    @PUT("sessions/{unique_id}/")
    suspend fun updateSession(
        @Path("unique_id") uniqueId: String,
        @Body session: Session
    ): Result<Session>

    @DELETE("sessions/{unique_id}/")
    suspend fun deleteSession(
        @Path("unique_id") uniqueId: String
    ): Result<Unit>

    @GET("sessions/get_user_sessions/")
    suspend fun getUserSessions(
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null,
        @Query("search") search: String? = null
    ): Result<LimitOffsetResponse<Session>>

    @GET("sessions/{unique_id}/get_associated_protocol_titles/")
    suspend fun getAssociatedProtocolTitles(
        @Path("unique_id") uniqueId: String
    ): Result<List<ProtocolModel>>

    @POST("sessions/{unique_id}/add_protocol/")
    suspend fun addProtocol(
        @Path("unique_id") uniqueId: String,
        @Body request: Map<String, Int>
    ): Result<Session>

    @POST("sessions/{unique_id}/remove_protocol/")
    suspend fun removeProtocol(
        @Path("unique_id") uniqueId: String,
        @Body request: Map<String, Int>
    ): Result<Session>

    @GET("sessions/calendar_get_sessions/")
    suspend fun calendarGetSessions(
        @Query("start") startDate: String,
        @Query("end") endDate: String
    ): Result<List<Session>>
}

interface SessionService {
    suspend fun getSessions(
        offset: Int? = null,
        limit: Int? = null,
        search: String? = null
    ): Result<LimitOffsetResponse<Session>>

    suspend fun getSessionByUniqueId(uniqueId: String): Result<Session>
    suspend fun createSession(requestBody: Map<String, List<Int>> = emptyMap()): Result<Session>
    suspend fun updateSession(uniqueId: String, session: Session): Result<Session>
    suspend fun deleteSession(uniqueId: String): Result<Unit>

    suspend fun getUserSessions(
        limit: Int? = null,
        offset: Int? = null,
        search: String? = null
    ): Result<LimitOffsetResponse<Session>>

    suspend fun getAssociatedProtocolTitles(uniqueId: String): Result<List<ProtocolModel>>
    suspend fun addProtocol(uniqueId: String, request: Map<String, Int>): Result<Session>
    suspend fun removeProtocol(uniqueId: String, request: Map<String, Int>): Result<Session>

    suspend fun calendarGetSessions(startDate: String, endDate: String): Result<List<Session>>
}


@Singleton
class SessionServiceImpl @Inject constructor(
    private val sessionApiService: SessionApiService,
    private val sessionDao: SessionDao,
    private val userRepository: UserRepository,
    private val dispatcherProvider: CoroutineDispatcher = Dispatchers.IO
) : SessionService {

    override suspend fun getSessions(
        offset: Int?,
        limit: Int?,
        search: String?
    ): Result<LimitOffsetResponse<Session>> {
        val networkResult = sessionApiService.getSessions(offset, limit, search)

        if (networkResult.isSuccess) {
            networkResult.getOrNull()?.let { response ->
                withContext(dispatcherProvider) {
                    response.results.forEach { session ->
                        sessionDao.insert(session.toEntity())
                    }
                }
            }
            return networkResult
        }

        val effectiveLimit = limit ?: 20
        val effectiveOffset = offset ?: 0

        val cachedSessions = withContext(dispatcherProvider) {
            try {
                sessionDao.searchSessions(
                    search = search,
                    limit = effectiveLimit,
                    offset = effectiveOffset
                ).firstOrNull()?.map { it.toDomainModel() }
                    ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }
        if (cachedSessions.isNotEmpty()) {
            val count = withContext(dispatcherProvider) {
                sessionDao.countSearchSessions(search).first()
            }
            return Result.success(
                LimitOffsetResponse(
                    count = count,
                    next = if (cachedSessions.size == effectiveLimit) "yes" else null,
                    previous = if (effectiveOffset > 0) "yes" else null,
                    results = cachedSessions
                )
            )
        } else {
            return networkResult
        }
    }

    override suspend fun getSessionByUniqueId(uniqueId: String): Result<Session> {
        val networkResult = sessionApiService.getSessionByUniqueId(uniqueId)

        if (networkResult.isSuccess) {
            networkResult.getOrNull()?.let { session ->
                withContext(dispatcherProvider) {
                    sessionDao.insert(session.toEntity())
                }
            }
            return networkResult
        }

        val cachedSession = withContext(dispatcherProvider) {
            sessionDao.getByUniqueId(uniqueId)
        }

        return if (cachedSession != null) {
            Result.success(cachedSession.toDomainModel())
        } else {
            networkResult
        }
    }

    override suspend fun createSession(requestBody: Map<String, List<Int>>): Result<Session> {
        val result = sessionApiService.createSession(requestBody)

        result.onSuccess { session ->
            withContext(dispatcherProvider) {
                sessionDao.insert(session.toEntity())
            }
        }

        return result
    }

    override suspend fun updateSession(uniqueId: String, session: Session): Result<Session> {
        val result = sessionApiService.updateSession(uniqueId, session)

        // Update cache with new data on success
        result.onSuccess { updatedSession ->
            withContext(dispatcherProvider) {
                sessionDao.insert(updatedSession.toEntity())
            }
        }

        return result
    }

    override suspend fun deleteSession(uniqueId: String): Result<Unit> {
        val result = sessionApiService.deleteSession(uniqueId)

        // Remove from cache if network delete was successful
        result.onSuccess {
            withContext(dispatcherProvider) {
                sessionDao.getByUniqueId(uniqueId)?.let {
                    sessionDao.delete(it)
                }
            }
        }

        return result
    }

    override suspend fun getUserSessions(
        limit: Int?,
        offset: Int?,
        search: String?
    ): Result<LimitOffsetResponse<Session>> {
        val networkResult = sessionApiService.getUserSessions(limit, offset, search)

        if (networkResult.isSuccess) {
            networkResult.getOrNull()?.let { response ->
                withContext(dispatcherProvider) {
                    response.results.forEach { session ->
                        sessionDao.insert(session.toEntity())
                    }
                }
            }
            return networkResult
        }

        if (search.isNullOrEmpty() && offset == 0) {
            val user = userRepository.getUserFromActivePreference() ?: return networkResult
            val userId = user.id
            val cachedSessions = withContext(dispatcherProvider) {
                try {
                    sessionDao.getByUser(userId)
                        .first()
                        .map { it.toDomainModel() }
                } catch (e: Exception) {
                    emptyList()
                }
            }

            if (cachedSessions.isNotEmpty()) {
                val response = LimitOffsetResponse(
                    count = cachedSessions.size,
                    next = null,
                    previous = null,
                    results = cachedSessions
                )
                return Result.success(response)
            }
        }

        return networkResult
    }

    override suspend fun getAssociatedProtocolTitles(uniqueId: String): Result<List<ProtocolModel>> {
        return sessionApiService.getAssociatedProtocolTitles(uniqueId)
    }

    override suspend fun addProtocol(uniqueId: String, request: Map<String, Int>): Result<Session> {
        val result = sessionApiService.addProtocol(uniqueId, request)

        result.onSuccess { updatedSession ->
            withContext(dispatcherProvider) {
                sessionDao.insert(updatedSession.toEntity())
            }
        }

        return result
    }

    override suspend fun removeProtocol(uniqueId: String, request: Map<String, Int>): Result<Session> {
        val result = sessionApiService.removeProtocol(uniqueId, request)

        result.onSuccess { updatedSession ->
            withContext(dispatcherProvider) {
                sessionDao.insert(updatedSession.toEntity())
            }
        }

        return result
    }

    override suspend fun calendarGetSessions(startDate: String, endDate: String): Result<List<Session>> {
        val networkResult = sessionApiService.calendarGetSessions(startDate, endDate)

        if (networkResult.isSuccess) {
            networkResult.getOrNull()?.let { sessions ->
                withContext(dispatcherProvider) {
                    sessions.forEach { session ->
                        sessionDao.insert(session.toEntity())
                    }
                }
            }
            return networkResult
        }

        val cachedSessions = withContext(dispatcherProvider) {
            try {
                sessionDao.getSessionsByDateRange(startDate, endDate).firstOrNull()?.map { it.toDomainModel() }
                    ?: emptyList()
            } catch (e: Exception) {
                emptyList()
            }
        }

        return if (cachedSessions.isNotEmpty()) {
            Result.success(cachedSessions)
        } else {
            networkResult
        }
    }

    private fun Session.toEntity(): SessionEntity {
        return SessionEntity(
            id = id,
            user = user,
            uniqueId = uniqueId,
            enabled = enabled,
            createdAt = createdAt,
            updatedAt = updatedAt,
            name = name,
            startedAt = startedAt,
            endedAt = endedAt
        )
    }

    private fun SessionEntity.toDomainModel(): Session {
        return Session(
            id = id,
            user = user,
            uniqueId = uniqueId,
            enabled = enabled,
            createdAt = createdAt,
            updatedAt = updatedAt,
            name = name,
            startedAt = startedAt,
            endedAt = endedAt,
            protocols = emptyList(),
            timeKeeper = emptyList(),
            projects = emptyList()
        )
    }
}