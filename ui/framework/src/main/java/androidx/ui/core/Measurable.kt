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
package androidx.ui.core

/**
 * A part of the composition that can be measured. This represents a [Layout] somewhere
 * down the hierarchy.
 *
 * @return a [Placeable] that can be used within a [layoutResult] block
 */
abstract class Measurable {
    private val receiver = MeasureReceiver()
    /**
     * Data provided by the [ParentData].
     */
    abstract val parentData: Any?
    protected abstract fun MeasureReceiver.measure(constraints: Constraints): Placeable
    internal fun measureInternal(constraints: Constraints): Placeable {
        return receiver.measure(constraints)
    }
}

class MeasureReceiver internal constructor() {
    fun Measurable.measure(constraints: Constraints): Placeable {
        return measureInternal(constraints)
    }
}
