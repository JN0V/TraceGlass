package io.github.jn0v.traceglass.feature.onboarding

import io.github.jn0v.traceglass.core.session.SessionData
import io.github.jn0v.traceglass.core.session.SessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

// NOTE: duplicated in feature/tracing and core/session tests — keep in sync (testFixtures incompatible with AGP 8.8)
class FakeSessionRepository : SessionRepository {

    private val _sessionData = MutableStateFlow(SessionData())
    override val sessionData: Flow<SessionData> = _sessionData.asStateFlow()

    var saveCount: Int = 0
        private set
    var clearCount: Int = 0
        private set

    fun lastSavedData(): SessionData = _sessionData.value

    override suspend fun save(data: SessionData) {
        saveCount++
        _sessionData.value = data
    }

    override suspend fun clear() {
        clearCount++
        _sessionData.value = SessionData()
    }
}
