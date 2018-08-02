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

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.Objects;

/**
 * A representation of a time and timezone that can be serialized as a {@link VersionedParcelable}.
 */
@VersionedParcelize
public class Time implements VersionedParcelable {
    @ParcelField(1)
    long mSecondsSinceEpoch;
    @ParcelField(2)
    String mZoneId;

    /**
     * Used by {@link VersionedParcelable}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    Time() {
    }

    /**
     * Creates a {@link Time} that wraps the given {@link ZonedDateTime}
     */
    public Time(@NonNull ZonedDateTime time) {
        mSecondsSinceEpoch = Preconditions.checkNotNull(time).toEpochSecond();
        mZoneId = time.getZone().getId();
    }

    /**
     * A {@link ZonedDateTime} representing the wrapped time and timezone.
     */
    @NonNull
    public ZonedDateTime getTime() {
        return ZonedDateTime.ofInstant(Instant.ofEpochSecond(mSecondsSinceEpoch),
                mZoneId != null ? ZoneId.of(mZoneId) : ZoneId.systemDefault());
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Time time = (Time) o;
        return mSecondsSinceEpoch == time.mSecondsSinceEpoch
                && Objects.equals(mZoneId, time.mZoneId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mSecondsSinceEpoch, mZoneId);
    }

    @Override
    public String toString() {
        return getTime().toString();
    }
}
