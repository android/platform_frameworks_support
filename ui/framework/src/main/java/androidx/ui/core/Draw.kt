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
package androidx.ui.core

import androidx.ui.painting.Canvas
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.ui.painting.Paint

/**
 * Use Draw to get a [Canvas] and [Paint] to draw into the parent.
 *
 *     Draw { canvas, paint, parentSize ->
 *         paint.color = Color(0xFF000000.toInt())
 *         canvas.drawRect(Rect(0.0f, 0.0f, parentSize.width, parentSize.height, paint)
 *     }
 *
 * Draw also accepts children as an argument. By default the children are drawn
 * after the draw commands. If it is important to order canvas operations in a
 * different way, use [DrawScope.drawChildren]:
 *
 *     Draw(children) { canvas, _, parentSize ->
 *         canvas.save()
 *         val circle = Path()
 *         circle.addOval(parentSize.toRect())
 *         canvas.clipPath(circle)
 *         drawChildren()
 *         canvas.restore()
 *     }
 */
@Composable
fun Draw(
    children: @Composable() () -> Unit = {},
    @Children(composable = false)
    onPaint: DrawScope.(canvas: Canvas, paint: Paint, parentSize: PxSize) -> Unit
) {
    // Hide the internals of DrawNode
    <DrawNode onPaint={ canvas, paint, parentSize ->
        DrawScope(this).onPaint(canvas, paint, parentSize)
    }> children()
    </DrawNode>
}

/**
 * Receiver scope for [Draw] lambda that allows ordering the child drawing between
 * canvas operations.
 */
class DrawScope internal constructor(private val drawNodeScope: DrawNodeScope) : DensityReceiver {
    /**
     * Causes child drawing operations to run.
     */
    fun drawChildren() {
        drawNodeScope.drawChildren()
    }

    override val density: Density
        get() = drawNodeScope.density
}