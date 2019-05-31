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

package androidx.ui.test

class Expectation internal constructor(
    internal val errorMessage: (actualCount: Int) -> String,
    internal val condition: (actualCount: Int) -> Boolean
)

fun expectExactly(expectedCount: Int): Expectation {
    return expect(
        { actualCount -> "Found '$actualCount' nodes but exactly '$expectedCount' was expected!" },
        { actualCount -> actualCount == expectedCount }
    )
}

fun expect(
    errorMessage: (actualCount: Int) -> String,
    condition: (actualCount: Int) -> Boolean
): Expectation {
    return Expectation(errorMessage, condition)
}