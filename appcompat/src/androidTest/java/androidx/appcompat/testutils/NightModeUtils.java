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

package androidx.appcompat.testutils;

import static org.junit.Assert.assertEquals;

import android.app.Instrumentation;
import android.content.Context;
import android.content.res.Configuration;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.rule.ActivityTestRule;

public class NightModeUtils {
    private static final String LOG_TAG = "NightModeUtils";

    public static void assertConfigurationNightModeEquals(int expectedNightMode,
            @NonNull Context context) {
        assertConfigurationNightModeEquals(expectedNightMode,
                context.getResources().getConfiguration());
    }

    public static void assertConfigurationNightModeEquals(
            int expectedNightMode, Configuration configuration) {
        assertEquals(expectedNightMode, configuration.uiMode & Configuration.UI_MODE_NIGHT_MASK);
    }

    public static void setLocalNightModeAndWait(
            final ActivityTestRule<? extends AppCompatActivity> activityRule,
            @AppCompatDelegate.NightMode final int nightMode
    ) throws Throwable {
        Log.d(LOG_TAG, "setNightModeAndWait on Activity: " + activity
                + " to mode: " + nightMode
                + " using set mode: " + setMode);

        final Instrumentation instrumentation = InstrumentationRegistry.getInstrumentation();
        activityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                activityRule.getActivity().getDelegate().setLocalNightMode(nightMode);
            }
        });
        instrumentation.waitForIdleSync();
    }
<<<<<<< HEAD   (e53308 Merge "Merge empty history for sparse-5498091-L6460000030224)
=======

    public static <T extends AppCompatActivity> void setNightModeAndWaitForDestroy(
            final ActivityTestRule<T> activityRule,
            @NightMode final int nightMode,
            final NightSetMode setMode
    ) throws Throwable {
        final T activity = activityRule.getActivity();

        Log.d(LOG_TAG, "setNightModeAndWaitForDestroy on Activity: " + activity
                + " to mode: " + nightMode
                + " using set mode: " + setMode);

        // Wait for the Activity to be resumed and visible
        LifecycleOwnerUtils.waitUntilState(activity, activityRule, Lifecycle.State.RESUMED);

        activityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                setNightMode(nightMode, activity, setMode);
            }
        });

        LifecycleOwnerUtils.waitUntilState(activity, activityRule, Lifecycle.State.DESTROYED);
    }

    private static void setNightMode(
            @NightMode final int nightMode,
            final AppCompatActivity activity,
            final NightSetMode setMode) {
        if (setMode == NightSetMode.DEFAULT) {
            AppCompatDelegate.setDefaultNightMode(nightMode);
        } else {
            activity.getDelegate().setLocalNightMode(nightMode);
        }
    }
>>>>>>> BRANCH (3a06c2 Merge "Merge cherrypicks of [954920] into sparse-5520679-L60)
}
