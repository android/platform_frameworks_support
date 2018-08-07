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

import java.util.Objects;

/**
 * Information regarding a road (or street, highway, etc.). This class might be extended in the
 * future with additional road information.
 */
@VersionedParcelize
public final class Segment implements VersionedParcelable {
    @ParcelField(1)
    String mName = "";

    /**
     * Used by {@link VersionedParcelable}
     *
     * @hide
     */
    @RestrictTo(LIBRARY_GROUP)
    Segment() {
    }

    /**
     * Creates a new segment with the given name
     */
    public Segment(@NonNull String name) {
        mName = Preconditions.checkNotNull(name);
    }

    /**
     * Segment name (e.g.: "Highway 1", "Rengstorff Ave.", "Dyott St.", "Pirrama Rd.") already
     * localized to the current user's language, or empty string if segment name is unknown.
     */
    @NonNull
    public String getName() {
        return mName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Segment segment = (Segment) o;
        return Objects.equals(mName, segment.mName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mName);
    }

    @Override
    public String toString() {
        return String.format("{name: '%s'}", mName);
    }
}
