package com.adrianrusu.pandawave.buildlogic

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir

import java.nio.file.Files
import java.nio.file.Path

import static org.junit.jupiter.api.Assertions.assertEquals
import static org.junit.jupiter.api.Assertions.assertTrue

class VerifyNoComposeMaterialIconsPluginTest {
    @TempDir
    Path projectDirectory

    @Test
    void "rejects a Material Icons import from an included project source root"() {
        writeBaseFixture()
        write("feature/src/main/kotlin/example/Screen.kt", forbiddenImport())

        BuildResult result = runAndFail("verifyNoComposeMaterialIcons")

        assertTrue(result.output.contains(
            "Kotlin import: feature/src/main/kotlin/example/Screen.kt:3"
        ))
    }

    @Test
    void "rejects configured direct dependencies and every parsed catalog notation"() {
        writeBaseFixture("""
            [libraries]
            stringNotation = "androidx.compose.material:material-icons-core:1.0"
            moduleNotation = { module = "androidx.compose.material:material-icons-extended", version = "1.0" }
            groupNameNotation = { group = "androidx.compose.material", name = "material-icons-core", version = "1.0" }
            reversedFields = { name = "material-icons-core", group = "androidx.compose.material", version = "1.0" }
        """.stripIndent())
        write("feature/build.gradle", """
            configurations.create("implementation")
            dependencies {
                add("implementation", "androidx.compose.material:material-icons-extended:1.0")
            }
        """.stripIndent())

        BuildResult result = runAndFail("verifyNoComposeMaterialIcons")

        assertTrue(result.output.contains(
            "Dependency: :feature:implementation -> androidx.compose.material:material-icons-extended"
        ))
        assertTrue(result.output.contains(
            "Version catalog: groupNameNotation -> androidx.compose.material:material-icons-core"
        ))
        assertTrue(result.output.contains(
            "Version catalog: moduleNotation -> androidx.compose.material:material-icons-extended"
        ))
        assertTrue(result.output.contains(
            "Version catalog: stringNotation -> androidx.compose.material:material-icons-core"
        ))
        assertTrue(result.output.contains(
            "Version catalog: reversedFields -> androidx.compose.material:material-icons-core"
        ))
    }

    @Test
    void "ignores lookalike sources outside included project source roots"() {
        writeBaseFixture()
        write("feature/src/main/kotlin/example/Screen.kt", """
            package example

            // androidx.compose.material.icons in prose is not an import.
        """.stripIndent())
        write(".worktrees/copy/feature/src/main/kotlin/example/Screen.kt", forbiddenImport())
        write(".gradle/caches/copied-project/src/main/kotlin/example/Screen.kt", forbiddenImport())
        write("scratch/src/main/kotlin/example/Screen.kt", forbiddenImport())

        BuildResult result = run("verifyNoComposeMaterialIcons")

        assertEquals(TaskOutcome.SUCCESS, result.task(":verifyNoComposeMaterialIcons").outcome)
    }

    @Test
    void "stores and reuses the configuration cache"() {
        writeBaseFixture()
        write("feature/src/main/kotlin/example/Screen.kt", "package example\n")

        BuildResult first = run("verifyNoComposeMaterialIcons", "--configuration-cache")
        BuildResult second = run("verifyNoComposeMaterialIcons", "--configuration-cache")

        assertEquals(TaskOutcome.SUCCESS, first.task(":verifyNoComposeMaterialIcons").outcome)
        assertTrue(first.output.contains("Configuration cache entry stored"))
        assertEquals(TaskOutcome.SUCCESS, second.task(":verifyNoComposeMaterialIcons").outcome)
        assertTrue(second.output.contains("Reusing configuration cache"))

        write("feature/src/main/kotlin/example/Screen.kt", forbiddenImport())
        BuildResult changed = runAndFail("verifyNoComposeMaterialIcons", "--configuration-cache")
        assertTrue(changed.output.contains("Reusing configuration cache"))
        assertTrue(changed.output.contains("Kotlin import: feature/src/main/kotlin/example/Screen.kt:3"))
    }

    private void writeBaseFixture(String catalog = "[libraries]\nsafe = \"com.example:safe:1.0\"\n") {
        write("settings.gradle", """
            rootProject.name = "material-icons-guard-test"
            include(":feature")
        """.stripIndent())
        write("build.gradle", """
            plugins {
                id("pandawave.ui-contract")
            }
        """.stripIndent())
        write("feature/build.gradle", "")
        write("gradle/libs.versions.toml", catalog)
    }

    private BuildResult run(String... arguments) {
        runner(arguments).build()
    }

    private BuildResult runAndFail(String... arguments) {
        runner(arguments).buildAndFail()
    }

    private GradleRunner runner(String... arguments) {
        List<String> buildArguments = arguments.toList() + ["--console=plain", "--stacktrace"]
        GradleRunner.create()
            .withProjectDir(projectDirectory.toFile())
            .withArguments(buildArguments)
            .withPluginClasspath()
    }

    private void write(String relativePath, String content) {
        Path target = projectDirectory.resolve(relativePath)
        Files.createDirectories(target.parent)
        Files.writeString(target, content)
    }

    private static String forbiddenImport() {
        "package example\n\nimport androidx.compose.material.icons.Icons\n"
    }
}
