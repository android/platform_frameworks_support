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

package androidx.benchmark.gradle

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.TaskAction
import java.io.File
import javax.inject.Inject

open class BenchmarkReportTask @Inject constructor(private val adb: Adb) : DefaultTask() {
    private val benchmarkReportDir: File

    init {
        group = "Android"
        description = "Run benchmarks found in the current project and output reports to the " +
                "benchmark_reports folder under the project's build directory."

        benchmarkReportDir = File(project.buildDir, "benchmark_reports")
        outputs.dir(benchmarkReportDir)

        // This task should mirror the upToDate behavior of connectedAndroidTest as we always want
        // this task to run after connectedAndroidTest is run to pull the most recent benchmark
        // report data, even when tests are triggered multiple times in a row without source
        // changes.
        outputs.upToDateWhen { false }
    }

    @Suppress("unused")
    @TaskAction
    fun exec() {
        // Fetch reports from all available devices as the default behaviour of connectedAndroidTest
        // is to run on all available devices.
        getReportsForDevices()
    }

    private fun getReportsForDevices() {
        if (benchmarkReportDir.exists()) {
            benchmarkReportDir.deleteRecursively()
        }
        benchmarkReportDir.mkdirs()

        val deviceIds = adb.execSync("devices -l").stdout
            .split("\n")
            .drop(1)
            .filter { !it.contains("unauthorized") }
            .map { it.split(Regex("\\s+")).first().trim() }
            .filter { !it.isBlank() }

        for (deviceId in deviceIds) {
            val dataDir = getReportDirForDevice(deviceId)
            if (dataDir.isBlank()) {
                throw StopExecutionException(
                    "Failed to find benchmark reports on device: $deviceId"
                )
            }

            val outDir = File(benchmarkReportDir, deviceId)
            outDir.mkdirs()
            getReportsForDevice(outDir, dataDir, deviceId)
        }
    }

    private fun getReportsForDevice(benchmarkReportDir: File, dataDir: String, deviceId: String) {
        adb.execSync("shell ls $dataDir", deviceId)
            .stdout
            .split("\n")
            .map { it.trim() }
            .filter { it.matches(Regex(".*benchmarkData[.](?:xml|json)$")) }
            .forEach {
                val src = "$dataDir/$it"
                adb.execSync("pull $src $benchmarkReportDir/$it", deviceId)
                adb.execSync("shell rm $src", deviceId)
            }
    }

    /**
     * Query for test runner user's Download dir on shared public external storage via content
     * provider APIs.
     *
     * This folder is typically accessed in Android code via
     * Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
     */
    private fun getReportDirForDevice(deviceId: String): String {
        val cmd = "shell content query --uri content://media/external/file --projection _data" +
                " --where \"_data LIKE '%Download'\""

        // NOTE: stdout of the above command is of the form:
        // Row: 0 _data=/storage/emulated/0/Download
        return adb.execSync(cmd, deviceId).stdout
            .split("\n")
            .first()
            .trim()
            .split(Regex("\\s+"))
            .last()
            .split("=")
            .last()
            .trim()
    }
}
