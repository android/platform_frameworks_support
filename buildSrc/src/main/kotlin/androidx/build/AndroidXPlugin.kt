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

package androidx.build

import androidx.build.SupportConfig.BUILD_TOOLS_VERSION
import androidx.build.SupportConfig.COMPILE_SDK_VERSION
import androidx.build.SupportConfig.TARGET_SDK_VERSION
import androidx.build.SupportConfig.DEFAULT_MIN_SDK_VERSION
import androidx.build.SupportConfig.INSTRUMENTATION_RUNNER
import androidx.build.checkapi.ApiType
import androidx.build.checkapi.getLastReleasedApiFileFromDir
import androidx.build.checkapi.hasApiFolder
import androidx.build.dependencyTracker.AffectedModuleDetector
import androidx.build.dokka.Dokka
import androidx.build.gradle.getByType
import androidx.build.gradle.isRoot
import androidx.build.jacoco.Jacoco
import androidx.build.license.CheckExternalDependencyLicensesTask
import androidx.build.license.configureExternalDependencyLicenseCheck
import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import com.android.build.gradle.BaseExtension
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.DefaultTask
import org.gradle.api.JavaVersion.VERSION_1_7
import org.gradle.api.JavaVersion.VERSION_1_8
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

import org.gradle.api.plugins.JavaLibraryPlugin
import org.gradle.api.plugins.JavaPlugin
import org.gradle.api.plugins.JavaPluginConvention
import org.gradle.api.tasks.TaskProvider
import org.gradle.api.tasks.bundling.Jar
import org.gradle.api.tasks.compile.JavaCompile
import org.gradle.kotlin.dsl.extra
import org.gradle.kotlin.dsl.get
import org.gradle.kotlin.dsl.getPlugin
import org.gradle.kotlin.dsl.withType
import java.util.concurrent.ConcurrentHashMap
import java.io.File

/**
 * A plugin which enables all of the Gradle customizations for AndroidX.
 * This plugin reacts to other plugins being added and adds required and optional functionality.
 */
class AndroidXPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        // This has to be first due to bad behavior by DiffAndDocs which is triggered on the root
        // project. It calls evaluationDependsOn on each subproject. This eagerly causes evaluation
        // *during* the root build.gradle evaluation. The subproject then applies this plugin (while
        // we're still halfway through applying it on the root). The check licenses code runs on the
        // subproject which then looks for the root project task to add itself as a dependency of.
        // Without the root project having created the task prior to DiffAndDocs running this fails.
        // TODO do not use evaluationDependsOn in DiffAndDocs to break this cycle!
        project.configureExternalDependencyLicenseCheck()

        if (project.isRoot) {
            project.configureRootProject()
        }

        project.plugins.all {
            when (it) {
                is JavaPlugin,
                is JavaLibraryPlugin -> {
                    project.configureErrorProneForJava()
                    project.configureSourceJarForJava()
                    project.convention.getPlugin<JavaPluginConvention>().apply {
                        sourceCompatibility = VERSION_1_7
                        targetCompatibility = VERSION_1_7
                    }
                    val verifyDependencyVersionsTask = project.createVerifyDependencyVersionsTask()
                    val compileJavaTask = project.properties["compileJava"] as JavaCompile
                    verifyDependencyVersionsTask.dependsOn(compileJavaTask)
                }
                is LibraryPlugin -> {
                    val extension = project.extensions.getByType<LibraryExtension>()
                    project.configureSourceJarForAndroid(extension)
                    project.configureAndroidCommonOptions(extension)
                    project.configureAndroidLibraryOptions(extension)
                    project.configureVersionFileWriter(extension)
                    project.configureResourceApiChecks()
                    val verifyDependencyVersionsTask = project.createVerifyDependencyVersionsTask()
                    extension.libraryVariants.all {
                        variant -> verifyDependencyVersionsTask
                                        .dependsOn(variant.javaCompileProvider)
                    }
                }
                is AppPlugin -> {
                    val extension = project.extensions.getByType<AppExtension>()
                    project.configureAndroidCommonOptions(extension)
                    project.configureAndroidApplicationOptions(extension)
                }
            }
        }

        // Disable timestamps and ensure filesystem-independent archive ordering to maximize
        // cross-machine byte-for-byte reproducibility of artifacts.
        project.tasks.withType<Jar> {
            isReproducibleFileOrder = true
            isPreserveFileTimestamps = false
        }
    }

    private fun Project.configureRootProject() {
        val buildOnServerTask = tasks.register(BUILD_ON_SERVER_TASK)
        val buildTestApksTask = tasks.register(BUILD_TEST_APKS)
        var projectModules = ConcurrentHashMap<String, String>()
        project.extra.set("projects", projectModules)
        buildOnServerTask.lazyDependsOn("distDocs")
        buildOnServerTask.lazyDependsOn(Dokka.ARCHIVE_TASK_NAME)
        buildOnServerTask.lazyDependsOn("partiallyDejetifyArchive")
        buildOnServerTask.lazyDependsOn("dejetifyArchive")
        buildOnServerTask.lazyDependsOn(CheckExternalDependencyLicensesTask.TASK_NAME)

        subprojects { project ->
            if (project.path == ":docs-fake") {
                return@subprojects
            }
            project.tasks.all { task ->
                // TODO remove androidTest from buildOnServer once test runners do not
                // expect them anymore. (wait for master)
                if ("assembleAndroidTest" == task.name ||
                        "assembleDebug" == task.name ||
                        ERROR_PRONE_TASK == task.name ||
                        "lintMinDepVersionsDebug" == task.name) {
                    buildOnServerTask.lazyDependsOn(task)
                }
                if ("assembleAndroidTest" == task.name ||
                        "assembleDebug" == task.name) {
                    buildTestApksTask.lazyDependsOn(task)
                }
            }
        }

        val createCoverageJarTask = Jacoco.createCoverageJarTask(this)
        buildOnServerTask.lazyDependsOn(createCoverageJarTask)
        buildTestApksTask.lazyDependsOn(createCoverageJarTask)

        Release.createGlobalArchiveTask(this)
        val allDocsTask = DiffAndDocs.configureDiffAndDocs(this, projectDir,
                DacOptions("androidx", "ANDROIDX_DATA"),
                listOf(RELEASE_RULE))
        buildOnServerTask.lazyDependsOn(allDocsTask)

        val jacocoUberJar = Jacoco.createUberJarTask(this)
        buildOnServerTask.lazyDependsOn(jacocoUberJar)

        project.createClockLockTasks()

        AffectedModuleDetector.configure(gradle, this)

        // Iterate through all the project and substitute any artifact dependency of a
        // maxdepversions future configuration with the corresponding tip of tree project.
        subprojects { project ->
            project.configurations.all { configuration ->
                if (configuration.name.toLowerCase().contains("maxdepversions") &&
                        project.extra.has("publish")) {
                    configuration.resolutionStrategy.dependencySubstitution.apply {
                        for (e in projectModules) {
                            substitute(module(e.key)).with(project(e.value))
                        }
                    }
                }
            }
        }
    }

    private fun Project.configureAndroidCommonOptions(extension: BaseExtension) {
        extension.compileSdkVersion(COMPILE_SDK_VERSION)
        extension.buildToolsVersion = BUILD_TOOLS_VERSION
        // Expose the compilation SDK for use as the target SDK in test manifests.
        extension.defaultConfig.addManifestPlaceholders(
                mapOf("target-sdk-version" to TARGET_SDK_VERSION))

        extension.defaultConfig.testInstrumentationRunner = INSTRUMENTATION_RUNNER
        extension.testOptions.unitTests.isReturnDefaultValues = true

        extension.defaultConfig.minSdkVersion(DEFAULT_MIN_SDK_VERSION)
        afterEvaluate {
            val minSdkVersion = extension.defaultConfig.minSdkVersion.apiLevel
            check(minSdkVersion >= DEFAULT_MIN_SDK_VERSION) {
                "minSdkVersion $minSdkVersion lower than the default of $DEFAULT_MIN_SDK_VERSION"
            }
            project.configurations.all { configuration ->
                configuration.resolutionStrategy.eachDependency { dep ->
                    val target = dep.target
                    // Enforce the ban on declaring dependencies with version ranges.
                    if (isDependencyRange(target.version)) {
                        throw IllegalArgumentException(
                                "Dependency ${dep.target} declares its version as " +
                                        "version range ${dep.target.version} however the use of " +
                                        "version ranges is not allowed, please update the " +
                                        "dependency to list a fixed version.")
                    }
                }
            }
        }
        if (project.name != "docs-fake") {
            // Add another "version" flavor dimension which would have two flavors minDepVersions
            // and maxDepVersions. Flavor minDepVersions builds the libraries against the specified
            // versions of their dependencies while maxDepVersions builds the libraries against
            // the local versions of their dependencies (so for example if library A specifies
            // androidx.collection:collection:1.2.0 as its dependency then minDepVersions would
            // build using exactly that version while maxDepVersions would build against
            // project(":collection") instead.)
            extension.flavorDimensions("version")
            extension.productFlavors {
                it.create("minDepVersions")
                it.get("minDepVersions").dimension = "version"
                it.create("maxDepVersions")
                it.get("maxDepVersions").dimension = "version"
            }
        }

        // Use a local debug keystore to avoid build server issues.
        extension.signingConfigs.getByName("debug").storeFile = SupportConfig.getKeystore(this)

        // Disable generating BuildConfig.java
        extension.variants.all {
            it.generateBuildConfigProvider.configure {
                it.enabled = false
            }
        }

        configureErrorProneForAndroid(extension.variants)

        // Enable code coverage for debug builds only if we are not running inside the IDE, since
        // enabling coverage reports breaks the method parameter resolution in the IDE debugger.
        extension.buildTypes.getByName("debug").isTestCoverageEnabled =
                !hasProperty("android.injected.invoked.from.ide") &&
                !isBenchmark()

        // Set the officially published version to be the release version with minimum dependency
        // versions.
        extension.defaultPublishConfig(Release.DEFAULT_PUBLISH_CONFIG)
    }

    private fun Project.configureAndroidLibraryOptions(extension: LibraryExtension) {
        extension.compileOptions.apply {
            setSourceCompatibility(VERSION_1_7)
            setTargetCompatibility(VERSION_1_7)
        }

        afterEvaluate {
            // Java 8 is only fully supported on API 24+ and not all Java 8 features are
            // binary compatible with API < 24
            val compilesAgainstJava8 = extension.compileOptions.sourceCompatibility > VERSION_1_7 ||
                    extension.compileOptions.targetCompatibility > VERSION_1_7
            val minSdkLessThan24 = extension.defaultConfig.minSdkVersion.apiLevel < 24
            if (compilesAgainstJava8 && minSdkLessThan24) {
                throw IllegalArgumentException(
                        "Libraries can only support Java 8 if minSdkVersion is 24 or higher")
            }
        }
    }

    private fun Project.configureAndroidApplicationOptions(extension: AppExtension) {
        extension.defaultConfig.apply {
            targetSdkVersion(TARGET_SDK_VERSION)

            versionCode = 1
            versionName = "1.0"
        }

        extension.compileOptions.apply {
            setSourceCompatibility(VERSION_1_8)
            setTargetCompatibility(VERSION_1_8)
        }

        extension.lintOptions.apply {
            isAbortOnError = true

            val baseline = lintBaseline
            if (baseline.exists()) {
                baseline(baseline)
            }
        }
    }

    private fun Project.createVerifyDependencyVersionsTask(): DefaultTask {
        return project.tasks.create("verifyDependencyVersions",
                VerifyDependencyVersionsTask::class.java)
    }

    companion object {
        private const val BUILD_ON_SERVER_TASK = "buildOnServer"
        private const val BUILD_TEST_APKS = "buildTestApks"

        fun getBuildOnServerTask(project : Project) : TaskProvider<Task> {
            return project.rootProject.tasks.named(BUILD_ON_SERVER_TASK)
        }
    }
}

fun Project.isBenchmark(): Boolean {
    // benchmark convention is to end name with "-benchmark"
    return name.endsWith("-benchmark")
}

fun Project.addToProjectMap(group: String?) {
    if (group != null) {
        val module = "$group:${project.name}"
        val projectName = "${project.path}"
        var projectModules = project.rootProject.extra.get("projects")
                as ConcurrentHashMap<String, String>
        projectModules.put(module, projectName)
    }
}

private fun isDependencyRange(version: String?): Boolean {
    return ((version!!.startsWith("[") || version.startsWith("(")) &&
            (version.endsWith("]") || version.endsWith(")")) ||
            version.endsWith("+"))
}

private fun Project.createCheckResourceApiTask(): TaskProvider<CheckResourceApiTask> {
    return project.tasks.register("checkResourceApi",
            CheckResourceApiTask::class.java) {
        it.newApiFile = getGenerateResourceApiFile()
        it.oldApiFile = File(project.projectDir, "api/res-${project.version}.txt")
    }
}

private fun Project.createUpdateResourceApiTask(): TaskProvider<UpdateResourceApiTask> {
    return project.tasks.register("updateResourceApi", UpdateResourceApiTask::class.java) {
        it.newApiFile = getGenerateResourceApiFile()
        it.oldApiFile = getLastReleasedApiFileFromDir(File(project.projectDir, "api/"),
                project.version(), true, false, ApiType.RESOURCEAPI)
    }
}

private fun Project.configureResourceApiChecks() {
    project.afterEvaluate {
        if (project.hasApiFolder()) {
            val checkResourceApiTask = project.createCheckResourceApiTask()
            val updateResourceApiTask = project.createUpdateResourceApiTask()
            checkResourceApiTask.lazyDependsOn("assembleRelease")
            updateResourceApiTask.lazyDependsOn("assembleRelease")
            project.tasks.named("updateApi").lazyDependsOn(updateResourceApiTask)
            AndroidXPlugin.getBuildOnServerTask(project).lazyDependsOn(checkResourceApiTask)
        }
    }
}

private fun Project.getGenerateResourceApiFile(): File {
    return File(project.buildDir, "intermediates/public_res/minDepVersionsRelease" +
            "/packageMinDepVersionsReleaseResources/public.txt")
}