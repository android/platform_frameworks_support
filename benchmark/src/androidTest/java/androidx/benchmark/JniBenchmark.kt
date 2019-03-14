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

package androidx.benchmark

import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import kotlin.random.Random

@LargeTest
@RunWith(JUnit4::class)
class JniBenchmark {

    @get:Rule
    val benchmarkRule = BenchmarkRule()

    @Test
    fun nativeBenchmark() {
        benchmarkRule.measure {
            Random.nextInt(0, 100) + Random.nextInt(0, 100)
        }
    }

    @Test
    fun jniBenchmark() {
        benchmarkRule.measure {
            JniLib().add(Random.nextInt(0, 100), Random.nextInt(0, 100))
        }
    }
}
