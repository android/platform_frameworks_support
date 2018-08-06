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
import com.android.build.gradle.internal.dsl.LintOptions
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.apply

/**
 * Support java library specific plugin that sets common configurations needed for
 * support library modules.
 */
class SupportJavaLibraryPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        project.apply<AndroidXPlugin>()

        val supportLibraryExtension = project.extensions.create("supportLibrary",
                SupportLibraryExtension::class.java, project)
        project.configureMavenArtifactUpload(supportLibraryExtension)

        project.apply(mapOf("plugin" to "java"))
        project.afterEvaluate {
            if (supportLibraryExtension.useMetalava) {
                Metalava.registerJavaProject(project, supportLibraryExtension)
            } else {
                DiffAndDocs.registerJavaProject(project, supportLibraryExtension)
            }
        }

        project.apply(mapOf("plugin" to "com.android.lint"))
        // Create fake variant tasks since that is what is invoked on CI and by developers.
        project.tasks.create("lintDebug").dependsOn("lint")
        project.tasks.create("lintRelease").dependsOn("lint")

        val lintOptions = project.extensions.getByType(LintOptions::class.java)
        project.configureLint(lintOptions, supportLibraryExtension)
    }
}
