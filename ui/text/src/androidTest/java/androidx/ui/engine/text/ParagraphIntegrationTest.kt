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
package androidx.ui.engine.text

import androidx.core.os.BuildCompat
import androidx.test.filters.SdkSuppress
import androidx.test.filters.SmallTest
import androidx.test.platform.app.InstrumentationRegistry
import androidx.ui.core.Sp
import androidx.ui.core.px
import androidx.ui.core.sp
import androidx.ui.engine.geometry.Offset
import androidx.ui.engine.geometry.Rect
import androidx.ui.engine.text.FontTestData.Companion.BASIC_KERN_FONT
import androidx.ui.engine.text.FontTestData.Companion.BASIC_MEASURE_FONT
import androidx.ui.engine.text.FontTestData.Companion.FONT_100_REGULAR
import androidx.ui.engine.text.FontTestData.Companion.FONT_200_REGULAR
import androidx.ui.engine.text.font.FontFamily
import androidx.ui.engine.text.font.asFontFamily
import androidx.ui.engine.text.platform.px
import androidx.ui.engine.window.Locale
import androidx.ui.graphics.Color
import androidx.ui.matchers.equalToBitmap
import androidx.ui.painting.AnnotatedString
import androidx.ui.painting.Path
import androidx.ui.painting.PathOperation
import androidx.ui.painting.Shadow
import androidx.ui.painting.TextStyle
import org.hamcrest.Matchers.equalTo
import org.hamcrest.Matchers.not
import org.junit.Assert.assertThat
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4

@RunWith(JUnit4::class)
@SmallTest
class ParagraphIntegrationTest {
    // TODO(Migration/haoyuchang): These native calls should be removed after the
    // counterparts are implemented in crane.
    private lateinit var fontFamilyMeasureFont: FontFamily
    private lateinit var fontFamilyKernFont: FontFamily
    private lateinit var fontFamilyCustom100: FontFamily
    private lateinit var fontFamilyCustom200: FontFamily

    @Before
    fun setup() {
        // This sample font provides the following features:
        // 1. The width of most of visible characters equals to font size.
        // 2. The LTR/RTL characters are rendered as ▶/◀.
        // 3. The fontMetrics passed to TextPaint has descend - ascend equal to 1.2 * fontSize.
        fontFamilyMeasureFont = BASIC_MEASURE_FONT.asFontFamily()
        fontFamilyMeasureFont.context = InstrumentationRegistry.getInstrumentation().context
        // The kern_font provides the following features:
        // 1. Characters from A to Z are rendered as ▲ while a to z are rendered as ▼.
        // 2. When kerning is off, the width of each character is equal to font size.
        // 3. When kerning is on, it will reduce the space between two characters by 0.4 * width.
        fontFamilyKernFont = BASIC_KERN_FONT.asFontFamily()
        fontFamilyKernFont.context = InstrumentationRegistry.getInstrumentation().context
        fontFamilyCustom100 = FONT_100_REGULAR.asFontFamily()
        fontFamilyCustom200 = FONT_200_REGULAR.asFontFamily()
        fontFamilyCustom100.context = fontFamilyMeasureFont.context
        fontFamilyCustom200.context = fontFamilyMeasureFont.context
    }

    @Test
    fun empty_string() {
        val fontSize = 50.sp
        val text = ""
        val paragraph = simpleParagraph(text = text, fontSize = fontSize)

        paragraph.layout(ParagraphConstraints(width = 100.0f))

        assertThat(paragraph.width, equalTo(100.0f))
        assertThat(paragraph.height, equalTo(fontSize.px()))
        // defined in sample_font
        assertThat(paragraph.baseline, equalTo(fontSize.px() * 0.8f))
        assertThat(paragraph.maxIntrinsicWidth, equalTo(0.0f))
        assertThat(paragraph.minIntrinsicWidth, equalTo(0.0f))
        // TODO(Migration/siyamed): no baseline query per line?
        // TODO(Migration/siyamed): no line count?
    }

    @Test
    fun single_line_default_values() {
        val fontSize = 50.sp
        for (text in arrayOf("xyz", "\u05D0\u05D1\u05D2")) {
            val paragraph = simpleParagraph(text = text, fontSize = fontSize)

            // width greater than text width - 150
            paragraph.layout(ParagraphConstraints(width = 200.0f))

            assertThat(text, paragraph.width, equalTo(200.0f))
            assertThat(text, paragraph.height, equalTo(fontSize.px()))
            // defined in sample_font
            assertThat(text, paragraph.baseline, equalTo(fontSize.px() * 0.8f))
            assertThat(
                text,
                paragraph.maxIntrinsicWidth,
                equalTo(fontSize.px() * text.length)
            )
            assertThat(text, paragraph.minIntrinsicWidth, equalTo(0.0f))
        }
    }

    @Test
    fun line_break_default_values() {
        val fontSize = 50.sp
        for (text in arrayOf("abcdef", "\u05D0\u05D1\u05D2\u05D3\u05D4\u05D5")) {
            val paragraph = simpleParagraph(text = text, fontSize = fontSize)

            // 3 chars width
            paragraph.layout(ParagraphConstraints(width = 3 * fontSize.px()))

            // 3 chars
            assertThat(text, paragraph.width, equalTo(3 * fontSize.px()))
            // 2 lines, 1 line gap
            assertThat(
                text,
                paragraph.height,
                equalTo(2 * fontSize.px() + fontSize.px() / 5.0f)
            )
            // defined in sample_font
            assertThat(text, paragraph.baseline, equalTo(fontSize.px() * 0.8f))
            assertThat(
                text,
                paragraph.maxIntrinsicWidth,
                equalTo(fontSize.px() * text.length)
            )
            assertThat(text, paragraph.minIntrinsicWidth, equalTo(0.0f))
        }
    }

    @Test
    fun newline_default_values() {
        val fontSize = 50.sp
        for (text in arrayOf("abc\ndef", "\u05D0\u05D1\u05D2\n\u05D3\u05D4\u05D5")) {
            val paragraph = simpleParagraph(text = text, fontSize = fontSize)

            // 3 chars width
            paragraph.layout(ParagraphConstraints(width = 3 * fontSize.px()))

            // 3 chars
            assertThat(text, paragraph.width, equalTo(3 * fontSize.px()))
            // 2 lines, 1 line gap
            assertThat(
                text,
                paragraph.height,
                equalTo(2 * fontSize.px() + fontSize.px() / 5.0f)
            )
            // defined in sample_font
            assertThat(text, paragraph.baseline, equalTo(fontSize.px() * 0.8f))
            assertThat(
                text,
                paragraph.maxIntrinsicWidth,
                equalTo(fontSize.px() * text.indexOf("\n"))
            )
            assertThat(text, paragraph.minIntrinsicWidth, equalTo(0.0f))
        }
    }

    @Test
    fun newline_and_line_break_default_values() {
        val fontSize = 50.sp
        for (text in arrayOf("abc\ndef", "\u05D0\u05D1\u05D2\n\u05D3\u05D4\u05D5")) {
            val paragraph = simpleParagraph(text = text, fontSize = fontSize)

            // 2 chars width
            paragraph.layout(ParagraphConstraints(width = 2 * fontSize.px()))

            // 2 chars
            assertThat(text, paragraph.width, equalTo(2 * fontSize.px()))
            // 4 lines, 3 line gaps
            assertThat(
                text,
                paragraph.height,
                equalTo(4 * fontSize.px() + 3 * fontSize.px() / 5.0f)
            )
            // defined in sample_font
            assertThat(text, paragraph.baseline, equalTo(fontSize.px() * 0.8f))
            assertThat(
                text,
                paragraph.maxIntrinsicWidth,
                equalTo(fontSize.px() * text.indexOf("\n"))
            )
            assertThat(text, paragraph.minIntrinsicWidth, equalTo(0.0f))
        }
    }

    @Test
    fun getPositionForOffset_ltr() {
        val text = "abc"
        val fontSize = 50.sp
        val paragraph = simpleParagraph(text = text, fontSize = fontSize)

        paragraph.layout(ParagraphConstraints(width = text.length * fontSize.px()))
        // test positions that are 1, fontSize+1, 2fontSize+1 which maps to chars 0, 1, 2 ...
        for (i in 0..text.length) {
            val offset = Offset(i * fontSize.px() + 1, fontSize.px() / 2)
            val position = paragraph.getPositionForOffset(offset)
            assertThat(
                "position at index $i, offset $offset does not match",
                position,
                equalTo(i)
            )
        }
    }

    @Test
    fun getPositionForOffset_rtl() {
        val text = "\u05D0\u05D1\u05D2"
        val fontSize = 50.sp
        val paragraph = simpleParagraph(text = text, fontSize = fontSize)

        paragraph.layout(ParagraphConstraints(width = text.length * fontSize.px()))

        // test positions that are 1, fontSize+1, 2fontSize+1 which maps to chars .., 2, 1, 0
        for (i in 0..text.length) {
            val offset = Offset(i * fontSize.px() + 1, fontSize.px() / 2)
            val position = paragraph.getPositionForOffset(offset)
            assertThat(
                "position at index $i, offset $offset does not match",
                position,
                equalTo(text.length - i)
            )
        }
    }

    @Test
    fun getPositionForOffset_ltr_multiline() {
        val firstLine = "abc"
        val secondLine = "def"
        val text = firstLine + secondLine
        val fontSize = 50.sp
        val paragraph = simpleParagraph(text = text, fontSize = fontSize)

        paragraph.layout(ParagraphConstraints(width = firstLine.length * fontSize.px()))

        // test positions are 1, fontSize+1, 2fontSize+1 and always on the second line
        // which maps to chars 3, 4, 5
        for (i in 0..secondLine.length) {
            val offset = Offset(i * fontSize.px() + 1, fontSize.px() * 1.5f)
            val position = paragraph.getPositionForOffset(offset)
            assertThat(
                "position at index $i, offset $offset, second line does not match",
                position,
                equalTo(i + firstLine.length)
            )
        }
    }

    @Test
    fun getPositionForOffset_rtl_multiline() {
        val firstLine = "\u05D0\u05D1\u05D2"
        val secondLine = "\u05D3\u05D4\u05D5"
        val text = firstLine + secondLine
        val fontSize = 50.sp
        val paragraph = simpleParagraph(text = text, fontSize = fontSize)

        paragraph.layout(ParagraphConstraints(width = firstLine.length * fontSize.px()))

        // test positions are 1, fontSize+1, 2fontSize+1 and always on the second line
        // which maps to chars 5, 4, 3
        for (i in 0..secondLine.length) {
            val offset = Offset(i * fontSize.px() + 1, fontSize.px() * 1.5f)
            val position = paragraph.getPositionForOffset(offset)
            assertThat(
                "position at index $i, offset $offset, second line does not match",
                position,
                equalTo(text.length - i)
            )
        }
    }

    @Test
    fun getPositionForOffset_ltr_width_outOfBounds() {
        val text = "abc"
        val fontSize = 50.sp
        val paragraph = simpleParagraph(text = text, fontSize = fontSize)

        paragraph.layout(ParagraphConstraints(width = text.length * fontSize.px()))

        // greater than width
        var offset = Offset(fontSize.px() * text.length * 2, fontSize.px() / 2)
        var position = paragraph.getPositionForOffset(offset)
        assertThat(position, equalTo(text.length))

        // negative
        offset = Offset(-1 * fontSize.px(), fontSize.px() / 2)
        position = paragraph.getPositionForOffset(offset)
        assertThat(position, equalTo(0))
    }

    @Test
    fun getPositionForOffset_ltr_height_outOfBounds() {
        val text = "abc"
        val fontSize = 50.sp
        val paragraph = simpleParagraph(text = text, fontSize = fontSize)

        paragraph.layout(ParagraphConstraints(width = text.length * fontSize.px()))

        // greater than height
        var offset = Offset(fontSize.px() / 2, fontSize.px() * text.length * 2)
        var position = paragraph.getPositionForOffset(offset)
        assertThat(position, equalTo(0))

        // negative
        offset = Offset(fontSize.px() / 2, -1 * fontSize.px())
        position = paragraph.getPositionForOffset(offset)
        assertThat(position, equalTo(0))
    }

    @Test
    fun getBoundingBoxForTextPosition_ltr_singleLine() {
        val text = "abc"
        val fontSize = 50.sp
        val paragraph = simpleParagraph(text = text, fontSize = fontSize)

        paragraph.layout(ParagraphConstraints(width = text.length * fontSize.px()))
        // test positions that are 0, 1, 2 ... which maps to chars 0, 1, 2 ...
        for (i in 0..text.length - 1) {
            val box = paragraph.getBoundingBoxForTextPosition(i)
            assertThat(box.left, equalTo(i * fontSize.px()))
            assertThat(box.right, equalTo((i + 1) * fontSize.px()))
            assertThat(box.top, equalTo(0f))
            assertThat(box.bottom, equalTo(fontSize.px()))
        }
    }

    @Test
    fun getBoundingBoxForTextPosition_ltr_multiLines() {
        val firstLine = "abc"
        val secondLine = "def"
        val text = firstLine + secondLine
        val fontSize = 50.sp
        val paragraph = simpleParagraph(text = text, fontSize = fontSize)

        paragraph.layout(ParagraphConstraints(width = firstLine.length * fontSize.px()))

        // test positions are 3, 4, 5 and always on the second line
        // which maps to chars 3, 4, 5
        for (i in 0..secondLine.length - 1) {
            val textPosition = i + firstLine.length
            val box = paragraph.getBoundingBoxForTextPosition(textPosition)
            assertThat(box.left, equalTo(i * fontSize.px()))
            assertThat(box.right, equalTo((i + 1) * fontSize.px()))
            assertThat(box.top, equalTo(fontSize.px()))
            assertThat(box.bottom, equalTo((2f + 1 / 5f) * fontSize.px()))
        }
    }

    @Test
    fun getBoundingBoxForTextPosition_ltr_textPosition_negative() {
        val text = "abc"
        val fontSize = 50.sp
        val paragraph = simpleParagraph(text = text, fontSize = fontSize)

        paragraph.layout(ParagraphConstraints(width = text.length * fontSize.px()))

        val textPosition = -1
        val box = paragraph.getBoundingBoxForTextPosition(textPosition)
        assertThat(box.left, equalTo(0f))
        assertThat(box.right, equalTo(0f))
        assertThat(box.top, equalTo(0f))
        assertThat(box.bottom, equalTo(fontSize.px()))
    }

    @Test(expected = java.lang.IndexOutOfBoundsException::class)
    fun getBoundingBoxForTextPosition_ltr_textPosition_larger_than_length_throw_exception() {
        val text = "abc"
        val fontSize = 50.sp
        val paragraph = simpleParagraph(text = text, fontSize = fontSize)

        paragraph.layout(ParagraphConstraints(width = text.length * fontSize.px()))

        val textPosition = text.length + 1
        paragraph.getBoundingBoxForTextPosition(textPosition)
    }

    @Test
    fun locale_withCJK_shouldNotDrawSame() {
        val text = "\u82B1"
        val fontSize = 10.sp
        val locales = arrayOf(
            // duplicate ja is on purpose
            Locale(_languageCode = "ja"),
            Locale(_languageCode = "ja"),
            Locale(_languageCode = "zh", _countryCode = "CN"),
            Locale(_languageCode = "zh", _countryCode = "TW")
        )

        val bitmaps = locales.map { locale ->
            val paragraph = Paragraph(
                text = text,
                textStyles = listOf(),
                style = TextStyle(
                    fontSize = fontSize,
                    locale = locale
                ),
                paragraphStyle = ParagraphStyle()
            )

            // just have 10x font size to have a bitmap
            paragraph.layout(ParagraphConstraints(width = fontSize.px() * 10))

            paragraph.bitmap()
        }

        assertThat(bitmaps[0], equalToBitmap(bitmaps[1]))
        assertThat(bitmaps[1], not(equalToBitmap(bitmaps[2])))
        assertThat(bitmaps[1], not(equalToBitmap(bitmaps[3])))
        // this does not work on API 21
        // assertThat(bitmaps[2], not(equalToBitmap(bitmaps[3])))
    }

    @Test
    fun locale_isDefaultLocaleIfNotProvided() {
        val text = "abc"
        val paragraph = simpleParagraph(text = text)

        paragraph.layout(ParagraphConstraints(width = Float.MAX_VALUE))

        assertThat(
            paragraph.paragraphImpl.textLocale.toLanguageTag(),
            equalTo(java.util.Locale.getDefault().toLanguageTag())
        )
    }

    @Test
    fun locale_isSetOnParagraphImpl_enUS() {
        val locale = Locale(_languageCode = "en", _countryCode = "US")
        val text = "abc"
        val paragraph = simpleParagraph(text = text, locale = locale)

        paragraph.layout(ParagraphConstraints(width = Float.MAX_VALUE))

        assertThat(paragraph.paragraphImpl.textLocale.toLanguageTag(), equalTo("en-US"))
    }

    @Test
    fun locale_isSetOnParagraphImpl_jpJP() {
        val locale = Locale(_languageCode = "ja", _countryCode = "JP")
        val text = "abc"
        val paragraph = simpleParagraph(text = text, locale = locale)

        paragraph.layout(ParagraphConstraints(width = Float.MAX_VALUE))

        assertThat(paragraph.paragraphImpl.textLocale.toLanguageTag(), equalTo("ja-JP"))
    }

    @Test
    fun locale_noCountryCode_isSetOnParagraphImpl() {
        val locale = Locale(_languageCode = "ja")
        val text = "abc"
        val paragraph = simpleParagraph(text = text, locale = locale)

        paragraph.layout(ParagraphConstraints(width = Float.MAX_VALUE))

        assertThat(paragraph.paragraphImpl.textLocale.toLanguageTag(), equalTo("ja"))
    }

    @Test
    fun maxLines_withMaxLineEqualsZero() {
        val text = "a\na\na"
        val maxLines = 0
        val paragraph = simpleParagraph(
            text = text,
            fontSize = 100.sp,
            maxLines = maxLines
        )
        paragraph.layout(ParagraphConstraints(width = Float.MAX_VALUE))
        assertThat(paragraph.height, equalTo(0f))
    }

    @Test(expected = java.lang.IllegalArgumentException::class)
    fun maxLines_withMaxLineNegative_throwsException() {
        val text = "a\na\na"
        val maxLines = -1
        val paragraph = simpleParagraph(
            text = text,
            fontSize = 100.sp,
            maxLines = maxLines
        )
        paragraph.layout(ParagraphConstraints(width = Float.MAX_VALUE))
    }

    @Test
    fun maxLines_withMaxLineSmallerThanTextLines_clipHeight() {
        val text = "a\na\na"
        val fontSize = 100.sp
        val lineCount = text.lines().size
        val maxLines = lineCount
        val paragraph = simpleParagraph(
            text = text,
            fontSize = fontSize,
            maxLines = maxLines
        )
        paragraph.layout(ParagraphConstraints(width = Float.MAX_VALUE))
        val expectHeight = (lineCount + (lineCount - 1) * 0.2f) * fontSize.px()
        assertThat(paragraph.height, equalTo(expectHeight))
    }

    @Test
    fun maxLines_withMaxLineEqualsTextLine() {
        val text = "a\na\na"
        val fontSize = 100.sp
        val maxLines = text.lines().size
        val paragraph = simpleParagraph(
            text = text,
            fontSize = fontSize,
            maxLines = maxLines
        )
        paragraph.layout(ParagraphConstraints(width = Float.MAX_VALUE))
        val expectHeight = (maxLines + (maxLines - 1) * 0.2f) * fontSize.px()
        assertThat(paragraph.height, equalTo(expectHeight))
    }

    @Test
    fun maxLines_withMaxLineGreaterThanTextLines() {
        val text = "a\na\na"
        val fontSize = 100.sp
        val lineCount = text.lines().size
        val maxLines = lineCount + 1
        val paragraph = simpleParagraph(
            text = text,
            fontSize = fontSize,
            maxLines = maxLines
        )
        paragraph.layout(ParagraphConstraints(width = 200f))
        val expectHeight = (lineCount + (lineCount - 1) * 0.2f) * fontSize.px()
        assertThat(paragraph.height, equalTo(expectHeight))
    }

    @Test
    fun didExceedMaxLines_withMaxLinesSmallerThanTextLines_returnsTrue() {
        val text = "aaa\naa"
        val maxLines = text.lines().size - 1
        val paragraph = simpleParagraph(text = text, maxLines = maxLines)

        paragraph.layout(ParagraphConstraints(width = Float.MAX_VALUE))
        assertThat(paragraph.didExceedMaxLines, equalTo(true))
    }

    @Test
    fun didExceedMaxLines_withMaxLinesEqualToTextLines_returnsFalse() {
        val text = "aaa\naa"
        val maxLines = text.lines().size
        val paragraph = simpleParagraph(text = text, maxLines = maxLines)

        paragraph.layout(ParagraphConstraints(width = Float.MAX_VALUE))
        assertThat(paragraph.didExceedMaxLines, equalTo(false))
    }

    @Test
    fun didExceedMaxLines_withMaxLinesGreaterThanTextLines_returnsFalse() {
        val text = "aaa\naa"
        val maxLines = text.lines().size + 1
        val paragraph = simpleParagraph(text = text, maxLines = maxLines)

        paragraph.layout(ParagraphConstraints(width = Float.MAX_VALUE))
        assertThat(paragraph.didExceedMaxLines, equalTo(false))
    }

    @Test
    fun didExceedMaxLines_withMaxLinesSmallerThanTextLines_withLineWrap_returnsTrue() {
        val text = "aa"
        val fontSize = 50.sp
        val maxLines = 1
        val paragraph = simpleParagraph(text = text, fontSize = fontSize, maxLines = maxLines)

        // One line can only contain 1 character
        paragraph.layout(ParagraphConstraints(width = fontSize.px()))
        assertThat(paragraph.didExceedMaxLines, equalTo(true))
    }

    @Test
    fun didExceedMaxLines_withMaxLinesEqualToTextLines_withLineWrap_returnsFalse() {
        val text = "a"
        val maxLines = text.lines().size
        val paragraph = simpleParagraph(text = text, fontSize = 50.sp, maxLines = maxLines)

        paragraph.layout(ParagraphConstraints(width = Float.MAX_VALUE))
        assertThat(paragraph.didExceedMaxLines, equalTo(false))
    }

    @Test
    fun didExceedMaxLines_withMaxLinesGreaterThanTextLines_withLineWrap_returnsFalse() {
        val text = "aa"
        val maxLines = 3
        val fontSize = 50.sp
        val paragraph = simpleParagraph(text = text, fontSize = fontSize, maxLines = maxLines)

        // One line can only contain 1 character
        paragraph.layout(ParagraphConstraints(width = fontSize.px()))
        assertThat(paragraph.didExceedMaxLines, equalTo(false))
    }

    @Test
    fun textAlign_defaultValue_alignsStart() {
        val textLTR = "aa"
        val textRTL = "\u05D0\u05D0"
        val fontSize = 20.sp

        val paragraphLTR = simpleParagraph(
            text = textLTR,
            fontSize = fontSize
        )
        val layoutLTRWidth = (textLTR.length + 2) * fontSize.px()
        paragraphLTR.layout(ParagraphConstraints(width = layoutLTRWidth))

        val paragraphRTL = simpleParagraph(
            text = textRTL,
            fontSize = fontSize
        )
        val layoutRTLWidth = (textRTL.length + 2) * fontSize.px()
        paragraphRTL.layout(ParagraphConstraints(width = layoutRTLWidth))

        // When textAlign is TextAlign.start, LTR aligns to left, RTL aligns to right.
        assertThat(paragraphLTR.paragraphImpl.getLineLeft(0), equalTo(0.0f))
        assertThat(paragraphRTL.paragraphImpl.getLineRight(0), equalTo(layoutRTLWidth))
    }

    @Test
    fun textAlign_whenAlignLeft_returnsZeroForGetLineLeft() {
        val texts = listOf("aa", "\u05D0\u05D0")
        val fontSize = 20.sp

        texts.map { text ->
            val paragraph = simpleParagraph(
                text = text,
                textAlign = TextAlign.Left,
                fontSize = fontSize
            )
            val layoutWidth = (text.length + 2) * fontSize.px()
            paragraph.layout(ParagraphConstraints(width = layoutWidth))
            val paragraphImpl = paragraph.paragraphImpl
            assertThat(paragraphImpl.getLineLeft(0), equalTo(0.0f))
        }
    }

    @Test
    fun textAlign_whenAlignRight_returnsLayoutWidthForGetLineRight() {
        val texts = listOf("aa", "\u05D0\u05D0")
        val fontSize = 20.sp

        texts.map { text ->
            val paragraph = simpleParagraph(
                text = text,
                textAlign = TextAlign.Right,
                fontSize = fontSize
            )
            val layoutWidth = (text.length + 2) * fontSize.px()
            paragraph.layout(ParagraphConstraints(width = layoutWidth))
            val paragraphImpl = paragraph.paragraphImpl
            assertThat(paragraphImpl.getLineRight(0), equalTo(layoutWidth))
        }
    }

    @Test
    fun textAlign_whenAlignCenter_textIsCentered() {
        val texts = listOf("aa", "\u05D0\u05D0")
        val fontSize = 20.sp

        texts.map { text ->
            val paragraph = simpleParagraph(
                text = text,
                textAlign = TextAlign.Center,
                fontSize = fontSize
            )
            val layoutWidth = (text.length + 2) * fontSize.px()
            paragraph.layout(ParagraphConstraints(width = layoutWidth))
            val textWidth = text.length * fontSize.px()
            val paragraphImpl = paragraph.paragraphImpl
            assertThat(
                paragraphImpl.getLineLeft(0),
                equalTo(layoutWidth / 2 - textWidth / 2)
            )
            assertThat(
                paragraphImpl.getLineRight(0),
                equalTo(layoutWidth / 2 + textWidth / 2)
            )
        }
    }

    @Test
    fun textAlign_whenAlignStart_withLTR_returnsZeroForGetLineLeft() {
        val text = "aa"
        val fontSize = 20.sp
        val layoutWidth = (text.length + 2) * fontSize.px()

        val paragraph = simpleParagraph(
            text = text,
            textAlign = TextAlign.Start,
            fontSize = fontSize
        )
        paragraph.layout(ParagraphConstraints(width = layoutWidth))
        val paragraphImpl = paragraph.paragraphImpl
        assertThat(paragraphImpl.getLineLeft(0), equalTo(0.0f))
    }

    @Test
    fun textAlign_whenAlignEnd_withLTR_returnsLayoutWidthForGetLineRight() {
        val text = "aa"
        val fontSize = 20.sp
        val layoutWidth = (text.length + 2) * fontSize.px()

        val paragraph = simpleParagraph(
            text = text,
            textAlign = TextAlign.End,
            fontSize = fontSize
        )
        paragraph.layout(ParagraphConstraints(width = layoutWidth))
        val paragraphImpl = paragraph.paragraphImpl
        assertThat(paragraphImpl.getLineRight(0), equalTo(layoutWidth))
    }

    @Test
    fun textAlign_whenAlignStart_withRTL_returnsLayoutWidthForGetLineRight() {
        val text = "\u05D0\u05D0"
        val fontSize = 20.sp
        val layoutWidth = (text.length + 2) * fontSize.px()

        val paragraph = simpleParagraph(
            text = text,
            textAlign = TextAlign.Start,
            fontSize = fontSize
        )
        paragraph.layout(ParagraphConstraints(width = layoutWidth))
        val paragraphImpl = paragraph.paragraphImpl
        assertThat(paragraphImpl.getLineRight(0), equalTo(layoutWidth))
    }

    @Test
    fun textAlign_whenAlignEnd_withRTL_returnsZeroForGetLineLeft() {
        val text = "\u05D0\u05D0"
        val fontSize = 20.sp
        val layoutWidth = (text.length + 2) * fontSize.px()

        val paragraph = simpleParagraph(
            text = text,
            textAlign = TextAlign.End,
            fontSize = fontSize
        )
        paragraph.layout(ParagraphConstraints(width = layoutWidth))
        val paragraphImpl = paragraph.paragraphImpl
        assertThat(paragraphImpl.getLineLeft(0), equalTo(0.0f))
    }

    @Test
    @SdkSuppress(minSdkVersion = 28)
    // We have to test justification above API 28 because of this bug b/68009059, where devices
    // before API 28 may have an extra space at the end of line.
    fun textAlign_whenAlignJustify_justifies() {
        val text = "a a a"
        val fontSize = 20.sp
        val layoutWidth = ("a a".length + 1) * fontSize.px()

        val paragraph = simpleParagraph(
            text = text,
            textAlign = TextAlign.Justify,
            fontSize = fontSize
        )
        paragraph.layout(ParagraphConstraints(width = layoutWidth))
        val paragraphImpl = paragraph.paragraphImpl
        assertThat(paragraphImpl.getLineLeft(0), equalTo(0.0f))
        assertThat(paragraphImpl.getLineRight(0), equalTo(layoutWidth))
        // Last line should align start
        assertThat(paragraphImpl.getLineLeft(1), equalTo(0.0f))
    }

    @Test
    fun textDirection_whenLTR_dotIsOnRight() {
        val text = "a.."
        val fontSize = 20.sp
        val layoutWidth = text.length * fontSize.px()

        val paragraph = simpleParagraph(
            text = text,
            textDirection = TextDirection.Ltr,
            fontSize = fontSize
        )
        paragraph.layout(ParagraphConstraints(width = layoutWidth))
        // The offset of the last character in display order.
        val offset = Offset("a.".length * fontSize.px() + 1, fontSize.px() / 2)
        val charIndex = paragraph.getPositionForOffset(offset = offset)
        assertThat(charIndex, equalTo(2))
    }

    @Test
    fun textDirection_whenRTL_dotIsOnLeft() {
        val text = "a.."
        val fontSize = 20.sp
        val layoutWidth = text.length * fontSize.px()

        val paragraph = simpleParagraph(
            text = text,
            textDirection = TextDirection.Rtl,
            fontSize = fontSize
        )
        paragraph.layout(ParagraphConstraints(width = layoutWidth))
        // The offset of the first character in display order.
        val offset = Offset(fontSize.px() / 2 + 1, fontSize.px() / 2)
        val charIndex = paragraph.getPositionForOffset(offset = offset)
        assertThat(charIndex, equalTo(2))
    }

    @Test
    fun textDirection_whenDefault_withoutStrongChar_directionIsLTR() {
        val text = "..."
        val fontSize = 20.sp
        val layoutWidth = text.length * fontSize.px()

        val paragraph = simpleParagraph(
            text = text,
            fontSize = fontSize
        )
        paragraph.layout(ParagraphConstraints(width = layoutWidth))
        for (i in 0..text.length) {
            // The offset of the i-th character in display order.
            val offset = Offset(i * fontSize.px() + 1, fontSize.px() / 2)
            val charIndex = paragraph.getPositionForOffset(offset = offset)
            assertThat(charIndex, equalTo(i))
        }
    }

    @Test
    fun textDirection_whenDefault_withFirstStrongCharLTR_directionIsLTR() {
        val text = "a\u05D0."
        val fontSize = 20.sp
        val layoutWidth = text.length * fontSize.px()

        val paragraph = simpleParagraph(
            text = text,
            fontSize = fontSize
        )
        paragraph.layout(ParagraphConstraints(width = layoutWidth))
        for (i in 0 until text.length) {
            // The offset of the i-th character in display order.
            val offset = Offset(i * fontSize.px() + 1, fontSize.px() / 2)
            val charIndex = paragraph.getPositionForOffset(offset = offset)
            assertThat(charIndex, equalTo(i))
        }
    }

    @Test
    fun textDirection_whenDefault_withFirstStrongCharRTL_directionIsRTL() {
        val text = "\u05D0a."
        val fontSize = 20.sp
        val layoutWidth = text.length * fontSize.px()

        val paragraph = simpleParagraph(
            text = text,
            fontSize = fontSize
        )
        paragraph.layout(ParagraphConstraints(width = layoutWidth))
        // The first character in display order should be '.'
        val offset = Offset(fontSize.px() / 2 + 1, fontSize.px() / 2)
        val index = paragraph.getPositionForOffset(offset = offset)
        assertThat(index, equalTo(2))
    }

    @Test
    fun lineHeight_returnsSameAsGiven() {
        val text = "abcdefgh"
        val fontSize = 20.sp
        // Make the layout 4 lines
        val layoutWidth = text.length * fontSize.px() / 4
        val lineHeight = 1.5f

        val paragraph = simpleParagraph(
            text = text,
            fontSize = fontSize,
            lineHeight = lineHeight
        )
        paragraph.layout(ParagraphConstraints(width = layoutWidth))
        val paragraphImpl = paragraph.paragraphImpl

        assertThat(paragraphImpl.lineCount, equalTo(4))
        // TODO(Migration/haoyuchang): Due to bug b/120530738, the height of the first line is
        // wrong in the framework. Will fix it when the lineHeight in TextSpan is implemented.
        for (i in 1 until paragraphImpl.lineCount - 1) {
            val actualHeight = paragraphImpl.getLineHeight(i)
            // In the sample_font.ttf, the height of the line should be
            // fontSize + 0.2f * fontSize(line gap)
            assertThat(
                "line number $i",
                actualHeight,
                equalTo(1.2f * fontSize.px() * lineHeight)
            )
        }
    }

    @Test
    fun lineHeight_hasNoEffectOnLastLine() {
        val text = "abc"
        val fontSize = 20.sp
        val layoutWidth = (text.length - 1) * fontSize.px()
        val lineHeight = 1.5f

        val paragraph = simpleParagraph(
            text = text,
            fontSize = fontSize,
            lineHeight = lineHeight
        )
        paragraph.layout(ParagraphConstraints(width = layoutWidth))
        val paragraphImpl = paragraph.paragraphImpl

        val lastLine = paragraphImpl.lineCount - 1
        // In the sample_font.ttf, the height of the line should be
        // fontSize + 0.2 * fontSize(line gap)
        assertThat(paragraphImpl.getLineHeight(lastLine), equalTo(1.2f * fontSize.px()))
    }

    @Test(expected = IllegalArgumentException::class)
    fun lineHeight_whenNegative_throwsIAE() {
        Paragraph(
            text = "",
            textStyles = listOf(),
            style = TextStyle(),
            paragraphStyle = ParagraphStyle(
                lineHeight = -1.0f
            )
        )
    }

    @Test
    fun testAnnotatedString_setFontSizeOnWholeText() {
        val text = "abcde"
        val fontSize = 20.sp
        val textStyle = TextStyle(fontSize = fontSize)
        val paragraphWidth = fontSize.px() * text.length

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length))
        )
        paragraph.layout(ParagraphConstraints(width = paragraphWidth))
        val paragraphImpl = paragraph.paragraphImpl

        // Make sure there is only one line, so that we can use getLineRight to test fontSize.
        assertThat(paragraphImpl.lineCount, equalTo(1))
        // Notice that in this test font, the width of character equals to fontSize.
        assertThat(paragraphImpl.getLineWidth(0), equalTo(fontSize.px() * text.length))
    }

    @Test
    fun testAnnotatedString_setFontSizeOnPartOfText() {
        val text = "abcde"
        val fontSize = 20.sp
        val textStyleFontSize = 30.sp
        val textStyle = TextStyle(fontSize = textStyleFontSize)
        val paragraphWidth = textStyleFontSize.px() * text.length

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, "abc".length)),
            fontSize = fontSize
        )
        paragraph.layout(ParagraphConstraints(width = paragraphWidth))
        val paragraphImpl = paragraph.paragraphImpl

        // Make sure there is only one line, so that we can use getLineRight to test fontSize.
        assertThat(paragraphImpl.lineCount, equalTo(1))
        // Notice that in this test font, the width of character equals to fontSize.
        val expectedLineRight =
            "abc".length * textStyleFontSize.px() + "de".length * fontSize.px()
        assertThat(paragraphImpl.getLineWidth(0), equalTo(expectedLineRight))
    }

    @Test
    fun testAnnotatedString_seFontSizeTwice_lastOneOverwrite() {
        val text = "abcde"
        val fontSize = 20.sp
        val textStyle = TextStyle(fontSize = fontSize)

        val fontSizeOverwrite = 30.sp
        val textStyleOverwrite = TextStyle(fontSize = fontSizeOverwrite)
        val paragraphWidth = fontSizeOverwrite.px() * text.length

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(
                AnnotatedString.Item(textStyle, 0, text.length),
                AnnotatedString.Item(textStyleOverwrite, 0, "abc".length)
            )
        )
        paragraph.layout(ParagraphConstraints(width = paragraphWidth))
        val paragraphImpl = paragraph.paragraphImpl

        // Make sure there is only one line, so that we can use getLineRight to test fontSize.
        assertThat(paragraphImpl.lineCount, equalTo(1))
        // Notice that in this test font, the width of character equals to fontSize.
        val expectedWidth =
            "abc".length * fontSizeOverwrite.px() + "de".length * fontSize.px()
        assertThat(paragraphImpl.getLineWidth(0), equalTo(expectedWidth))
    }

    @Test
    fun testAnnotatedString_fontSizeScale() {
        val text = "abcde"
        val fontSize = 20.sp
        val fontSizeScale = 0.5f
        val textStyle = TextStyle(fontSizeScale = fontSizeScale)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length)),
            fontSize = fontSize
        )
        paragraph.layout(ParagraphConstraints(width = Float.MAX_VALUE))
        val paragraphImpl = paragraph.paragraphImpl

        assertThat(
            paragraphImpl.getLineRight(0),
            equalTo(text.length * fontSize.px() * fontSizeScale)
        )
    }

    @Test
    fun testAnnotatedString_fontSizeScaleNested() {
        val text = "abcde"
        val fontSize = 20.sp
        val fontSizeScale = 0.5f
        val textStyle = TextStyle(fontSizeScale = fontSizeScale)

        val fontSizeScaleNested = 2f
        val textStyleNested = TextStyle(fontSizeScale = fontSizeScaleNested)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(
                AnnotatedString.Item(textStyle, 0, text.length),
                AnnotatedString.Item(textStyleNested, 0, text.length)
            ),
            fontSize = fontSize
        )
        paragraph.layout(ParagraphConstraints(width = Float.MAX_VALUE))
        val paragraphImpl = paragraph.paragraphImpl

        assertThat(
            paragraphImpl.getLineRight(0),
            equalTo(text.length * fontSize.px() * fontSizeScale * fontSizeScaleNested)
        )
    }

    @Test
    fun testAnnotatedString_fontSizeScaleWithFontSizeFirst() {
        val text = "abcde"
        val paragraphFontSize = 20.sp

        val fontSize = 30.sp
        val fontSizeStyle = TextStyle(fontSize = fontSize)

        val fontSizeScale = 0.5f
        val fontSizeScaleStyle = TextStyle(fontSizeScale = fontSizeScale)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(
                AnnotatedString.Item(fontSizeStyle, 0, text.length),
                AnnotatedString.Item(fontSizeScaleStyle, 0, text.length)
            ),
            fontSize = paragraphFontSize
        )
        paragraph.layout(ParagraphConstraints(width = Float.MAX_VALUE))
        val paragraphImpl = paragraph.paragraphImpl

        assertThat(
            paragraphImpl.getLineRight(0),
            equalTo(text.length * fontSize.px() * fontSizeScale)
        )
    }

    @Test
    fun testAnnotatedString_fontSizeScaleWithFontSizeSecond() {
        val text = "abcde"
        val paragraphFontSize = 20.sp

        val fontSize = 30.sp
        val fontSizeStyle = TextStyle(fontSize = fontSize)

        val fontSizeScale = 0.5f
        val fontSizeScaleStyle = TextStyle(fontSizeScale = fontSizeScale)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(
                AnnotatedString.Item(fontSizeScaleStyle, 0, text.length),
                AnnotatedString.Item(fontSizeStyle, 0, text.length)
            ),
            fontSize = paragraphFontSize
        )
        paragraph.layout(ParagraphConstraints(width = Float.MAX_VALUE))
        val paragraphImpl = paragraph.paragraphImpl

        assertThat(
            paragraphImpl.getLineRight(0),
            equalTo(text.length * fontSize.px())
        )
    }

    @Test
    fun testAnnotatedString_fontSizeScaleWithFontSizeNested() {
        val text = "abcde"
        val paragraphFontSize = 20.sp

        val fontSize = 30.sp
        val fontSizeStyle = TextStyle(fontSize = fontSize)

        val fontSizeScale1 = 0.5f
        val fontSizeScaleStyle1 = TextStyle(fontSizeScale = fontSizeScale1)

        val fontSizeScale2 = 2f
        val fontSizeScaleStyle2 = TextStyle(fontSizeScale = fontSizeScale2)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(
                AnnotatedString.Item(fontSizeScaleStyle1, 0, text.length),
                AnnotatedString.Item(fontSizeStyle, 0, text.length),
                AnnotatedString.Item(fontSizeScaleStyle2, 0, text.length)
            ),
            fontSize = paragraphFontSize
        )
        paragraph.layout(ParagraphConstraints(width = Float.MAX_VALUE))
        val paragraphImpl = paragraph.paragraphImpl

        assertThat(
            paragraphImpl.getLineRight(0),
            equalTo(text.length * fontSize.px() * fontSizeScale2)
        )
    }

    @Test
    fun testAnnotatedString_setLetterSpacingOnWholeText() {
        val text = "abcde"
        val fontSize = 20.sp
        val letterSpacing = 5.0f
        val textStyle = TextStyle(letterSpacing = letterSpacing)
        val paragraphWidth = fontSize.px() * (1 + letterSpacing) * text.length

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length)),
            fontSize = fontSize
        )
        paragraph.layout(ParagraphConstraints(width = paragraphWidth))
        val paragraphImpl = paragraph.paragraphImpl

        // Make sure there is only one line, so that we can use getLineRight to test fontSize.
        assertThat(paragraphImpl.lineCount, equalTo(1))
        // Notice that in this test font, the width of character equals to fontSize.
        assertThat(
            paragraphImpl.getLineWidth(0),
            equalTo(fontSize.px() * text.length * (1 + letterSpacing))
        )
    }

    @Test
    fun testAnnotatedString_setLetterSpacingOnPartText() {
        val text = "abcde"
        val fontSize = 20.sp
        val letterSpacing = 5.0f
        val textStyle = TextStyle(letterSpacing = letterSpacing)
        val paragraphWidth = fontSize.px() * (1 + letterSpacing) * text.length

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, "abc".length)),
            fontSize = fontSize
        )
        paragraph.layout(ParagraphConstraints(width = paragraphWidth))
        val paragraphImpl = paragraph.paragraphImpl

        // Make sure there is only one line, so that we can use getLineRight to test fontSize.
        assertThat(paragraphImpl.lineCount, equalTo(1))
        // Notice that in this test font, the width of character equals to fontSize.
        val expectedWidth = ("abc".length * letterSpacing + text.length) * fontSize.px()
        assertThat(paragraphImpl.getLineWidth(0), equalTo(expectedWidth))
    }

    @Test
    fun testAnnotatedString_setLetterSpacingTwice_lastOneOverwrite() {
        val text = "abcde"
        val fontSize = 20.sp

        val letterSpacing = 5.0f
        val textStyle = TextStyle(letterSpacing = letterSpacing)

        val letterSpacingOverwrite = 10.0f
        val textStyleOverwrite = TextStyle(letterSpacing = letterSpacingOverwrite)
        val paragraphWidth = fontSize.px() * (1 + letterSpacingOverwrite) * text.length

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(
                AnnotatedString.Item(textStyle, 0, text.length),
                AnnotatedString.Item(textStyleOverwrite, 0, "abc".length)
            ),
            fontSize = fontSize
        )
        paragraph.layout(ParagraphConstraints(width = paragraphWidth))
        val paragraphImpl = paragraph.paragraphImpl

        // Make sure there is only one line, so that we can use getLineRight to test fontSize.
        assertThat(paragraphImpl.lineCount, equalTo(1))
        // Notice that in this test font, the width of character equals to fontSize.
        val expectedWidth = "abc".length * (1 + letterSpacingOverwrite) * fontSize.px() +
                "de".length * (1 + letterSpacing) * fontSize.px()
        assertThat(paragraphImpl.getLineWidth(0), equalTo(expectedWidth))
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun testAnnotatedString_setWordSpacingOnWholeText() {
        if (!BuildCompat.isAtLeastQ()) return
        val text = "ab cd"
        val fontSize = 20.sp
        val wordSpacing = 5.0f
        val textStyle = TextStyle(wordSpacing = wordSpacing)
        val paragraphWidth = fontSize.px() * (1 + text.length)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, text.length)),
            fontSize = fontSize
        )
        paragraph.layout(ParagraphConstraints(width = paragraphWidth))
        val paragraphImpl = paragraph.paragraphImpl

        // Make sure there is only one line, so that we can use getLineWidth to test fontSize.
        assertThat(paragraphImpl.lineCount, equalTo(1))
        // Notice that in this test font, the width of character equals to fontSize.
        assertThat(
            paragraphImpl.getLineWidth(0),
            equalTo(fontSize.px() * text.length + wordSpacing)
        )
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun testAnnotatedString_setWordSpacingOnPartText() {
        if (!BuildCompat.isAtLeastQ()) return
        val text = "a b c"
        val fontSize = 20.sp
        val wordSpacing = 5.0f
        val textStyle = TextStyle(wordSpacing = wordSpacing)
        val paragraphWidth = fontSize.px() * (1 + text.length)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(AnnotatedString.Item(textStyle, 0, "a b".length)),
            fontSize = fontSize
        )
        paragraph.layout(ParagraphConstraints(width = paragraphWidth))
        val paragraphImpl = paragraph.paragraphImpl

        // Make sure there is only one line, so that we can use getLineWidth to test fontSize.
        assertThat(paragraphImpl.lineCount, equalTo(1))
        // Notice that in this test font, the width of character equals to fontSize.
        assertThat(
            paragraphImpl.getLineWidth(0),
            equalTo(fontSize.px() * text.length + wordSpacing)
        )
    }

    @SdkSuppress(minSdkVersion = 29)
    @Test
    fun testAnnotatedString_setWordSpacingTwice_lastOneOverwrite() {
        if (!BuildCompat.isAtLeastQ()) return
        val text = "a b c"
        val fontSize = 20.sp

        val wordSpacing = 2.0f
        val textStyle = TextStyle(wordSpacing = wordSpacing)

        val wordSpacingOverwrite = 5.0f
        val textStyleOverwrite = TextStyle(wordSpacing = wordSpacingOverwrite)
        val paragraphWidth = fontSize.px() * (1 + text.length)

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(
                AnnotatedString.Item(textStyle, 0, text.length),
                AnnotatedString.Item(textStyleOverwrite, 0, "a b".length)
            ),
            fontSize = fontSize
        )
        paragraph.layout(ParagraphConstraints(width = paragraphWidth))
        val paragraphImpl = paragraph.paragraphImpl

        // Make sure there is only one line, so that we can use getLineWidth to test fontSize.
        assertThat(paragraphImpl.lineCount, equalTo(1))
        // Notice that in this test font, the width of character equals to fontSize.
        assertThat(
            paragraphImpl.getLineWidth(0),
            equalTo(fontSize.px() * text.length + wordSpacing + wordSpacingOverwrite)
        )
    }

    @Test
    fun textIndent_onSingleLine() {
        val text = "abc"
        val fontSize = 20.sp
        val indent = 20.0f

        val paragraph = simpleParagraph(
            text = text,
            textIndent = TextIndent(firstLine = indent.px),
            fontSize = fontSize,
            fontFamily = fontFamilyMeasureFont
        )
        paragraph.layout(ParagraphConstraints(width = Float.MAX_VALUE))
        val paragraphImpl = paragraph.paragraphImpl

        // This offset should point to the first character 'a' if indent is applied.
        // Otherwise this offset will point to the second character 'b'.
        val offset = Offset(indent + 1, fontSize.px() / 2)
        // The position corresponding to the offset should be the first char 'a'.
        assertThat(paragraphImpl.getPositionForOffset(offset), equalTo(0))
    }

    @Test
    fun textIndent_onFirstLine() {
        val text = "abcdef"
        val fontSize = 20.sp
        val indent = 20.0f
        val paragraphWidth = "abcd".length * fontSize.px()

        val paragraph = simpleParagraph(
            text = text,
            textIndent = TextIndent(firstLine = indent.px),
            fontSize = fontSize,
            fontFamily = fontFamilyMeasureFont
        )
        paragraph.layout(ParagraphConstraints(width = paragraphWidth))
        val paragraphImpl = paragraph.paragraphImpl

        assertThat(paragraphImpl.lineCount, equalTo(2))
        // This offset should point to the first character of the first line if indent is applied.
        // Otherwise this offset will point to the second character of the second line.
        val offset = Offset(indent + 1, fontSize.px() / 2)
        // The position corresponding to the offset should be the first char 'a'.
        assertThat(paragraphImpl.getPositionForOffset(offset), equalTo(0))
    }

    @Test
    fun textIndent_onRestLine() {
        val text = "abcde"
        val fontSize = 20.sp
        val indent = 20.0f
        val paragraphWidth = "abc".length * fontSize.px()

        val paragraph = simpleParagraph(
            text = text,
            textIndent = TextIndent(firstLine = 0.px, restLine = indent.px),
            fontSize = fontSize,
            fontFamily = fontFamilyMeasureFont
        )
        paragraph.layout(ParagraphConstraints(width = paragraphWidth))
        val paragraphImpl = paragraph.paragraphImpl
        // This offset should point to the first character of the second line if indent is applied.
        // Otherwise this offset will point to the second character of the second line.
        val offset = Offset(indent + 1, fontSize.px() / 2 + fontSize.px())
        // The position corresponding to the offset should be the 'd' in the second line.
        assertThat(
            paragraphImpl.getPositionForOffset(offset),
            equalTo("abcd".length - 1)
        )
    }

    @Test
    fun testAnnotatedString_fontFamily_changesMeasurement() {
        val text = "ad"
        val fontSize = 20.sp
        // custom 100 regular font has b as the wide glyph
        // custom 200 regular font has d as the wide glyph
        val textStyle = TextStyle(fontFamily = fontFamilyCustom200)
        // a is rendered in paragraphStyle font (custom 100), it will not have wide glyph
        // d is rendered in defaultTextStyle font (custom 200), and it will be wide glyph
        val expectedWidth = fontSize.px() + fontSize.px() * 3

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(
                AnnotatedString.Item(textStyle, "a".length, text.length)
            ),
            fontSize = fontSize,
            fontFamily = fontFamilyCustom100
        )
        paragraph.layout(ParagraphConstraints(width = Float.MAX_VALUE))
        val paragraphImpl = paragraph.paragraphImpl

        assertThat(paragraphImpl.lineCount, equalTo(1))
        assertThat(paragraphImpl.getLineWidth(0), equalTo(expectedWidth))
    }

    @Test
    fun testAnnotatedString_fontFeature_turnOffKern() {
        val text = "AaAa"
        val fontSize = 20.sp
        // This fontFeatureSetting turns off the kerning
        val textStyle = TextStyle(fontFeatureSettings = "\"kern\" 0")

        val paragraph = simpleParagraph(
            text = text,
            textStyles = listOf(
                AnnotatedString.Item(textStyle, 0, "aA".length)
            ),
            fontSize = fontSize,
            fontFamily = fontFamilyKernFont
        )
        paragraph.layout(ParagraphConstraints(width = Float.MAX_VALUE))
        val paragraphImpl = paragraph.paragraphImpl

        // Two characters are kerning, so minus 0.4 * fontSize
        val expectedWidth = text.length * fontSize.px() - 0.4f * fontSize.px()
        assertThat(paragraphImpl.lineCount, equalTo(1))
        assertThat(paragraphImpl.getLineWidth(0), equalTo(expectedWidth))
    }

    @Test
    fun testAnnotatedString_shadow() {
        val text = "abcde"
        val fontSize = 20.sp
        val paragraphWidth = fontSize.px() * text.length

        val textStyle = TextStyle(shadow = Shadow(Color(0xFF00FF00.toInt()), Offset(1f, 2f), 3.px))
        val paragraphShadow = simpleParagraph(
            text = text,
            textStyles = listOf(
                AnnotatedString.Item(textStyle, 0, text.length)
            )
        )
        paragraphShadow.layout(ParagraphConstraints(width = paragraphWidth))

        val paragraph = simpleParagraph(text = text)
        paragraph.layout(ParagraphConstraints(width = paragraphWidth))

        assertThat(paragraphShadow.bitmap(), not(equalToBitmap(paragraph.bitmap())))
    }

    @Test
    fun testDefaultTextStyle_setColor() {
        val text = "abc"
        // FontSize doesn't matter here, but it should be big enough for bitmap comparison.
        val fontSize = 100.sp
        val paragraphWidth = fontSize.px() * text.length
        val textStyle = TextStyle(color = Color.Red)

        val paragraphWithoutColor = simpleParagraph(
            text = text,
            fontSize = fontSize
        )
        paragraphWithoutColor.layout(ParagraphConstraints(paragraphWidth))

        val paragraphWithColor = simpleParagraph(
            text = text,
            textStyle = textStyle,
            fontSize = fontSize
        )
        paragraphWithColor.layout(ParagraphConstraints(paragraphWidth))

        assertThat(
            paragraphWithColor.bitmap(),
            not(equalToBitmap(paragraphWithoutColor.bitmap()))
        )
    }

    @Test
    fun testDefaultTextStyle_setLetterSpacing() {
        val text = "abc"
        // FontSize doesn't matter here, but it should be big enough for bitmap comparison.
        val fontSize = 100.sp
        val letterSpacing = 1f
        val textStyle = TextStyle(letterSpacing = letterSpacing)

        val paragraph = simpleParagraph(
            text = text,
            textStyle = textStyle,
            fontSize = fontSize
        )
        paragraph.layout(ParagraphConstraints(Float.MAX_VALUE))

        assertThat(
            paragraph.getLineRight(0),
            equalTo(fontSize.px() * (1 + letterSpacing) * text.length)
        )
    }

    @Test
    fun testGetPathForRange_singleLine() {
        // Setup test.
        val text = "abc"
        val fontSize = 20.sp
        val paragraph = simpleParagraph(
            text = text,
            fontFamily = fontFamilyMeasureFont,
            fontSize = fontSize
        )
        paragraph.layout(ParagraphConstraints(width = Float.MAX_VALUE))

        val paragraphImpl = paragraph.paragraphImpl
        val expectedPath = Path()
        val lineLeft = paragraphImpl.getLineLeft(0)
        val lineRight = paragraphImpl.getLineRight(0)
        expectedPath.addRect(
            Rect(
                lineLeft,
                0f,
                lineRight - fontSize.px(),
                fontSize.px()
            )
        )

        // Run.
        // Select "ab"
        val actualPath = paragraph.getPathForRange(0, 2)

        // Assert.
        val diff = Path.combine(PathOperation.difference, expectedPath, actualPath).getBounds()
        assertThat(diff, equalTo(Rect.zero))
    }

    @Test
    fun testGetPathForRange_multiLines() {
        // Setup test.
        val text = "abc\nabc"
        val fontSize = 20.sp
        val paragraph = simpleParagraph(
            text = text,
            fontFamily = fontFamilyMeasureFont,
            fontSize = fontSize
        )
        paragraph.layout(ParagraphConstraints(width = Float.MAX_VALUE))

        val paragraphImpl = paragraph.paragraphImpl
        val expectedPath = Path()
        val firstLineLeft = paragraphImpl.getLineLeft(0)
        val secondLineLeft = paragraphImpl.getLineLeft(1)
        val firstLineRight = paragraphImpl.getLineRight(0)
        val secondLineRight = paragraphImpl.getLineRight(1)
        expectedPath.addRect(
            Rect(
                firstLineLeft + fontSize.px(),
                0f,
                firstLineRight,
                fontSize.px()
            )
        )
        expectedPath.addRect(
            Rect(
                secondLineLeft,
                fontSize.px(),
                secondLineRight - fontSize.px(),
                paragraph.height
            )
        )

        // Run.
        // Select "bc\nab"
        val actualPath = paragraph.getPathForRange(1, 6)

        // Assert.
        val diff = Path.combine(PathOperation.difference, expectedPath, actualPath).getBounds()
        assertThat(diff, equalTo(Rect.zero))
    }

    @Test
    fun testGetPathForRange_Bidi() {
        // Setup test.
        val textLTR = "Hello"
        val textRTL = "שלום"
        val text = textLTR + textRTL
        val selectionLTRStart = 2
        val selectionRTLEnd = 2
        val fontSize = 20.sp
        val paragraph = simpleParagraph(
            text = text,
            fontFamily = fontFamilyMeasureFont,
            fontSize = fontSize
        )
        paragraph.layout(ParagraphConstraints(width = Float.MAX_VALUE))

        val paragraphImpl = paragraph.paragraphImpl
        val expectedPath = Path()
        val lineLeft = paragraphImpl.getLineLeft(0)
        val lineRight = paragraphImpl.getLineRight(0)
        expectedPath.addRect(
            Rect(
                lineLeft + selectionLTRStart * fontSize.px(),
                0f,
                lineLeft + textLTR.length * fontSize.px(),
                fontSize.px()
            )
        )
        expectedPath.addRect(
            Rect(
                lineRight - selectionRTLEnd * fontSize.px(),
                0f,
                lineRight,
                fontSize.px()
            )
        )

        // Run.
        // Select "llo..של"
        val actualPath =
            paragraph.getPathForRange(selectionLTRStart, textLTR.length + selectionRTLEnd)

        // Assert.
        val diff = Path.combine(PathOperation.difference, expectedPath, actualPath).getBounds()
        assertThat(diff, equalTo(Rect.zero))
    }

    @Test
    fun testGetPathForRange_Start_Equals_End_Returns_Empty_Path() {
        val text = "abc"
        val paragraph = simpleParagraph(
            text = text,
            fontFamily = fontFamilyMeasureFont,
            fontSize = 20.sp
        )
        paragraph.layout(ParagraphConstraints(width = Float.MAX_VALUE))

        val actualPath = paragraph.getPathForRange(1, 1)

        assertThat(actualPath.getBounds(), equalTo(Rect.zero))
    }

    @Test
    fun testGetPathForRange_Empty_Text() {
        val text = ""
        val paragraph = simpleParagraph(
            text = text,
            fontFamily = fontFamilyMeasureFont,
            fontSize = 20.sp
        )
        paragraph.layout(ParagraphConstraints(width = Float.MAX_VALUE))

        val actualPath = paragraph.getPathForRange(0, 0)

        assertThat(actualPath.getBounds(), equalTo(Rect.zero))
    }

    @Test
    fun testGetPathForRange_Surrogate_Pair_Start_Middle_Second_Character_Selected() {
        // Setup test.
        val text = "\uD834\uDD1E\uD834\uDD1F"
        val fontSize = 20.sp
        val paragraph = simpleParagraph(
            text = text,
            fontFamily = fontFamilyMeasureFont,
            fontSize = fontSize
        )
        paragraph.layout(ParagraphConstraints(width = Float.MAX_VALUE))

        val paragraphImpl = paragraph.paragraphImpl
        val expectedPath = Path()
        val lineRight = paragraphImpl.getLineRight(0)
        expectedPath.addRect(Rect(lineRight / 2, 0f, lineRight, fontSize.px()))

        // Run.
        // Try to select "\uDD1E\uD834\uDD1F", only "\uD834\uDD1F" is selected.
        val actualPath = paragraph.getPathForRange(1, text.length)

        // Assert.
        val diff = Path.combine(PathOperation.difference, expectedPath, actualPath).getBounds()
        assertThat(diff, equalTo(Rect.zero))
    }

    @Test
    fun testGetPathForRange_Surrogate_Pair_End_Middle_Second_Character_Selected() {
        // Setup test.
        val text = "\uD834\uDD1E\uD834\uDD1F"
        val fontSize = 20.sp
        val paragraph = simpleParagraph(
            text = text,
            fontFamily = fontFamilyMeasureFont,
            fontSize = fontSize
        )
        paragraph.layout(ParagraphConstraints(width = Float.MAX_VALUE))

        val paragraphImpl = paragraph.paragraphImpl
        val expectedPath = Path()
        val lineRight = paragraphImpl.getLineRight(0)
        expectedPath.addRect(Rect(lineRight / 2, 0f, lineRight, fontSize.px()))

        // Run.
        // Try to select "\uDD1E\uD834", actually "\uD834\uDD1F" is selected.
        val actualPath = paragraph.getPathForRange(1, text.length - 1)

        // Assert.
        val diff = Path.combine(PathOperation.difference, expectedPath, actualPath).getBounds()
        assertThat(diff, equalTo(Rect.zero))
    }

    @Test
    fun testGetPathForRange_Surrogate_Pair_Start_Middle_End_Same_Character_Returns_Line_Segment() {
        // Setup test.
        val text = "\uD834\uDD1E\uD834\uDD1F"
        val fontSize = 20.sp
        val paragraph = simpleParagraph(
            text = text,
            fontFamily = fontFamilyMeasureFont,
            fontSize = fontSize
        )
        paragraph.layout(ParagraphConstraints(width = Float.MAX_VALUE))

        val paragraphImpl = paragraph.paragraphImpl
        val expectedPath = Path()
        val lineRight = paragraphImpl.getLineRight(0)
        expectedPath.addRect(Rect(lineRight / 2, 0f, lineRight / 2, fontSize.px()))

        // Run.
        // Try to select "\uDD1E", get vertical line segment after this character.
        val actualPath = paragraph.getPathForRange(1, 2)

        // Assert.
        val diff = Path.combine(PathOperation.difference, expectedPath, actualPath).getBounds()
        assertThat(diff, equalTo(Rect.zero))
    }

    @Test
    fun testGetPathForRange_Emoji_Sequence() {
        // Setup test.
        val text = "\u1F600\u1F603\u1F604\u1F606"
        val fontSize = 20.sp
        val paragraph = simpleParagraph(
            text = text,
            fontFamily = fontFamilyMeasureFont,
            fontSize = fontSize
        )
        paragraph.layout(ParagraphConstraints(width = Float.MAX_VALUE))

        val paragraphImpl = paragraph.paragraphImpl
        val expectedPath = Path()
        val lineLeft = paragraphImpl.getLineLeft(0)
        val lineRight = paragraphImpl.getLineRight(0)
        expectedPath.addRect(
            Rect(
                lineLeft + fontSize.px(),
                0f,
                lineRight - fontSize.px(),
                fontSize.px()
            )
        )

        // Run.
        // Select "\u1F603\u1F604"
        val actualPath = paragraph.getPathForRange(1, text.length - 1)

        // Assert.
        val diff = Path.combine(PathOperation.difference, expectedPath, actualPath).getBounds()
        assertThat(diff, equalTo(Rect.zero))
    }

    @Test
    fun testGetPathForRange_Unicode_200D_Return_Line_Segment() {
        // Setup test.
        val text = "\u200D"
        val fontSize = 20.sp
        val paragraph = simpleParagraph(
            text = text,
            fontFamily = fontFamilyMeasureFont,
            fontSize = fontSize
        )
        paragraph.layout(ParagraphConstraints(width = Float.MAX_VALUE))

        val paragraphImpl = paragraph.paragraphImpl
        val expectedPath = Path()
        val lineLeft = paragraphImpl.getLineLeft(0)
        val lineRight = paragraphImpl.getLineRight(0)
        expectedPath.addRect(Rect(lineLeft, 0f, lineRight, fontSize.px()))

        // Run.
        val actualPath = paragraph.getPathForRange(0, 1)

        // Assert.
        assertThat(lineLeft, equalTo(lineRight))
        val diff = Path.combine(PathOperation.difference, expectedPath, actualPath).getBounds()
        assertThat(diff, equalTo(Rect.zero))
    }

    @Test
    fun testGetPathForRange_Unicode_2066_Return_Line_Segment() {
        // Setup tests.
        val text = "\u2066"
        val fontSize = 20f.sp
        val paragraph = simpleParagraph(
            text = text,
            fontFamily = fontFamilyMeasureFont,
            fontSize = fontSize
        )
        paragraph.layout(ParagraphConstraints(width = Float.MAX_VALUE))

        val paragraphImpl = paragraph.paragraphImpl
        val expectedPath = Path()
        val lineLeft = paragraphImpl.getLineLeft(0)
        val lineRight = paragraphImpl.getLineRight(0)
        expectedPath.addRect(Rect(lineLeft, 0f, lineRight, fontSize.px()))

        // Run.
        val actualPath = paragraph.getPathForRange(0, 1)

        // Assert.
        assertThat(lineLeft, equalTo(lineRight))
        val diff = Path.combine(PathOperation.difference, expectedPath, actualPath).getBounds()
        assertThat(diff, equalTo(Rect.zero))
    }

    @Test
    fun testGetWordBoundary() {
        val text = "abc def"
        val paragraph = simpleParagraph(
            text = text,
            fontFamily = fontFamilyMeasureFont,
            fontSize = 20.sp
        )
        paragraph.layout(ParagraphConstraints(width = Float.MAX_VALUE))

        val result = paragraph.getWordBoundary(text.indexOf('a'))

        assertThat(result.start, equalTo(text.indexOf('a')))
        assertThat(result.end, equalTo(text.indexOf(' ')))
    }

    @Test
    fun testGetWordBoundary_Bidi() {
        val text = "abc \u05d0\u05d1\u05d2 def"
        val paragraph = simpleParagraph(
            text = text,
            fontFamily = fontFamilyMeasureFont,
            fontSize = 20.sp
        )
        paragraph.layout(ParagraphConstraints(width = Float.MAX_VALUE))

        val resultEnglish = paragraph.getWordBoundary(text.indexOf('a'))
        val resultHebrew = paragraph.getWordBoundary(text.indexOf('\u05d1'))

        assertThat(resultEnglish.start, equalTo(text.indexOf('a')))
        assertThat(resultEnglish.end, equalTo(text.indexOf(' ')))
        assertThat(resultHebrew.start, equalTo(text.indexOf('\u05d0')))
        assertThat(resultHebrew.end, equalTo(text.indexOf('\u05d2') + 1))
    }

    private fun simpleParagraph(
        text: String = "",
        textIndent: TextIndent? = null,
        textAlign: TextAlign? = null,
        textDirection: TextDirection? = null,
        fontSize: Sp? = null,
        maxLines: Int? = null,
        lineHeight: Float? = null,
        textStyles: List<AnnotatedString.Item<TextStyle>> = listOf(),
        fontFamily: FontFamily = fontFamilyMeasureFont,
        locale: Locale? = null,
        textStyle: TextStyle? = null
    ): Paragraph {
        return Paragraph(
            text = text,
            textStyles = textStyles,
            style = TextStyle(
                fontFamily = fontFamily,
                fontSize = fontSize,
                locale = locale
            ).merge(textStyle),
            paragraphStyle = ParagraphStyle(
                textIndent = textIndent,
                textAlign = textAlign,
                textDirection = textDirection,
                maxLines = maxLines,
                lineHeight = lineHeight
            )
        )
    }
}
