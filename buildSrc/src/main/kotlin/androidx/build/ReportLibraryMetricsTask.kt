/*
 * Copyright 2019 The Android Open Source Project
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

import androidx.build.gradle.getByType
import com.android.build.gradle.LibraryExtension
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction
import org.json.simple.JSONObject
import java.io.File

private const val AAR_FILE_EXTENSION = ".aar"
private const val AAR_SIZE = "aar_size"
private const val METRICS_DIRECTORY = "librarymetrics"
private const val METRICS_FILE_NAME = "library_metrics.json"

open class ReportLibraryMetricsTask : DefaultTask() {

    init {
        group = "LibraryMetrics"
        description = "Task for reporting build time library metrics. Currently gathers .aar sizes."
    }

    @TaskAction
    fun reportLibraryMetrics() {
        val distDir = project.rootProject.getDistributionDirectory()
        val outputDir = File(distDir.canonicalPath + '/' + METRICS_DIRECTORY + '/' + project.name)
        outputDir.mkdirs()
        val outputFile = outputDir.resolve(METRICS_FILE_NAME)
        val json = JSONObject()

        val aarSize = getAarSize()
        if (aarSize > 0L) {
            json[AAR_SIZE] = aarSize
        }

        outputFile.writeText(json.toJSONString())
    }

    private fun getAarSize(): Long {
        val debugVariants = project.extensions.getByType<LibraryExtension>()
            .libraryVariants.filter { it.buildType.name == "debug" }
        val aarFiles = debugVariants[0].packageLibraryProvider.get()
            .outputs.files.files.filter { file ->
                file.name.contains(AAR_FILE_EXTENSION)
            }
        return when {
            aarFiles.isEmpty() -> 0
            aarFiles.size == 1 -> aarFiles[0].length()
            else -> {
                throw IllegalStateException("Found ${aarFiles.size} .aar files, was expecting 1.")
            }
        }
    }
}
