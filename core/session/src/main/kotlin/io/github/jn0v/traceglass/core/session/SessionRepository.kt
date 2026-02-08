package io.github.jn0v.traceglass.core.session

import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    val sessionData: Flow<SessionData>
    suspend fun save(data: SessionData)
    suspend fun clear()
}
