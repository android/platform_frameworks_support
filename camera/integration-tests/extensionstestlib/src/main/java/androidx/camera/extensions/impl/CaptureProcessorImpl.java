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

package androidx.camera.extensions.impl;

import android.media.Image;
import android.view.Surface;

import java.util.List;
import java.util.Map;

/**
 * The interface for processing a set of {@link Image}s that have captured.
 */
public interface CaptureProcessorImpl {
    /**
     * This gets called to update where the CaptureProcessor should write the output of {@link
     * #process(Map)}.
     *
     * @param surface     The {@link Surface} that the CaptureProcessor should write data into.
     * @param imageFormat The format of that the surface expects.
     */
    void onOutputSurface(Surface surface, int imageFormat);

    /**
     * Process a set images captured that were requested.
     *
     * <p> The result of the processing step should be written to the {@link Surface} that was
     * received by {@link #onOutputSurface(Surface, int)}.
     *
     * @param images The map of images to process. The {@link Image} that are contained within the
     *               map will become invalid after this method completes, so no references to them
     *               should be kept.
     */
    void process(Map<Integer, Image> images);

    /** The set of captures that are needed to create an image with the effect. */
    List<CaptureStageImpl> getCaptureStages();

    /**
     * To get a maximum capture stage count to capture.
     * @return the maximum count.
     */
    int getMaxCaptureStage();
}
