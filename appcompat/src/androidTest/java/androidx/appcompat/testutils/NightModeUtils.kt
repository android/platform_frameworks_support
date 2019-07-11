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

@file:JvmName("NightModeUtils")

package androidx.appcompat.testutils

import android.app.UiModeManager
import android.content.Context
import android.content.res.Configuration
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.app.AppCompatDelegate
import androidx.appcompat.app.AppCompatDelegate.NightMode
import androidx.lifecycle.Lifecycle
import androidx.test.platform.app.InstrumentationRegistry
import androidx.testutils.runOnUiThreadBlocking
import androidx.testutils.waitForRecreation
import androidx.testutils.waitUntilState
import org.junit.Assert.assertEquals

private val LOG_TAG = "NightModeUtils"

enum class NightSetMode {
    /**
     * Set the night mode using [AppCompatDelegate.setDefaultNightMode]
     */
    DEFAULT,

    /**
     * Set the night mode using [AppCompatDelegate.setLocalNightMode]
     */
    LOCAL
}

fun assertConfigurationNightModeEquals(
    expectedNightMode: Int,
    context: Context
) = assertConfigurationNightModeEquals(expectedNightMode, context.resources.configuration)

fun assertConfigurationNightModeEquals(
    expectedNightMode: Int,
    configuration: Configuration
) = assertEquals(expectedNightMode, configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK)

fun <T : AppCompatActivity> setNightModeAndWait(
    activity: T,
    @NightMode nightMode: Int,
    setMode: NightSetMode
) {
    Log.d(
        LOG_TAG,
        "setNightModeAndWait on Activity: $activity to mode: $nightMode using set mode: $setMode"
    )

    val instrumentation = InstrumentationRegistry.getInstrumentation()
    activity.runOnUiThreadBlocking {
        setNightMode(nightMode, activity, setMode)
    }
    instrumentation.waitForIdleSync()
}

@Throws(Throwable::class)
fun <T : AppCompatActivity> setNightModeAndWaitForRecreate(
    activity: T,
    @NightMode nightMode: Int,
    setMode: NightSetMode
): T {
    Log.d(
        LOG_TAG,
        "setNightModeAndWaitForRecreate on Activity: " + activity + " to mode: " +
                nightMode + " using set mode: " + setMode
    )

    // Wait for the Activity to be resumed and visible
    waitUntilState(activity, Lifecycle.State.RESUMED)

    // Now wait for the Activity to be recreated
    return waitForRecreation(activity) {
        it.runOnUiThreadBlocking {
            setNightMode(nightMode, it, setMode)
        }
    }
}

fun isSystemNightThemeEnabled(context: Context): Boolean {
    val manager = context.getSystemService(Context.UI_MODE_SERVICE) as UiModeManager
    return manager.nightMode == UiModeManager.MODE_NIGHT_YES
}

fun setNightMode(
    @NightMode nightMode: Int,
    activity: AppCompatActivity,
    setMode: NightSetMode
) {
    if (setMode == NightSetMode.DEFAULT) {
        AppCompatDelegate.setDefaultNightMode(nightMode)
    } else {
        activity.delegate.localNightMode = nightMode
    }
}
