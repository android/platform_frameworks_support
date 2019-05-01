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

// This file creates tasks for generating documentation from source code using Dokka
// TODO: after DiffAndDocs and Doclava are fully obsoleted and removed, rename this from Dokka to just Docs
package androidx.build.dokka

import androidx.build.DiffAndDocs
import androidx.build.Release
import androidx.build.SupportLibraryExtension
import androidx.build.getBuildId
import androidx.build.getDistributionDirectory
import androidx.build.java.JavaCompileInputs
import com.android.build.gradle.LibraryExtension
import org.gradle.api.Project
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.bundling.Zip
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.getPlugin
import org.jetbrains.dokka.gradle.DokkaPlugin
import org.jetbrains.dokka.gradle.DokkaTask
import org.jetbrains.dokka.gradle.PackageOptions

object Dokka {
    private val RUNNER_TASK_NAME = "dokka"
    public val ARCHIVE_TASK_NAME: String = "distDokkaDocs" // TODO(b/72330103) make "generateDocs" be the only archive task once Doclava is fully removed
    private val ALTERNATE_ARCHIVE_TASK_NAME: String = "generateDocs"

    private val hiddenPackages = listOf(
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
        "androidx.work.impl.utils.taskexecutor"
    )

    fun getDocsTask(project: Project): DokkaTask {
        return project.rootProject.getOrCreateDocsTask()
    }
<<<<<<< HEAD   (e53308 Merge "Merge empty history for sparse-5498091-L6460000030224)

    @Synchronized
    fun Project.getOrCreateDocsTask(): DokkaTask {
        val runnerProject = this
        if (runnerProject.tasks.findByName(Dokka.RUNNER_TASK_NAME) == null) {
            project.apply<DokkaPlugin>()
            val docsTask = project.tasks.getByName(Dokka.RUNNER_TASK_NAME) as DokkaTask
=======
    fun archiveTaskNameForType(docsType: String): String {
        return "dist${docsType}DokkaDocs"
    }
    fun createDocsTask(
        docsType: String, // "public" or "tipOfTree"
        project: Project,
        hiddenPackages: List<String>
    ) {
        val taskName = generatorTaskNameForType(docsType)
        val archiveTaskName = archiveTaskNameForType(docsType)
        project.apply<DokkaAndroidPlugin>()
        // We don't use the `dokka` task, but it normally appears in `./gradlew tasks`
        // so replace it with a new task that doesn't show up and doesn't do anything
        project.tasks.replace("dokka")
        if (project.name != "support" && project.name != "docs-runner") {
            throw Exception("Illegal project passed to createDocsTask: " + project.name)
        }
        val docsTask = project.tasks.create(taskName, DokkaAndroidTask::class.java) { docsTask ->
            docsTask.moduleName = project.name
            docsTask.outputDirectory = File(project.buildDir, taskName).absolutePath
            docsTask.description = "Generates $docsType Kotlin documentation in the style of " +
                    "d.android.com.  Places docs in ${docsTask.outputDirectory}"
>>>>>>> BRANCH (3a06c2 Merge "Merge cherrypicks of [954920] into sparse-5520679-L60)
            docsTask.outputFormat = "dac"
            for (hiddenPackage in hiddenPackages) {
                val opts = PackageOptions()
                opts.prefix = hiddenPackage
                opts.suppress = true
                docsTask.perPackageOptions.add(opts)
            }
            val archiveTask = project.tasks.create(ARCHIVE_TASK_NAME, Zip::class.java) { task ->
                task.dependsOn(docsTask)
                task.description =
                        "Generates documentation artifact for pushing to developer.android.com"
                task.from(docsTask.outputDirectory)
                task.baseName = "android-support-dokka-docs"
                task.version = getBuildId()
                task.destinationDir = project.getDistributionDirectory()
            }
            if (project.tasks.findByName(ALTERNATE_ARCHIVE_TASK_NAME) == null) {
                project.tasks.create(ALTERNATE_ARCHIVE_TASK_NAME)
            }
            project.tasks.getByName(ALTERNATE_ARCHIVE_TASK_NAME).dependsOn(archiveTask)
        }
        return runnerProject.tasks.getByName(Dokka.RUNNER_TASK_NAME) as DokkaTask
    }

    fun registerAndroidProject(
        project: Project,
        library: LibraryExtension,
        extension: SupportLibraryExtension
    ) {
        if (extension.toolingProject) {
            project.logger.info("Project ${project.name} is tooling project; ignoring API tasks.")
            return
        }
        library.libraryVariants.all { variant ->
            if (variant.name == Release.DEFAULT_PUBLISH_CONFIG) {
                project.afterEvaluate({
                    val inputs = JavaCompileInputs.fromLibraryVariant(library, variant)
                    registerInputs(inputs, project)
                })
            }
        }
        DiffAndDocs.get(project).registerPrebuilts(extension)
    }

    fun registerJavaProject(
        project: Project,
        extension: SupportLibraryExtension
    ) {
        if (extension.toolingProject) {
            project.logger.info("Project ${project.name} is tooling project; ignoring API tasks.")
            return
        }
        val javaPluginConvention = project.convention.getPlugin<JavaPluginConvention>()
        val mainSourceSet = javaPluginConvention.sourceSets.getByName("main")
        project.afterEvaluate({
            val inputs = JavaCompileInputs.fromSourceSet(mainSourceSet, project)
            registerInputs(inputs, project)
        })
        DiffAndDocs.get(project).registerPrebuilts(extension)
    }

    fun registerInputs(inputs: JavaCompileInputs, project: Project) {
        val docsTask = getDocsTask(project)
        docsTask.sourceDirs += inputs.sourcePaths
        docsTask.classpath =
                docsTask.classpath.plus(inputs.dependencyClasspath).plus(inputs.bootClasspath)
        docsTask.dependsOn(inputs.dependencyClasspath)
    }
}
