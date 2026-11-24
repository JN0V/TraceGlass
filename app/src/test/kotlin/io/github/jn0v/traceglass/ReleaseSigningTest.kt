package io.github.jn0v.traceglass

import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.File

/**
 * Checks the release pipeline's build side: the release signing and versioning in
 * `app/build.gradle.kts`. The workflow cannot run in a unit test, and it is CI
 * configuration outside what the change may write, so these tests read what the
 * build script says.
 */
class ReleaseSigningTest {

    private fun repoRoot(): File {
        var dir: File? = File(System.getProperty("user.dir")).absoluteFile
        while (dir != null && !File(dir, "settings.gradle.kts").exists()) {
            dir = dir.parentFile
        }
        return requireNotNull(dir) { "repository root not found from user.dir" }
    }

    private fun appBuildScript(): String = File(repoRoot(), "app/build.gradle.kts").readText()

    // Runs the project's own Gradle wrapper as a tag build would see it: the version
    // tag set and none of the signing variables, whatever the machine has in its
    // environment. Returns the exit code and the merged output.
    private fun runTagBuildWithoutSecrets(vararg args: String): Pair<Int, String> {
        val command = listOf(File(repoRoot(), "gradlew").absolutePath, "--offline", "--console=plain") + args
        val builder = ProcessBuilder(command).directory(repoRoot()).redirectErrorStream(true)
        val env = builder.environment()
        listOf(
            "TRACEGLASS_KEYSTORE_FILE",
            "TRACEGLASS_KEYSTORE_PASSWORD",
            "TRACEGLASS_KEY_ALIAS",
            "TRACEGLASS_KEY_PASSWORD"
        ).forEach { env.remove(it) }
        env["TRACEGLASS_VERSION_NAME"] = "1.2.3"
        val process = builder.start()
        val output = process.inputStream.bufferedReader().readText()
        return process.waitFor() to output
    }

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
    //
    // The workflow file is CI configuration, outside what the change may write, so this
    // test checks the part the change owns: building the release without the signing
    // secrets in the environment fails the build with a message saying a secret is
    // missing, scoped to release builds so debug builds and unit tests still run, and
    // the release never falls back to the debug key or to an unsigned APK.
    @Test
    fun `no secrets, no APK`() {
        val script = appBuildScript()

        assertTrue(script.contains("signingConfigs"), "app/build.gradle.kts must define a signingConfig")
        listOf(
            "TRACEGLASS_KEYSTORE_PASSWORD",
            "TRACEGLASS_KEY_ALIAS",
            "TRACEGLASS_KEY_PASSWORD"
        ).forEach { name ->
            assertTrue(script.contains(name), "the build must check $name")
        }

        assertTrue(
            Regex("""throw\s+\w*Exception|GradleException\(|\berror\(|\brequire\(|\bcheck\(""")
                .containsMatchIn(script),
            "the build must fail when a signing secret is missing"
        )
        assertTrue(
            script.lowercase().contains("missing"),
            "the failure message must say a signing secret is missing"
        )
        assertTrue(
            Regex("""taskGraph|taskNames|doFirst|tasks\.(register|named|matching|configureEach|withType)|gradle\.""")
                .containsMatchIn(script),
            "the failure must be scoped to release builds, not every build"
        )

        assertTrue(!script.contains("app-release-unsigned"), "the build must never publish an unsigned APK")
        assertTrue(
            !script.contains("""signingConfigs.getByName("debug")"""),
            "the release build must not fall back to the debug key"
        )
    }

    // Given the signing secrets are missing and a release tag is being built,
    // When the release APK is assembled,
    // Then the signing check is part of the build before the APK, and running it fails
    // with a message naming each missing variable, so no unsigned APK is produced.
    //
    // This runs the real Gradle build rather than reading the script: the check task must
    // exist, be wired in front of assembleRelease, and fail with the secrets missing.
    @Test
    fun `a tag build without secrets fails before any APK`() {
        val (planCode, plan) = runTagBuildWithoutSecrets(":app:assembleRelease", "--dry-run")
        assertTrue(planCode == 0, "the dry run of assembleRelease must succeed:\n$plan")
        val checkAt = plan.indexOf(":app:verifyReleaseSigning")
        val assembleAt = plan.indexOf(":app:assembleRelease")
        assertTrue(checkAt >= 0, "assembleRelease of a tag build must run verifyReleaseSigning:\n$plan")
        assertTrue(
            assembleAt > checkAt,
            "verifyReleaseSigning must come before assembleRelease in the plan:\n$plan"
        )

        val (code, output) = runTagBuildWithoutSecrets(":app:verifyReleaseSigning")
        assertTrue(code != 0, "verifyReleaseSigning must fail when the signing secrets are missing:\n$output")
        assertTrue(
            output.contains("Release signing secret missing"),
            "the failure must say a release signing secret is missing:\n$output"
        )
        listOf(
            "TRACEGLASS_KEYSTORE_FILE",
            "TRACEGLASS_KEYSTORE_PASSWORD",
            "TRACEGLASS_KEY_ALIAS",
            "TRACEGLASS_KEY_PASSWORD"
        ).forEach { name ->
            assertTrue(output.contains(name), "the failure message must name $name:\n$output")
        }
    }
}
