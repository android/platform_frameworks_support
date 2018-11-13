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

import android.os.Bundle;
import android.net.Uri;

import androidx.media2.IMediaController2;
import androidx.media2.ParcelImplListSlice;
import androidx.versionedparcelable.ParcelImpl;

/**
 * Interface from MediaController2 to MediaSession2.
 * <p>
 * Keep this interface oneway. Otherwise a malicious app may implement fake version of this,
 * and holds calls from session to make session owner(s) frozen.
 * @hide
 */
oneway interface IMediaSession2 {
    void connect(IMediaController2 caller, int seq, String callingPackage) = 0;
    void release(IMediaController2 caller, int seq) = 1;

    void setVolumeTo(IMediaController2 caller, int seq, int value, int flags) = 2;
    void adjustVolume(IMediaController2 caller, int seq, int direction, int flags) = 3;

    void play(IMediaController2 caller, int seq) = 4;
    void pause(IMediaController2 caller, int seq) = 5;
    void prepare(IMediaController2 caller, int seq) = 6;
    void fastForward(IMediaController2 caller, int seq) = 7;
    void rewind(IMediaController2 caller, int seq) = 8;
    void skipForward(IMediaController2 caller, int seq) = 9;
    void skipBackward(IMediaController2 caller, int seq) = 10;
    void seekTo(IMediaController2 caller, int seq, long pos) = 11;
    void onCustomCommand(IMediaController2 caller, int seq, in ParcelImpl sessionCommand2,
            in Bundle args) = 12;
    void prepareFromUri(IMediaController2 caller, int seq, in Uri uri, in Bundle extras) = 13;
    void prepareFromSearch(IMediaController2 caller, int seq, String query, in Bundle extras) = 14;
    void prepareFromMediaId(IMediaController2 caller, int seq, String mediaId,
            in Bundle extras) = 15;
    void playFromUri(IMediaController2 caller, int seq, in Uri uri, in Bundle extras) = 16;
    void playFromSearch(IMediaController2 caller, int seq, String query, in Bundle extras) = 17;
    void playFromMediaId(IMediaController2 caller, int seq, String mediaId, in Bundle extras) = 18;
    void setRating(IMediaController2 caller, int seq, String mediaId, in ParcelImpl rating2) = 19;
    void setPlaybackSpeed(IMediaController2 caller, int seq, float speed) = 20;

    void setPlaylist(IMediaController2 caller, int seq, in List<String> list,
            in ParcelImpl metadata) = 21;
    void setMediaItem(IMediaController2 caller, int seq, String mediaId) = 22;
    void updatePlaylistMetadata(IMediaController2 caller, int seq, in ParcelImpl metadata) = 23;
    void addPlaylistItem(IMediaController2 caller, int seq, int index, String mediaId) = 24;
    void removePlaylistItem(IMediaController2 caller, int seq, int index) = 25;
    void replacePlaylistItem(IMediaController2 caller, int seq, int index, String mediaId) = 26;
    void skipToPlaylistItem(IMediaController2 caller, int seq, int index) = 27;
    void skipToPreviousItem(IMediaController2 caller, int seq) = 28;
    void skipToNextItem(IMediaController2 caller, int seq) = 29;
    void setRepeatMode(IMediaController2 caller, int seq, int repeatMode) = 30;
    void setShuffleMode(IMediaController2 caller, int seq, int shuffleMode) = 31;

    void onControllerResult(IMediaController2 caller, int seq,
            in ParcelImpl controllerResult) = 32;

    //////////////////////////////////////////////////////////////////////////////////////////////
    // library service specific
    //////////////////////////////////////////////////////////////////////////////////////////////
    void getLibraryRoot(IMediaController2 caller, int seq, in ParcelImpl libraryParams) = 33;
    void getItem(IMediaController2 caller, int seq, String mediaId) = 34;
    void getChildren(IMediaController2 caller, int seq, String parentId, int page, int pageSize,
            in ParcelImpl libraryParams) = 35;
    void search(IMediaController2 caller, int seq, String query, in ParcelImpl libraryParams) = 36;
    void getSearchResult(IMediaController2 caller, int seq, String query, int page, int pageSize,
            in ParcelImpl libraryParams) = 37;
    void subscribe(IMediaController2 caller, int seq, String parentId,
            in ParcelImpl libraryParams) = 38;
    void unsubscribe(IMediaController2 caller, int seq, String parentId) = 39;
    // Next Id : 40
}
