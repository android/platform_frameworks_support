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

import org.gradle.api.Task
import org.gradle.api.tasks.TaskProvider

fun <T : Task, O : Task> TaskProvider<T>.lazyDependsOn(other: TaskProvider<O>) {
    this.configure {
        it.dependsOn(other)
    }
}

fun <T : Task> TaskProvider<T>.lazyDependsOn(other: String) {
    this.configure {
        it.dependsOn(other)
    }
}

fun <T : Task> TaskProvider<T>.lazyDependsOn(other: Task) {
    this.configure {
        it.dependsOn(other)
    }
}