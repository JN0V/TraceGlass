package io.github.jn0v.traceglass.feature.tracing

import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class ColorTintTest {

    @Test
    fun `NONE returns null color filter`() {
        assertNull(ColorTint.NONE.toColorFilter())
    }

    @Test
    fun `RED returns non-null tint filter`() {
        assertNotNull(ColorTint.RED.toColorFilter())
    }

    @Test
    fun `GREEN returns non-null tint filter`() {
        assertNotNull(ColorTint.GREEN.toColorFilter())
    }

    @Test
    fun `BLUE returns non-null tint filter`() {
        assertNotNull(ColorTint.BLUE.toColorFilter())
    }

    @Test
    fun `GRAYSCALE returns non-null colorMatrix filter`() {
        assertNotNull(ColorTint.GRAYSCALE.toColorFilter())
    }
}
