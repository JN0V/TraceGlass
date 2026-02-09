package io.github.jn0v.traceglass.feature.onboarding.di

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore
import io.github.jn0v.traceglass.feature.onboarding.DataStoreOnboardingRepository
import io.github.jn0v.traceglass.feature.onboarding.OnboardingRepository
import io.github.jn0v.traceglass.feature.onboarding.OnboardingViewModel
import io.github.jn0v.traceglass.feature.onboarding.SetupGuideViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

private val Context.onboardingDataStore by preferencesDataStore(name = "onboarding_prefs")

val onboardingModule = module {
    single<OnboardingRepository> { DataStoreOnboardingRepository(get<Context>().onboardingDataStore) }
    viewModel { OnboardingViewModel(get()) }
    viewModel { SetupGuideViewModel() }
}
