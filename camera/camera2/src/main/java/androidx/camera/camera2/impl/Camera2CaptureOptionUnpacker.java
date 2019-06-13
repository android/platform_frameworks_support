<<<<<<< HEAD   (9d364e Merge "Merge empty history for sparse-5611434-L1110000032658)
=======
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

import android.hardware.camera2.CaptureRequest;

import androidx.camera.camera2.Camera2Config;
import androidx.camera.core.CaptureConfig;
import androidx.camera.core.Config;
import androidx.camera.core.Config.Option;
import androidx.camera.core.OptionsBundle;
import androidx.camera.core.UseCaseConfig;

/**
 * A {@link CaptureConfig.OptionUnpacker} implementation for unpacking Camera2 options into
 * {@link CaptureConfig.Builder}.
 */
class Camera2CaptureOptionUnpacker implements CaptureConfig.OptionUnpacker {

    static final Camera2CaptureOptionUnpacker INSTANCE = new Camera2CaptureOptionUnpacker();

    @Override
    public void unpack(UseCaseConfig<?> config, final CaptureConfig.Builder builder) {
        CaptureConfig defaultCaptureConfig =
                config.getDefaultCaptureConfig(/*valueIfMissing=*/ null);

        Config implOptions = OptionsBundle.emptyBundle();
        int templateType = CaptureConfig.defaultEmptyCaptureConfig().getTemplateType();

        // Apply/extract defaults from session config
        if (defaultCaptureConfig != null) {
            templateType = defaultCaptureConfig.getTemplateType();
            builder.addAllCameraCaptureCallbacks(defaultCaptureConfig.getCameraCaptureCallbacks());
            implOptions = defaultCaptureConfig.getImplementationOptions();
        }

        // Set the any additional implementation options
        builder.setImplementationOptions(implOptions);

        // Get Camera2 extended options
        final Camera2Config camera2Config = new Camera2Config(config);

        // Apply template type
        builder.setTemplateType(camera2Config.getCaptureRequestTemplate(templateType));

        // Add extension callbacks
        builder.addCameraCaptureCallback(
                CaptureCallbackContainer.create(
                        camera2Config.getSessionCaptureCallback(
                                Camera2CaptureCallbacks.createNoOpCallback())));

        // Copy extension keys
        Camera2Config.Builder configBuilder = new Camera2Config.Builder();
        for (Option<?> option : camera2Config.getCaptureRequestOptions()) {
            @SuppressWarnings("unchecked")
            // No way to get actual type info here, so treat as Object
                    Option<Object> typeErasedOption = (Option<Object>) option;
            @SuppressWarnings("unchecked")
            CaptureRequest.Key<Object> key = (CaptureRequest.Key<Object>) option.getToken();
            configBuilder.setCaptureRequestOption(key,
                    camera2Config.retrieveOption(typeErasedOption));
        }
        builder.addImplementationOptions(configBuilder.build());
    }
}
>>>>>>> BRANCH (8875d3 Merge "Merge cherrypicks of [982717, 982718] into sparse-564)
