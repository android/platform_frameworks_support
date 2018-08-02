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

import android.os.Parcelable;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.car.cluster.navigation.util.Common;
import androidx.car.cluster.navigation.util.EnumWrapper;
import androidx.core.util.Preconditions;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.ParcelUtils;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.List;
import java.util.Objects;

/**
 * Navigation state data to be displayed on the instrument cluster of a car. This is composed by:
 * <ul>
 * <li>a mode (e.g.: whether the navigation state provider is producing turn-by-turn guidance or
 * not)
 * <li>a service status (e.g.: whether the navigation state is accurate, or there is any anomaly
 * affecting the provider).
 * <li>optional data associated with that mode (e.g: list of navigation steps).
 * </ul>
 */
@VersionedParcelize
public final class NavigationState implements VersionedParcelable {
    /**
     * Possible navigation modes
     */
    public enum NavigationMode {
        /**
         * Navigation is currently not giving any sort of guidance.
         */
        NOT_NAVIGATING,
        /**
         * The user is being given turn-by-turn guidance.
         */
        TURN_BY_TURN,
    }

    @ParcelField(1)
    ProducerStatus mStatus;
    @ParcelField(2)
    EnumWrapper<NavigationMode> mMode;
    @ParcelField(3)
    List<Step> mSteps;
    @ParcelField(4)
    Segment mSegment;
    @ParcelField(5)
    List<Destination> mDestinations;

    /**
     * Creates a default {@link NavigationState}
     */
    public NavigationState() {
    }

    /**
     * @see #getStatus()
     */
    public NavigationState setStatus(@NonNull ProducerStatus status) {
        mStatus = Preconditions.checkNotNull(status);
        return this;
    }

    /**
     * @see #getMode()
     */
    public NavigationState setMode(@NonNull NavigationMode mode) {
        mMode = EnumWrapper.of(mode);
        return this;
    }

    /**
     * @see #getSteps()
     */
    public NavigationState setSteps(@NonNull List<Step> steps) {
        mSteps = Preconditions.checkNotNull(steps);
        return this;
    }

    /**
     * @see #getSegment()
     */
    public NavigationState setSegment(@Nullable Segment segment) {
        mSegment = segment;
        return this;
    }

    /**
     * @see #getDestinations()
     */
    public NavigationState setDestinations(@NonNull List<Destination> destinations) {
        mDestinations = Preconditions.checkNotNull(destinations);
        return this;
    }

    /**
     * Navigation service status.
     */
    @NonNull
    public ProducerStatus getStatus() {
        return Common.nonNullOrDefault(mStatus, () ->
                new ProducerStatus(ProducerStatus.Code.OK, ""));
    }

    /**
     * Current navigation mode.
     */
    @NonNull
    public NavigationMode getMode() {
        return EnumWrapper.getValue(mMode, NavigationMode.NOT_NAVIGATING);
    }

    /**
     * Navigation steps, in order of execution. It is up to the producer to decide how many
     * steps in advance will be provided.
     */
    @NonNull
    public List<Step> getSteps() {
        return Common.nonNullOrEmpty(mSteps);
    }

    /**
     * Current segment being driven, or null if the segment being driven is unknown.
     */
    @Nullable
    public Segment getSegment() {
        return mSegment;
    }

    /**
     * Destination and intermediate stops in the navigation, sorted from nearest to furthest.
     */
    @NonNull
    public List<Destination> getDestinations() {
        return Common.nonNullOrEmpty(mDestinations);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        NavigationState that = (NavigationState) o;
        return Objects.equals(mStatus, that.mStatus)
                && Objects.equals(mMode, that.mMode)
                && Objects.equals(mSteps, that.mSteps)
                && Objects.equals(mSegment, that.mSegment)
                && Objects.equals(mDestinations, that.mDestinations);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mStatus, mMode, mSteps, mSegment, mDestinations);
    }

    @Override
    public String toString() {
        return String.format("{status: %s, mode: %s, steps: %s, segment: %s, destinations: %s}",
                mStatus, mMode, mSteps, mSegment, mDestinations);
    }

    /**
     * Returns this {@link NavigationState} as a {@link Parcelable}
     */
    @NonNull
    public Parcelable toParcelable() {
        return ParcelUtils.toParcelable(this);
    }

    /**
     * Creates a {@link NavigationState} based on data stored in the given {@link Parcelable}
     */
    public static NavigationState fromParcelable(@Nullable Parcelable parcelable) {
        return parcelable != null ? ParcelUtils.fromParcelable(parcelable) : new NavigationState();
    }
}
