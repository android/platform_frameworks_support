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

import static androidx.media2.MediaSession2.SessionResult.RESULT_CODE_BAD_VALUE;
import static androidx.media2.MediaSession2.SessionResult.RESULT_CODE_INVALID_STATE;
import static androidx.media2.MediaSession2.SessionResult.RESULT_CODE_NOT_SUPPORTED;
import static androidx.media2.MediaSession2.SessionResult.RESULT_CODE_SUCCESS;
import static androidx.media2.SessionCommand2.COMMAND_CODE_CUSTOM;
import static androidx.media2.SessionCommand2.COMMAND_VERSION_CURRENT;

import android.app.PendingIntent;
import android.content.Context;
import android.net.Uri;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.os.RemoteException;
import android.os.ResultReceiver;
import android.os.SystemClock;
import android.support.v4.media.session.MediaSessionCompat;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;

import androidx.annotation.GuardedBy;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.concurrent.futures.SettableFuture;
import androidx.media.MediaSessionManager;
import androidx.media2.MediaController2.PlaybackInfo;
import androidx.media2.MediaLibraryService2.MediaLibrarySession;
import androidx.media2.MediaLibraryService2.MediaLibrarySession.MediaLibrarySessionImpl;
import androidx.media2.MediaSession2.CommandButton;
import androidx.media2.MediaSession2.ControllerCb;
import androidx.media2.MediaSession2.ControllerInfo;
import androidx.media2.MediaSession2.MediaSession2Impl;
import androidx.media2.MediaSession2.SessionResult;
import androidx.media2.SessionPlayer2.PlayerResult;
import androidx.versionedparcelable.ParcelImpl;
import androidx.versionedparcelable.ParcelUtils;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;

/**
 * Handles incoming commands from {@link MediaController2} and {@link MediaBrowser2}
 * to both {@link MediaSession2} and {@link MediaLibrarySession}.
 * <p>
 * We cannot create a subclass for library service specific function because AIDL doesn't support
 * subclassing and it's generated stub class is an abstract class.
 */
class MediaSession2Stub extends IMediaSession2.Stub {

    private static final String TAG = "MediaSession2Stub";
    private static final boolean DEBUG = true; //Log.isLoggable(TAG, Log.DEBUG);

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static final Executor DIRECT_EXECUTOR = new Executor() {
        @Override
        public void execute(Runnable command) {
            command.run();
        }
    };

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static final SparseArray<SessionCommand2> sCommandsForOnCommandRequest =
            new SparseArray<>();

    static {
        SessionCommandGroup2 group = new SessionCommandGroup2.Builder()
                .addAllPlayerCommands(COMMAND_VERSION_CURRENT)
                .addAllVolumeCommands(COMMAND_VERSION_CURRENT)
                .build();
        Set<SessionCommand2> commands = group.getCommands();
        for (SessionCommand2 command : commands) {
            sCommandsForOnCommandRequest.append(command.getCommandCode(), command);
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final ConnectedControllersManager<IBinder> mConnectedControllersManager;

    final Object mLock = new Object();

    final MediaSession2Impl mSessionImpl;
    final Context mContext;
    final MediaSessionManager mSessionManager;

    @GuardedBy("mLock")
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Set<IBinder> mConnectingControllers = new HashSet<>();

    MediaSession2Stub(MediaSession2Impl sessionImpl) {
        mSessionImpl = sessionImpl;
        mContext = mSessionImpl.getContext();
        mSessionManager = MediaSessionManager.getSessionManager(mContext);
        mConnectedControllersManager = new ConnectedControllersManager<>(sessionImpl);
    }

    ConnectedControllersManager<IBinder> getConnectedControllersManager() {
        return mConnectedControllersManager;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static ListenableFuture<PlayerResult> createResult(int resultCode) {
        SettableFuture<PlayerResult> result = SettableFuture.create();
        result.set(new PlayerResult(resultCode, null));
        return result;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static void sendSessionResult(@NonNull ControllerInfo controller, int seq,
            int resultCode) {
        sendSessionResult(controller, seq, new SessionResult(resultCode));
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static void sendSessionResult(@NonNull ControllerInfo controller, int seq,
            @NonNull SessionResult result) {
        try {
            controller.getControllerCb().onSessionResult(seq, result);
        } catch (RemoteException e) {
            Log.w(TAG, "Exception in " + controller.toString(), e);
        }
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static void sendPlayerResult(@NonNull ControllerInfo controller, int seq,
            @NonNull PlayerResult result) {
        try {
            controller.getControllerCb().onPlayerResult(seq, result);
        } catch (RemoteException e) {
            Log.w(TAG, "Exception in " + controller.toString(), e);
        }
    }

    private void onSessionCommand(@NonNull IMediaController2 caller, int seq, final int commandCode,
            @NonNull final SessionCommand runnable) {
        onSessionCommandInternal(caller, seq, null, commandCode, runnable);
    }

    private void onSessionCommand(@NonNull IMediaController2 caller, int seq,
            @NonNull final SessionCommand2 sessionCommand,
            @NonNull final SessionCommand runnable) {
        onSessionCommandInternal(caller, seq, sessionCommand, COMMAND_CODE_CUSTOM, runnable);
    }

    private void onSessionCommandInternal(@NonNull IMediaController2 caller, final int seq,
            @Nullable final SessionCommand2 sessionCommand, final int commandCode,
            @NonNull final SessionCommand runnable) {
        final ControllerInfo controller = mConnectedControllersManager.getController(
                caller == null ? null : caller.asBinder());
        if (mSessionImpl.isClosed() || controller == null) {
            return;
        }
        mSessionImpl.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (!mConnectedControllersManager.isConnected(controller)) {
                    return;
                }
                SessionCommand2 command;
                if (sessionCommand != null) {
                    if (!mConnectedControllersManager.isAllowedCommand(
                            controller, sessionCommand)) {
                        return;
                    }
                    command = sCommandsForOnCommandRequest.get(sessionCommand.getCommandCode());
                } else {
                    if (!mConnectedControllersManager.isAllowedCommand(controller, commandCode)) {
                        return;
                    }
                    command = sCommandsForOnCommandRequest.get(commandCode);
                }
                try {
                    if (command != null) {
                        int resultCode = mSessionImpl.getCallback().onCommandRequest(
                                mSessionImpl.getInstance(), controller, command);
                        if (resultCode != RESULT_CODE_SUCCESS) {
                            // Don't run rejected command.
                            if (DEBUG) {
                                Log.d(TAG, "Command (" + command + ") from "
                                        + controller + " was rejected by " + mSessionImpl
                                        + ", code=" + resultCode);
                            }
                            sendSessionResult(controller, seq, resultCode);
                            return;
                        }
                    }
                    if (runnable instanceof SessionPlayerCommand) {
                        final ListenableFuture<PlayerResult> result =
                                ((SessionPlayerCommand) runnable).run(controller);
                        if (result == null) {
                            sendSessionResult(controller, seq, RESULT_CODE_NOT_SUPPORTED);
                        } else {
                            result.addListener(new Runnable() {
                                @Override
                                public void run() {
                                    try {
                                        sendPlayerResult(controller, seq, result.get(
                                                0, TimeUnit.MILLISECONDS));
                                    } catch (Exception e) {
                                        Log.w(TAG, "Cannot obtain PlayerResult after the"
                                                + " command is finished", e);
                                        sendSessionResult(controller, seq,
                                                RESULT_CODE_INVALID_STATE);
                                    }
                                }
                            }, DIRECT_EXECUTOR);
                        }
                    } else if (runnable instanceof SessionCallbackCommand) {
                        int resultCode = ((SessionCallbackCommand) runnable).run(controller);
                        sendSessionResult(controller, seq, resultCode);
                    } else {
                        ((SessionCustomCommand) runnable).run(controller);
                    }
                } catch (RemoteException e) {
                    // Currently it's TransactionTooLargeException or DeadSystemException.
                    // We'd better to leave log for those cases because
                    //   - TransactionTooLargeException means that we may need to fix our code.
                    //     (e.g. add pagination or special way to deliver Bitmap)
                    //   - DeadSystemException means that errors around it can be ignored.
                    Log.w(TAG, "Exception in " + controller.toString(), e);
                }
            }
        });
    }

    private void onBrowserCommand(@NonNull IMediaController2 caller, final int commandCode,
            final @NonNull SessionCommand runnable) {
        if (!(mSessionImpl instanceof MediaLibrarySessionImpl)) {
            throw new RuntimeException("MediaSession2 cannot handle MediaLibrarySession command");
        }

        onSessionCommandInternal(caller, -1, null, commandCode, runnable);
    }

    void connect(final IMediaController2 caller, final String callingPackage, final int pid,
            final int uid) {
        MediaSessionManager.RemoteUserInfo
                remoteUserInfo = new MediaSessionManager.RemoteUserInfo(callingPackage, pid, uid);
        final ControllerInfo controllerInfo = new ControllerInfo(remoteUserInfo,
                mSessionManager.isTrustedForMediaControl(remoteUserInfo),
                new Controller2Cb(caller));
        mSessionImpl.getCallbackExecutor().execute(new Runnable() {
            @Override
            public void run() {
                if (mSessionImpl.isClosed()) {
                    return;
                }
                final IBinder callbackBinder = ((Controller2Cb) controllerInfo.getControllerCb())
                        .getCallbackBinder();
                synchronized (mLock) {
                    // Keep connecting controllers.
                    // This helps sessions to call APIs in the onConnect()
                    // (e.g. setCustomLayout()) instead of pending them.
                    mConnectingControllers.add(callbackBinder);
                }
                SessionCommandGroup2 allowedCommands = mSessionImpl.getCallback().onConnect(
                        mSessionImpl.getInstance(), controllerInfo);
                // Don't reject connection for the request from trusted app.
                // Otherwise server will fail to retrieve session's information to dispatch
                // media keys to.
                boolean accept = allowedCommands != null || controllerInfo.isTrusted();
                if (accept) {
                    if (DEBUG) {
                        Log.d(TAG, "Accepting connection, controllerInfo=" + controllerInfo
                                + " allowedCommands=" + allowedCommands);
                    }
                    if (allowedCommands == null) {
                        // For trusted apps, send non-null allowed commands to keep
                        // connection.
                        allowedCommands = new SessionCommandGroup2();
                    }
                    synchronized (mLock) {
                        mConnectingControllers.remove(callbackBinder);
                        mConnectedControllersManager.addController(
                                callbackBinder, controllerInfo, allowedCommands);
                    }
                    // If connection is accepted, notify the current state to the controller.
                    // It's needed because we cannot call synchronous calls between
                    // session/controller.
                    // Note: We're doing this after the onConnectionChanged(), but there's no
                    //       guarantee that events here are notified after the onConnected()
                    //       because IMediaController2 is oneway (i.e. async call) and Stub will
                    //       use thread poll for incoming calls.
                    final int playerState = mSessionImpl.getPlayerState();
                    final ParcelImpl currentItem = (ParcelImpl) ParcelUtils.toParcelable(
                            mSessionImpl.getCurrentMediaItem());
                    final long positionEventTimeMs = SystemClock.elapsedRealtime();
                    final long positionMs = mSessionImpl.getCurrentPosition();
                    final float playbackSpeed = mSessionImpl.getPlaybackSpeed();
                    final long bufferedPositionMs = mSessionImpl.getBufferedPosition();
                    final ParcelImpl playbackInfo =
                            (ParcelImpl) ParcelUtils.toParcelable(mSessionImpl.getPlaybackInfo());
                    final int repeatMode = mSessionImpl.getRepeatMode();
                    final int shuffleMode = mSessionImpl.getShuffleMode();
                    final PendingIntent sessionActivity = mSessionImpl.getSessionActivity();
                    final List<MediaItem2> playlist =
                            allowedCommands.hasCommand(
                                    SessionCommand2.COMMAND_CODE_PLAYER_GET_PLAYLIST)
                                    ? mSessionImpl.getPlaylist() : null;
                    final List<ParcelImpl> playlistParcel =
                            MediaUtils2.convertMediaItem2ListToParcelImplList(playlist);

                    // Double check if session is still there, because close() can be called in
                    // another thread.
                    if (mSessionImpl.isClosed()) {
                        return;
                    }
                    try {
                        caller.onConnected(MediaSession2Stub.this,
                                (ParcelImpl) ParcelUtils.toParcelable(allowedCommands),
                                playerState, currentItem, positionEventTimeMs, positionMs,
                                playbackSpeed, bufferedPositionMs, playbackInfo, repeatMode,
                                shuffleMode, playlistParcel, sessionActivity);
                    } catch (RemoteException e) {
                        // Controller may be died prematurely.
                    }
                } else {
                    synchronized (mLock) {
                        mConnectingControllers.remove(callbackBinder);
                    }
                    if (DEBUG) {
                        Log.d(TAG, "Rejecting connection, controllerInfo=" + controllerInfo);
                    }
                    try {
                        caller.onDisconnected();
                    } catch (RemoteException e) {
                        // Controller may be died prematurely.
                        // Not an issue because we'll ignore it anyway.
                    }
                }
            }
        });
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    @Nullable MediaItem2 convertMediaItem2OnExecutor(ControllerInfo controller,
            @NonNull ParcelImpl itemParcelImpl) {
        if (itemParcelImpl == null) {
            return null;
        }
        MediaItem2 item = ParcelUtils.fromParcelable(itemParcelImpl);
        MediaItem2 newItem = mSessionImpl.getCallback().onCreateMediaItem(
                mSessionImpl.getInstance(), controller, item);
        if (newItem == null) {
            Log.w(TAG, "onCreateMediaItem(" + newItem + ") returned null");
            return null;
        }
        newItem.mParcelUuid = item.mParcelUuid;
        return newItem;
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    // AIDL methods for session overrides
    //////////////////////////////////////////////////////////////////////////////////////////////

    @Override
    public void connect(final IMediaController2 caller, int seq, final String callingPackage)
            throws RuntimeException {
        connect(caller, callingPackage, Binder.getCallingPid(), Binder.getCallingUid());
    }

    @Override
    public void release(final IMediaController2 caller, int seq) throws RemoteException {
        mConnectedControllersManager.removeController(caller == null ? null : caller.asBinder());
    }

    @Override
    public void setVolumeTo(final IMediaController2 caller, int seq, final int value,
            final int flags) throws RuntimeException {
        onSessionCommand(caller, seq, SessionCommand2.COMMAND_CODE_VOLUME_SET_VOLUME,
                new SessionCallbackCommand() {
                    @Override
                    public int run(ControllerInfo controller) {
                        MediaSessionCompat sessionCompat = mSessionImpl.getSessionCompat();
                        if (sessionCompat != null) {
                            sessionCompat.getController().setVolumeTo(value, flags);
                        }
                        // TODO: Handle remote player case
                        return RESULT_CODE_SUCCESS;
                    }
                });
    }

    @Override
    public void adjustVolume(IMediaController2 caller, int seq, final int direction,
            final int flags) throws RuntimeException {
        onSessionCommand(caller, seq, SessionCommand2.COMMAND_CODE_VOLUME_ADJUST_VOLUME,
                new SessionCallbackCommand() {
                    @Override
                    public int run(ControllerInfo controller) {
                        MediaSessionCompat sessionCompat = mSessionImpl.getSessionCompat();
                        if (sessionCompat != null) {
                            sessionCompat.getController().adjustVolume(direction, flags);
                        }
                        // TODO: Handle remote player case
                        return RESULT_CODE_SUCCESS;
                    }
                });
    }

    @Override
    public void play(IMediaController2 caller, int seq) throws RuntimeException {
        onSessionCommand(caller, seq, SessionCommand2.COMMAND_CODE_PLAYER_PLAY,
                new SessionPlayerCommand() {
                    @Override
                    public ListenableFuture<PlayerResult> run(ControllerInfo controller) {
                        return mSessionImpl.play();
                    }
                });
    }

    @Override
    public void pause(IMediaController2 caller, int seq) throws RuntimeException {
        onSessionCommand(caller, seq, SessionCommand2.COMMAND_CODE_PLAYER_PAUSE,
                new SessionPlayerCommand() {
                    @Override
                    public ListenableFuture<PlayerResult> run(ControllerInfo controller) {
                        return mSessionImpl.pause();
                    }
                });
    }

    @Override
    public void prepare(IMediaController2 caller, int seq) throws RuntimeException {
        onSessionCommand(caller, seq, SessionCommand2.COMMAND_CODE_PLAYER_PREPARE,
                new SessionPlayerCommand() {
                    @Override
                    public ListenableFuture<PlayerResult> run(ControllerInfo controller) {
                        return mSessionImpl.prepare();
                    }
                });
    }

    @Override
    public void fastForward(IMediaController2 caller, int seq) {
        onSessionCommand(caller, seq, SessionCommand2.COMMAND_CODE_SESSION_FAST_FORWARD,
                new SessionCallbackCommand() {
                    @Override
                    public int run(ControllerInfo controller) {
                        return mSessionImpl.getCallback().onFastForward(
                                mSessionImpl.getInstance(), controller);
                    }
                });
    }

    @Override
    public void rewind(IMediaController2 caller, int seq) {
        onSessionCommand(caller, seq, SessionCommand2.COMMAND_CODE_SESSION_REWIND,
                new SessionCallbackCommand() {
                    @Override
                    public int run(ControllerInfo controller) {
                        return mSessionImpl.getCallback().onRewind(
                                mSessionImpl.getInstance(), controller);
                    }
                });
    }

    @Override
    public void seekTo(IMediaController2 caller, int seq, final long pos) throws RuntimeException {
        onSessionCommand(caller, seq, SessionCommand2.COMMAND_CODE_PLAYER_SEEK_TO,
                new SessionPlayerCommand() {
                    @Override
                    public ListenableFuture<PlayerResult> run(ControllerInfo controller) {
                        return mSessionImpl.seekTo(pos);
                    }
                });
    }

    @Override
    public void sendCustomCommand(final IMediaController2 caller, final int seq,
            final ParcelImpl command, final Bundle args) {
        final SessionCommand2 sessionCommand = ParcelUtils.fromParcelable(command);
        onSessionCommand(caller, seq, sessionCommand, new SessionCustomCommand() {
            @Override
            public void run(final ControllerInfo controller) {
                SessionResult result = mSessionImpl.getCallback().onCustomCommand(
                        mSessionImpl.getInstance(), controller, sessionCommand, args);
                sendSessionResult(controller, seq, result);
            }
        });
    }

    @Override
    public void prepareFromUri(final IMediaController2 caller, int seq, final Uri uri,
            final Bundle extras) {
        onSessionCommand(caller, seq, SessionCommand2.COMMAND_CODE_SESSION_PREPARE_FROM_URI,
                new SessionCallbackCommand() {
                    @Override
                    public int run(ControllerInfo controller) {
                        if (uri == null) {
                            Log.w(TAG, "prepareFromUri(): Ignoring null uri from " + controller);
                            return RESULT_CODE_BAD_VALUE;
                        }
                        return mSessionImpl.getCallback().onPrepareFromUri(
                                mSessionImpl.getInstance(), controller, uri, extras);
                    }
                });
    }

    @Override
    public void prepareFromSearch(final IMediaController2 caller, int seq, final String query,
            final Bundle extras) {
        onSessionCommand(caller, seq, SessionCommand2.COMMAND_CODE_SESSION_PREPARE_FROM_SEARCH,
                new SessionCallbackCommand() {
                    @Override
                    public int run(ControllerInfo controller) {
                        if (TextUtils.isEmpty(query)) {
                            Log.w(TAG, "prepareFromSearch(): Ignoring empty query from "
                                    + controller);
                            return RESULT_CODE_BAD_VALUE;
                        }
                        return mSessionImpl.getCallback().onPrepareFromSearch(
                                mSessionImpl.getInstance(), controller, query, extras);
                    }
                });
    }

    @Override
    public void prepareFromMediaId(final IMediaController2 caller, int seq, final String mediaId,
            final Bundle extras) {
        onSessionCommand(caller, seq, SessionCommand2.COMMAND_CODE_SESSION_PREPARE_FROM_MEDIA_ID,
                new SessionCallbackCommand() {
                    @Override
                    public int run(ControllerInfo controller) {
                        if (mediaId == null) {
                            Log.w(TAG, "prepareFromMediaId(): Ignoring null mediaId from "
                                    + controller);
                            return RESULT_CODE_BAD_VALUE;
                        }
                        return mSessionImpl.getCallback().onPrepareFromMediaId(
                                mSessionImpl.getInstance(), controller, mediaId, extras);
                    }
                });
    }

    @Override
    public void playFromUri(final IMediaController2 caller, int seq, final Uri uri,
            final Bundle extras) {
        onSessionCommand(caller, seq, SessionCommand2.COMMAND_CODE_SESSION_PLAY_FROM_URI,
                new SessionCallbackCommand() {
                    @Override
                    public int run(ControllerInfo controller) {
                        if (uri == null) {
                            Log.w(TAG, "playFromUri(): Ignoring null uri from " + controller);
                            return RESULT_CODE_BAD_VALUE;
                        }
                        return mSessionImpl.getCallback().onPlayFromUri(
                                mSessionImpl.getInstance(), controller, uri, extras);
                    }
                });
    }

    @Override
    public void playFromSearch(final IMediaController2 caller, int seq, final String query,
            final Bundle extras) {
        onSessionCommand(caller, seq, SessionCommand2.COMMAND_CODE_SESSION_PLAY_FROM_SEARCH,
                new SessionCallbackCommand() {
                    @Override
                    public int run(ControllerInfo controller) {
                        if (TextUtils.isEmpty(query)) {
                            Log.w(TAG, "playFromSearch(): Ignoring empty query from " + controller);
                            return RESULT_CODE_BAD_VALUE;
                        }
                        return mSessionImpl.getCallback().onPlayFromSearch(
                                mSessionImpl.getInstance(), controller, query, extras);
                    }
                });
    }

    @Override
    public void playFromMediaId(final IMediaController2 caller, int seq, final String mediaId,
            final Bundle extras) {
        onSessionCommand(caller, seq, SessionCommand2.COMMAND_CODE_SESSION_PLAY_FROM_MEDIA_ID,
                new SessionCallbackCommand() {
                    @Override
                    public int run(ControllerInfo controller) {
                        if (mediaId == null) {
                            Log.w(TAG,
                                    "playFromMediaId(): Ignoring null mediaId from " + controller);
                            return RESULT_CODE_BAD_VALUE;
                        }
                        return mSessionImpl.getCallback().onPlayFromMediaId(
                                mSessionImpl.getInstance(), controller, mediaId, extras);
                    }
                });
    }

    @Override
    public void setRating(final IMediaController2 caller, int seq, final String mediaId,
            final ParcelImpl rating) {
        final Rating2 rating2 = ParcelUtils.fromParcelable(rating);
        onSessionCommand(caller, seq, SessionCommand2.COMMAND_CODE_SESSION_SET_RATING,
                new SessionCallbackCommand() {
                    @Override
                    public int run(ControllerInfo controller) {
                        if (mediaId == null) {
                            Log.w(TAG, "setRating(): Ignoring null mediaId from " + controller);
                            return RESULT_CODE_BAD_VALUE;
                        }
                        if (rating2 == null) {
                            Log.w(TAG,
                                    "setRating(): Ignoring null ratingBundle from " + controller);
                            return RESULT_CODE_BAD_VALUE;
                        }
                        return mSessionImpl.getCallback().onSetRating(
                                mSessionImpl.getInstance(), controller, mediaId, rating2);
                    }
                });
    }

    @Override
    public void setPlaybackSpeed(final IMediaController2 caller, int seq, final float speed) {
        onSessionCommand(caller, seq, SessionCommand2.COMMAND_CODE_PLAYER_SET_SPEED,
                new SessionPlayerCommand() {
                    @Override
                    public ListenableFuture<PlayerResult> run(ControllerInfo controller) {
                        return mSessionImpl.setPlaybackSpeed(speed);
                    }
                });
    }

    @Override
    public void setPlaylist(final IMediaController2 caller, int seq,
            final List<ParcelImpl> playlist, final Bundle metadata) {
        onSessionCommand(caller, seq, SessionCommand2.COMMAND_CODE_PLAYER_SET_PLAYLIST,
                new SessionPlayerCommand() {
                    @Override
                    public ListenableFuture<PlayerResult> run(ControllerInfo controller) {
                        if (playlist == null) {
                            Log.w(TAG, "setPlaylist(): Ignoring null playlist from " + controller);
                            return createResult(RESULT_CODE_BAD_VALUE);
                        }
                        List<MediaItem2> list = new ArrayList<>();
                        for (int i = 0; i < playlist.size(); i++) {
                            MediaItem2 item = convertMediaItem2OnExecutor(controller,
                                    playlist.get(i));
                            if (item != null) {
                                list.add(item);
                            }
                        }
                        return mSessionImpl.setPlaylist(list, MediaMetadata2.fromBundle(metadata));
                    }
                });
    }

    @Override
    public void setMediaItem(final IMediaController2 caller, int seq, final ParcelImpl mediaItem) {
        onSessionCommand(caller, seq, SessionCommand2.COMMAND_CODE_PLAYER_SET_MEDIA_ITEM,
                new SessionPlayerCommand() {
                    @Override
                    public ListenableFuture<PlayerResult> run(ControllerInfo controller) {
                        if (mediaItem == null) {
                            Log.w(TAG, "setMediaItem(): Ignoring null item from " + controller);
                            return createResult(RESULT_CODE_BAD_VALUE);
                        }
                        MediaItem2 item = convertMediaItem2OnExecutor(controller, mediaItem);
                        if (item == null) {
                            return createResult(RESULT_CODE_BAD_VALUE);
                        }
                        return mSessionImpl.setMediaItem(item);
                    }
                });
    }

    @Override
    public void updatePlaylistMetadata(final IMediaController2 caller, int seq,
            final Bundle metadata) {
        onSessionCommand(caller, seq, SessionCommand2.COMMAND_CODE_PLAYER_UPDATE_LIST_METADATA,
                new SessionPlayerCommand() {
                    @Override
                    public ListenableFuture<PlayerResult> run(ControllerInfo controller) {
                        return mSessionImpl.updatePlaylistMetadata(
                                MediaMetadata2.fromBundle(metadata));
                    }
                });
    }

    @Override
    public void addPlaylistItem(IMediaController2 caller, int seq, final int index,
            final ParcelImpl mediaItem) {
        onSessionCommand(caller, seq, SessionCommand2.COMMAND_CODE_PLAYER_ADD_PLAYLIST_ITEM,
                new SessionPlayerCommand() {
                    @Override
                    public ListenableFuture<PlayerResult> run(ControllerInfo controller) {
                        if (mediaItem == null) {
                            Log.w(TAG, "addPlaylistItem(): Ignoring null item from " + controller);
                            return createResult(RESULT_CODE_BAD_VALUE);
                        }
                        MediaItem2 item = convertMediaItem2OnExecutor(controller, mediaItem);
                        if (item == null) {
                            return createResult(RESULT_CODE_BAD_VALUE);
                        }
                        return mSessionImpl.addPlaylistItem(index, item);
                    }
                });
    }

    @Override
    public void removePlaylistItem(IMediaController2 caller, int seq, final ParcelImpl mediaItem) {
        onSessionCommand(caller, seq, SessionCommand2.COMMAND_CODE_PLAYER_REMOVE_PLAYLIST_ITEM,
                new SessionPlayerCommand() {
                    @Override
                    public ListenableFuture<PlayerResult> run(ControllerInfo controller) {
                        MediaItem2 item = ParcelUtils.fromParcelable(mediaItem);
                        // Note: MediaItem2 has hidden UUID to identify it across the processes.
                        return mSessionImpl.removePlaylistItem(item);
                    }
                });
    }

    @Override
    public void replacePlaylistItem(IMediaController2 caller, int seq, final int index,
            final ParcelImpl mediaItem) {
        onSessionCommand(caller, seq, SessionCommand2.COMMAND_CODE_PLAYER_REPLACE_PLAYLIST_ITEM,
                new SessionPlayerCommand() {
                    @Override
                    public ListenableFuture<PlayerResult> run(ControllerInfo controller) {
                        if (mediaItem == null) {
                            Log.w(TAG, "replacePlaylistItem(): Ignoring null item from "
                                    + controller);
                            return createResult(RESULT_CODE_BAD_VALUE);
                        }
                        MediaItem2 item = convertMediaItem2OnExecutor(controller, mediaItem);
                        if (item == null) {
                            return createResult(RESULT_CODE_BAD_VALUE);
                        }
                        return mSessionImpl.replacePlaylistItem(index, item);
                    }
                });
    }

    @Override
    public void skipToPlaylistItem(IMediaController2 caller, int seq, final ParcelImpl mediaItem) {
        onSessionCommand(caller, seq, SessionCommand2.COMMAND_CODE_PLAYER_SKIP_TO_PLAYLIST_ITEM,
                new SessionPlayerCommand() {
                    @Override
                    public ListenableFuture<PlayerResult> run(ControllerInfo controller) {
                        if (mediaItem == null) {
                            Log.w(TAG, "skipToPlaylistItem(): Ignoring null mediaItem from "
                                    + controller);
                            return createResult(RESULT_CODE_BAD_VALUE);
                        }
                        // Note: MediaItem2 has hidden UUID to identify it across the processes.
                        return mSessionImpl.skipToPlaylistItem(
                                (MediaItem2) ParcelUtils.fromParcelable(mediaItem));
                    }
                });
    }

    @Override
    public void skipToPreviousItem(IMediaController2 caller, int seq) {
        onSessionCommand(caller, seq,
                SessionCommand2.COMMAND_CODE_PLAYER_SKIP_TO_PREVIOUS_PLAYLIST_ITEM,
                new SessionPlayerCommand() {
                    @Override
                    public ListenableFuture<PlayerResult> run(ControllerInfo controller) {
                        return mSessionImpl.skipToPreviousItem();
                    }
                });
    }

    @Override
    public void skipToNextItem(IMediaController2 caller, int seq) {
        onSessionCommand(caller, seq,
                SessionCommand2.COMMAND_CODE_PLAYER_SKIP_TO_NEXT_PLAYLIST_ITEM,
                new SessionPlayerCommand() {
                    @Override
                    public ListenableFuture<PlayerResult> run(ControllerInfo controller) {
                        return mSessionImpl.skipToNextItem();
                    }
                });
    }

    @Override
    public void setRepeatMode(IMediaController2 caller, int seq, final int repeatMode) {
        onSessionCommand(caller, seq, SessionCommand2.COMMAND_CODE_PLAYER_SET_REPEAT_MODE,
                new SessionPlayerCommand() {
                    @Override
                    public ListenableFuture<PlayerResult> run(ControllerInfo controller) {
                        return mSessionImpl.setRepeatMode(repeatMode);
                    }
                });
    }

    @Override
    public void setShuffleMode(IMediaController2 caller, int seq, final int shuffleMode) {
        onSessionCommand(caller, seq, SessionCommand2.COMMAND_CODE_PLAYER_SET_SHUFFLE_MODE,
                new SessionPlayerCommand() {
                    @Override
                    public ListenableFuture<PlayerResult> run(ControllerInfo controller) {
                        return mSessionImpl.setShuffleMode(shuffleMode);
                    }
                });
    }

    @Override
    public void subscribeRoutesInfo(IMediaController2 caller, int seq) {
        onSessionCommand(caller, seq, SessionCommand2.COMMAND_CODE_SESSION_SUBSCRIBE_ROUTES_INFO,
                new SessionCallbackCommand() {
                    @Override
                    public int run(ControllerInfo controller) {
                        return mSessionImpl.getCallback().onSubscribeRoutesInfo(
                                mSessionImpl.getInstance(), controller);
                    }
                });
    }

    @Override
    public void unsubscribeRoutesInfo(IMediaController2 caller, int seq) {
        onSessionCommand(caller, seq, SessionCommand2.COMMAND_CODE_SESSION_UNSUBSCRIBE_ROUTES_INFO,
                new SessionCallbackCommand() {
                    @Override
                    public int run(ControllerInfo controller) {
                        return  mSessionImpl.getCallback().onUnsubscribeRoutesInfo(
                                mSessionImpl.getInstance(), controller);
                    }
                });
    }

    @Override
    public void selectRoute(IMediaController2 caller, int seq, final Bundle route) {
        if (MediaUtils2.isUnparcelableBundle(route)) {
            throw new RuntimeException("Unexpected route bundle: " + route);
        }
        onSessionCommand(caller, seq, SessionCommand2.COMMAND_CODE_SESSION_UNSUBSCRIBE_ROUTES_INFO,
                new SessionCallbackCommand() {
                    @Override
                    public int run(ControllerInfo controller) {
                        return mSessionImpl.getCallback().onSelectRoute(mSessionImpl.getInstance(),
                                controller, route);
                    }
                });
    }

    //////////////////////////////////////////////////////////////////////////////////////////////
    // AIDL methods for LibrarySession overrides
    //////////////////////////////////////////////////////////////////////////////////////////////

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    MediaLibrarySessionImpl getLibrarySession() {
        if (!(mSessionImpl instanceof MediaLibrarySessionImpl)) {
            throw new RuntimeException("Session cannot be casted to library session");
        }
        return (MediaLibrarySessionImpl) mSessionImpl;
    }

    @Override
    public void getLibraryRoot(final IMediaController2 caller, final Bundle rootHints)
            throws RuntimeException {
        onBrowserCommand(caller, SessionCommand2.COMMAND_CODE_LIBRARY_GET_LIBRARY_ROOT,
                new SessionCustomCommand() {
                    @Override
                    public void run(ControllerInfo controller) {
                        getLibrarySession().onGetLibraryRootOnExecutor(controller, rootHints);
                    }
                });
    }

    @Override
    public void getItem(final IMediaController2 caller, final String mediaId)
            throws RuntimeException {
        onBrowserCommand(caller, SessionCommand2.COMMAND_CODE_LIBRARY_GET_ITEM,
                new SessionCustomCommand() {
                    @Override
                    public void run(ControllerInfo controller) {
                        if (mediaId == null) {
                            Log.w(TAG, "getItem(): Ignoring null mediaId from " + controller);
                            return;
                        }
                        getLibrarySession().onGetItemOnExecutor(controller, mediaId);
                    }
                });
    }

    @Override
    public void getChildren(final IMediaController2 caller, final String parentId,
            final int page, final int pageSize, final Bundle extras) throws RuntimeException {
        onBrowserCommand(caller, SessionCommand2.COMMAND_CODE_LIBRARY_GET_CHILDREN,
                new SessionCustomCommand() {
                    @Override
                    public void run(ControllerInfo controller) {
                        if (parentId == null) {
                            Log.w(TAG, "getChildren(): Ignoring null parentId from " + controller);
                            return;
                        }
                        if (page < 0) {
                            Log.w(TAG, "getChildren(): Ignoring negative page from " + controller);
                            return;
                        }
                        if (pageSize < 1) {
                            Log.w(TAG, "getChildren(): Ignoring pageSize less than 1 from "
                                    + controller);
                            return;
                        }
                        getLibrarySession().onGetChildrenOnExecutor(controller, parentId, page,
                                pageSize, extras);
                    }
                });
    }

    @Override
    public void search(IMediaController2 caller, final String query, final Bundle extras) {
        onBrowserCommand(caller, SessionCommand2.COMMAND_CODE_LIBRARY_SEARCH,
                new SessionCustomCommand() {
                    @Override
                    public void run(ControllerInfo controller) {
                        if (TextUtils.isEmpty(query)) {
                            Log.w(TAG, "search(): Ignoring empty query from " + controller);
                            return;
                        }
                        getLibrarySession().onSearchOnExecutor(controller, query, extras);
                    }
                });
    }

    @Override
    public void getSearchResult(final IMediaController2 caller, final String query,
            final int page, final int pageSize, final Bundle extras) {
        onBrowserCommand(caller, SessionCommand2.COMMAND_CODE_LIBRARY_GET_SEARCH_RESULT,
                new SessionCustomCommand() {
                    @Override
                    public void run(ControllerInfo controller) {
                        if (TextUtils.isEmpty(query)) {
                            Log.w(TAG, "getSearchResult(): Ignoring empty query from "
                                    + controller);
                            return;
                        }
                        if (page < 0) {
                            Log.w(TAG, "getSearchResult(): Ignoring negative page from "
                                    + controller);
                            return;
                        }
                        if (pageSize < 1) {
                            Log.w(TAG, "getSearchResult(): Ignoring pageSize less than 1 from "
                                    + controller);
                            return;
                        }
                        getLibrarySession().onGetSearchResultOnExecutor(controller,
                                query, page, pageSize, extras);
                    }
                });
    }

    @Override
    public void subscribe(final IMediaController2 caller, final String parentId,
            final Bundle option) {
        onBrowserCommand(caller, SessionCommand2.COMMAND_CODE_LIBRARY_SUBSCRIBE,
                new SessionCustomCommand() {
                    @Override
                    public void run(ControllerInfo controller) {
                        if (parentId == null) {
                            Log.w(TAG, "subscribe(): Ignoring null parentId from " + controller);
                            return;
                        }
                        getLibrarySession().onSubscribeOnExecutor(controller, parentId, option);
                    }
                });
    }

    @Override
    public void unsubscribe(final IMediaController2 caller, final String parentId) {
        onBrowserCommand(caller, SessionCommand2.COMMAND_CODE_LIBRARY_UNSUBSCRIBE,
                new SessionCustomCommand() {
                    @Override
                    public void run(ControllerInfo controller) {
                        if (parentId == null) {
                            Log.w(TAG, "unsubscribe(): Ignoring null parentId from " + controller);
                            return;
                        }
                        getLibrarySession().onUnsubscribeOnExecutor(controller, parentId);
                    }
                });
    }

    private interface SessionCommand {
        // Empty interface
    }

    @FunctionalInterface
    private interface SessionPlayerCommand extends SessionCommand {
        ListenableFuture<PlayerResult> run(ControllerInfo controller) throws RemoteException;
    }

    @FunctionalInterface
    private interface SessionCallbackCommand extends SessionCommand {
        @SessionResult.ResultCode int run(ControllerInfo controller) throws RemoteException;
    }

    @FunctionalInterface
    private interface SessionCustomCommand extends SessionCommand {
        void run(ControllerInfo controller) throws RemoteException;
    }

    final class Controller2Cb extends ControllerCb {
        private final IMediaController2 mIControllerCallback;

        Controller2Cb(@NonNull IMediaController2 callback) {
            mIControllerCallback = callback;
        }

        @NonNull IBinder getCallbackBinder() {
            return mIControllerCallback.asBinder();
        }

        @Override
        void onPlayerResult(int seq, @Nullable PlayerResult result) throws RemoteException {
            onSessionResult(seq, new SessionResult(result));
        }

        @Override
        void onSessionResult(int seq, SessionResult result) throws RemoteException {
            if (result == null) {
                result = new SessionResult(RESULT_CODE_NOT_SUPPORTED, null);
            }
            mIControllerCallback.onSessionResult(seq,
                    (ParcelImpl) ParcelUtils.toParcelable(result));
        }

        @Override
        void onCustomLayoutChanged(List<CommandButton> layout) throws RemoteException {
            mIControllerCallback.onCustomLayoutChanged(
                    MediaUtils2.convertCommandButtonListToParcelImplList(layout));
        }

        @Override
        void onPlaybackInfoChanged(PlaybackInfo info) throws RemoteException {
            mIControllerCallback.onPlaybackInfoChanged((ParcelImpl) ParcelUtils.toParcelable(info));
        }

        @Override
        void onAllowedCommandsChanged(SessionCommandGroup2 commands) throws RemoteException {
            mIControllerCallback.onAllowedCommandsChanged(
                    (ParcelImpl) ParcelUtils.toParcelable(commands));
        }

        @Override
        void onCustomCommand(SessionCommand2 command, Bundle args, ResultReceiver receiver)
                throws RemoteException {
            mIControllerCallback.onCustomCommand((ParcelImpl) ParcelUtils.toParcelable(command),
                    args, receiver);
        }

        @Override
        void onPlayerStateChanged(long eventTimeMs, long positionMs, int playerState)
                throws RemoteException {
            mIControllerCallback.onPlayerStateChanged(eventTimeMs, positionMs, playerState);
        }

        @Override
        void onPlaybackSpeedChanged(long eventTimeMs, long positionMs, float speed)
                throws RemoteException {
            mIControllerCallback.onPlaybackSpeedChanged(eventTimeMs, positionMs, speed);
        }

        @Override
        void onBufferingStateChanged(MediaItem2 item, int bufferingState, long bufferedPositionMs)
                throws RemoteException {
            mIControllerCallback.onBufferingStateChanged(
                    (ParcelImpl) ParcelUtils.toParcelable(item),
                    bufferingState, bufferedPositionMs);
        }

        @Override
        void onSeekCompleted(long eventTimeMs, long positionMs, long seekPositionMs)
                throws RemoteException {
            mIControllerCallback.onSeekCompleted(eventTimeMs, positionMs, seekPositionMs);
        }

        @Override
        void onCurrentMediaItemChanged(MediaItem2 item) throws RemoteException {
            mIControllerCallback.onCurrentMediaItemChanged(
                    (ParcelImpl) ParcelUtils.toParcelable(item));
        }

        @Override
        void onPlaylistChanged(List<MediaItem2> playlist, MediaMetadata2 metadata)
                throws RemoteException {
            ControllerInfo controller = mConnectedControllersManager.getController(
                    getCallbackBinder());
            if (mConnectedControllersManager.isAllowedCommand(controller,
                    SessionCommand2.COMMAND_CODE_PLAYER_GET_PLAYLIST)) {
                mIControllerCallback.onPlaylistChanged(
                        MediaUtils2.convertMediaItem2ListToParcelImplList(playlist),
                        metadata == null ? null : metadata.toBundle());
            } else if (mConnectedControllersManager.isAllowedCommand(controller,
                    SessionCommand2.COMMAND_CODE_PLAYER_GET_PLAYLIST_METADATA)) {
                mIControllerCallback.onPlaylistMetadataChanged(metadata.toBundle());
            }
        }

        @Override
        void onPlaylistMetadataChanged(MediaMetadata2 metadata) throws RemoteException {
            ControllerInfo controller = mConnectedControllersManager.getController(
                    getCallbackBinder());
            if (mConnectedControllersManager.isAllowedCommand(controller,
                    SessionCommand2.COMMAND_CODE_PLAYER_GET_PLAYLIST_METADATA)) {
                mIControllerCallback.onPlaylistMetadataChanged(metadata.toBundle());
            }
        }

        @Override
        void onShuffleModeChanged(int shuffleMode) throws RemoteException {
            mIControllerCallback.onShuffleModeChanged(shuffleMode);
        }

        @Override
        void onRepeatModeChanged(int repeatMode) throws RemoteException {
            mIControllerCallback.onRepeatModeChanged(repeatMode);
        }

        @Override
        void onPlaybackCompleted() throws RemoteException {
            mIControllerCallback.onPlaybackCompleted();
        }

        @Override
        void onRoutesInfoChanged(List<Bundle> routes) throws RemoteException {
            mIControllerCallback.onRoutesInfoChanged(routes);
        }

        @Override
        void onGetLibraryRootDone(Bundle rootHints, String rootMediaId, Bundle rootExtra)
                throws RemoteException {
            mIControllerCallback.onGetLibraryRootDone(rootHints, rootMediaId, rootExtra);
        }

        @Override
        void onChildrenChanged(String parentId, int itemCount, Bundle extras)
                throws RemoteException {
            mIControllerCallback.onChildrenChanged(parentId, itemCount, extras);
        }

        @Override
        void onGetChildrenDone(String parentId, int page, int pageSize, List<MediaItem2> result,
                Bundle extras) throws RemoteException {
            List<ParcelImpl> parcelList = MediaUtils2.convertMediaItem2ListToParcelImplList(result);
            mIControllerCallback.onGetChildrenDone(parentId, page, pageSize, parcelList, extras);
        }

        @Override
        void onGetItemDone(String mediaId, MediaItem2 result) throws RemoteException {
            mIControllerCallback.onGetItemDone(mediaId,
                    (ParcelImpl) ParcelUtils.toParcelable(result));
        }

        @Override
        void onSearchResultChanged(String query, int itemCount, Bundle extras)
                throws RemoteException {
            mIControllerCallback.onSearchResultChanged(query, itemCount, extras);
        }

        @Override
        void onGetSearchResultDone(String query, int page, int pageSize, List<MediaItem2> result,
                Bundle extras) throws RemoteException {
            List<ParcelImpl> parcelList = MediaUtils2.convertMediaItem2ListToParcelImplList(result);
            mIControllerCallback.onGetSearchResultDone(query, page, pageSize, parcelList, extras);
        }

        @Override
        void onDisconnected() throws RemoteException {
            mIControllerCallback.onDisconnected();
        }
    }
}
