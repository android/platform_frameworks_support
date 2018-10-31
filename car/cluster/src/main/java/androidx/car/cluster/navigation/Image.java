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

package androidx.car.cluster.navigation;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import android.annotation.SuppressLint;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.Objects;

/**
 * Reference to an image. This class encapsulates a 'content://' style URI plus metadata that allows
 * consumers to know the image they will receive and how to handle it.
 * <ul>
 * <li><b>Sizing:</b> All images will have an "original" size which define their aspect ratio.
 * Consumers might should always request a specific size (width and height) based on the available
 * space and the provided aspect ration defined by this "original" size. Producers can optionally
 * select a version of the requested image that most closely matches the requested size.
 * Producers can optionally also resize the image to exactly match the size constraints. Consumers
 * should not assume that the received image will match the requested size. Instead, consumers
 * should assume that the image might require additional scaling.
 * <li><b>Content:</b> Producers should avoid including margins around the image content.
 * <li><b>Format:</b> Content URI must reference a file with MIME type 'image/png', 'image/jpeg'
 * or 'image/bmp' (vector images are not supported).
 * <li><b>Color:</b> Images can be either "tintable" or not. A "tintable" image is such that all its
 * content is defined in its alpha channel, while its color (all other channels) can be altered
 * without loosing information (e.g.: icons). Non "tintable" images contains information in all its
 * channels (e.g.: photos).
 * <li><b>Caching:</b> Given the same image reference and the same requested size, producers must
 * return the exact the same image. This means that it should be safe for the consumer to cache an
 * image once downloaded and use this image reference plus requested size as key.
 * </ul>
 */
@VersionedParcelize
public class Image implements VersionedParcelable {
    private static final String SCHEME = "content://";
    private static final String WIDTH_HINT_PARAMETER = "w";
    private static final String HEIGHT_HINT_PARAMETER = "h";

    @ParcelField(1)
    String mContentUri;
    @ParcelField(2)
    int mOriginalWidth;
    @ParcelField(3)
    int mOriginalHeight;
    @ParcelField(4)
    boolean mIsTintable;

    /**
     * Used by {@link VersionedParcelable}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    Image() {
    }

    /**
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    Image(@NonNull String contentUri, int originalWidth, int originalHeight, boolean isTintable) {
        mContentUri = Preconditions.checkNotNull(contentUri);
        mOriginalWidth = Preconditions.checkArgumentInRange(originalWidth, 1,
                Integer.MAX_VALUE, "originalWidth");
        mOriginalHeight = Preconditions.checkArgumentInRange(originalHeight, 1,
                Integer.MAX_VALUE, "originalHeight");
        mIsTintable = isTintable;
    }

    /**
     * Builder for creating an {@link Image}.
     */
    public static final class Builder {
        String mContentUri;
        int mOriginalWidth;
        int mOriginalHeight;
        boolean mIsTintable;

        /**
         * Sets a 'content://' style URI
         *
         * @return this object for chaining
         * @throws NullPointerException if the provided {@code contentUri} is null
         * @throws IllegalArgumentException if the provided {@code contentUri} doesn't start with
         *                                  'content://'.
         */
        @NonNull
        public Builder setContentUri(@NonNull String contentUri) {
            Preconditions.checkNotNull(contentUri);
            Preconditions.checkArgument(contentUri.startsWith(SCHEME));
            mContentUri = contentUri;
            return this;
        }

        /**
         * Sets the aspect ratio of this image, expressed as with and height sizes. Both dimensions
         * must be greater than 0.
         *
         * @return this object for chaining
         * @throws IllegalArgumentException if any of the dimensions is not positive.
         */
        @NonNull
        public Builder setOriginalSize(int width, int height) {
            Preconditions.checkArgument(width > 0 && height > 0);
            mOriginalWidth = width;
            mOriginalHeight = height;
            return this;
        }

        /**
         * Sets whether this image is "tintable" or not. An image is "tintable" when all its
         * content is defined in its alpha-channel, designed to be colorized (e.g. using
         * {@link android.graphics.PorterDuff.Mode#SRC_ATOP} image composition).
         *
         * @return this object for chaining
         */
        @NonNull
        public Builder setIsTintable(boolean isTintable) {
            mIsTintable = isTintable;
            return this;
        }

        /**
         * Returns a {@link Image} built with the provided information. Calling
         * {@link Image.Builder#setContentUri(String)} and
         * {@link Image.Builder#setOriginalSize(int, int)} before calling this method is mandatory.
         *
         * @return an {@link Image} instance
         * @throws NullPointerException if content URI is not provided.
         * @throws IllegalArgumentException if original size is not set.
         */
        @NonNull
        public Image build() {
            return new Image(mContentUri, mOriginalWidth, mOriginalHeight, mIsTintable);
        }
    }

    /**
     * Returns a 'content://' style URI that can be used to retrieve the actual image, or an empty
     * string if the URI provided by the producer doesn't comply with the format requirements. If
     * this URI is used as-is, the size of the resulting image is undefined.
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    @NonNull
    public String getRawContentUri() {
        String value = Common.nonNullOrEmpty(mContentUri);
        return value.startsWith(SCHEME) ? value : "";
    }

    /**
     * Returns a fully formed {@link Uri} that can be used to retrieve the actual image, including
     * sizing hints, or null if this image reference is not properly formed.
     * <p>
     * Producers can optionally use this hints to provide an optimized version of the image, but the
     * resulting image might still not match the requested size exactly.
     * <p>
     * Consumers must confirm the size of the received image and resize it accordingly if it doesn't
     * match the desired dimensions.
     *
     * @param width desired width (must be greater than 0)
     * @param height desired height (must be greater than 0)
     * @return fully formed {@link Uri}, or null if this image reference can not be used.
     */
    @Nullable
    public Uri getContentUri(int width, int height) {
        Preconditions.checkArgument(width > 0 && height > 0);
        String contentUri = getRawContentUri();
        if (contentUri.isEmpty()) {
            // We have an invalid content URI.
            return null;
        }
        return Uri.parse(contentUri).buildUpon()
                .appendQueryParameter(WIDTH_HINT_PARAMETER, String.valueOf(width))
                .appendQueryParameter(HEIGHT_HINT_PARAMETER, String.valueOf(height))
                .build();
    }

    /**
     * Returns the image width, which should only be used to determine the image aspect ratio.
     */
    public int getOriginalWidth() {
        return mOriginalWidth;
    }

    /**
     * Returns the image height, which should only be used to determine the image aspect ratio.
     */
    public int getOriginalHeight() {
        return mOriginalHeight;
    }

    /**
     * Returns whether this image is "tintable" or not. An image is "tintable" when all its
     * content is defined in its alpha-channel, designed to be colorized (e.g. using
     * {@link android.graphics.PorterDuff.Mode#SRC_ATOP} image composition).
     */
    public boolean isTintable() {
        return mIsTintable;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Image image = (Image) o;
        return Objects.equals(getRawContentUri(), image.getRawContentUri())
                && getOriginalWidth() == image.getOriginalWidth()
                && getOriginalHeight() == image.getOriginalHeight()
                && isTintable() == image.isTintable();
    }

    @Override
    public int hashCode() {
        return Objects.hash(getRawContentUri(), getOriginalWidth(), getOriginalHeight(),
                isTintable());
    }

    // DefaultLocale suppressed as this method is only offered for debugging purposes.
    @SuppressLint("DefaultLocale")
    @Override
    public String toString() {
        return String.format("{contentUri: '%s', originalWidth: %d, originalHeight: %d, "
                        + "isTintable: %s}",
                mContentUri, mOriginalWidth, mOriginalHeight, mIsTintable);
    }
}
