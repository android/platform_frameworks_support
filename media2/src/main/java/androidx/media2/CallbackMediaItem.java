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

import androidx.annotation.NonNull;
import androidx.versionedparcelable.NonParcelField;
import androidx.versionedparcelable.ParcelUtils;
import androidx.versionedparcelable.VersionedParcelize;

/**
 * Structure for media item descriptor for {@link DataSourceCallback}.
 * <p>
 * Users should use {@link Builder} to create {@link CallbackMediaItem}.
 * <p>
 * You cannot directly send this object across the process through {@link ParcelUtils}. See
 * {@link MediaItem} for detail.
 *
 * @see MediaItem
 * @hide
 */
@VersionedParcelize(isCustom = true)
public class CallbackMediaItem extends MediaItem {
    @NonParcelField
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    DataSourceCallback mDataSourceCallback;

    /**
     * Used for VersionedParcelable
     */
    CallbackMediaItem() {
        // no-op
    }

    CallbackMediaItem(Builder builder) {
        super(builder);
        mDataSourceCallback = builder.mDataSourceCallback;
    }

    /**
     * Return the DataSourceCallback that implements the callback for the data source of this media
     * item.
     *
     * @return the DataSourceCallback that implements the callback for the data source of this
     *         media item,
     */
    public @NonNull DataSourceCallback getDataSourceCallback() {
        return mDataSourceCallback;
    }
}
