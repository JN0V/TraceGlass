package io.github.jn0v.traceglass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.jn0v.traceglass.feature.onboarding.OnboardingMode
import io.github.jn0v.traceglass.feature.onboarding.OnboardingRepository
import io.github.jn0v.traceglass.feature.onboarding.OnboardingScreen
import io.github.jn0v.traceglass.feature.onboarding.SetupGuideScreen
import io.github.jn0v.traceglass.feature.tracing.TracingScreen
import io.github.jn0v.traceglass.feature.tracing.settings.AboutScreen
import io.github.jn0v.traceglass.feature.tracing.settings.SettingsScreen
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.koin.android.ext.android.inject

class MainActivity : ComponentActivity() {

    private val onboardingRepository: OnboardingRepository by inject()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val onboardingDone = runBlocking { onboardingRepository.isOnboardingCompleted.first() }

        setContent {
            MaterialTheme {
                TraceGlassNavigation(startOnboarding = !onboardingDone)
            }
        }
    }
}

@Composable
private fun TraceGlassNavigation(startOnboarding: Boolean) {
    val navController = rememberNavController()
    val startDestination = if (startOnboarding) "onboarding" else "tracing"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("onboarding") {
            OnboardingScreen(
                onComplete = {
                    navController.navigate("tracing") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                },
                onNavigateToGuide = {
                    navController.navigate("setup-guide")
                }
            )
        }
        composable("setup-guide") {
            SetupGuideScreen(
                onBack = { navController.popBackStack() }
            )
        }
        composable("tracing") {
            TracingScreen(
                onNavigateToSettings = {
                    navController.navigate("settings")
                }
            )
        }
        composable("settings") {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onReopenOnboarding = {
                    navController.navigate("onboarding-reopen")
                },
                onAbout = {
                    navController.navigate("about")
                }
            )
        }
        composable("about") {
            AboutScreen(
                versionName = BuildConfig.VERSION_NAME,
                versionCode = BuildConfig.VERSION_CODE,
                onBack = { navController.popBackStack() }
            )
        }
        composable("onboarding-reopen") {
            OnboardingScreen(
                mode = OnboardingMode.REOPENED,
                onComplete = {
                    navController.popBackStack()
                },
                onNavigateToGuide = {
                    navController.navigate("setup-guide")
                }
            )
        }
    }
}
