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

package androidx.camera.testing.fakes;

import android.graphics.ImageFormat;
import android.os.Handler;
import android.view.Surface;

import androidx.annotation.Nullable;
import androidx.camera.core.ImageProxy;
import androidx.camera.core.ImageReaderProxy;

import java.util.NoSuchElementException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * A fake implementation of ImageReaderProxy where the values are settable and the
 * OnImageAvailableListener can be triggered.
 */
public class FakeImageReaderProxy implements ImageReaderProxy {
    private int mWidth = 100;
    private int mHeight = 100;
    private int mImageFormat = ImageFormat.JPEG;
    private final int mMaxImages;
    private Surface mSurface;
    private Handler mHandler;

    private BlockingQueue<ImageProxy> mImageProxyQueue;

    @SuppressWarnings("WeakerAccess") /* synthetic accessor */
    ImageReaderProxy.OnImageAvailableListener mListener;

    /**
     * Create a new {@link FakeImageReaderProxy} instance.
     *
     * @param maxImages The maximum number of images that can be acquired at once
     */
    public FakeImageReaderProxy(int maxImages) {
        mMaxImages = maxImages;
        mImageProxyQueue = new LinkedBlockingQueue<>(maxImages);
    }

    @Override
    public ImageProxy acquireLatestImage() {
        ImageProxy imageProxy;

        try {
            do {
                imageProxy = mImageProxyQueue.remove();
            } while (!mImageProxyQueue.isEmpty());
        } catch (NoSuchElementException e) {
            throw new IllegalStateException(
                    "Unable to acquire latest image from empty FakeImageReader");
        }

        return imageProxy;
    }

    @Override
    public ImageProxy acquireNextImage() {
        ImageProxy imageProxy;

        try {
            imageProxy = mImageProxyQueue.remove();
        } catch (NoSuchElementException e) {
            throw new IllegalStateException(
                    "Unable to acquire next image from empty FakeImageReader");
        }

        return imageProxy;
    }

    @Override
    public void close() {

    }

    @Override
    public int getWidth() {
        return mWidth;
    }

    @Override
    public int getHeight() {
        return mHeight;
    }

    @Override
    public int getImageFormat() {
        return mImageFormat;
    }

    @Override
    public int getMaxImages() {
        return mMaxImages;
    }

    @Override
    @Nullable
    public Surface getSurface() {
        return mSurface;
    }

    @Override
    public void setOnImageAvailableListener(
            @Nullable final ImageReaderProxy.OnImageAvailableListener listener,
            @Nullable Handler handler) {
        mListener = listener;
        mHandler = handler;
    }

    public void setSurface(Surface surface) {
        mSurface = surface;
    }

    /**
     * Manually trigger OnImageAvailableListener to notify the Image is ready.
     *
     * <p> Blocks until successfully added an ImageProxy. This can block if the maximum number of
     * ImageProxy have been triggered without a {@link #acquireLatestImage()} or {@link
     * #acquireNextImage()} being called.
     */
    public void triggerImageAvailable(Object tag, long timestamp) throws InterruptedException {
        FakeImageProxy fakeImageProxy = new FakeImageProxy();
        fakeImageProxy.setFormat(mImageFormat);
        fakeImageProxy.setHeight(mHeight);
        fakeImageProxy.setWidth(mWidth);
        fakeImageProxy.setTimestamp(timestamp);

        if (tag != null) {
            FakeImageInfo fakeImageInfo = new FakeImageInfo();
            fakeImageInfo.setTag(tag);
            fakeImageInfo.setTimestamp(timestamp);
            fakeImageProxy.setImageInfo(fakeImageInfo);
        }

        mImageProxyQueue.put(fakeImageProxy);

        if (mListener != null) {
            if (mHandler != null) {
                mHandler.post(
                        new Runnable() {
                            @Override
                            public void run() {
                                mListener.onImageAvailable(FakeImageReaderProxy.this);
                            }
                        });
            } else {
                mListener.onImageAvailable(FakeImageReaderProxy.this);
            }
        }
    }
}
