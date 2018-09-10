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

import androidx.build.metalava.Metalava
import com.android.build.gradle.LibraryExtension
import com.android.build.gradle.LibraryPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.logging.LogLevel
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.kotlin.dsl.apply
import org.gradle.kotlin.dsl.get

/**
 * Support library specific com.android.library plugin that sets common configurations needed for
 * support library modules.
 */
class SupportAndroidLibraryPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.apply<AndroidXPlugin>()

        val supportLibraryExtension = project.extensions.create("supportLibrary",
                SupportLibraryExtension::class.java, project)
        project.setupVersion(supportLibraryExtension)
        project.configureMavenArtifactUpload(supportLibraryExtension)

        project.afterEvaluate {
            val library = project.extensions.findByType(LibraryExtension::class.java)
                    ?: return@afterEvaluate

            project.injectCompilationForBenchmarks(library, supportLibraryExtension)

            if (supportLibraryExtension.useMetalava) {
                Metalava.registerAndroidProject(project, library, supportLibraryExtension)
            } else {
                DiffAndDocs.registerAndroidProject(project, library, supportLibraryExtension)
            }

            project.configurations.all { configuration ->
                configuration.resolutionStrategy.eachDependency { dep ->
                    val target = dep.target
                    // Enforce inclusive exclusive Androidx dependency version range specification.
                    if (artifactSupportsSemVer(target.group) && isDependencyRange(target.version) &&
                            !isSupportedDependencyRange(target.version)) {
                            throw IllegalArgumentException(
                                    "Androidx dependency version ranges can only be specified" +
                                            " in the format [x.y.z, a.b.c) " +
                                            "(i.e inclusive on the left and exclusive on the" +
                                            " right. The + operator" +
                                            " is not yet supported")
                    }
                }
            }

            library.libraryVariants.all { libraryVariant ->
                if (libraryVariant.flavorName.contains("minDepVersions")) {
                    useMinimumDependencyVersions(project.configurations)
                }
                if (libraryVariant.getBuildType().getName().equals("debug")) {
                    @Suppress("DEPRECATION")
                    val javaCompile = libraryVariant.javaCompile
                    if (supportLibraryExtension.failOnUncheckedWarnings) {
                        javaCompile.options.compilerArgs.add("-Xlint:unchecked")
                    }
                    if (supportLibraryExtension.failOnDeprecationWarnings) {
                        javaCompile.options.compilerArgs.add("-Xlint:deprecation")
                    }
                    javaCompile.options.compilerArgs.add("-Werror")
                }
            }
        }

        project.apply<LibraryPlugin>()

        val library = project.extensions.findByType(LibraryExtension::class.java)
                ?: throw Exception("Failed to find Android extension")

        library.flavorDimensions("version")
        library.productFlavors {
            it.create("minDepVersions")
            it.get("minDepVersions").dimension = "version"
            it.create("maxDepVersions")
            it.get("maxDepVersions").dimension = "version"
        }

        project.configureLint(library.lintOptions, supportLibraryExtension)
    }
}

/**
 * For benchmarks, inject an extra adb command to AOT compile the APK after install.
 *
 * This is disgusting, but seems to be the only way to AOT-compile an APK without essentially
 * reimplementing connectedCheck. If AGP adds support for this in some other way, we can avoid this.
 *
 * Additionally, AOT isn't a great solution here, since it's not very representative of real world.
 * Ideally we'd use profile-driven compilation, but that requires significantly more steps. However
 * AOT still gives us much better performance stability.
 *
 * For more info about AOT compilation, and why it's only used on N+:
 *
 * https://source.android.com/devices/tech/dalvik/jit-compiler
 *
 * https://android.googlesource.com/platform/system/extras/+/master/simpleperf/doc/README.md#why-we-suggest-profiling-on-android-n-devices
 */
private fun Project.injectCompilationForBenchmarks(
    extension: LibraryExtension,
    supportLibraryExtension: SupportLibraryExtension
) {
    if (isBenchmark()) {
        tasks.filter { it.group == JavaBasePlugin.VERIFICATION_GROUP }.forEach {
            it.doFirst {
                logger.log(LogLevel.WARN,
                        "Warning: ADB command injection used to force AOT-compile benchmark")
            }
        }

        val group = supportLibraryExtension.mavenGroup

        // NOTE: we assume here that all benchmarks have package name $groupname.benchmark.test
        val aotCompile = "cmd package compile -m speed -f $group.benchmark.test"
        // only run aotCompile on N+, where it's supported
        val inject = "if ((`getprop ro.build.version.sdk` >= 24)); then $aotCompile; fi"
        // NOTE: we assume here that all benchmarks have apk name $projectname-debug-androidTest.apk
        val options = "/data/local/tmp/$name-debug-androidTest.apk && $inject #"
        extension.adbOptions.setInstallOptions(*options.split(" ").toTypedArray())
    }
}

/**
 * Goes through all the dependencies in the passed in configurations and finds each dependency
 * depending on an androidx library. Then For any that is specified as a version range, overrides
 * the resolution strategy for this dependency to resolve to the earliest version (i.e if the
 * version range is [x.y.z, a.b.c) then make the dependency resolve to x.y.z.)
 */
private fun useMinimumDependencyVersions(configurations: ConfigurationContainer) {
    configurations.all { configuration ->
        configuration.resolutionStrategy.eachDependency { dep ->
                if (artifactSupportsSemVer(dep.target.group) &&
                        isSupportedDependencyRange(dep.target.version)) {
                    // Set dependency version to the first value in the range (i.e the minimum
                    // dependency, this is a hard set so if the version does not exist the build will
                    // error)
                    dep.useVersion(dep.target.version.removePrefix("[")
                        .split(",")[0])
                }
            }
        }
    }

private fun artifactSupportsSemVer(artifactGroup: String): Boolean {
    return artifactGroup.startsWith("androidx.")
}

private fun isDependencyRange(version: String): Boolean {
    return ((version.startsWith("[") || version.startsWith("(")) &&
            (version.endsWith("]") || version.endsWith(")"))) ||
            version.contains("+")
}

private fun isSupportedDependencyRange(version: String): Boolean {
    if (!isDependencyRange(version)) return false
    if (!version.startsWith("[") || !version.endsWith(")")) return false
    return true
}
