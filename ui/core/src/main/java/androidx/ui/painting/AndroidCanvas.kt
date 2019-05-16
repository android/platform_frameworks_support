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

package androidx.ui.painting

import android.graphics.Matrix
import androidx.ui.Vertices
import androidx.ui.core.toFrameworkRect
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.RRect
import androidx.ui.engine.geometry.Rect
import androidx.ui.graphics.Color
import androidx.ui.vectormath64.Matrix4
import androidx.ui.vectormath64.degrees
import androidx.ui.vectormath64.isIdentity

/* actual */ fun Canvas(image: Image): Canvas =
    AndroidCanvas(android.graphics.Canvas(image.nativeImage))

/* actual */ fun Canvas(
    recorder: PictureRecorder,
    cullRect: Rect = Rect.largest
): Canvas {
    return AndroidCanvas(
        recorder.frameworkPicture.beginRecording(
            cullRect.width.toInt(),
            cullRect.height.toInt()
        )
    )
}

fun Canvas(c: android.graphics.Canvas): Canvas = AndroidCanvas(c)

private class AndroidCanvas(val internalCanvas: android.graphics.Canvas) : Canvas {

    private val internalPath = Path()

    private val internalRectF = android.graphics.RectF()

    /**
     * @see Canvas.save
     */
    override fun save(block: Canvas.() -> Unit) {
        internalCanvas.save()
        block(this)
        internalCanvas.restore()
    }

    /**
     * @see Canvas.save
     */
    override fun save() {
        internalCanvas.save()
    }

    /**
     * @see Canvas.restore
     */
    override fun restore() {
        internalCanvas.restore()
    }

    override val nativeCanvas: NativeCanvas
        get() = internalCanvas

    /**
     * @see Canvas.saveLayer
     */
    @SuppressWarnings("deprecation")
    override fun saveLayer(bounds: Rect?, paint: Paint, block: Canvas.() -> Unit) {
        if (bounds == null) {
            TODO("Migration/njawad framework does not have " +
                    "equivalent for saveLayerWithoutBounds")
        } else {
            @Suppress("DEPRECATION")
            internalCanvas.saveLayer(
                bounds.left,
                bounds.top,
                bounds.right,
                bounds.bottom,
                paint.toFrameworkPaint(),
                android.graphics.Canvas.ALL_SAVE_FLAG
            )
            block(this)
            internalCanvas.restore()
        }
    }

    // TODO(Migration/njawad find equivalent implementation for _saveLayerWithoutBounds or not
//    void _saveLayerWithoutBounds(List<dynamic> paintObjects, ByteData paintData)
//    native 'Canvas_saveLayerWithoutBounds';
//    void _saveLayer(double left,
//    double top,
//    double right,
//    double bottom,
//    List<dynamic> paintObjects,
//    ByteData paintData) native 'Canvas_saveLayer';

    /**
     * @see Canvas.translate
     */
    override fun translate(dx: Float, dy: Float) {
        internalCanvas.translate(dx, dy)
    }

    /**
     * @see Canvas.scale
     */
    override fun scale(sx: Float, sy: Float) {
        internalCanvas.scale(sx, sy)
    }

    /**
     * @see Canvas.rotate
     */
    override fun rotate(radians: Float) {
        internalCanvas.rotate(degrees(radians))
    }

    /**
     * @see Canvas.skew
     */
    override fun skew(sx: Float, sy: Float) {
        internalCanvas.skew(sx, sy)
    }

    /**
     * @see Canvas.transform
     */
    override fun transform(matrix4: Matrix4) {
        if (!matrix4.isIdentity()) {
            val frameworkMatrix = Matrix()
            if (matrix4.get(2, 0) != 0f ||
                matrix4.get(2, 1) != 0f ||
                matrix4.get(2, 0) != 0f ||
                matrix4.get(2, 1) != 0f ||
                matrix4.get(2, 2) != 1f ||
                matrix4.get(2, 3) != 0f ||
                matrix4.get(3, 2) != 0f) {
                throw IllegalStateException("Android does not support arbitrary transforms")
            }
            val values = floatArrayOf(
                matrix4.get(0, 0),
                matrix4.get(1, 0),
                matrix4.get(3, 0),
                matrix4.get(0, 1),
                matrix4.get(1, 1),
                matrix4.get(3, 1),
                matrix4.get(0, 3),
                matrix4.get(1, 3),
                matrix4.get(3, 3)
            )
            frameworkMatrix.setValues(values)
            internalCanvas.concat(frameworkMatrix)
        }
    }

    /**
     * @see Canvas.clipRect
     */
    @SuppressWarnings("deprecation")
    override fun clipRect(rect: Rect, clipOp: ClipOp) {
        val frameworkRect = rect.toFrameworkRect()
        @Suppress("DEPRECATION")
        when (clipOp) {
            ClipOp.intersect -> internalCanvas.clipRect(frameworkRect)
            ClipOp.difference -> internalCanvas.clipRect(frameworkRect,
                android.graphics.Region.Op.DIFFERENCE)
        }
    }

    /**
     * @see Canvas.clipRRect
     */
    override fun clipRRect(rrect: RRect) {
        internalPath.reset()
        internalPath.addRRect(rrect)
        clipPath(internalPath)
    }

    /**
     * @see Canvas.clipPath
     */
    override fun clipPath(path: Path) {
        internalCanvas.clipPath(path.toFrameworkPath())
    }

    /**
     * @see Canvas.drawColor
     */
    override fun drawColor(color: Color, blendMode: BlendMode) {
        internalCanvas.drawColor(color.toArgb(), blendMode.toPorterDuffMode())
    }

    /**
     * @see Canvas.drawLine
     */
    override fun drawLine(p1: Offset, p2: Offset, paint: Paint) {
        internalCanvas.drawLine(
            p1.dx,
            p1.dy,
            p2.dx,
            p2.dy,
            paint.toFrameworkPaint()
        )
    }

    /**
     * @see Canvas.drawPaint
     */
    override fun drawPaint(paint: Paint) {
        internalCanvas.drawPaint(paint.toFrameworkPaint())
    }

    /**
     * @see Canvas.drawRect
     */
    override fun drawRect(rect: Rect, paint: Paint) {
        internalCanvas.drawRect(rect.toFrameworkRect(), paint.toFrameworkPaint())
    }

    /**
     * @see Canvas.drawDRRect
     */
    override fun drawRRect(rrect: RRect, paint: Paint) {
        internalPath.reset()
        internalPath.addRRect(rrect)
        internalCanvas.drawPath(internalPath.toFrameworkPath(), paint.toFrameworkPaint())
    }

    /**
     * @see Canvas.drawDRRect
     */
    override fun drawDRRect(outer: RRect, inner: RRect, paint: Paint) {
        // TODO(Migration/njawad find better way to recreate functionality with Framework APIs
        // without creating temporary paths on each draw call
        val outerPath = Path()
        val innerPath = Path()

        outerPath.addRRect(outer)
        innerPath.addRRect(inner)

        internalPath.reset()
        internalPath.op(outerPath, innerPath, PathOperation.difference)
        internalCanvas.drawPath(internalPath.toFrameworkPath(), paint.toFrameworkPaint())
    }

    /**
     * @see Canvas.drawOval
     */
    override fun drawOval(rect: Rect, paint: Paint) {
        internalRectF.set(rect.toFrameworkRect())
        internalCanvas.drawOval(internalRectF,
            paint.toFrameworkPaint())
    }

    /**
     * @see Canvas.drawCircle
     */
    override fun drawCircle(c: Offset, radius: Float, paint: Paint) {
        internalCanvas.drawCircle(
            c.dx,
            c.dy,
            radius,
            paint.toFrameworkPaint()
        )
    }

    /**
     * @see Canvas.drawArc
     */
    override fun drawArc(
        rect: Rect,
        startAngle: Float,
        sweepAngle: Float,
        useCenter: Boolean,
        paint: Paint
    ) {
        internalRectF.set(rect.toFrameworkRect())
        internalCanvas.drawArc(
            internalRectF,
            degrees(startAngle),
            degrees(sweepAngle),
            useCenter,
            paint.toFrameworkPaint()
        )
    }

    /**
     * @see Canvas.drawPath
     */
    override fun drawPath(path: Path, paint: Paint) {
        internalCanvas.drawPath(path.toFrameworkPath(), paint.toFrameworkPaint())
    }

    /**
     * @see Canvas.drawImage
     */
    override fun drawImage(image: Image, p: Offset, paint: Paint) {
        internalCanvas.drawBitmap(
            image.nativeImage,
            p.dx,
            p.dy,
            paint.toFrameworkPaint()
        )
    }

    /**
     * @See Canvas.drawImageRect
     */
    override fun drawImageRect(image: Image, src: Rect, dst: Rect, paint: Paint) {
        internalCanvas.drawBitmap(
            image.nativeImage,
            src.toFrameworkRect(),
            dst.toFrameworkRect(),
            paint.toFrameworkPaint()
        )
    }

    /**
     * @see Canvas.drawPicture
     */
    override fun drawPicture(picture: Picture) {
        internalCanvas.drawPicture(picture.frameworkPicture)
    }

    /**
     * @see Canvas.drawParagraph
     */
    // TODO(siyamed): Decide what to do with this method. Should it exist on Canvas?
//    fun drawParagraph(paragraph: Paragraph, offset: Offset) {
//        assert(paragraph != null)
//        assert(Offset.isValid(offset))
//        paragraph.paint(this, offset.dx, offset.dy)
//    }

    /**
     * @see Canvas.drawPoints
     */
    override fun drawPoints(pointMode: PointMode, points: List<Offset>, paint: Paint) {
        when (pointMode) {
            // Draw a line between each pair of points, each point has at most one line
            // If the number of points is odd, then the last point is ignored.
            PointMode.lines -> drawLines(points, paint, 2)

            // Connect each adjacent point with a line
            PointMode.polygon -> drawLines(points, paint, 1)

            // Draw a point at each provided coordinate
            PointMode.points -> drawPoints(points, paint)
        }
    }

    private fun drawPoints(points: List<Offset>, paint: Paint) {
        for (point in points) {
            internalCanvas.drawPoint(point.dx,
                point.dy,
                paint.toFrameworkPaint())
        }
    }

    /**
     * Draw lines connecting points based on the corresponding step.
     *
     * ex. 3 points with a step of 1 would draw 2 lines between the first and second points
     * and another between the second and third
     *
     * ex. 4 points with a step of 2 would draw 2 lines between the first and second and another
     * between the third and fourth. If there is an odd number of points, the last point is
     * ignored
     *
     * @see drawRawLines
     */
    private fun drawLines(points: List<Offset>, paint: Paint, stepBy: Int) {
        if (points.size >= 2) {
            for (i in 0 until points.size - 1 step stepBy) {
                val p1 = points[i]
                val p2 = points[i + 1]
                internalCanvas.drawLine(
                    p1.dx,
                    p1.dy,
                    p2.dx,
                    p2.dy,
                    paint.toFrameworkPaint()
                )
            }
        }
    }

    /**
     * @see Canvas.drawRawPoints
     */
    override fun drawRawPoints(pointMode: PointMode, points: FloatArray, paint: Paint) {
        if (points.size % 2 != 0) {
            throw IllegalArgumentException("points must have an even number of values")
        }
        when (pointMode) {
            PointMode.lines -> drawRawLines(points, paint, 2)
            PointMode.polygon -> drawRawLines(points, paint, 1)
            PointMode.points -> drawRawPoints(points, paint, 2)
        }
    }

    private fun drawRawPoints(points: FloatArray, paint: Paint, stepBy: Int) {
        if (points.size % 2 == 0) {
            for (i in 0 until points.size - 1 step stepBy) {
                val x = points[i]
                val y = points[i + 1]
                internalCanvas.drawPoint(x, y, paint.toFrameworkPaint())
            }
        }
    }

    /**
     * Draw lines connecting points based on the corresponding step. The points are interpreted
     * as x, y coordinate pairs in alternating index positions
     *
     * ex. 3 points with a step of 1 would draw 2 lines between the first and second points
     * and another between the second and third
     *
     * ex. 4 points with a step of 2 would draw 2 lines between the first and second and another
     * between the third and fourth. If there is an odd number of points, the last point is
     * ignored
     *
     * @see drawLines
     */
    private fun drawRawLines(points: FloatArray, paint: Paint, stepBy: Int) {
        // Float array is treated as alternative set of x and y coordinates
        // x1, y1, x2, y2, x3, y3, ... etc.
        if (points.size >= 4 && points.size % 2 == 0) {
            for (i in 0 until points.size - 3 step stepBy * 2) {
                val x1 = points[i]
                val y1 = points[i + 1]
                val x2 = points[i + 2]
                val y2 = points[i + 3]
                internalCanvas.drawLine(
                    x1,
                    y1,
                    x2,
                    y2,
                    paint.toFrameworkPaint()
                )
            }
        }
    }

    override fun drawVertices(vertices: Vertices, blendMode: BlendMode, paint: Paint) {
        // TODO(Migration/njawad align drawVertices blendMode parameter usage with framework
        // android.graphics.Canvas#drawVertices does not consume a blendmode argument
        internalCanvas.drawVertices(
            vertices.vertexMode.toFrameworkVertexMode(),
            vertices.positions.size,
            vertices.positions,
            0, // TODO(Migration/njawad figure out proper vertOffset)
            vertices.textureCoordinates,
            0, // TODO(Migration/njawad figure out proper texOffset)
            vertices.colors,
            0, // TODO(Migration/njawad figure out proper colorOffset)
            vertices.indices,
            0, // TODO(Migration/njawad figure out proper indexOffset)
            vertices.indices.size,
            paint.toFrameworkPaint()
        )
    }

    // TODO(Migration/njawad add drawAtlas API)

    // TODO(Migration/njawad add drawRawAtlas API)

    // TODO(Migration/njawad add drawShadow API)
}