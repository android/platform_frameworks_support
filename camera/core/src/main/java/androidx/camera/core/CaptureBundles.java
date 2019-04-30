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

import androidx.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Different implementations of {@link CaptureBundle}.
 */
final class CaptureBundles {
    /** Creates a {@link CaptureBundle} which contain a single default {@link CaptureStage}. */
    static CaptureBundle singleDefaultCaptureBundle() {
        return createCaptureBundle(new CaptureStage.DefaultCaptureStage());
    }

    /** Returns a {@link CaptureBundle} which contains a list of {@link CaptureStage}. */
    static CaptureBundle createCaptureBundle(CaptureStage ... captureStages) {
        return new CaptureBundleImpl(Arrays.asList(captureStages));
    }

    /** Returns a {@link CaptureBundle} which contains a list of {@link CaptureStage}. */
    static CaptureBundle createCaptureBundle(List<CaptureStage> captureStageList) {
        return new CaptureBundleImpl(captureStageList);
    }

    /** Creates a new {@link CaptureBundle} instance. */
    static CaptureBundle createFrom(CaptureBundle captureBundle) {
        return createCaptureBundle(captureBundle.getCaptureStages());
    }

    /**
     * An ordered collection of {@link CaptureStage}.
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    static final class CaptureBundleImpl implements CaptureBundle {

        final List<CaptureStage> mCaptureStageList;

        CaptureBundleImpl(List<CaptureStage> captureStageList) {
            if (captureStageList != null && !captureStageList.isEmpty()) {
                mCaptureStageList = Collections.unmodifiableList(new ArrayList<>(captureStageList));
            } else {
                throw new IllegalArgumentException("Cannot set an empty CaptureStage list.");
            }
        }

        @Override
        public List<CaptureStage> getCaptureStages() {
            return mCaptureStageList;
        }
    }

    private CaptureBundles() {}
}
