/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

// This file sets up building public docs from source jars
// TODO: after DiffAndDocs and Doclava are fully obsoleted and removed, rename this from DokkaPublicDocs to just PublicDocs
package androidx.build.dokka

import java.io.File
import androidx.build.getBuildId
import androidx.build.getDistributionDirectory
import androidx.build.java.JavaCompileInputs
import androidx.build.SupportLibraryExtension
import androidx.build.Release
import androidx.build.RELEASE_RULE
import androidx.build.Strategy.Ignore
import androidx.build.Strategy.Prebuilts
import org.gradle.api.artifacts.ResolveException
import org.gradle.api.file.FileCollection
import org.gradle.api.file.FileTree
import org.gradle.api.GradleException
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.artifacts.Configuration
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.Copy
import org.gradle.api.tasks.TaskContainer
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getPlugin
import org.jetbrains.dokka.gradle.DokkaAndroidPlugin
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.PackageOptions
import androidx.build.DiffAndDocs


object DokkaPublicDocs {
    private val RUNNER_TASK_NAME = "dokkaPublicDocs"
    public val ARCHIVE_TASK_NAME: String = "distPublicDokkaDocs"

    public val hiddenPackages = listOf(
        "androidx.core.internal",
        "androidx.preference.internal",
        "androidx.wear.internal.widget.drawer",
        "androidx.webkit.internal",
        "androidx.work.impl",
        "androidx.work.impl.background",
        "androidx.work.impl.background.systemalarm",
        "androidx.work.impl.background.systemjob",
        "androidx.work.impl.constraints",
        "androidx.work.impl.constraints.controllers",
        "androidx.work.impl.constraints.trackers",
        "androidx.work.impl.model",
        "androidx.work.impl.utils",
        "androidx.work.impl.utils.futures",
        "androidx.work.impl.utils.taskexecutor")

    fun getDocsTask(project: Project): DokkaTask {
        var rootProject = project.rootProject
        return rootProject.tasks.getOrCreateDocsTask(rootProject)
    }

    @Synchronized fun TaskContainer.getOrCreateDocsTask(runnerProject: Project): DokkaTask {
        val tasks = this
        if (tasks.findByName(DokkaPublicDocs.RUNNER_TASK_NAME) == null) {
            Dokka.createDocsTask(DokkaPublicDocs.RUNNER_TASK_NAME, runnerProject, hiddenPackages, DokkaPublicDocs.ARCHIVE_TASK_NAME)
        }
        return runnerProject.tasks.getByName(DokkaPublicDocs.RUNNER_TASK_NAME) as DokkaTask
    }

    // specifies that <project> exists and might need us to generate documentation for it
    fun registerProject(
        project: Project,
        extension: SupportLibraryExtension
    ) {
        val projectSourcesLocationType = RELEASE_RULE.resolve(extension)?.strategy
        if (projectSourcesLocationType is Prebuilts) {
            val dependency = projectSourcesLocationType.dependency(extension)
            assignPrebuiltForProject(project, dependency)
        } else if (projectSourcesLocationType != null && projectSourcesLocationType !is Ignore) {
            throw Exception("Unsupported strategy " + projectSourcesLocationType + " specified for publishing public docs of project " + extension + "; must be Prebuilts or Ignore or null (which means Ignore)")
        }
    }

    // specifies that <project> has docs and that those docs come from a prebuilt
    private fun assignPrebuiltForProject(project: Project, dependency: String) {
        val docsTask = getDocsTask(project)
        registerPrebuilt(dependency, docsTask, project.rootProject)
    }

    // specifies that <dependency> describes an artifact containing sources that we want to include in our generated documentation
    private fun registerPrebuilt(dependency: String, docsTask: Task, runnerProject: Project) {
        val docsTask = getDocsTask(runnerProject)
        val configuration = runnerProject.configurations.detachedConfiguration(runnerProject.dependencies.create(dependency))
        val unzipTask = getPrebuiltSources(runnerProject, configuration, dependency)
        val sourceDir = unzipTask.destinationDir
        val inputs = JavaCompileInputs.fromSource(unzipTask.destinationDir, configuration, runnerProject)
        registerInputs(inputs, runnerProject)
        docsTask.dependsOn(unzipTask)
    }

    // returns a Copy task that provides source files for the given prebuilt
    private fun getPrebuiltSources(
        root: Project,
        configuration: Configuration,
        mavenId: String
    ): Copy {
        val artifacts = try {
            configuration.resolvedConfiguration.resolvedArtifacts
        } catch (e: ResolveException) {
            root.logger.error("DokkaPublicDocs failed to find prebuilts for $mavenId. " +
                    "specified in PublichDocsRules.kt ." +
                    "You should either add a prebuilt sources jar, " +
                    "or add an overriding \"ignore\" rule into PublishDocsRules.kt")
            throw e
        }

        val artifact = artifacts.find { it.moduleVersion.id.toString() == mavenId }
                ?: throw GradleException()

        val sourceDir = artifact.file.parentFile
        val tree = root.zipTree(File(sourceDir, "${artifact.file.nameWithoutExtension}-sources.jar"))
                .matching {
                    it.exclude("**/*.MF")
                    it.exclude("**/*.aidl")
                    it.exclude("**/META-INF/**")
                }
        val sanitizedMavenId = mavenId.replace(":", "-")
        val destDir = root.file("${root.buildDir}/sources-unzipped/${sanitizedMavenId}")
        val unzipTask = root.tasks.create("unzip${sanitizedMavenId}" , Copy::class.java) { copyTask ->
            copyTask.from(tree)
            copyTask.destinationDir = destDir
            // TODO(123020809) remove this filter once it is no longer necessary to prevent Dokka from failing
            val regex = Regex("@attr ref ([^*]*)styleable#([^_*]*)_([^*]*)$")
            copyTask.filter({ line ->
                regex.replace(line, "{@link $1attr#$3}")
            })
        }

        return unzipTask
    }

    private fun registerInputs(inputs: JavaCompileInputs, project: Project) {
        val docsTask = getDocsTask(project)
        docsTask.sourceDirs += inputs.sourcePaths
        docsTask.classpath = docsTask.classpath.plus(inputs.dependencyClasspath).plus(inputs.bootClasspath)
        docsTask.dependsOn(inputs.dependencyClasspath)
    }


}
