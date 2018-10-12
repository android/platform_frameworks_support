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

package androidx.work;

import android.arch.persistence.room.ColumnInfo;
import android.net.Uri;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RequiresApi;
import android.support.annotation.RestrictTo;

/**
 * The constraints that can be applied to one {@link WorkRequest}.
 */
public final class Constraints {

    public static final Constraints NONE = new Constraints.Builder().build();

    // TODO(sumir): Need to make this @NonNull, but that requires a db migration.
    @ColumnInfo(name = "required_network_type")
    private NetworkType mRequiredNetworkType;

    @ColumnInfo(name = "requires_charging")
    private boolean mRequiresCharging;

    @ColumnInfo(name = "requires_device_idle")
    private boolean mRequiresDeviceIdle;

    @ColumnInfo(name = "requires_battery_not_low")
    private boolean mRequiresBatteryNotLow;

    @ColumnInfo(name = "requires_storage_not_low")
    private boolean mRequiresStorageNotLow;

    @ColumnInfo(name = "content_uri_triggers")
    private @Nullable ContentUriTriggers mContentUriTriggers;

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public Constraints() { // stub required for room
    }

    Constraints(Builder builder) {
        mRequiresCharging = builder.mRequiresCharging;
        mRequiresDeviceIdle = Build.VERSION.SDK_INT >= 23 && builder.mRequiresDeviceIdle;
        mRequiredNetworkType = builder.mRequiredNetworkType;
        mRequiresBatteryNotLow = builder.mRequiresBatteryNotLow;
        mRequiresStorageNotLow = builder.mRequiresStorageNotLow;
        mContentUriTriggers = (Build.VERSION.SDK_INT >= 24)
                ? builder.mContentUriTriggers
                : new ContentUriTriggers();
    }

    public Constraints(@NonNull Constraints other) {
        mRequiresCharging = other.mRequiresCharging;
        mRequiresDeviceIdle = other.mRequiresDeviceIdle;
        mRequiredNetworkType = other.mRequiredNetworkType;
        mRequiresBatteryNotLow = other.mRequiresBatteryNotLow;
        mRequiresStorageNotLow = other.mRequiresStorageNotLow;
        mContentUriTriggers = other.mContentUriTriggers;
    }

    public @NonNull NetworkType getRequiredNetworkType() {
        return mRequiredNetworkType;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void setRequiredNetworkType(@NonNull NetworkType requiredNetworkType) {
        mRequiredNetworkType = requiredNetworkType;
    }

    /**
     * @return If the constraints require charging.
     */
    public boolean requiresCharging() {
        return mRequiresCharging;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void setRequiresCharging(boolean requiresCharging) {
        mRequiresCharging = requiresCharging;
    }

    /**
     * @return If the constraints require device idle.
     */
    @RequiresApi(23)
    public boolean requiresDeviceIdle() {
        return mRequiresDeviceIdle;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @RequiresApi(23)
    public void setRequiresDeviceIdle(boolean requiresDeviceIdle) {
        mRequiresDeviceIdle = requiresDeviceIdle;
    }

    /**
     * @return If the constraints require battery not low status.
     */
    public boolean requiresBatteryNotLow() {
        return mRequiresBatteryNotLow;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void setRequiresBatteryNotLow(boolean requiresBatteryNotLow) {
        mRequiresBatteryNotLow = requiresBatteryNotLow;
    }

    /**
     * @return If the constraints require storage not low status.
     */
    public boolean requiresStorageNotLow() {
        return mRequiresStorageNotLow;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public void setRequiresStorageNotLow(boolean requiresStorageNotLow) {
        mRequiresStorageNotLow = requiresStorageNotLow;
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @RequiresApi(24)
    public void setContentUriTriggers(@Nullable ContentUriTriggers mContentUriTriggers) {
        this.mContentUriTriggers = mContentUriTriggers;
    }

    @RequiresApi(24)
    public @Nullable ContentUriTriggers getContentUriTriggers() {
        return mContentUriTriggers;
    }

    /**
     * @return {@code true} if {@link ContentUriTriggers} is not empty
     */
    @RequiresApi(24)
    public boolean hasContentUriTriggers() {
        return mContentUriTriggers != null && mContentUriTriggers.size() > 0;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Constraints other = (Constraints) o;
        return mRequiredNetworkType == other.mRequiredNetworkType
                && mRequiresCharging == other.mRequiresCharging
                && mRequiresDeviceIdle == other.mRequiresDeviceIdle
                && mRequiresBatteryNotLow == other.mRequiresBatteryNotLow
                && mRequiresStorageNotLow == other.mRequiresStorageNotLow
                && (mContentUriTriggers != null ? mContentUriTriggers.equals(
                        other.mContentUriTriggers) : other.mContentUriTriggers == null);
    }

    @Override
    public int hashCode() {
        int result = mRequiredNetworkType.hashCode();
        result = 31 * result + (mRequiresCharging ? 1 : 0);
        result = 31 * result + (mRequiresDeviceIdle ? 1 : 0);
        result = 31 * result + (mRequiresBatteryNotLow ? 1 : 0);
        result = 31 * result + (mRequiresStorageNotLow ? 1 : 0);
        result = 31 * result + (mContentUriTriggers != null ? mContentUriTriggers.hashCode() : 0);
        return result;
    }

    /**
     * Builder for {@link Constraints} class.
     */
    public static final class Builder {
        boolean mRequiresCharging = false;
        boolean mRequiresDeviceIdle = false;
        NetworkType mRequiredNetworkType = NetworkType.NOT_REQUIRED;
        boolean mRequiresBatteryNotLow = false;
        boolean mRequiresStorageNotLow = false;
        ContentUriTriggers mContentUriTriggers = new ContentUriTriggers();

        /**
         * Specify whether device should be plugged in for {@link WorkRequest} to run.
         * Default is false.
         *
         * @param requiresCharging true if device must be plugged in, false otherwise
         * @return current builder
         */
        public @NonNull Builder setRequiresCharging(boolean requiresCharging) {
            this.mRequiresCharging = requiresCharging;
            return this;
        }

        /**
         * Specify whether device should be idle for {@link WorkRequest} to run. Default is
         * false.
         *
         * @param requiresDeviceIdle true if device must be idle, false otherwise
         * @return current builder
         */
        @RequiresApi(23)
        public @NonNull Builder setRequiresDeviceIdle(boolean requiresDeviceIdle) {
            this.mRequiresDeviceIdle = requiresDeviceIdle;
            return this;
        }

        /**
         * Specify whether device should have a particular {@link NetworkType} for
         * {@link WorkRequest} to run. Default is {@link NetworkType#NOT_REQUIRED}.
         *
         * @param networkType type of network required
         * @return current builder
         */
        public @NonNull Builder setRequiredNetworkType(@NonNull NetworkType networkType) {
            this.mRequiredNetworkType = networkType;
            return this;
        }

        /**
         * Specify whether device battery should not be below critical threshold for
         * {@link WorkRequest} to run. Default is false.
         *
         * @param requiresBatteryNotLow true if battery should not be below critical threshold,
         *                              false otherwise
         * @return current builder
         */
        public @NonNull Builder setRequiresBatteryNotLow(boolean requiresBatteryNotLow) {
            this.mRequiresBatteryNotLow = requiresBatteryNotLow;
            return this;
        }

        /**
         * Specify whether device available storage should not be below critical threshold for
         * {@link WorkRequest} to run. Default is {@code false}.
         *
         * @param requiresStorageNotLow true if available storage should not be below critical
         *                              threshold, false otherwise
         * @return current builder
         */
        public @NonNull Builder setRequiresStorageNotLow(boolean requiresStorageNotLow) {
            this.mRequiresStorageNotLow = requiresStorageNotLow;
            return this;
        }

        /**
         * Specify whether {@link WorkRequest} should run when a content: URI is updated.  This
         * functionality is identical to the one found in {@code JobScheduler} and is described in
         * {@code JobInfo.Builder#addTriggerContentUri(android.app.job.JobInfo.TriggerContentUri)}.
         *
         * @param uri The content: URI to observe
         * @param triggerForDescendants {@code true} if any changes in descendants cause this
         *                              {@link WorkRequest} to run
         * @return The current {@link Builder}
         */
        @RequiresApi(24)
        public @NonNull Builder addContentUriTrigger(Uri uri, boolean triggerForDescendants) {
            mContentUriTriggers.add(uri, triggerForDescendants);
            return this;
        }

        /**
         * Generates the {@link Constraints} from this Builder.
         *
         * @return new {@link Constraints} which can be attached to a {@link WorkRequest}
         */
        public @NonNull Constraints build() {
            return new Constraints(this);
        }
    }
}
