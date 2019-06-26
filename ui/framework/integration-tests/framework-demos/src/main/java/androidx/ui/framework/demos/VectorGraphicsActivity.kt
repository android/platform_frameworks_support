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

package androidx.ui.framework.demos

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import androidx.compose.Children
import androidx.compose.Composable
import androidx.compose.composer
import androidx.compose.setContent
import androidx.compose.unaryPlus
import androidx.ui.core.CraneWrapper

import androidx.ui.core.IntPx
import androidx.ui.core.Layout
import androidx.ui.core.dp
import androidx.ui.core.vectorgraphics.DrawVector
import androidx.ui.core.vectorgraphics.compat.vectorResource
import androidx.ui.core.vectorgraphics.group
import androidx.ui.core.vectorgraphics.path
import androidx.ui.core.withDensity
import androidx.ui.graphics.vectorgraphics.PathBuilder
import androidx.ui.graphics.vectorgraphics.PathDelegate
import androidx.ui.layout.Center
import androidx.ui.vector.VectorScope

class VectorGraphicsActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)


        val res = getResources()
        setContent {
            CraneWrapper {
                Column {
                    val width = +withDensity {
                        480.dp.toIntPx()
                    }
                    val height = +withDensity {
                        480.dp.toIntPx()
                    }
                    FixedLayout(width, height) {
                        vectorResource(
                            res = res,
                            resId = R.drawable.ic_crane
                        )
                    }

                    Center {
                        FixedLayout(IntPx(300.0f.toInt()), IntPx(300.0f.toInt())) {
                            vectorShape(300.0f, 300.0f)
                        }
                    }
                }
            }
        }
    }

    @Composable
    fun FixedLayout(width: IntPx, height: IntPx, @Children child: @Composable() () -> Unit) {
        Layout(children = { child() },
            layoutBlock = { _, _ ->
                layout(width, height) {}
            })
    }

    @Composable
    fun vectorShape(defaultWidth: Float, defaultHeight: Float) {
        val viewportWidth = defaultWidth
        val viewportHeight = defaultHeight
        DrawVector(
            name = "vectorShape",
            defaultWidth = defaultWidth,
            defaultHeight = defaultHeight,
            viewportWidth = viewportWidth,
            viewportHeight = viewportHeight
        ) {
            group(
                scaleX = 0.75f,
                scaleY = 0.75f,
                rotate = 45.0f,
                pivotX = (viewportWidth / 2),
                pivotY = (viewportHeight / 2)
            ) {
                backgroundPath(vectorWidth = viewportWidth, vectorHeight = viewportHeight)
                stripePath(vectorWidth = viewportWidth, vectorHeight = viewportHeight)
                group(
                    translateX = 50.0f,
                    translateY = 50.0f,
                    pivotX = (viewportWidth / 2),
                    pivotY = (viewportHeight / 2),
                    rotate = 25.0f
                ) {
                    val pathData = PathDelegate {
                        moveTo(viewportWidth / 2 - 100, viewportHeight / 2 - 100)
                        horizontalLineToRelative(200.0f)
                        verticalLineToRelative(200.0f)
                        horizontalLineToRelative(-200.0f)
                        close()
                    }
                    path(fill = Color.MAGENTA, pathData = pathData)
                }
            }
        }
    }

    @Composable
    fun VectorScope.backgroundPath(vectorWidth: Float, vectorHeight: Float) {
        val background = PathDelegate {
            horizontalLineTo(vectorWidth)
            verticalLineTo(vectorHeight)
            horizontalLineTo(0.0f)
            close()
        }

        path(fill = Color.CYAN, pathData = background)
    }

    @Composable
    fun VectorScope.stripePath(vectorWidth: Float, vectorHeight: Float) {
        val stripeDelegate = PathDelegate {
            stripe(vectorWidth, vectorHeight, 10)
        }

        path(stroke = Color.BLUE, pathData = stripeDelegate)
    }

    private fun PathBuilder.stripe(vectorWidth: Float, vectorHeight: Float, numLines: Int) {
        val stepSize = vectorWidth / numLines
        var currentStep = stepSize
        for (i in 1..numLines) {
            moveTo(currentStep, 0.0f)
            verticalLineTo(vectorHeight)
            currentStep += stepSize
        }
    }
}
