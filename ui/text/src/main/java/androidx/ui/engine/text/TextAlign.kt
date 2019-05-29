<<<<<<< HEAD   (5a228e Merge "Merge empty history for sparse-5593360-L5240000032052)
=======
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
package androidx.ui.engine.text

/**
 * Whether and how to align text horizontally.
 */
enum class TextAlign {
    /** Align the text on the left edge of the container. */
    Left,

    /** Align the text on the right edge of the container. */
    Right,

    /** Align the text in the center of the container. */
    Center,

    /**
     * Stretch lines of text that end with a soft line break to fill the width of
     * the container.
     *
     * Lines that end with hard line breaks are aligned towards the [Start] edge.
     */
    Justify,

    /**
     * Align the text on the leading edge of the container.
     *
     * For left-to-right text ([TextDirection.ltr]), this is the left edge.
     *
     * For right-to-left text ([TextDirection.rtl]), this is the right edge.
     */
    Start,

    /**
     * Align the text on the trailing edge of the container.
     *
     * For left-to-right text ([TextDirection.ltr]), this is the right edge.
     *
     * For right-to-left text ([TextDirection.rtl]), this is the left edge.
     */
    End
}
>>>>>>> BRANCH (2bab7f Merge "Merge cherrypicks of [972846] into sparse-5613706-L34)
