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

import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CaptureRequest;
import android.media.Image;
import android.media.ImageWriter;
import android.os.Build;
import android.util.Log;
import android.view.Surface;

import androidx.camera.core.CameraX;

import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Implementation for bokeh image capture use case.
 *
 * <p>This class should be implemented by OEM and deployed to the target devices. 3P developers
 * don't need to implement this, unless this is used for related testing usage.
 */
public final class BokehImageCaptureExtenderImpl implements ImageCaptureExtenderImpl {
    private static final String TAG = "BokehICExtender";
    private static final int DEFAULT_STAGE_ID = 0;
    private static final String FRONT_BOKEH_CAMERA_ID = "4";
    private static final String BACK_BOKEH_CAMERA_ID = "5";

    public BokehImageCaptureExtenderImpl() {
    }

    @Override
    public void enableExtension(String cameraId, CameraCharacteristics cameraCharacteristics) {
    }

    @Override
    public boolean isExtensionAvailable(String cameraId,
            CameraCharacteristics cameraCharacteristics) {
        // Requires API 23 for ImageWriter
        return android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M;
    }

    @Override
    public List<CaptureStageImpl> getCaptureStages() {
        // Placeholder set of CaptureRequest.Key values
        SettableCaptureStage captureStage = new SettableCaptureStage(DEFAULT_STAGE_ID);
        captureStage.addCaptureRequestParameters(CaptureRequest.CONTROL_EFFECT_MODE,
                CaptureRequest.CONTROL_EFFECT_MODE_SEPIA);
        List<CaptureStageImpl> captureStages = new ArrayList<>();
        captureStages.add(captureStage);
        return captureStages;
    }

    @Override
    public CaptureProcessorImpl getCaptureProcessor() {
        CaptureProcessorImpl captureProcessor =
                new CaptureProcessorImpl() {
                    private ImageWriter mImageWriter;

                    @Override
                    public void onOutputSurface(Surface surface, int imageFormat) {
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                            mImageWriter = ImageWriter.newInstance(surface, 1);
                        }
                    }

                    @Override
                    public void process(Map<Integer, Image> images) {
                        Log.d(TAG, "Started bokeh CaptureProcessor");

                        Image result = images.get(DEFAULT_STAGE_ID);

                        if (result == null) {
                            Log.w(TAG,
                                    "Unable to process since images does not contain all stages.");
                            return;
                        } else {
                            if (android.os.Build.VERSION.SDK_INT
                                    >= android.os.Build.VERSION_CODES.M) {
                                Image image = mImageWriter.dequeueInputImage();

                                // Do processing here
                                ByteBuffer yByteBuffer = image.getPlanes()[0].getBuffer();
                                ByteBuffer uByteBuffer = image.getPlanes()[2].getBuffer();
                                ByteBuffer vByteBuffer = image.getPlanes()[1].getBuffer();

                                // Sample here just simply copy/paste the capture image result
                                yByteBuffer.put(result.getPlanes()[0].getBuffer());
                                uByteBuffer.put(result.getPlanes()[2].getBuffer());
                                vByteBuffer.put(result.getPlanes()[1].getBuffer());

                                mImageWriter.queueInputImage(image);
                            }
                        }

                        Log.d(TAG, "Completed bokeh CaptureProcessor");
                    }
                };
        return captureProcessor;
    }

    @Override
    public String getCustomizedCameraId(CameraX.LensFacing lensFacing) {
        // Do the camera id selection logic, then return the specific camera id needed for this
        // extender.
        if (lensFacing == CameraX.LensFacing.FRONT) {
            return FRONT_BOKEH_CAMERA_ID;
        } else {
            return BACK_BOKEH_CAMERA_ID;
        }
    }
}
