package io.github.jn0v.traceglass

import io.github.jn0v.traceglass.core.cv.DetectionModeSelector
import io.github.jn0v.traceglass.feature.onboarding.OnboardingRepository

/** Keeps [selector] on the detection mode stored in the settings; suspends while collecting. */
internal suspend fun syncDetectionMode(
    repository: OnboardingRepository,
    selector: DetectionModeSelector
) {
    repository.detectionMode.collect { selector.mode = it }
}
