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

import com.android.ddmlib.Log
import org.gradle.api.DefaultTask
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.StopExecutionException
import org.gradle.api.tasks.TaskAction
import org.gradle.kotlin.dsl.property
import java.io.File

open class BenchmarkReportTask : DefaultTask() {
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

    @Input
    val adbPath: Property<String> = project.objects.property()

    @TaskAction
    fun exec() {
        // Fetch reports from all available devices as the default behaviour of connectedAndroidTest
        // is to run on all available devices.
        getReportsForDevices(Adb(adbPath.get(), logger))
    }

    private fun getReportsForDevices(adb: Adb) {
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
            val filesDir = getExternalFilesDirForDevice(adb, deviceId)
            if (filesDir.isBlank()) {
                throw StopExecutionException("Failed to find benchmark data on device: $deviceId")
            }

            val reportDirs = getReportDirsForDevice(adb, deviceId, filesDir)
            if (reportDirs.isEmpty()) {
                throw StopExecutionException("Failed to find benchmark data on device: $deviceId")
            }

            val outDir = File(benchmarkReportDir, deviceId)
            outDir.mkdirs()
            for (reportDir in reportDirs) {
                getReportsForDevice(adb, outDir, reportDir, deviceId)
            }
            Log.logAndDisplay(
                Log.LogLevel.INFO,
                "Benchmark",
                "Benchmark report files generated at ${benchmarkReportDir.absolutePath}"
            )
        }
    }

    private fun getReportsForDevice(
        adb: Adb,
        benchmarkReportDir: File,
        dataDir: String,
        deviceId: String
    ) {
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
     * Query for test runner user's external Download dir on shared public external storage via
     * content provider APIs.
     *
     * This folder is typically accessed in Android code via
     * `Context.getExternalFilesDir(Environment.DIRECTORY_ALARMS)`
     */
    private fun getExternalFilesDirForDevice(adb: Adb, deviceId: String): String {
        val cmd = "shell content query --uri content://media/external/file --projection _data" +
                " --where \"_data LIKE '%Android/data'\""

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

    /**
     * Recursively find all app-scoped external storage for Alarms, where benchmark reports are
     * saved.
     */
    private fun getReportDirsForDevice(
        adb: Adb,
        deviceId: String,
        externalFilesDir: String
    ): List<String> {
        val cmd = "shell ls -1p $externalFilesDir"

        // NOTE: stdout of the above command is of the form:
        // Row: 0 _data=/storage/emulated/0/Download
        return adb.execSync(cmd, deviceId).stdout
            .split("\n")
            .filter { it.endsWith('/') }
            .flatMap { adb.execSync("ls $it", deviceId).stdout.split("\n") }
            .filter { it.endsWith("Alarm") }
    }
}
