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

package androidx.camera.core;

import static com.google.common.truth.Truth.assertThat;

import androidx.camera.testing.fakes.FakeCameraIdFilter;
import androidx.test.filters.SmallTest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.internal.DoNotInstrument;

@SmallTest
@RunWith(RobolectricTestRunner.class)
@DoNotInstrument
public class CameraIdFilterSetTest {
    private CameraIdFilterSet mFilterSet = new CameraIdFilterSet();

    @Test
    public void canAddCameraIdFilter() {
        CameraIdFilter filter = new FakeCameraIdFilter();
        mFilterSet.addCameraIdFilter(filter);
        assertThat(mFilterSet.getCameraIdFilters()).containsExactly(filter);
    }

    @Test
    public void canRemoveCameraIdFilter() {
        CameraIdFilter filter1 = new FakeCameraIdFilter();
        CameraIdFilter filter2 = new FakeCameraIdFilter();
        mFilterSet.addCameraIdFilter(filter1);
        mFilterSet.addCameraIdFilter(filter2);
        mFilterSet.removeCameraIdFilter(filter1);
        assertThat(mFilterSet.getCameraIdFilters()).doesNotContain(filter1);
        assertThat(mFilterSet.getCameraIdFilters()).contains(filter2);
    }

    @Test
    public void canReplaceCameraIdFilter() {
        CameraIdFilter backFilter = new LensFacingCameraIdFilter(CameraX.LensFacing.BACK);
        CameraIdFilter otherFilter = new FakeCameraIdFilter();
        CameraIdFilter frontFilter = new LensFacingCameraIdFilter(CameraX.LensFacing.FRONT);

        mFilterSet.addCameraIdFilter(backFilter);
        mFilterSet.addCameraIdFilter(otherFilter);
        mFilterSet.replaceCameraIdFilter(frontFilter);
        assertThat(mFilterSet.getCameraIdFilters()).contains(otherFilter);
        assertThat(mFilterSet.getCameraIdFilters()).contains(frontFilter);
        assertThat(mFilterSet.getCameraIdFilters()).doesNotContain(backFilter);
    }
}
