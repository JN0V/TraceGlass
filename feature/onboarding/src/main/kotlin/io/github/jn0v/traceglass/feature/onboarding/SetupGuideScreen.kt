package io.github.jn0v.traceglass.feature.onboarding

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Setup Guide") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
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
                .verticalScroll(rememberScrollState())
        ) {
            SectionSelector(
                selectedSection = uiState.selectedSection,
                onSectionSelected = viewModel::onSectionSelected
            )

            Spacer(modifier = Modifier.height(16.dp))

            when (uiState.selectedSection) {
                SetupGuideSection.MARKER_GUIDE -> MarkerGuideContent()
                SetupGuideSection.MACGYVER_GUIDE -> MacGyverGuideContent()
            }

            Spacer(modifier = Modifier.height(24.dp))

            ExternalLinksSection()

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
            label = { Text("Marker Guide") }
        )
        FilterChip(
            selected = selectedSection == SetupGuideSection.MACGYVER_GUIDE,
            onClick = { onSectionSelected(SetupGuideSection.MACGYVER_GUIDE) },
            label = { Text("Stand Setup") }
        )
    }
}

@Composable
private fun MarkerGuideContent() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "ArUco Marker Guide",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "TraceGlass uses ArUco markers (small black-and-white grid patterns) for tracking. Place two markers at opposite corners of your drawing area.",
            style = MaterialTheme.typography.bodyLarge
        )

        StepCard(
            step = "1",
            title = "Print or display markers",
            instructions = "Download the marker sheet from the link below, or search for \"ArUco 4x4 marker 0\" and \"ArUco 4x4 marker 1\" online. Print them at about 2 cm each."
        )
        StepCard(
            step = "2",
            title = "Place at top corners",
            instructions = "Cut out markers #0 and #1. Tape them at the top-left and top-right corners of your drawing area, just outside the zone you want to trace. This keeps them visible while you draw."
        )
        StepCard(
            step = "3",
            title = "Position your phone",
            instructions = "Place your phone about 25\u201330 cm above the drawing surface, camera pointing down. It should be high enough to see both markers, but low enough to trace comfortably."
        )
        StepCard(
            step = "4",
            title = "Keep markers visible",
            instructions = "Make sure both markers stay visible to the camera while you draw. Avoid covering them with your hand or paper."
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
                .fillMaxWidth(),
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
            text = "Improvised Phone Stand",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "No tripod? No problem. Use everyday objects to hold your phone about 25\u201330 cm above your drawing surface, camera pointing down.",
            style = MaterialTheme.typography.bodyLarge
        )

        StandOptionCard(
            title = "Glass of Water",
            instructions = "Place a tall glass of water next to your paper. Lean your phone against it, camera pointing down. The glass provides stable support and the water adds weight. Aim for about 25\u201330 cm above the surface."
        )
        StandOptionCard(
            title = "Stack of Books",
            instructions = "Stack 3\u20134 books at the edge of your desk to reach about 25\u201330 cm high. Prop your phone between the top two books, angled downward toward your drawing area."
        )
        StandOptionCard(
            title = "Box or Container",
            instructions = "Place a box or container (about 25\u201330 cm tall) upside down near your paper. Balance your phone on the edge with the camera hanging over, pointing down. Use a rubber band to secure if needed."
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
            modifier = Modifier.padding(16.dp)
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
private fun ExternalLinksSection() {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Downloads",
            style = MaterialTheme.typography.titleMedium
        )
        OutlinedButton(
            onClick = { },
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Printable marker sheet \u2014 coming soon")
        }
        OutlinedButton(
            onClick = { },
            enabled = false,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("3D phone stand model \u2014 coming soon")
        }
    }
}
