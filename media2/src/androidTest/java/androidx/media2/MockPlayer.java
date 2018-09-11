/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.media2;

import androidx.media.AudioAttributesCompat;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executor;

/**
 * A mock implementation of {@link SessionPlayer2} for testing.
 */
public class MockPlayer extends SessionPlayer2 {
    public final CountDownLatch mCountDownLatch;
    public final boolean mChangePlayerStateWithTransportControl;

    public boolean mPlayCalled;
    public boolean mPauseCalled;
    public boolean mPrepareCalled;
    public boolean mSeekToCalled;
    public boolean mSetPlaybackSpeedCalled;
    public long mSeekPosition;
    public long mCurrentPosition;
    public long mBufferedPosition;
    public float mPlaybackSpeed = 1.0f;
    public @PlayerState int mLastPlayerState;
    public @BuffState int mLastBufferingState;
    public long mDuration;

    public List<MediaItem2> mPlaylist;
    public MediaMetadata2 mMetadata;
    public MediaItem2 mCurrentMediaItem;
    public MediaItem2 mItem;
    public int mIndex = -1;
    public @RepeatMode
    int mRepeatMode = -1;
    public @ShuffleMode
    int mShuffleMode = -1;

    public boolean mSetPlaylistCalled;
    public boolean mUpdatePlaylistMetadataCalled;
    public boolean mAddPlaylistItemCalled;
    public boolean mRemovePlaylistItemCalled;
    public boolean mReplacePlaylistItemCalled;
    public boolean mSkipToPlaylistItemCalled;
    public boolean mSkipToPreviousItemCalled;
    public boolean mSkipToNextItemCalled;
    public boolean mSetRepeatModeCalled;
    public boolean mSetShuffleModeCalled;

    private AudioAttributesCompat mAudioAttributes;

    public MockPlayer(int count) {
        this(count, false);
    }

    public MockPlayer(boolean changePlayerStateWithTransportControl) {
        this(0, changePlayerStateWithTransportControl);
    }

    private MockPlayer(int count, boolean changePlayerStateWithTransportControl) {
        mCountDownLatch = (count > 0) ? new CountDownLatch(count) : null;
        mChangePlayerStateWithTransportControl = changePlayerStateWithTransportControl;
        // This prevents MS2#play() from triggering SessionPlayer2#prepare().
        mLastPlayerState = PLAYER_STATE_PAUSED;

        // Sets default audio attributes to prevent setVolume() from being called with the play().
        mAudioAttributes = new AudioAttributesCompat.Builder()
                .setUsage(AudioAttributesCompat.USAGE_MEDIA).build();
    }

    @Override
    public void close() {
        // no-op
    }

    @Override
    public ListenableFuture<CommandResult2> play() {
        mPlayCalled = true;
        if (mCountDownLatch != null) {
            mCountDownLatch.countDown();
        }
        if (mChangePlayerStateWithTransportControl) {
            notifyPlayerStateChanged(PLAYER_STATE_PLAYING);
        }
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    public ListenableFuture<CommandResult2> pause() {
        mPauseCalled = true;
        if (mCountDownLatch != null) {
            mCountDownLatch.countDown();
        }
        if (mChangePlayerStateWithTransportControl) {
            notifyPlayerStateChanged(PLAYER_STATE_PAUSED);
        }
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    public ListenableFuture<CommandResult2> prepare() {
        mPrepareCalled = true;
        if (mCountDownLatch != null) {
            mCountDownLatch.countDown();
        }
        if (mChangePlayerStateWithTransportControl) {
            notifyPlayerStateChanged(PLAYER_STATE_PAUSED);
        }
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    public ListenableFuture<CommandResult2> seekTo(long pos) {
        mSeekToCalled = true;
        mSeekPosition = pos;
        if (mCountDownLatch != null) {
            mCountDownLatch.countDown();
        }
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    public int getPlayerState() {
        return mLastPlayerState;
    }

    @Override
    public long getCurrentPosition() {
        return mCurrentPosition;
    }

    @Override
    public long getBufferedPosition() {
        return mBufferedPosition;
    }

    @Override
    public float getPlaybackSpeed() {
        return mPlaybackSpeed;
    }

    @Override
    public int getBufferingState() {
        return mLastBufferingState;
    }

    @Override
    public long getDuration() {
        return mDuration;
    }

    public void notifyPlayerStateChanged(final int state) {
        mLastPlayerState = state;

        Map<PlayerCallback, Executor> callbacks = getCallbacks();
        for (Map.Entry<PlayerCallback, Executor> entry : callbacks.entrySet()) {
            final PlayerCallback callback = entry.getKey();
            final Executor executor = entry.getValue();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onPlayerStateChanged(MockPlayer.this, state);
                }
            });
        }
    }

    public void notifyCurrentMediaItemChanged(final MediaItem2 item) {
        Map<PlayerCallback, Executor> callbacks = getCallbacks();
        for (Map.Entry<PlayerCallback, Executor> entry : callbacks.entrySet()) {
            final PlayerCallback callback = entry.getKey();
            final Executor executor = entry.getValue();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onCurrentMediaItemChanged(MockPlayer.this, item);
                }
            });
        }
    }

    public void notifyBufferingStateChanged(final MediaItem2 item,
            final @BuffState int buffState) {
        Map<PlayerCallback, Executor> callbacks = getCallbacks();
        for (Map.Entry<PlayerCallback, Executor> entry : callbacks.entrySet()) {
            final PlayerCallback callback = entry.getKey();
            final Executor executor = entry.getValue();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onBufferingStateChanged(MockPlayer.this, item, buffState);
                }
            });
        }
    }

    public void notifyPlaybackSpeedChanged(final float speed) {
        Map<PlayerCallback, Executor> callbacks = getCallbacks();
        for (Map.Entry<PlayerCallback, Executor> entry : callbacks.entrySet()) {
            final PlayerCallback callback = entry.getKey();
            final Executor executor = entry.getValue();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onPlaybackSpeedChanged(MockPlayer.this, speed);
                }
            });
        }
    }

    public void notifySeekCompleted(final long position) {
        Map<PlayerCallback, Executor> callbacks = getCallbacks();
        for (Map.Entry<PlayerCallback, Executor> entry : callbacks.entrySet()) {
            final PlayerCallback callback = entry.getKey();
            final Executor executor = entry.getValue();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onSeekCompleted(MockPlayer.this, position);
                }
            });
        }
    }

    @Override
    public ListenableFuture<CommandResult2> setAudioAttributes(AudioAttributesCompat attributes) {
        mAudioAttributes = attributes;
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    public AudioAttributesCompat getAudioAttributes() {
        return mAudioAttributes;
    }

    @Override
    public ListenableFuture<CommandResult2> setPlaybackSpeed(float speed) {
        mSetPlaybackSpeedCalled = true;
        mPlaybackSpeed = speed;
        if (mCountDownLatch != null) {
            mCountDownLatch.countDown();
        }
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    /////////////////////////////////////////////////////////////////////////////////
    // Playlist APIs
    /////////////////////////////////////////////////////////////////////////////////

    @Override
    public List<MediaItem2> getPlaylist() {
        return mPlaylist;
    }

    @Override
    public ListenableFuture<CommandResult2> setMediaItem(MediaItem2 item) {
        ArrayList list = new ArrayList<>();
        list.add(item);
        return setPlaylist(list, null);
    }

    @Override
    public ListenableFuture<CommandResult2> setPlaylist(
            List<MediaItem2> list, MediaMetadata2 metadata) {
        mSetPlaylistCalled = true;
        mPlaylist = list;
        mMetadata = metadata;
        mCountDownLatch.countDown();
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    public MediaMetadata2 getPlaylistMetadata() {
        return mMetadata;
    }

    @Override
    public ListenableFuture<CommandResult2> updatePlaylistMetadata(MediaMetadata2 metadata) {
        mUpdatePlaylistMetadataCalled = true;
        mMetadata = metadata;
        mCountDownLatch.countDown();
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    public MediaItem2 getCurrentMediaItem() {
        return mCurrentMediaItem;
    }

    @Override
    public ListenableFuture<CommandResult2> addPlaylistItem(int index, MediaItem2 item) {
        mAddPlaylistItemCalled = true;
        mIndex = index;
        mItem = item;
        mCountDownLatch.countDown();
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    public ListenableFuture<CommandResult2> removePlaylistItem(MediaItem2 item) {
        mRemovePlaylistItemCalled = true;
        mItem = item;
        mCountDownLatch.countDown();
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    public ListenableFuture<CommandResult2> replacePlaylistItem(int index, MediaItem2 item) {
        mReplacePlaylistItemCalled = true;
        mIndex = index;
        mItem = item;
        mCountDownLatch.countDown();
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    public ListenableFuture<CommandResult2> skipToPlaylistItem(MediaItem2 item) {
        mSkipToPlaylistItemCalled = true;
        mItem = item;
        mCountDownLatch.countDown();
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    public ListenableFuture<CommandResult2> skipToPreviousItem() {
        mSkipToPreviousItemCalled = true;
        mCountDownLatch.countDown();
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    public ListenableFuture<CommandResult2> skipToNextItem() {
        mSkipToNextItemCalled = true;
        mCountDownLatch.countDown();
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    public int getRepeatMode() {
        return mRepeatMode;
    }

    @Override
    public ListenableFuture<CommandResult2> setRepeatMode(int repeatMode) {
        mSetRepeatModeCalled = true;
        mRepeatMode = repeatMode;
        mCountDownLatch.countDown();
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    @Override
    public int getShuffleMode() {
        return mShuffleMode;
    }

    @Override
    public ListenableFuture<CommandResult2> setShuffleMode(int shuffleMode) {
        mSetShuffleModeCalled = true;
        mShuffleMode = shuffleMode;
        mCountDownLatch.countDown();
        return new SyncListenableFuture(mCurrentMediaItem);
    }

    public void notifyShuffleModeChanged() {
        final int shuffleMode = mShuffleMode;
        Map<PlayerCallback, Executor> callbacks = getCallbacks();
        for (Map.Entry<PlayerCallback, Executor> entry : callbacks.entrySet()) {
            final PlayerCallback callback = entry.getKey();
            final Executor executor = entry.getValue();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onShuffleModeChanged(MockPlayer.this, shuffleMode);
                }
            });
        }
    }

    public void notifyRepeatModeChanged() {
        final int repeatMode = mRepeatMode;
        Map<PlayerCallback, Executor> callbacks = getCallbacks();
        for (Map.Entry<PlayerCallback, Executor> entry : callbacks.entrySet()) {
            final PlayerCallback callback = entry.getKey();
            final Executor executor = entry.getValue();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onRepeatModeChanged(MockPlayer.this, repeatMode);
                }
            });
        }
    }

    public void notifyPlaylistChanged() {
        final List<MediaItem2> list = mPlaylist;
        final MediaMetadata2 metadata = mMetadata;
        Map<PlayerCallback, Executor> callbacks = getCallbacks();
        for (Map.Entry<PlayerCallback, Executor> entry : callbacks.entrySet()) {
            final PlayerCallback callback = entry.getKey();
            final Executor executor = entry.getValue();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onPlaylistChanged(MockPlayer.this, list, metadata);
                }
            });
        }
    }

    public void notifyPlaylistMetadataChanged() {
        final MediaMetadata2 metadata = mMetadata;
        Map<PlayerCallback, Executor> callbacks = getCallbacks();
        for (Map.Entry<PlayerCallback, Executor> entry : callbacks.entrySet()) {
            final PlayerCallback callback = entry.getKey();
            final Executor executor = entry.getValue();
            executor.execute(new Runnable() {
                @Override
                public void run() {
                    callback.onPlaylistMetadataChanged(MockPlayer.this, metadata);
                }
            });
        }
    }
}