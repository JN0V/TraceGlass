package io.github.jn0v.traceglass

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

class PublishedApkTest {

    private fun repoRoot(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null) {
            if (File(dir, ".github/workflows/ci.yml").isFile) return dir
            dir = dir.parentFile
        }
        error("repository root not found from ${System.getProperty("user.dir")}")
    }

    private fun ciText(): String = File(repoRoot(), ".github/workflows/ci.yml").readText()

    private fun readmeText(): String = File(repoRoot(), "README.md").readText()

    /** The workflow's steps, each as its own block of text, in order. */
    private fun steps(ci: String): List<String> =
        ci.split(Regex("(?m)^(?=      - name: )")).drop(1)

    /** The step that uploads the debug APK: an upload-artifact step other than the lint one. */
    private fun apkUploadStep(ci: String): String? =
        steps(ci).firstOrNull {
            it.contains("actions/upload-artifact@v4") && !it.contains("lint-results")
        }

    private fun artifactName(step: String): String? =
        Regex("(?m)^\\s+name:\\s*['\"]?([^'\"\\s]+)").find(step)?.groupValues?.get(1)

    // Given a push to main, When CI passes, Then the debug APK is attached to the run
    // as an artifact with a findable name, uploaded after "Build debug", only on push
    // to main, with a short retention, and no secret is read.
    @Test
    fun `debug APK is uploaded as an artifact after Build debug on push to main`() {
        val ci = ciText()
        val all = steps(ci)

        val buildIndex = all.indexOfFirst { it.startsWith("      - name: Build debug") }
        assertTrue(buildIndex >= 0, "the Build debug step must exist")

        val upload = apkUploadStep(ci)
        assertNotNull(upload, "an upload-artifact@v4 step for the debug APK is missing")
        upload!!
        assertTrue(all.indexOf(upload) > buildIndex, "the APK upload must come after Build debug")

        val name = artifactName(upload)
        assertNotNull(name, "the APK artifact needs a name")
        assertTrue(
            name!!.contains("apk", ignoreCase = true) && name.contains("debug", ignoreCase = true),
            "artifact name should say it is the debug APK, was: $name"
        )

        assertTrue(
            upload.contains("app/build/outputs/apk/debug"),
            "the upload must take the debug APK from app/build/outputs/apk/debug"
        )

        val condition = Regex("(?m)^\\s+if:\\s*(.+)$").find(upload)?.groupValues?.get(1)
        assertNotNull(condition, "the APK upload must be conditional on a push to main")
        assertTrue(
            condition!!.contains("refs/heads/main") || condition.contains("github.event_name == 'push'"),
            "the condition must restrict the upload to push on main, was: $condition"
        )

        val retention = Regex("retention-days:\\s*(\\d+)").find(upload)?.groupValues?.get(1)?.toInt()
        assertNotNull(retention, "the APK upload needs a retention-days")
        assertTrue(retention!! in 1..14, "retention should be short (1 to 14 days), was: $retention")

        assertFalse(ci.contains("secrets."), "no secret may be read by CI")
    }

    // Given a person reading the README, When they look for how to install,
    // Then an install section says where to download the APK (the CI artifact)
    // and how to install it.
    @Test
    fun `README has an install section pointing to the APK artifact`() {
        val name = artifactName(apkUploadStep(ciText()) ?: "")
        assertNotNull(name, "the CI must define the APK artifact the README points to")

        val lines = readmeText().lines()
        val headingRegex = Regex("^(#{2,6})\\s+(.*)$")
        val start = lines.indexOfFirst {
            headingRegex.matches(it) && it.contains("install", ignoreCase = true)
        }
        assertTrue(start >= 0, "README needs a heading about installing")

        val level = headingRegex.find(lines[start])!!.groupValues[1].length
        val end = (start + 1 until lines.size).firstOrNull {
            val m = headingRegex.find(lines[it])
            m != null && m.groupValues[1].length <= level
        } ?: lines.size
        val section = lines.subList(start, end).joinToString("\n")

        assertTrue(section.contains(name!!), "install section must name the artifact $name")
        assertTrue(section.contains("Actions"), "install section must say to download it from the Actions run")
        assertTrue(section.contains("app-debug.apk"), "install section must name app-debug.apk")
        assertEquals(
            true,
            section.contains("adb install", ignoreCase = true) ||
                section.contains("open the", ignoreCase = true) ||
                section.contains("tap", ignoreCase = true) ||
                section.contains("sideload", ignoreCase = true),
            "install section must say how to install the APK"
        )
    }
}
