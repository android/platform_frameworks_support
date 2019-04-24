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

package androidx.camera.extensions;

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.hardware.camera2.CaptureResult;
import android.hardware.camera2.TotalCaptureResult;
import android.util.Pair;

import androidx.camera.camera2.Camera2Config;
import androidx.camera.camera2.impl.Camera2CameraCaptureResultConverter;
import androidx.camera.core.CameraCaptureResult;
import androidx.camera.core.CameraCaptureResults;
import androidx.camera.core.CameraX;
import androidx.camera.core.CaptureStage;
import androidx.camera.core.Config;
import androidx.camera.core.ImageInfo;
import androidx.camera.core.ImageInfoProcessor;
import androidx.camera.core.PreviewConfig;
import androidx.camera.extensions.impl.CaptureStageImpl;
import androidx.camera.extensions.impl.PreviewExtenderImpl;

/**
 * Class for using an OEM provided extension on view finder.
 */
public abstract class PreviewExtender {
    private PreviewConfig.Builder mBuilder;
    private PreviewExtenderImpl mImpl;

    void init(PreviewConfig.Builder builder, PreviewExtenderImpl implementation) {
        mBuilder = builder;
        mImpl = implementation;
    }

    /**
     * Indicates whether extension function can support with
     * {@link PreviewConfig.Builder}
     *
     * @return True if the specific extension function is supported for the camera device.
     */
    public boolean isExtensionAvailable() {
        CameraX.LensFacing lensFacing = mBuilder.build().getLensFacing();
        String cameraId = CameraUtil.getCameraId(lensFacing);
        CameraCharacteristics cameraCharacteristics = CameraUtil.getCameraCharacteristics(cameraId);
        return mImpl.isExtensionAvailable(cameraId, cameraCharacteristics);
    }

    public void enableExtension() {
        CameraX.LensFacing lensFacing = mBuilder.build().getLensFacing();
        String cameraId = CameraUtil.getCameraId(lensFacing);
        CameraCharacteristics cameraCharacteristics = CameraUtil.getCameraCharacteristics(cameraId);
        mImpl.enableExtension(cameraId, cameraCharacteristics);

        switch (mImpl.getProcessorType()) {
            case PROCESSOR_TYPE_NONE:
                CaptureStageImpl captureStage = mImpl.getCaptureStage();

                Camera2Config.Builder camera2ConfigurationBuilder =
                        new Camera2Config.Builder();

                for (Pair<CaptureRequest.Key, Object> captureParameter :
                        captureStage.getParameters()) {
                    camera2ConfigurationBuilder.setCaptureRequestOption(captureParameter.first,
                            captureParameter.second);
                }

                final Camera2Config camera2Config = camera2ConfigurationBuilder.build();

                for (Config.Option<?> option : camera2Config.listOptions()) {
                    @SuppressWarnings("unchecked") // Options/values are being copied directly
                            Config.Option<Object> objectOpt = (Config.Option<Object>) option;
                    mBuilder.getMutableConfig().insertOption(objectOpt,
                            camera2Config.retrieveOption(objectOpt));
                }
                break;
            case PROCESSOR_TYPE_REQUEST_UPDATE_ONLY:
                mBuilder.setImageInfoProcessor(new ImageInfoProcessor() {
                    @Override
                    public CaptureStage getCaptureStage() {
                        return new AdaptingCaptureStage(mImpl.getCaptureStage());
                    }

                    @Override
                    public boolean process(ImageInfo imageInfo) {
                        CameraCaptureResult result =
                                CameraCaptureResults.retrieveCameraCaptureResult(imageInfo);
                        if (result == null) {
                            return false;
                        }

                        CaptureResult captureResult =
                                Camera2CameraCaptureResultConverter.getCaptureResult(result);
                        if (captureResult == null) {
                            return false;
                        }

                        TotalCaptureResult totalCaptureResult = (TotalCaptureResult) captureResult;
                        if (totalCaptureResult == null) {
                            return false;
                        }

                        CaptureStageImpl captureStageImpl =
                                mImpl.getRequestUpdatePreviewProcessor().process(
                                        totalCaptureResult);
                        if (captureStageImpl == null) {
                            return false;
                        }

                        return true;
                    }
                });
        }
    }
}
