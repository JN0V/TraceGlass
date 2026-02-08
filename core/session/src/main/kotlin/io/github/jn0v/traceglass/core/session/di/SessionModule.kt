package io.github.jn0v.traceglass.core.session.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import io.github.jn0v.traceglass.core.session.DataStoreSessionRepository
import io.github.jn0v.traceglass.core.session.SessionRepository
import org.koin.dsl.module

private val Context.sessionDataStore by preferencesDataStore(name = "session_prefs")

val sessionModule = module {
    single<SessionRepository> { DataStoreSessionRepository(get<Context>().sessionDataStore) }
}
