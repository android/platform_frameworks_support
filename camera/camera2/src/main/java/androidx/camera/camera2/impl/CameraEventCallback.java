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

package androidx.camera.camera2.impl;

import androidx.annotation.RestrictTo;
import androidx.camera.core.CaptureConfig;

import java.util.ArrayList;
import java.util.List;

/**
 * A callback object for tracking the CameraX session event and get request data.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class CameraEventCallback {

    /**
     * This would be invoked before create capture session of Camera2. The returned parameter in
     * CaptureConfig will be passed to the camera device as part of the capture session
     * initialization via setSessionParameters(). The valid parameter is a subset of the
     * available capture request parameters.
     *
     * @return List<CaptureConfig> The request information to customize the session.
     */
    public List<CaptureConfig> onPresetSession() {
        return new ArrayList<>();
    }

    /**
     * This would be invoked once after a Camera2 capture session was created. The returned
     * parameter in CaptureConfig will be used to generate a single request to the current
     * configured camera device. The generated request would be submitted to camera before process
     * other single request.
     *
     * @return List<CaptureConfig> The request information to customize the session.
     */
    public List<CaptureConfig> onEnableSession() {
        return new ArrayList<>();
    }

    /**
     * This callback would be invoked before start the repeating request in Camera2 capture
     * session. The returned CaptureConfig will be used to generate a capture request, and would
     * be used in setRepeatingRequest().
     *
     * @return List<CaptureConfig> The request information to customize the session.
     */
    public List<CaptureConfig> onRepeating() {
        return new ArrayList<>();
    }

    /**
     * This would be invoked once before the Camera2 capture session was going to close. The
     * returned parameter in CaptureConfig will be used to generate a single request to the current
     * configured camera device. The generated request would be submitted to camera before the
     * capture session was closed.
     *
     * @return List<CaptureConfig> The request information to customize the session.
     */
    public List<CaptureConfig> onDisableSession() {
        return new ArrayList<>();
    }

}
