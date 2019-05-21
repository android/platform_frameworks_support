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

import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.media2.common.MediaItem;
import androidx.media2.common.MediaMetadata;
import androidx.media2.common.SessionPlayer;
import androidx.media2.common.SessionPlayer.TrackInfo;
import androidx.media2.common.SubtitleData;
import androidx.media2.common.VideoSize;
import androidx.media2.session.MediaController;
import androidx.media2.session.SessionCommand;
import androidx.media2.session.SessionCommandGroup;
import androidx.media2.session.SessionResult;

import java.util.List;
import java.util.concurrent.Executor;

/**
 * Wrapper for MediaController and SessionPlayer
 */
class WidgetPlayer {
    private final MediaController mController;
    private final Executor mCallbackExecutor;
    private final MediaController.ControllerCallback mControllerCallback;
    private boolean mCallbackAttached;

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mSavedPlayerState = SessionPlayer.PLAYER_STATE_IDLE;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    SessionCommandGroup mAllowedCommands;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    MediaMetadata mMediaMetadata;

    WidgetPlayer(@NonNull MediaController controller, @NonNull Executor executor,
            @NonNull WidgetCallback callback) {
        mController = controller;
        mCallbackExecutor = executor;
        mControllerCallback = new MediaControllerCallback(callback);
    }

    void attachCallback() {
        if (mCallbackAttached) return;
        if (mController != null) {
            mController.registerExtraCallback(mCallbackExecutor, mControllerCallback);

            mSavedPlayerState = mController.getPlayerState();
            mAllowedCommands = mController.getAllowedCommands();
            MediaItem item = mController.getCurrentMediaItem();
            mMediaMetadata = item == null ? null : item.getMetadata();
        }
        mCallbackAttached = true;
    }

    void detachCallback() {
        if (!mCallbackAttached) return;
        if (mController != null) {
            mController.unregisterExtraCallback(mControllerCallback);
        }
        mCallbackAttached = false;
    }

    boolean isPlaying() {
        if (mController != null) {
            return mController.getPlayerState() == SessionPlayer.PLAYER_STATE_PLAYING;
        }
        return false;
    }

    long getCurrentPosition() {
        if (mController != null) {
            long currentPosition = mController.getCurrentPosition();
            return (currentPosition < 0) ? 0 : currentPosition;
        }
        return 0;
    }

    long getBufferPercentage() {
        long duration = getDurationMs();
        if (mController != null && duration != 0) {
            long bufferedPos = mController.getBufferedPosition();
            return (bufferedPos < 0) ? -1 : (bufferedPos * 100 / duration);
        }
        return 0;
    }

    int getPlaybackState() {
        if (mController != null) {
            return mController.getPlayerState();
        }
        return SessionPlayer.PLAYER_STATE_IDLE;
    }

    boolean canPause() {
        return mAllowedCommands != null && mAllowedCommands.hasCommand(
                SessionCommand.COMMAND_CODE_PLAYER_PAUSE);
    }

    boolean canSeekBackward() {
        return mAllowedCommands != null && mAllowedCommands.hasCommand(
                SessionCommand.COMMAND_CODE_SESSION_REWIND);
    }

    boolean canSeekForward() {
        return mAllowedCommands != null && mAllowedCommands.hasCommand(
                SessionCommand.COMMAND_CODE_SESSION_FAST_FORWARD);
    }

    boolean canSkipToNext() {
        return mAllowedCommands != null && mAllowedCommands.hasCommand(
                SessionCommand.COMMAND_CODE_PLAYER_SKIP_TO_NEXT_PLAYLIST_ITEM);
    }

    boolean canSkipToPrevious() {
        return mAllowedCommands != null && mAllowedCommands.hasCommand(
                SessionCommand.COMMAND_CODE_PLAYER_SKIP_TO_PREVIOUS_PLAYLIST_ITEM);
    }

    boolean canSeekTo() {
        return mAllowedCommands != null && mAllowedCommands.hasCommand(
                SessionCommand.COMMAND_CODE_PLAYER_SEEK_TO);
    }

    boolean canShowHideSubtitle() {
        return mAllowedCommands != null
                && mAllowedCommands.hasCommand(
                        new SessionCommand(MediaControlView.COMMAND_SHOW_SUBTITLE, null))
                && mAllowedCommands.hasCommand(
                        new SessionCommand(MediaControlView.COMMAND_HIDE_SUBTITLE, null));
    }

    void pause() {
        if (mController != null) {
            mController.pause();
        }
    }

    void play() {
        if (mController != null) {
            mController.play();
        }
    }

    void seekTo(long posMs) {
        if (mController != null) {
            mController.seekTo(posMs);
        }
    }

    void skipToNextItem() {
        if (mController != null) {
            mController.skipToNextPlaylistItem();
        }
    }

    void skipToPreviousItem() {
        if (mController != null) {
            mController.skipToPreviousPlaylistItem();
        }
    }

    void setSpeed(float speed) {
        if (mController != null) {
            mController.setPlaybackSpeed(speed);
        }
    }

    void selectAudioTrack(int trackIndex) {
        if (mController != null) {
            Bundle extra = new Bundle();
            extra.putInt(MediaControlView.KEY_SELECTED_AUDIO_INDEX, trackIndex);
            mController.sendCustomCommand(
                    new SessionCommand(MediaControlView.COMMAND_SELECT_AUDIO_TRACK, null),
                    extra);
        }
    }

    void showSubtitle(int trackIndex) {
        if (mController != null) {
            Bundle extra = new Bundle();
            extra.putInt(MediaControlView.KEY_SELECTED_SUBTITLE_INDEX, trackIndex);
            mController.sendCustomCommand(
                    new SessionCommand(MediaControlView.COMMAND_SHOW_SUBTITLE, null), extra);
        }
    }

    void hideSubtitle() {
        if (mController != null) {
            mController.sendCustomCommand(
                    new SessionCommand(MediaControlView.COMMAND_HIDE_SUBTITLE, null), null);
        }
    }

    long getDurationMs() {
        if (mController != null) {
            return mController.getDuration();
        }
        return 0;
    }

    CharSequence getTitle() {
        if (mMediaMetadata != null) {
            if (mMediaMetadata.containsKey(MediaMetadata.METADATA_KEY_TITLE)) {
                return mMediaMetadata.getText(MediaMetadata.METADATA_KEY_TITLE);
            }
        }
        return null;
    }

    CharSequence getArtistText() {
        if (mMediaMetadata != null) {
            if (mMediaMetadata.containsKey(MediaMetadata.METADATA_KEY_ARTIST)) {
                return mMediaMetadata.getText(MediaMetadata.METADATA_KEY_ARTIST);
            }
        }
        return null;
    }

    MediaItem getCurrentMediaItem() {
        if (mController != null) {
            return mController.getCurrentMediaItem();
        }
        return null;
    }

    private class MediaControllerCallback extends MediaController.ControllerCallback {
        private final WidgetCallback mWidgetCallback;

        MediaControllerCallback(@NonNull WidgetCallback callback) {
            mWidgetCallback = callback;
        }

        @Override
        public void onConnected(@NonNull MediaController controller,
                @NonNull SessionCommandGroup allowedCommands) {
            onAllowedCommandsChanged(controller, allowedCommands);
            onCurrentMediaItemChanged(controller, controller.getCurrentMediaItem());
        }

        @Override
        public void onAllowedCommandsChanged(@NonNull MediaController controller,
                @NonNull SessionCommandGroup commands) {
            mAllowedCommands = commands;
            mWidgetCallback.onAllowedCommandsChanged(WidgetPlayer.this, commands);
        }

        @NonNull
        @Override
        public SessionResult onCustomCommand(@NonNull MediaController controller,
                @NonNull SessionCommand command, @Nullable Bundle args) {
            return mWidgetCallback.onCustomCommand(WidgetPlayer.this, command, args);
        }

        @Override
        public void onPlayerStateChanged(@NonNull MediaController controller, int state) {
            if (mSavedPlayerState == state) return;
            mSavedPlayerState = state;
            mWidgetCallback.onPlayerStateChanged(WidgetPlayer.this, state);
        }

        @Override
        public void onPlaybackSpeedChanged(@NonNull MediaController controller, float speed) {
            mWidgetCallback.onPlaybackSpeedChanged(WidgetPlayer.this, speed);
        }

        @Override
        public void onSeekCompleted(@NonNull MediaController controller, long position) {
            mWidgetCallback.onSeekCompleted(WidgetPlayer.this, position);
        }

        @Override
        public void onCurrentMediaItemChanged(@NonNull MediaController controller,
                @Nullable MediaItem item) {
            mMediaMetadata = item == null ? null : item.getMetadata();
            mWidgetCallback.onCurrentMediaItemChanged(WidgetPlayer.this, item);
        }

        @Override
        public void onPlaybackCompleted(@NonNull MediaController controller) {
            mWidgetCallback.onPlaybackCompleted(WidgetPlayer.this);
        }

        @Override
        public void onVideoSizeChanged(@NonNull MediaController controller, @NonNull MediaItem item,
                @NonNull VideoSize videoSize) {
            mWidgetCallback.onVideoSizeChanged(WidgetPlayer.this, item, videoSize);
        }

        @Override
        public void onTrackInfoChanged(@NonNull MediaController controller,
                @NonNull List<TrackInfo> trackInfos) {
            mWidgetCallback.onTrackInfoChanged(WidgetPlayer.this, trackInfos);
        }

        @Override
        public void onTrackSelected(@NonNull MediaController controller,
                @NonNull TrackInfo trackInfo) {
            mWidgetCallback.onTrackSelected(WidgetPlayer.this, trackInfo);
        }

        @Override
        public void onTrackDeselected(@NonNull MediaController controller,
                @NonNull TrackInfo trackInfo) {
            mWidgetCallback.onTrackDeselected(WidgetPlayer.this, trackInfo);
        }

        @Override
        public void onSubtitleData(@NonNull MediaController controller, @NonNull MediaItem item,
                @NonNull TrackInfo track, @NonNull SubtitleData data) {
            mWidgetCallback.onSubtitleData(WidgetPlayer.this, item, track, data);
        }
    }

    abstract static class WidgetCallback {
        void onAllowedCommandsChanged(@NonNull WidgetPlayer player,
                @NonNull SessionCommandGroup commands) {
        }
        void onCurrentMediaItemChanged(@NonNull WidgetPlayer player, @Nullable MediaItem item) {
        }
        // TODO: Remove onCustomCommand (b/133124026)
        SessionResult onCustomCommand(@NonNull WidgetPlayer player,
                @NonNull SessionCommand command, @Nullable Bundle args) {
            return new SessionResult(SessionResult.RESULT_ERROR_NOT_SUPPORTED, null);
        }
        void onPlayerStateChanged(@NonNull WidgetPlayer player, int state) {
        }
        void onPlaybackSpeedChanged(@NonNull WidgetPlayer player, float speed) {
        }
        void onSeekCompleted(@NonNull WidgetPlayer player, long position) {
        }
        void onPlaybackCompleted(@NonNull WidgetPlayer player) {
        }
        void onVideoSizeChanged(@NonNull WidgetPlayer player, @NonNull MediaItem item,
                @NonNull VideoSize videoSize) {
        }
        void onTrackInfoChanged(@NonNull WidgetPlayer player,
                @NonNull List<TrackInfo> trackInfos) {
        }
        void onTrackSelected(@NonNull WidgetPlayer player, @NonNull TrackInfo trackInfo) {
        }
        void onTrackDeselected(@NonNull WidgetPlayer player, @NonNull TrackInfo trackInfo) {
        }
        void onSubtitleData(@NonNull WidgetPlayer player, @NonNull MediaItem item,
                @NonNull TrackInfo track, @NonNull SubtitleData data) {
        }
    }
}
