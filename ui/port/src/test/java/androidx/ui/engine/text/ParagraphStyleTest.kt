package androidx.ui.engine.text

import org.hamcrest.CoreMatchers.`is`
import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.CoreMatchers.not
import org.hamcrest.CoreMatchers.nullValue
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith
import org.junit.runners.JUnit4
import java.util.Locale

@RunWith(JUnit4::class)
class ParagraphStyleTest {

    @Test
    fun `toString with null values`() {
        val paragraphStyle = ParagraphStyle()
        assertThat(
            paragraphStyle.toString(), `is`(
                equalTo(
                    "ParagraphStyle(" +
                        "textAlign: unspecified, " +
                        "textDirection: unspecified, " +
                        "fontWeight: unspecified, " +
                        "fontStyle: unspecified, " +
                        "maxLines: unspecified, " +
                        "fontFamily: unspecified, " +
                        "fontSize: unspecified, " +
                        "lineHeight: unspecified, " +
                        "ellipsis: unspecified, " +
                        "locale: unspecified" +
                        ")"
                )
            )
        )
    }

    @Test
    fun `getTextStyle with non-null values`() {
        val textAlign = TextAlign.end
        val textDirection = TextDirection.RTL
        val fontWeight = FontWeight.bold
        val fontStyle = FontStyle.italic
        val maxLines = 2
        val fontFamily = "san-serif"
        val fontSize = 1.0
        val lineHeight = 2.0
        val ellipsis = "dot dot"
        val locale = Locale.ENGLISH

        val paragraphStyle = ParagraphStyle(
            textAlign = textAlign,
            textDirection = textDirection,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            maxLines = maxLines,
            fontFamily = fontFamily,
            fontSize = fontSize,
            lineHeight = lineHeight,
            ellipsis = ellipsis,
            locale = locale
        )

        val textStyle = paragraphStyle.getTextStyle()
        assertThat(textStyle, not(nullValue()))

        assertThat(textStyle.fontWeight, equalTo(paragraphStyle.fontWeight))
        assertThat(textStyle.fontStyle, equalTo(paragraphStyle.fontStyle))
        assertThat(textStyle.fontFamily, equalTo(paragraphStyle.fontFamily))
        assertThat(textStyle.fontSize, equalTo(paragraphStyle.fontSize))
        assertThat(textStyle.locale, equalTo(paragraphStyle.locale))
        assertThat(textStyle.height, equalTo(paragraphStyle.lineHeight))
    }

    @Test
    fun `getTextStyle with null values`() {
        val paragraphStyle = ParagraphStyle(
            textAlign = null,
            textDirection = null,
            fontWeight = null,
            fontStyle = null,
            maxLines = null,
            fontFamily = null,
            fontSize = null,
            lineHeight = null,
            ellipsis = null,
            locale = null
        )

        val textStyle = paragraphStyle.getTextStyle()
        assertThat(textStyle, not(nullValue()))

        assertThat(textStyle.fontWeight, `is`(nullValue()))
        assertThat(textStyle.fontStyle, `is`(nullValue()))
        assertThat(textStyle.fontFamily, `is`(nullValue()))
        assertThat(textStyle.fontSize, `is`(nullValue()))
        assertThat(textStyle.locale, `is`(nullValue()))
        assertThat(textStyle.height, `is`(nullValue()))
    }

    @Test
    fun `toString with values`() {
        val textAlign = TextAlign.end
        val textDirection = TextDirection.RTL
        val fontWeight = FontWeight.bold
        val fontStyle = FontStyle.italic
        val maxLines = 2
        val fontFamily = "san-serif"
        val fontSize = 1.0
        val lineHeight = 2.0
        val ellipsis = "dot dot"
        val locale = Locale.ENGLISH

        val paragraphStyle = ParagraphStyle(
            textAlign = textAlign,
            textDirection = textDirection,
            fontWeight = fontWeight,
            fontStyle = fontStyle,
            maxLines = maxLines,
            fontFamily = fontFamily,
            fontSize = fontSize,
            lineHeight = lineHeight,
            ellipsis = ellipsis,
            locale = locale
        )

        assertThat(
            paragraphStyle.toString(), `is`(
                equalTo(
                    "ParagraphStyle(" +
                        "textAlign: $textAlign, " +
                        "textDirection: $textDirection, " +
                        "fontWeight: $fontWeight, " +
                        "fontStyle: $fontStyle, " +
                        "maxLines: $maxLines, " +
                        "fontFamily: $fontFamily, " +
                        "fontSize: $fontSize, " +
                        "lineHeight: ${lineHeight}x, " +
                        "ellipsis: \"$ellipsis\", " +
                        "locale: $locale" +
                        ")"
                )
            )
        )
    }
}