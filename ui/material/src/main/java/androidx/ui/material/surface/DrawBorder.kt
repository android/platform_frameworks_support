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

package androidx.ui.material.surface

import androidx.ui.core.Draw
import androidx.ui.core.toRect
import androidx.ui.material.borders.ShapeBorder
import androidx.compose.Composable
import androidx.compose.composer

/**
 * Draws the border of the provided [shape].
 *
 * TODO("Andrey: Find the proper module and package for it")
 */
@Composable
fun DrawBorder(shape: ShapeBorder) {
    Draw { canvas, _, parentSize ->
        //TODO accept paint as a param here and not create every time
        shape.paint(
            canvas,
            density,
            parentSize.toRect(),
            null
        )
    }
}
