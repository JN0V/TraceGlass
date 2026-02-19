package io.github.jn0v.traceglass.feature.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@Composable
fun OnboardingScreen(
    viewModel: OnboardingViewModel = koinViewModel(),
    onComplete: () -> Unit,
    onSkip: () -> Unit = onComplete,
    onNavigateToGuide: () -> Unit = {},
    mode: OnboardingMode = OnboardingMode.FIRST_TIME
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { OnboardingViewModel.PAGE_COUNT })

    BackHandler(enabled = uiState.currentPage > 0) {
        viewModel.onPreviousPage()
    }

    LaunchedEffect(mode) {
        if (mode == OnboardingMode.REOPENED) {
            viewModel.onReopen()
        }
    }

    LaunchedEffect(uiState.isCompleted, uiState.wasSkipped) {
        if (uiState.isCompleted) {
            if (uiState.wasSkipped) onSkip() else onComplete()
        }
    }

    LaunchedEffect(uiState.currentPage) {
        if (pagerState.settledPage != uiState.currentPage) {
            pagerState.animateScrollToPage(uiState.currentPage)
        }
    }

    LaunchedEffect(pagerState) {
        snapshotFlow { pagerState.settledPage }.collect { page ->
            viewModel.onPageChanged(page)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        TextButton(
            onClick = viewModel::onSkip,
            modifier = Modifier.align(Alignment.End)
        ) {
            Text(stringResource(R.string.onboarding_skip))
        }

        HorizontalPager(
            state = pagerState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) { page ->
            when (page) {
                0 -> WelcomePage()
                1 -> TierSelectionPage(
                    selectedTier = uiState.selectedTier,
                    onTierSelected = viewModel::onTierSelected
                )
                2 -> MarkerPreparationPage(
                    selectedTier = uiState.selectedTier,
                    onViewGuide = onNavigateToGuide
                )
            }
        }

        PageIndicator(
            pageCount = OnboardingViewModel.PAGE_COUNT,
            currentPage = pagerState.settledPage
        )

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = {
                if (uiState.currentPage < OnboardingViewModel.PAGE_COUNT - 1) viewModel.onNextPage()
                else viewModel.onComplete()
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                if (uiState.currentPage < OnboardingViewModel.PAGE_COUNT - 1)
                    stringResource(R.string.onboarding_next)
                else
                    stringResource(R.string.onboarding_get_started)
            )
        }
    }
}

@Composable
private fun PageIndicator(pageCount: Int, currentPage: Int) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.semantics {
            contentDescription = "Page ${currentPage + 1} of $pageCount"
        }
    ) {
        repeat(pageCount) { index ->
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(
                        if (index == currentPage) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                    )
                    .semantics {
                        contentDescription = if (index == currentPage) {
                            "Page ${index + 1}, current"
                        } else {
                            "Page ${index + 1}"
                        }
                    }
            )
        }
    }
}
