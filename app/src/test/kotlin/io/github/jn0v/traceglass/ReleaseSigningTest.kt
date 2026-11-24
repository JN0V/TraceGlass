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
    @Test
    fun `a tag makes a release`() {
        val workflow = releaseWorkflow()

        assertTrue(
            Regex("""tags:\s*(\[\s*|\n\s*-\s*)['"]?v\*['"]?""").containsMatchIn(workflow),
            "release.yml must be triggered on v* tags"
        )
        assertTrue(workflow.contains("TRACEGLASS_KEYSTORE_B64"), "the workflow must decode the keystore secret")
        assertTrue(workflow.contains("assembleRelease"), "the workflow must build the release APK")
        assertTrue(
            Regex("""gh release create|action-gh-release""").containsMatchIn(workflow),
            "the workflow must create a GitHub Release"
        )
        assertTrue(
            workflow.contains("apksigner verify --print-certs"),
            "the workflow must check the APK with apksigner verify --print-certs"
        )
        assertTrue(
            !Regex("""echo\s+["']?\$\{\{\s*secrets\.""").containsMatchIn(workflow) && !workflow.contains("set -x"),
            "the workflow must never print a secret to its log"
        )

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
    @Test
    fun `versions go up`() {
        val script = appBuildScript()

        assertTrue(
            !Regex("""(?m)^\s*versionCode\s*=\s*\d+\s*$""").containsMatchIn(script),
            "versionCode must come from the tag or a file, not a fixed number"
        )
        assertTrue(
            !Regex("""(?m)^\s*versionName\s*=\s*"[^"]*"\s*$""").containsMatchIn(script),
            "versionName must come from the tag or a file, not a fixed string"
        )

        val workflow = releaseWorkflow()
        assertTrue(
            workflow.contains("GITHUB_REF_NAME") || workflow.contains("github.ref_name"),
            "the workflow must take the version from the pushed tag"
        )
        assertTrue(
            workflow.contains("badging"),
            "the workflow must check the built APK's versionCode and versionName (aapt dump badging)"
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
