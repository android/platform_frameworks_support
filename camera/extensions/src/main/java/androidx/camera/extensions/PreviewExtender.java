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
import android.util.Pair;
import android.util.Size;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.camera.camera2.Camera2Config;
import androidx.camera.camera2.impl.CameraEventCallback;
import androidx.camera.camera2.impl.CameraEventCallbacks;
import androidx.camera.core.CameraX;
import androidx.camera.core.CaptureConfig;
import androidx.camera.core.Config;
import androidx.camera.core.PreviewConfig;
import androidx.camera.core.SessionEventListener;
import androidx.camera.extensions.impl.CaptureStageImpl;
import androidx.camera.extensions.impl.PreviewExtenderImpl;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.Executor;

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

        CaptureStageImpl captureStage = mImpl.getCaptureStage();

        Camera2Config.Builder camera2ConfigurationBuilder =
                new Camera2Config.Builder();

        for (Pair<CaptureRequest.Key, Object> captureParameter : captureStage.getParameters()) {
            camera2ConfigurationBuilder.setCaptureRequestOption(captureParameter.first,
                    captureParameter.second);
        }

        Camera2Config camera2Config = camera2ConfigurationBuilder.build();

        for (Config.Option<?> option : camera2Config.listOptions()) {
            @SuppressWarnings("unchecked") // Options/values are being copied directly
                    Config.Option<Object> objectOpt = (Config.Option<Object>) option;
            mBuilder.getMutableConfig().insertOption(objectOpt,
                    camera2Config.retrieveOption(objectOpt));
        }

        PreviewExtenderAdapter previewExtenderAdapter = new PreviewExtenderAdapter(mImpl);
        new Camera2Config.Extender(mBuilder).setCameraEventCallback(
                new CameraEventCallbacks(previewExtenderAdapter));
        mBuilder.setSessionEventListener(previewExtenderAdapter);
    }


    /**
     * An implementation to adapt the OEM provided implementation to core.
     */
    static class PreviewExtenderAdapter extends CameraEventCallback implements
            SessionEventListener {

        private PreviewExtenderImpl mImpl;

        private final Object mLock = new Object();
        @GuardedBy("mLock")
        private volatile int mEnabledSessionCount = 0;
        @GuardedBy("mLock")
        private Executor mExecutor = null;

        PreviewExtenderAdapter(PreviewExtenderImpl impl) {
            mImpl = impl;
        }

        @Override
        public void onInit(String cameraId) {
            CameraCharacteristics cameraCharacteristics =
                    CameraUtil.getCameraCharacteristics(cameraId);
            mImpl.onInit(cameraId, cameraCharacteristics, CameraX.getContext());
        }

        @Override
        public void onDeInit(@NonNull Executor executor) {
            boolean callNow = false;
            synchronized (mLock) {
                mExecutor = executor;
                if (mEnabledSessionCount == 0) {
                    callNow = true;
                }
            }

            if (callNow) {
                callDeInit(executor);
            }
        }

        private void callDeInit(Executor executor) {
            if (executor != null) {
                executor.execute(new Runnable() {
                    @Override
                    public void run() {
                        mImpl.onDeInit();
                    }
                });
            }
        }

        @Override
        public List<CaptureConfig> onPresetSession() {
            CaptureStageImpl captureStageImpl = mImpl.onPresetSession();
            if (captureStageImpl != null) {
                return Arrays.asList(new AdaptingCaptureStage(captureStageImpl).getCaptureConfig());
            }

            return null;
        }

        @Override
        public List<CaptureConfig> onEnableSession() {
            try {
                CaptureStageImpl captureStageImpl = mImpl.onEnableSession();
                if (captureStageImpl != null) {
                    return Arrays.asList(
                            new AdaptingCaptureStage(captureStageImpl).getCaptureConfig());
                }

                return null;
            } finally {
                synchronized (mLock) {
                    mEnabledSessionCount++;
                }
            }
        }

        @Override
        public List<CaptureConfig> onDisableSession() {
            try {
                CaptureStageImpl captureStageImpl = mImpl.onDisableSession();
                if (captureStageImpl != null) {
                    return Arrays.asList(
                            new AdaptingCaptureStage(captureStageImpl).getCaptureConfig());
                }

                return null;
            } finally {
                Executor executor = null;
                synchronized (mLock) {
                    mEnabledSessionCount--;
                    if (mEnabledSessionCount == 0) {
                        executor = mExecutor;
                    }
                }
                callDeInit(executor);
            }
        }

        @Override
        public List<CaptureConfig> onRepeating() {
            CaptureStageImpl captureStageImpl = mImpl.getCaptureStage();
            if (captureStageImpl != null) {
                return Arrays.asList(
                        new AdaptingCaptureStage(captureStageImpl).getCaptureConfig());
            }

            return null;
        }

        @Override
        public void onResolutionUpdate(Size size) {
            mImpl.onResolutionUpdate(size);
        }

        @Override
        public void onImageFormatUpdate(int imageFormat) {
            mImpl.onImageFormatUpdate(imageFormat);
        }

    }

}
