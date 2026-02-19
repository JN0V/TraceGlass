package io.github.jn0v.traceglass.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import org.koin.androidx.compose.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupGuideScreen(
    viewModel: SetupGuideViewModel = koinViewModel(),
    onBack: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    SetupGuideContent(
        uiState = uiState,
        onSectionSelected = viewModel::onSectionSelected,
        onBack = onBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun SetupGuideContent(
    uiState: SetupGuideUiState,
    onSectionSelected: (SetupGuideSection) -> Unit,
    onBack: () -> Unit
) {
    val scrollState = rememberScrollState()

    LaunchedEffect(uiState.selectedSection) {
        scrollState.scrollTo(0)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.guide_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.guide_back)
                        )
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp)
                .verticalScroll(scrollState)
        ) {
            SectionSelector(
                selectedSection = uiState.selectedSection,
                onSectionSelected = onSectionSelected
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (uiState.selectedSection) {
                SetupGuideSection.MARKER_GUIDE -> MarkerGuideContent()
                SetupGuideSection.MACGYVER_GUIDE -> MacGyverGuideContent()
            }

            Spacer(modifier = Modifier.height(24.dp))

            ExternalLinksSection(selectedSection = uiState.selectedSection)

            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun SectionSelector(
    selectedSection: SetupGuideSection,
    onSectionSelected: (SetupGuideSection) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterChip(
            selected = selectedSection == SetupGuideSection.MARKER_GUIDE,
            onClick = { onSectionSelected(SetupGuideSection.MARKER_GUIDE) },
            label = { Text(stringResource(R.string.guide_section_markers)) },
            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
        )
        FilterChip(
            selected = selectedSection == SetupGuideSection.MACGYVER_GUIDE,
            onClick = { onSectionSelected(SetupGuideSection.MACGYVER_GUIDE) },
            label = { Text(stringResource(R.string.guide_section_stand)) },
            modifier = Modifier.defaultMinSize(minHeight = 48.dp)
        )
    }
}

@Composable
private fun MarkerGuideContent() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.guide_marker_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = stringResource(R.string.guide_marker_intro),
            style = MaterialTheme.typography.bodyLarge
        )

        StepCard(
            step = "1",
            title = stringResource(R.string.guide_marker_step1_title),
            instructions = stringResource(R.string.guide_marker_step1_body)
        )
        StepCard(
            step = "2",
            title = stringResource(R.string.guide_marker_step2_title),
            instructions = stringResource(R.string.guide_marker_step2_body)
        )
        StepCard(
            step = "3",
            title = stringResource(R.string.guide_marker_step3_title),
            instructions = stringResource(R.string.guide_marker_step3_body)
        )
        StepCard(
            step = "4",
            title = stringResource(R.string.guide_marker_step4_title),
            instructions = stringResource(R.string.guide_marker_step4_body)
        )
    }
}

@Composable
private fun StepCard(
    step: String,
    title: String,
    instructions: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth()
                .semantics(mergeDescendants = true) {},
            verticalAlignment = Alignment.Top
        ) {
            Text(
                text = step,
                style = MaterialTheme.typography.headlineMedium,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = instructions,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun MacGyverGuideContent() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = stringResource(R.string.guide_stand_title),
            style = MaterialTheme.typography.headlineSmall,
            modifier = Modifier.semantics { heading() }
        )
        Text(
            text = stringResource(R.string.guide_stand_intro),
            style = MaterialTheme.typography.bodyLarge
        )

        StandOptionCard(
            title = stringResource(R.string.guide_stand_glass_title),
            instructions = stringResource(R.string.guide_stand_glass_body)
        )
        StandOptionCard(
            title = stringResource(R.string.guide_stand_books_title),
            instructions = stringResource(R.string.guide_stand_books_body)
        )
        StandOptionCard(
            title = stringResource(R.string.guide_stand_box_title),
            instructions = stringResource(R.string.guide_stand_box_body)
        )
    }
}

@Composable
private fun StandOptionCard(
    title: String,
    instructions: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .padding(16.dp)
                .semantics(mergeDescendants = true) {}
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = instructions,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun ExternalLinksSection(selectedSection: SetupGuideSection) {
    val uriHandler = LocalUriHandler.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = stringResource(R.string.guide_downloads_title),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.semantics { heading() }
        )
        OutlinedButton(
            onClick = { uriHandler.openUri(SetupGuideViewModel.MARKER_SHEET_URL) },
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
        ) {
            Text(stringResource(R.string.guide_download_markers))
        }
        if (selectedSection == SetupGuideSection.MACGYVER_GUIDE) {
            OutlinedButton(
                onClick = { uriHandler.openUri(SetupGuideViewModel.STAND_MODEL_URL) },
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 48.dp)
            ) {
                Text(stringResource(R.string.guide_download_stand))
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun MarkerGuidePreview() {
    MaterialTheme {
        SetupGuideContent(
            uiState = SetupGuideUiState(selectedSection = SetupGuideSection.MARKER_GUIDE),
            onSectionSelected = {},
            onBack = {}
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun MacGyverGuidePreview() {
    MaterialTheme {
        SetupGuideContent(
            uiState = SetupGuideUiState(selectedSection = SetupGuideSection.MACGYVER_GUIDE),
            onSectionSelected = {},
            onBack = {}
        )
    }
}
