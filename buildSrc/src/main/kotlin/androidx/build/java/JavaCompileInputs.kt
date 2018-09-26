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

package androidx.build.java

import androidx.build.androidJarFile
import com.android.build.gradle.api.BaseVariant
import com.android.build.gradle.LibraryExtension
import org.gradle.api.attributes.Attribute
import org.gradle.api.file.FileCollection
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.tasks.SourceSet
import java.io.File

/**
 * JavaCompileInputs contains the information required to compile Java/Kotlin code
 * This can be helpful for creating Metalava and Dokka tasks with the same settings
 */
open class JavaCompileInputs {
    companion object {
        /** Constructs a JavaCompileInputs from a library and its variant */
        fun fromLibraryVariant(library: LibraryExtension, variant: BaseVariant): JavaCompileInputs {
            val inputs = JavaCompileInputs()
            inputs.sourcePaths = variant.sourceSets.find({ it -> it.name == "main" })!!.javaDirectories
            inputs.bootClasspath = library.bootClasspath
            var incomingDeps = variant.compileConfiguration.incoming
            inputs.dependencyClasspath = incomingDeps.artifactView { config ->
                config.attributes { container ->
                    container.attribute(Attribute.of("artifactType", String::class.java), "jar")
                }
            }.artifacts.artifactFiles
            return inputs
        }

        /** Constructs a JavaCompileInputs from a sourceset */
        fun fromSourceSet(sourceSet: SourceSet, project: Project): JavaCompileInputs {
            val inputs = JavaCompileInputs()
            inputs.sourcePaths = sourceSet.allSource.srcDirs
            inputs.bootClasspath = androidJarFile(project).files
            inputs.dependencyClasspath = sourceSet.compileClasspath
            return inputs
        }
    }

    /** Source files to process */
    lateinit var sourcePaths: Collection<File>

    /** Dependencies of [sourcePaths]. */
    lateinit var dependencyClasspath: FileCollection

    /** Android's boot classpath. */
    lateinit var bootClasspath: Collection<File>
}

