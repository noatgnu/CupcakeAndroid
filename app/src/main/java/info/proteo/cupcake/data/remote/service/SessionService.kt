package info.proteo.cupcake.data.remote.service

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
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
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import javax.inject.Inject
import javax.inject.Singleton

@JsonClass(generateAdapter = true)
data class SessionCreateRequest(
    @Json(name = "protocol_ids") val protocolIds: List<Int>
)

interface SessionApiService {
    @GET("api/session/")
    suspend fun getSessions(
        @Query("offset") offset: Int? = null,
        @Query("limit") limit: Int? = null,
        @Query("search") search: String? = null
    ): LimitOffsetResponse<Session>

    @GET("api/session/{unique_id}/")
    suspend fun getSessionByUniqueId(
        @Path("unique_id") uniqueId: String
    ): Session

    @POST("api/session/")
    suspend fun createSession(
        @Body requestBody: SessionCreateRequest
    ): Session

    @PUT("api/session/{unique_id}/")
    suspend fun updateSession(
        @Path("unique_id") uniqueId: String,
        @Body session: Session
    ): Session

    @DELETE("api/session/{unique_id}/")
    suspend fun deleteSession(
        @Path("unique_id") uniqueId: String
    ): Response<Unit>

    @GET("api/session/get_user_sessions/")
    suspend fun getUserSessions(
        @Query("limit") limit: Int? = null,
        @Query("offset") offset: Int? = null,
        @Query("search") search: String? = null
    ): LimitOffsetResponse<Session>

    @GET("api/session/{unique_id}/get_associated_protocol_titles/")
    suspend fun getAssociatedProtocolTitles(
        @Path("unique_id") uniqueId: String
    ): List<ProtocolModel>

    @POST("api/session/{unique_id}/add_protocol/")
    suspend fun addProtocol(
        @Path("unique_id") uniqueId: String,
        @Body request: Map<String, Int>
    ): Session

    @POST("api/session/{unique_id}/remove_protocol/")
    suspend fun removeProtocol(
        @Path("unique_id") uniqueId: String,
        @Body request: Map<String, Int>
    ): Session

    @GET("api/session/calendar_get_sessions/")
    suspend fun calendarGetSessions(
        @Query("start") startDate: String,
        @Query("end") endDate: String
    ): List<Session>
}

interface SessionService {
    suspend fun getSessions(
        offset: Int? = null,
        limit: Int? = null,
        search: String? = null
    ): Result<LimitOffsetResponse<Session>>

    suspend fun getSessionByUniqueId(uniqueId: String): Result<Session>
    suspend fun createSession(requestBody: SessionCreateRequest): Result<Session>
    suspend fun updateSession(uniqueId: String, session: Session): Result<Session>
    suspend fun deleteSession(uniqueId: String): Response<Unit>

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
        return try {
            val response = sessionApiService.getSessions(offset, limit, search)

            withContext(dispatcherProvider) {
                response.results.forEach { session ->
                    sessionDao.insert(session.toEntity())
                }
            }

            Result.success(response)
        } catch (e: Exception) {
            val effectiveLimit = limit ?: 20
            val effectiveOffset = offset ?: 0

            try {
                val cachedSessions = withContext(dispatcherProvider) {
                    sessionDao.searchSessions(search, effectiveLimit, effectiveOffset).first()
                        .map { sessionEntity -> sessionEntity.toDomainModel() }
                }

                val totalCount = withContext(dispatcherProvider) {
                    sessionDao.countSearchSessions(search).first()
                }

                val response = LimitOffsetResponse(
                    count = totalCount,
                    next = if (effectiveOffset + effectiveLimit < totalCount) "next" else null,
                    previous = if (effectiveOffset > 0) "previous" else null,
                    results = cachedSessions
                )

                Result.success(response)
            } catch (innerEx: Exception) {
                Result.failure(innerEx)
            }
        }
    }

    override suspend fun getSessionByUniqueId(uniqueId: String): Result<Session> {
        return try {
            val cachedSession = sessionDao.getByUniqueId(uniqueId)?.toDomainModel()

            if (cachedSession != null) {
                Result.success(cachedSession)
            } else {
                val apiSession = sessionApiService.getSessionByUniqueId(uniqueId)

                withContext(dispatcherProvider) {
                    sessionDao.insert(apiSession.toEntity())
                }

                Result.success(apiSession)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun createSession(requestBody: SessionCreateRequest): Result<Session> {
        return try {
            val session = sessionApiService.createSession(requestBody)

            withContext(dispatcherProvider) {
                sessionDao.insert(session.toEntity())
            }

            Result.success(session)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun updateSession(uniqueId: String, session: Session): Result<Session> {
        return try {
            val updatedSession = sessionApiService.updateSession(uniqueId, session)

            // Update cache
            withContext(dispatcherProvider) {
                sessionDao.insert(updatedSession.toEntity())
            }

            Result.success(updatedSession)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    // Fix return type to match interface
    override suspend fun deleteSession(uniqueId: String): Response<Unit> {
        return try {
            val response = sessionApiService.deleteSession(uniqueId)

            // Remove from cache
            withContext(dispatcherProvider) {
                val entity = sessionDao.getByUniqueId(uniqueId)
                if (entity != null) {
                    sessionDao.delete(entity)
                }
            }

            response
        } catch (e: Exception) {
            throw e
        }
    }

    override suspend fun getUserSessions(
        limit: Int?,
        offset: Int?,
        search: String?
    ): Result<LimitOffsetResponse<Session>> {
        return try {
            val response = sessionApiService.getUserSessions(limit, offset, search)

            withContext(dispatcherProvider) {
                response.results.forEach { session ->
                    sessionDao.insert(session.toEntity())
                }
            }

            Result.success(response)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun getAssociatedProtocolTitles(uniqueId: String): Result<List<ProtocolModel>> {
        return try {
            val protocols = sessionApiService.getAssociatedProtocolTitles(uniqueId)
            Result.success(protocols)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun addProtocol(uniqueId: String, request: Map<String, Int>): Result<Session> {
        return try {
            val updatedSession = sessionApiService.addProtocol(uniqueId, request)

            // Update cache
            withContext(dispatcherProvider) {
                sessionDao.insert(updatedSession.toEntity())
            }

            Result.success(updatedSession)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun removeProtocol(uniqueId: String, request: Map<String, Int>): Result<Session> {
        return try {
            val updatedSession = sessionApiService.removeProtocol(uniqueId, request)

            withContext(dispatcherProvider) {
                sessionDao.insert(updatedSession.toEntity())
            }

            Result.success(updatedSession)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    override suspend fun calendarGetSessions(startDate: String, endDate: String): Result<List<Session>> {
        return try {
            val sessions = sessionApiService.calendarGetSessions(startDate, endDate)

            withContext(dispatcherProvider) {
                sessions.forEach { session ->
                    sessionDao.insert(session.toEntity())
                }
            }

            Result.success(sessions)
        } catch (e: Exception) {
            Result.failure(e)
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
            endedAt = endedAt,
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
            timeKeeper = emptyList(),
            projects = emptyList()
        )
    }
}