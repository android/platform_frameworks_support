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

import org.gradle.api.logging.LogLevel
import org.gradle.api.tasks.TaskAction
import javax.inject.Inject

open class UnlockClocksTask @Inject constructor(sdkPath: String) : BenchmarkBaseTask(sdkPath) {
    init {
        description = "unlocks clocks of device by rebooting"
    }

    @Suppress("unused")
    @TaskAction
    fun exec() {
        project.logger.log(LogLevel.LIFECYCLE, "Rebooting device to reset clocks")
        execAdbSync(arrayOf("reboot"))
    }
}
