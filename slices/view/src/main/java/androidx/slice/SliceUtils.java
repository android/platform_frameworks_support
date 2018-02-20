/*
 * Copyright 2017 The Android Open Source Project
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

package androidx.slice;

import static android.app.slice.Slice.HINT_ACTIONS;
import static android.app.slice.Slice.HINT_PARTIAL;
import static android.app.slice.Slice.HINT_SHORTCUT;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_REMOTE_INPUT;
import static android.app.slice.SliceItem.FORMAT_SLICE;

import android.content.Context;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;

import androidx.slice.core.SliceQuery;

/**
 * Utilities for dealing with slices.
 */
public class SliceUtils {

    private SliceUtils() {
    }

    /**
     * Serialize a slice to an OutputStream.
     * <p>
     * The slice can later be read into slice form again with {@link #parseSlice}.
     * Some slice types cannot be serialized, their handling is controlled by
     * {@link SerializeOptions}.
     *
     * @param s The slice to serialize.
     * @param context Context used to load any resources in the slice.
     * @param output The output of the serialization.
     * @param encoding The encoding to use for serialization.
     * @param options Options defining how to handle non-serializable items.
     */
    public static void serializeSlice(@NonNull Slice s, @NonNull Context context,
            @NonNull OutputStream output, @NonNull String encoding,
            @NonNull SerializeOptions options) throws IOException {
        SliceXml.serializeSlice(s, context, output, encoding, options);
    }

    /**
     * Parse a slice that has been previously serialized.
     * <p>
     * Parses a slice that was serialized with {@link #serializeSlice}.
     *
     * @param input The input stream to read from.
     * @param encoding The encoding to read as.
     */
    public static @NonNull Slice parseSlice(@NonNull InputStream input, @NonNull String encoding)
            throws IOException {
        return SliceXml.parseSlice(input, encoding);
    }

    /**
     * Holds options for how to handle SliceItems that cannot be serialized.
     */
    public static class SerializeOptions {
        /**
         * Constant indicating that the an {@link IllegalArgumentException} should be thrown
         * when this format is encountered.
         */
        public static final int MODE_THROW = 0;
        /**
         * Constant indicating that the SliceItem should be removed when this format is encountered.
         */
        public static final int MODE_REMOVE = 1;
        /**
         * Constant indicating that the SliceItem should be serialized as much as possible.
         * <p>
         * For images this means it will be replaced with an empty image. For actions, the
         * action will be removed but the content of the action will be serialized.
         */
        public static final int MODE_DISABLE = 2;

        @IntDef({MODE_THROW, MODE_REMOVE, MODE_DISABLE})
        @interface FormatMode {
        }

        private int mActionMode = MODE_THROW;
        private int mImageMode = MODE_THROW;

        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public void checkThrow(String format) {
            switch (format) {
                case FORMAT_ACTION:
                case FORMAT_REMOTE_INPUT:
                    if (mActionMode != MODE_THROW) return;
                    break;
                case FORMAT_IMAGE:
                    if (mImageMode != MODE_THROW) return;
                    break;
                default:
                    return;
            }
            throw new IllegalArgumentException(format + " cannot be serialized");
        }

        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public @FormatMode int getActionMode() {
            return mActionMode;
        }

        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY)
        public @FormatMode int getImageMode() {
            return mImageMode;
        }

        /**
         * Sets how {@link android.app.slice.SliceItem#FORMAT_ACTION} items should be handled.
         *
         * The default mode is {@link #MODE_THROW}.
         * @param mode The desired mode.
         */
        public SerializeOptions setActionMode(@FormatMode int mode) {
            mActionMode = mode;
            return this;
        }

        /**
         * Sets how {@link android.app.slice.SliceItem#FORMAT_IMAGE} items should be handled.
         *
         * The default mode is {@link #MODE_THROW}.
         * @param mode The desired mode.
         */
        public SerializeOptions setImageMode(@FormatMode int mode) {
            mImageMode = mode;
            return this;
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
            LOADING_ALL, LOADING_PARTIAL, LOADING_COMPLETE
    })
    public @interface SliceLoadingState{}

    /**
     * Indicates this slice is empty and waiting for content to be loaded.
     */
    public static final int LOADING_ALL = 0;
    /**
     * Indicates this slice has some content but is waiting for other content to be loaded.
     */
    public static final int LOADING_PARTIAL = 1;
    /**
     * Indicates this slice has fully loaded and is not waiting for other content.
     */
    public static final int LOADING_COMPLETE = 2;

    /**
     * @return the current loading state of the provided {@link Slice}.
     *
     * @see #LOADING_ALL
     * @see #LOADING_PARTIAL
     * @see #LOADING_COMPLETE
     */
    public static int getLoadingState(@NonNull Slice slice) {
        // Check loading state
        boolean hasHintPartial =
                SliceQuery.find(slice, null, HINT_PARTIAL, null) != null;
        if (hasHintPartial && slice.getItems().size() == 0) {
            // Empty slice
            return LOADING_ALL;
        } else if (hasHintPartial) {
            // Slice with specific content to load
            return LOADING_PARTIAL;
        } else {
            // Full slice
            return LOADING_COMPLETE;
        }
    }

    /**
     * @return the group of actions associated with the provided slice, if they exist.
     */
    @Nullable
    public static List<SliceItem> getSliceActions(@NonNull Slice slice) {
        SliceItem actionGroup = SliceQuery.find(slice, FORMAT_SLICE, HINT_ACTIONS, null);
        String[] hints = new String[] {HINT_ACTIONS, HINT_SHORTCUT};
        return (actionGroup != null)
                ? SliceQuery.findAll(actionGroup, FORMAT_SLICE, hints, null)
                : null;
    }
}
