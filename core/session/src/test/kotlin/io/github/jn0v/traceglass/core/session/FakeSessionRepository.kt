package io.github.jn0v.traceglass.core.session

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class FakeSessionRepository : SessionRepository {

    private val _sessionData = MutableStateFlow(SessionData())
    override val sessionData: Flow<SessionData> = _sessionData.asStateFlow()

    var saveCount: Int = 0
        private set
    var clearCount: Int = 0
        private set

    override suspend fun save(data: SessionData) {
        saveCount++
        _sessionData.value = data
    }

    override suspend fun clear() {
        clearCount++
        _sessionData.value = SessionData()
    }
}
