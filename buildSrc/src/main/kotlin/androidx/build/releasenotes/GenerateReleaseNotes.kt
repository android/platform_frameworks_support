/*
 * Copyright (C) 2019 The Android Open Source Project
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

package androidx.build.releasenotes

import org.gradle.api.DefaultTask
import org.gradle.api.tasks.TaskAction

/**
 * Task for verifying the androidx dependency-stability-suffix rule
 * (A library is only as stable as its lease stable dependency)
 */
open class GenerateReleaseNotes : DefaultTask() {

    init {
        group = "Documentation"
        description = "Task for creating release notes for a specific library"
    }

    /**
     * <Task description>
     */
    @TaskAction
    fun createReleaseNotes() {
    }
}
