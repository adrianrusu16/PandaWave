package com.adrianrusu.pandawave.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction

import java.util.regex.Pattern

abstract class VerifyNoComposeMaterialIconsTask extends DefaultTask {
    private static final Pattern MATERIAL_ICON_IMPORT =
        Pattern.compile(/^\s*import\s+androidx\.compose\.material\.icons(?:[.\s;]|$)/)

    @InputFiles
    @PathSensitive(PathSensitivity.RELATIVE)
    abstract ConfigurableFileCollection getKotlinSources()

    @Input
    abstract ListProperty<String> getDependencyViolations()

    @Input
    abstract ListProperty<String> getCatalogViolations()

    @Internal
    abstract DirectoryProperty getRootDirectory()

    @TaskAction
    void verifyIcons() {
        List<String> violations = []
        kotlinSources.files.sort().each { File source ->
            source.readLines("UTF-8").eachWithIndex { String line, int index ->
                if (MATERIAL_ICON_IMPORT.matcher(line).find()) {
                    String relative = rootDirectory.get().asFile.toPath()
                        .relativize(source.toPath()).toString().replace('\\', '/')
                    violations.add("Kotlin import: ${relative}:${index + 1}".toString())
                }
            }
        }
        violations.addAll(dependencyViolations.get().collect { "Dependency: ${it}".toString() })
        violations.addAll(catalogViolations.get().collect { "Version catalog: ${it}".toString() })
        if (!violations.isEmpty()) {
            throw new GradleException(
                "Compose Material Icons are forbidden; use PandaWaveIcons/local vectors.\n" +
                    violations.unique().sort().join("\n")
            )
        }
    }
}
