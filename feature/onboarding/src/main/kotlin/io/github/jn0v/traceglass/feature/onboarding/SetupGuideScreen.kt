package io.github.jn0v.traceglass.feature.onboarding

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
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
            text = "DIY Marker Drawing Guide",
            style = MaterialTheme.typography.headlineSmall
        )
        Text(
            text = "Draw these 5 shapes on paper to use as tracking markers. Make each shape about 3 cm wide and space them 10\u201315 cm apart on your drawing surface.",
            style = MaterialTheme.typography.bodyLarge
        )

        MarkerShapeCard(
            drawableRes = R.drawable.ic_marker_heart,
            shapeName = "Heart",
            instructions = "Draw a symmetrical heart shape. Start from the top center, curve down to the left, come to a point at the bottom, then curve back up to the right."
        )
        MarkerShapeCard(
            drawableRes = R.drawable.ic_marker_star,
            shapeName = "Star",
            instructions = "Draw a 5-pointed star. Start at the top point and draw each line connecting alternate points without lifting your pen."
        )
        MarkerShapeCard(
            drawableRes = R.drawable.ic_marker_cross,
            shapeName = "Cross",
            instructions = "Draw a plus sign (+) with equal-length arms. Use a ruler for clean straight lines."
        )
        MarkerShapeCard(
            drawableRes = R.drawable.ic_marker_circle,
            shapeName = "Circle",
            instructions = "Draw a circle freehand or trace around a coin or bottle cap for a clean edge."
        )
        MarkerShapeCard(
            drawableRes = R.drawable.ic_marker_diamond,
            shapeName = "Diamond",
            instructions = "Draw a diamond (rotated square). Four equal sides meeting at points at the top, bottom, left, and right."
        )
    }
}

@Composable
private fun MarkerShapeCard(
    drawableRes: Int,
    shapeName: String,
    instructions: String
) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = drawableRes),
                contentDescription = "$shapeName marker shape",
                modifier = Modifier
                    .size(64.dp)
                    .semantics { contentDescription = "$shapeName marker outline" }
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = shapeName,
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
            text = "No tripod? No problem. Use everyday objects to hold your phone above your drawing surface.",
            style = MaterialTheme.typography.bodyLarge
        )

        StandOptionCard(
            title = "Glass of Water",
            instructions = "Place a tall glass of water next to your paper. Lean your phone against the glass at a slight angle, camera pointing down at the drawing surface. The glass provides stable support and the water adds weight."
        )
        StandOptionCard(
            title = "Stack of Books",
            instructions = "Stack 2\u20133 books at the edge of your desk. Prop your phone between the top two books, angled downward. Adjust the angle by changing how far the phone slides between the books."
        )
        StandOptionCard(
            title = "Box or Container",
            instructions = "Place a small box or container upside down near your paper. Balance your phone on the edge with the camera hanging over, pointing down at your drawing surface. Use a rubber band to secure if needed."
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
    val uriHandler = LocalUriHandler.current

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            text = "Downloads",
            style = MaterialTheme.typography.titleMedium
        )
        OutlinedButton(
            onClick = {
                uriHandler.openUri("https://github.com/jn0v/traceglass/releases/latest/download/markers.pdf")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Download printable marker sheet (opens browser)")
        }
        OutlinedButton(
            onClick = {
                uriHandler.openUri("https://github.com/jn0v/traceglass/releases/latest/download/phone-stand.stl")
            },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text("Download 3D phone stand model (opens browser)")
        }
    }
}
