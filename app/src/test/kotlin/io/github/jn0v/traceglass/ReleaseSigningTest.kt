package io.github.jn0v.traceglass

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Checks the release pipeline's files: `.github/workflows/release.yml` and the release
 * signing and versioning in `app/build.gradle.kts`. The workflow cannot run in a unit
 * test, so these tests read what it and the build script say.
 */
class ReleaseSigningTest {

    private fun repoRoot(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) {
            dir = dir.parentFile
        }
        return requireNotNull(dir) { "repository root not found from user.dir" }
    }

    private fun releaseWorkflow(): String {
        val file = File(repoRoot(), ".github/workflows/release.yml")
        assertTrue(file.exists(), ".github/workflows/release.yml must exist")
        return file.readText()
    }

    private fun appBuildScript(): String = File(repoRoot(), "app/build.gradle.kts").readText()

    // Given a `v*` tag pushed,
    // When the release workflow runs,
    // Then a GitHub Release holds a release APK signed with the project key.
    //
    // The workflow file is CI configuration, outside what the change may write, so this
    // test checks the part the change owns: the release build is signed with the project
    // key, read from the environment the workflow provides.
    @Test
    fun `a tag makes a release`() {
        val script = appBuildScript()
        assertTrue(script.contains("signingConfigs"), "app/build.gradle.kts must define a signingConfig")
        assertTrue(
            Regex("""signingConfig\s*=\s*signingConfigs""").containsMatchIn(script),
            "the release build type must use the signing config"
        )
        listOf(
            "TRACEGLASS_KEYSTORE_PASSWORD",
            "TRACEGLASS_KEY_ALIAS",
            "TRACEGLASS_KEY_PASSWORD"
        ).forEach { name ->
            assertTrue(script.contains(name), "the signing config must read $name from the environment")
        }
        assertTrue(script.contains("System.getenv"), "the signing config must read env vars")
    }

    // Given two successive release tags,
    // When both APKs are built,
    // Then the second has a higher versionCode, and its versionName matches the tag.
    //
    // The workflow file is CI configuration, outside what the change may write, so this
    // test checks the part the change owns: the build script computes versionCode and
    // versionName from an outside source (the tag the workflow passes, a property or a
    // file) instead of fixing them, so two tags cannot build the same version.
    @Test
    fun `versions go up`() {
        val script = appBuildScript()

        val code = Regex("""(?m)^\s*versionCode\s*=\s*(.+?)\s*$""").find(script)
        val name = Regex("""(?m)^\s*versionName\s*=\s*(.+?)\s*$""").find(script)
        assertTrue(code != null, "app/build.gradle.kts must set versionCode")
        assertTrue(name != null, "app/build.gradle.kts must set versionName")

        assertTrue(
            !Regex("""\d+""").matches(code!!.groupValues[1]),
            "versionCode must come from the tag or a file, not a fixed number"
        )
        assertTrue(
            !Regex(""""[^"]*"""").matches(name!!.groupValues[1]),
            "versionName must come from the tag or a file, not a fixed string"
        )

        val outside = Regex("""System\.getenv|providers\.|findProperty|getProperty|gradleProperty|readText|File\(""")
        assertTrue(
            outside.containsMatchIn(code.groupValues[1]) || outside.containsMatchIn(script),
            "versionCode must be read from the environment, a property or a file"
        )
        assertTrue(
            outside.containsMatchIn(name.groupValues[1]) || outside.containsMatchIn(script),
            "versionName must be read from the environment, a property or a file"
        )
    }

    // Given the signing secrets are missing,
    // When the release workflow runs,
    // Then it fails with a clear message and publishes no unsigned APK.
    @Test
    fun `no secrets, no APK`() {
        val workflow = releaseWorkflow()

        val errorAt = workflow.indexOf("::error")
        val buildAt = workflow.indexOf("assembleRelease")
        val publishAt = Regex("""gh release create|action-gh-release""").find(workflow)?.range?.first ?: -1

        assertTrue(errorAt >= 0, "the workflow must report missing secrets with an ::error message")
        assertTrue(buildAt >= 0 && publishAt >= 0, "the workflow must build and publish")
        assertTrue(errorAt < buildAt, "the secrets check must run before the build")
        assertTrue(buildAt < publishAt, "the release is created after the build")

        val check = workflow.substring(0, buildAt)
        listOf(
            "TRACEGLASS_KEYSTORE_B64",
            "TRACEGLASS_KEYSTORE_PASSWORD",
            "TRACEGLASS_KEY_ALIAS",
            "TRACEGLASS_KEY_PASSWORD"
        ).forEach { name ->
            assertTrue(check.contains(name), "the check before the build must cover $name")
        }
        assertTrue(check.contains("exit 1"), "the secrets check must fail the run")
        assertTrue(check.lowercase().contains("missing"), "the failure message must say a secret is missing")

        assertTrue(!workflow.contains("app-release-unsigned"), "the workflow must never publish an unsigned APK")
        assertTrue(
            !appBuildScript().contains("""signingConfigs.getByName("debug")"""),
            "the release build must not fall back to the debug key"
        )
    }
}
