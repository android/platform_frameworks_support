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

package androidx.camera.integration.extensions;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static junit.framework.TestCase.assertNotNull;

import static org.junit.Assume.assumeTrue;

import androidx.camera.integration.extensions.idlingresource.WaitForViewToShow;
import androidx.camera.testing.CameraUtil;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.rule.GrantPermissionRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * Test toggle buttons in extension test app.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public final class ToggleButtonTest {

    @Rule
    public ActivityTestRule<CameraExtensionsActivity> mActivityRule =
            new ActivityTestRule<>(CameraExtensionsActivity.class);

    @Rule
    public GrantPermissionRule mCameraPermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.CAMERA);
    @Rule
    public GrantPermissionRule mStoragePermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
    @Rule
    public GrantPermissionRule mAudioPermissionRule =
            GrantPermissionRule.grant(android.Manifest.permission.RECORD_AUDIO);

    public static void waitFor(IdlingResource idlingResource) {
        IdlingRegistry.getInstance().register(idlingResource);
        Espresso.onIdle();
        IdlingRegistry.getInstance().unregister(idlingResource);
    }

    @Before
    public void setUp() {
        assumeTrue(CameraUtil.deviceHasCamera());
    }

    @After
    public void tearDown() {
        mActivityRule.finishActivity();
    }

    @Test
    public void testSwitchAllExtensionsAndTakePicture() {

        // To switch all extensions.
        for (int i = 0; i < CameraExtensionsActivity.ImageCaptureType.values().length; i++) {

            if (mActivityRule.getActivity().getCurrentImageCaptureType()
                    != CameraExtensionsActivity.ImageCaptureType.IMAGE_CAPTURE_TYPE_NONE) {
                // Wait for the take picture button show.
                waitFor(new WaitForViewToShow(R.id.Picture));

                // Issue take picture.
                onView(withId(R.id.Picture)).perform(click());

                // Wait for the take picture success callback.
                waitFor(mActivityRule.getActivity().mTakePictureIdlingResource);

                assertNotNull(mActivityRule.getActivity().getImageCapture());
                assertNotNull(mActivityRule.getActivity().getPreview());
            }

            // Switch to the next extension effect.
            onView(withId(R.id.PhotoToggle)).perform(click());
        }
    }

}

