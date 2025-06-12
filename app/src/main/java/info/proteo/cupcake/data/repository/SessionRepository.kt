package info.proteo.cupcake.data.repository

import com.squareup.moshi.Json
import info.proteo.cupcake.data.local.dao.protocol.RecentSessionDao
import info.proteo.cupcake.data.local.entity.protocol.RecentSessionEntity
import info.proteo.cupcake.data.remote.model.LimitOffsetResponse
import info.proteo.cupcake.data.remote.model.protocol.ProtocolModel
import info.proteo.cupcake.data.remote.model.protocol.Session
import info.proteo.cupcake.data.remote.service.SessionCreateRequest
import info.proteo.cupcake.data.remote.service.SessionService
import retrofit2.Response
import javax.inject.Inject
import javax.inject.Singleton




@Singleton
class SessionRepository @Inject constructor(
    private val sessionService: SessionService,
    private val recentSessionDao: RecentSessionDao
) {
    suspend fun getSessions(
        offset: Int? = null,
        limit: Int? = null,
        search: String? = null
    ): Result<LimitOffsetResponse<Session>> {
        return sessionService.getSessions(offset, limit, search)
    }

    suspend fun getSessionByUniqueId(uniqueId: String): Result<Session> {
        return sessionService.getSessionByUniqueId(uniqueId)
    }

    suspend fun createSession(requestBody: SessionCreateRequest): Result<Session> {
        return sessionService.createSession(requestBody)
    }

    suspend fun updateSession(uniqueId: String, session: Session): Result<Session> {
        return sessionService.updateSession(uniqueId, session)
    }

    suspend fun deleteSession(uniqueId: String): Response<Unit> {
        return sessionService.deleteSession(uniqueId)
    }

    suspend fun getUserSessions(
        limit: Int? = null,
        offset: Int? = null,
        search: String? = null
    ): Result<LimitOffsetResponse<Session>> {
        return sessionService.getUserSessions(limit, offset, search)
    }

    suspend fun getAssociatedProtocolTitles(uniqueId: String): Result<List<ProtocolModel>> {
        return sessionService.getAssociatedProtocolTitles(uniqueId)
    }

    suspend fun addProtocol(uniqueId: String, request: Map<String, Int>): Result<Session> {
        return sessionService.addProtocol(uniqueId, request)
    }

    suspend fun removeProtocol(uniqueId: String, request: Map<String, Int>): Result<Session> {
        return sessionService.removeProtocol(uniqueId, request)
    }

    suspend fun calendarGetSessions(startDate: String, endDate: String): Result<List<Session>> {
        return sessionService.calendarGetSessions(startDate, endDate)
    }

    suspend fun getMostRecentSessionForUser(userId: Int): RecentSessionEntity? {
        return recentSessionDao.getMostRecentSession(userId)
    }

}