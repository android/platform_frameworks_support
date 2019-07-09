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

package androidx.media2.widget;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;

import android.app.Activity;
import android.content.res.AssetFileDescriptor;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.view.PixelCopy;
import android.view.SurfaceView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media2.common.FileMediaItem;
import androidx.media2.common.MediaItem;
import androidx.media2.common.SessionPlayer;
import androidx.media2.session.MediaController;
import androidx.media2.widget.test.R;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;

import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Test {@link VideoView} with a {@link SessionPlayer} or a {@link MediaController}.
 */
@RunWith(Parameterized.class)
@LargeTest
public class VideoView_WithPlayerTest extends MediaWidgetTestBase {
    static final String TAG = "VideoView_WithPlayerTest";
    @Parameterized.Parameters(name = "PlayerType={0}")
    public static List<String> getPlayerTypes() {
        return Arrays.asList(PLAYER_TYPE_MEDIA_CONTROLLER, PLAYER_TYPE_MEDIA_PLAYER);
    }

    private String mPlayerType;
    private Activity mActivity;
    private VideoView mVideoView;
    private MediaItem mMediaItem;

    @Rule
    public ActivityTestRule<VideoViewTestActivity> mActivityRule =
            new ActivityTestRule<>(VideoViewTestActivity.class);

    public VideoView_WithPlayerTest(String playerType) {
        mPlayerType = playerType;
    }

    @Before
    public void setup() throws Throwable {
        mActivity = mActivityRule.getActivity();
        mVideoView = mActivity.findViewById(R.id.videoview);
        mMediaItem = createTestMediaItem();

        setKeepScreenOn(mActivityRule);
        checkAttachedToWindow(mVideoView);
    }

    @After
    public void tearDown() throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                closeAll();
            }
        });
    }

    @Test
    public void testPlayVideo() throws Throwable {
        DefaultPlayerCallback callback = new DefaultPlayerCallback();
        PlayerWrapper playerWrapper = createPlayerWrapper(callback, mMediaItem);
        setPlayerWrapper(playerWrapper);
        assertTrue(callback.mItemLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertTrue(callback.mPausedLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertEquals(1, callback.mPlayingLatch.getCount());
        assertEquals(SessionPlayer.PLAYER_STATE_PAUSED, playerWrapper.getPlayerState());

        playerWrapper.play();
        assertTrue(callback.mPlayingLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        checkVideoRendering();
    }

    @Test
    public void testPlayVideoWithMediaItemFromFileDescriptor() throws Throwable {
        AssetFileDescriptor afd = mContext.getResources()
                .openRawResourceFd(R.raw.testvideo_with_2_subtitle_tracks);
        final MediaItem item = new FileMediaItem.Builder(
                ParcelFileDescriptor.dup(afd.getFileDescriptor()))
                .setFileDescriptorOffset(afd.getStartOffset())
                .setFileDescriptorLength(afd.getLength())
                .build();
        afd.close();

        DefaultPlayerCallback callback = new DefaultPlayerCallback();
        PlayerWrapper playerWrapper = createPlayerWrapper(callback, item);
        setPlayerWrapper(playerWrapper);
        assertTrue(callback.mItemLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertTrue(callback.mPausedLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));

        playerWrapper.play();
        assertTrue(callback.mPlayingLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        checkVideoRendering();
    }

    @Test
    public void testPlayVideoOnTextureView() throws Throwable {
        final VideoView.OnViewTypeChangedListener mockViewTypeListener =
                mock(VideoView.OnViewTypeChangedListener.class);

        DefaultPlayerCallback callback = new DefaultPlayerCallback();
        PlayerWrapper playerWrapper = createPlayerWrapper(callback, mMediaItem);
        setPlayerWrapper(playerWrapper);

        // The default view type is surface view.
        assertEquals(mVideoView.getViewType(), VideoView.VIEW_TYPE_SURFACEVIEW);

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setOnViewTypeChangedListener(mockViewTypeListener);
                mVideoView.setViewType(VideoView.VIEW_TYPE_TEXTUREVIEW);
            }
        });
        verify(mockViewTypeListener, timeout(WAIT_TIME_MS))
                .onViewTypeChanged(mVideoView, VideoView.VIEW_TYPE_TEXTUREVIEW);
        assertTrue(callback.mItemLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        assertTrue(callback.mPausedLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));

        playerWrapper.play();
        assertTrue(callback.mPlayingLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        checkVideoRendering();
    }

    @Test
    public void testSetViewType() throws Throwable {
        final VideoView.OnViewTypeChangedListener mockViewTypeListener =
                mock(VideoView.OnViewTypeChangedListener.class);

        DefaultPlayerCallback callback = new DefaultPlayerCallback();
        PlayerWrapper playerWrapper = createPlayerWrapper(callback, mMediaItem);
        setPlayerWrapper(playerWrapper);

        // The default view type is surface view.
        assertEquals(mVideoView.getViewType(), VideoView.VIEW_TYPE_SURFACEVIEW);

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setOnViewTypeChangedListener(mockViewTypeListener);
                mVideoView.setViewType(VideoView.VIEW_TYPE_TEXTUREVIEW);
                mVideoView.setViewType(VideoView.VIEW_TYPE_SURFACEVIEW);
                mVideoView.setViewType(VideoView.VIEW_TYPE_TEXTUREVIEW);
                mVideoView.setViewType(VideoView.VIEW_TYPE_SURFACEVIEW);
            }
        });

        assertTrue(callback.mItemLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        // WAIT_TIME_MS multiplied by the number of operations.
        assertTrue(callback.mPausedLatch.await(WAIT_TIME_MS * 5, TimeUnit.MILLISECONDS));
        assertEquals(mVideoView.getViewType(), VideoView.VIEW_TYPE_SURFACEVIEW);

        playerWrapper.play();
        assertTrue(callback.mPlayingLatch.await(WAIT_TIME_MS, TimeUnit.MILLISECONDS));
        checkVideoRendering();

        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                mVideoView.setViewType(VideoView.VIEW_TYPE_TEXTUREVIEW);
            }
        });
        verify(mockViewTypeListener, timeout(WAIT_TIME_MS))
                .onViewTypeChanged(mVideoView, VideoView.VIEW_TYPE_TEXTUREVIEW);
        checkVideoRendering();
    }

    // @UiThreadTest will be ignored by Parameterized test runner (b/30746303)
    @Test
    public void testAttachedMediaControlView_setPlayerOrController() throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                PlayerWrapper playerWrapper = createPlayerWrapper(new DefaultPlayerCallback(),
                        mMediaItem);

                MediaControlView defaultMediaControlView = mVideoView.getMediaControlView();
                assertNotNull(defaultMediaControlView);
                try {
                    if (playerWrapper.mPlayer != null) {
                        defaultMediaControlView.setPlayer(playerWrapper.mPlayer);
                    } else if (playerWrapper.mController != null) {
                        defaultMediaControlView.setMediaController(playerWrapper.mController);
                    } else {
                        fail("playerWrapper doesn't have neither mPlayer or mController");
                    }
                    fail("setPlayer or setMediaController should not be allowed "
                            + "for MediaControlView attached to VideoView");
                } catch (IllegalStateException ex) {
                    // expected
                }

                MediaControlView newMediaControlView = new MediaControlView(mContext);
                mVideoView.setMediaControlView(newMediaControlView, -1);
                try {
                    if (playerWrapper.mPlayer != null) {
                        newMediaControlView.setPlayer(playerWrapper.mPlayer);
                    } else if (playerWrapper.mController != null) {
                        newMediaControlView.setMediaController(playerWrapper.mController);
                    } else {
                        fail("playerWrapper doesn't have neither mPlayer or mController");
                    }
                    fail("setPlayer or setMediaController should not be allowed "
                            + "for MediaControlView attached to VideoView");
                } catch (IllegalStateException ex) {
                    // expected
                }
            }
        });
    }

    private void setPlayerWrapper(final PlayerWrapper playerWrapper) throws Throwable {
        mActivityRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (playerWrapper.mPlayer != null) {
                    mVideoView.setPlayer(playerWrapper.mPlayer);
                } else if (playerWrapper.mController != null) {
                    mVideoView.setMediaController(playerWrapper.mController);
                }
            }
        });
    }

    private PlayerWrapper createPlayerWrapper(@NonNull PlayerWrapper.PlayerCallback callback,
            @Nullable MediaItem item) {
        return createPlayerWrapperOfType(callback, item, mPlayerType);
    }

    private void checkVideoRendering() throws InterruptedException {
        if (Build.VERSION.SDK_INT >= 24) {
            final int bufferQueueToleranceMs = 200;
            final int elapsedTimeForSecondScreenshotMs = 400;

            // Tolerance until the video buffers are actually queued.
            Thread.sleep(bufferQueueToleranceMs);
            Bitmap beforeBitmap = getVideoScreenshot();
            Thread.sleep(elapsedTimeForSecondScreenshotMs);
            Bitmap afterBitmap = getVideoScreenshot();
            assertFalse(afterBitmap.sameAs(beforeBitmap));
        }
    }

    private Bitmap getVideoScreenshot() {
        Bitmap bitmap = Bitmap.createBitmap(mVideoView.getWidth(),
                mVideoView.getHeight(), Bitmap.Config.RGB_565);
        if (mVideoView.getViewType() == mVideoView.VIEW_TYPE_SURFACEVIEW) {
            SynchronousPixelCopy pixelCopy = new SynchronousPixelCopy();
            int copyResult = pixelCopy.request(mVideoView.mSurfaceView, bitmap);
            pixelCopy.release();
            assertEquals("PixelCopy failed.", PixelCopy.SUCCESS, copyResult);
        } else {
            bitmap = mVideoView.mTextureView.getBitmap(bitmap);
        }
        return bitmap;
    }

    private static class SynchronousPixelCopy implements PixelCopy.OnPixelCopyFinishedListener {
        private final Handler mHandler;
        private final HandlerThread mHandlerThread;
        private int mStatus = PixelCopy.SUCCESS;

        SynchronousPixelCopy() {
            this.mHandlerThread = new HandlerThread("PixelCopyHelper");
            mHandlerThread.start();
            this.mHandler = new Handler(mHandlerThread.getLooper());
        }

        public void release() {
            if (mHandlerThread.isAlive()) {
                mHandlerThread.quit();
            }
        }

        public int request(SurfaceView source, Bitmap dest) {
            synchronized (this) {
                try {
                    PixelCopy.request(source, dest, this, mHandler);
                    return getResultLocked();
                } catch (Exception e) {
                    Log.e(TAG, "Exception occurred when copying a SurfaceView.", e);
                    return PixelCopy.ERROR_UNKNOWN;
                }
            }
        }

        private int getResultLocked() {
            try {
                this.wait(500);
            } catch (InterruptedException e) {
                /* PixelCopy request didn't complete within 500ms */
            }
            return mStatus;
        }

        @Override
        public void onPixelCopyFinished(int copyResult) {
            synchronized (this) {
                mStatus = copyResult;
                this.notify();
            }
        }

    }
}
