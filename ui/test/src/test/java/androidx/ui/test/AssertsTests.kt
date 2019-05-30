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

import androidx.ui.core.semantics.SemanticsConfiguration
import androidx.ui.semantics.hidden
import androidx.ui.semantics.testTag
import androidx.ui.semantics.value
import androidx.ui.test.helpers.FakeSemanticsTreeInteraction
import org.junit.Test

// TODO(i18n): Hardcoded strings
class AssertsTests {

    @Test
    fun assertIsVisible_forVisibleElement_isOk() {
        semanticsTreeInteractionFactory = {
            FakeSemanticsTreeInteraction()
                .withProperties(SemanticsConfiguration().also {
                    it.testTag = "test"
                    it.hidden = false
                })
        }

        findByTag("test")
            .assertIsVisible()
    }

    @Test(expected = AssertionError::class)
    fun assertIsVisible_forNotVisibleElement_throwsError() {
        semanticsTreeInteractionFactory = {
            FakeSemanticsTreeInteraction()
                .withProperties(SemanticsConfiguration().also {
                    it.testTag = "test"
                    it.hidden = true
                })
        }

        findByTag("test")
            .assertIsVisible()
    }

    @Test
    fun assertIsHidden_forHiddenElement_isOk() {
        semanticsTreeInteractionFactory = {
            FakeSemanticsTreeInteraction()
                .withProperties(SemanticsConfiguration().also {
                    it.testTag = "test"
                    it.hidden = true
                })
        }

        findByTag("test")
            .assertIsHidden()
    }

    @Test(expected = AssertionError::class)
    fun assertIsHidden_forNotHiddenElement_throwsError() {
        semanticsTreeInteractionFactory = {
            FakeSemanticsTreeInteraction()
                .withProperties(SemanticsConfiguration().also {
                    it.testTag = "test"
                    it.hidden = false
                })
        }

        findByTag("test")
            .assertIsHidden()
    }

    @Test
    fun assertIsChecked_forCheckedElement_isOk() {
        semanticsTreeInteractionFactory = {
            FakeSemanticsTreeInteraction()
                .withProperties(SemanticsConfiguration().also {
                    it.testTag = "test"
                    it.value = "Checked"
                })
        }

        findByTag("test")
            .assertIsChecked()
    }

    @Test(expected = AssertionError::class)
    fun assertIsChecked_forNotCheckedElement_throwsError() {
        semanticsTreeInteractionFactory = {
            FakeSemanticsTreeInteraction()
                .withProperties(SemanticsConfiguration().also {
                    it.testTag = "test"
                    it.value = "Unchecked"
                })
        }

        findByTag("test")
            .assertIsChecked()
    }

    @Test(expected = AssertionError::class)
    fun assertIsSelected_forNotSelectedElement_throwsError() {
        semanticsTreeInteractionFactory = {
            FakeSemanticsTreeInteraction()
                .withProperties(SemanticsConfiguration().also {
                    it.testTag = "test"
                    it.value = "Not selected"
                })
        }

        findByTag("test")
            .assertIsSelected(true)
    }

    @Test
    fun assertIsSelected_forSelectedElement_isOk() {
        semanticsTreeInteractionFactory = {
            FakeSemanticsTreeInteraction()
                .withProperties(SemanticsConfiguration().also {
                    it.testTag = "test"
                    it.value = "Selected"
                })
        }

        findByTag("test")
            .assertIsSelected(true)
    }

    @Test(expected = AssertionError::class)
    fun assertIsNotSelected_forSelectedElement_throwsError() {
        semanticsTreeInteractionFactory = {
            FakeSemanticsTreeInteraction()
                .withProperties(SemanticsConfiguration().also {
                    it.testTag = "test"
                    it.value = "Selected"
                })
        }

        findByTag("test")
            .assertIsSelected(false)
    }

    @Test
    fun assertIsNotSelected_forNotSelectedElement_isOk() {
        semanticsTreeInteractionFactory = {
            FakeSemanticsTreeInteraction()
                .withProperties(SemanticsConfiguration().also {
                    it.testTag = "test"
                    it.value = "Not selected"
                })
        }

        findByTag("test")
            .assertIsSelected(false)
    }
}