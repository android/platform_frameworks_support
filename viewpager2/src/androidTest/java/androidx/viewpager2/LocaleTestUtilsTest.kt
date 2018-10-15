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

package androidx.viewpager2

import android.content.res.Configuration
import android.os.Build
import androidx.core.os.ConfigurationCompat
import androidx.core.view.ViewCompat.LAYOUT_DIRECTION_LTR
import androidx.core.view.ViewCompat.LAYOUT_DIRECTION_RTL
import androidx.test.InstrumentationRegistry
import androidx.test.filters.LargeTest
import androidx.test.runner.AndroidJUnit4
import org.hamcrest.CoreMatchers.equalTo
import org.junit.Assert.assertThat
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
@LargeTest
class LocaleTestUtilsTest {
    private val configuration: Configuration get() =
        InstrumentationRegistry.getTargetContext().resources.configuration
    private val localeUtil: LocaleTestUtils by lazy {
        LocaleTestUtils(InstrumentationRegistry.getTargetContext())
    }

    @Test
    fun test_set_reset_locale() {
        assertDefaultValues()
        localeUtil.setLocale(LocaleTestUtils.LTR_LANGUAGE)
        assertLocaleIs(LocaleTestUtils.LTR_LANGUAGE, false)
        localeUtil.resetLocale()
        assertDefaultValues()
        localeUtil.setLocale(LocaleTestUtils.RTL_LANGUAGE)
        assertLocaleIs(LocaleTestUtils.RTL_LANGUAGE, true)
        localeUtil.resetLocale()
        assertDefaultValues()
    }

    private fun assertDefaultValues() {
        assertLocaleIs("en_US", false)
    }

    private fun assertLocaleIs(lang: String, expectRtl: Boolean) {
        assertThat(
            "Locale should be $lang",
            ConfigurationCompat.getLocales(configuration).get(0).toString(),
            equalTo(lang)
        )
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN_MR1) {
            assertThat(
                "Layout direction should be ${if (expectRtl) "RTL" else "LTR"}",
                configuration.layoutDirection,
                equalTo(if (expectRtl) LAYOUT_DIRECTION_RTL else LAYOUT_DIRECTION_LTR)
            )
        }
    }
}