package io.github.jn0v.traceglass

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.jn0v.traceglass.feature.onboarding.OnboardingMode
import io.github.jn0v.traceglass.feature.onboarding.OnboardingRepository
import io.github.jn0v.traceglass.feature.onboarding.OnboardingScreen
import io.github.jn0v.traceglass.feature.onboarding.SetupGuideScreen
import io.github.jn0v.traceglass.feature.onboarding.WalkthroughScreen
import io.github.jn0v.traceglass.feature.tracing.TracingScreen
import io.github.jn0v.traceglass.feature.tracing.settings.AboutScreen
import io.github.jn0v.traceglass.feature.tracing.settings.SettingsScreen
import org.koin.compose.koinInject

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            MaterialTheme {
                TraceGlassNavigation()
            }
        }
    }
}

@Composable
private fun TraceGlassNavigation() {
    val onboardingRepository: OnboardingRepository = koinInject()
    val onboardingCompleted by onboardingRepository.isOnboardingCompleted
        .collectAsStateWithLifecycle(initialValue = null)

    // Wait for DataStore to load — avoids runBlocking on main thread
    val completed = onboardingCompleted
    if (completed == null) {
        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        return
    }

    val navController = rememberNavController()
    val startDestination = if (completed) "tracing" else "onboarding"

    NavHost(navController = navController, startDestination = startDestination) {
        composable("onboarding") {
            OnboardingScreen(
                onComplete = {
                    navController.navigate("walkthrough") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                },
                onSkip = {
                    navController.navigate("tracing") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                },
                onNavigateToGuide = {
                    navController.navigate("setup-guide")
                }
            )
        }
        composable("walkthrough") {
            WalkthroughScreen(
                onComplete = {
                    navController.navigate("tracing") {
                        popUpTo("walkthrough") { inclusive = true }
                    }
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
                onSetupGuide = {
                    navController.navigate("setup-guide")
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
