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

package androidx.car.cluster.navigation.util;

import static androidx.annotation.RestrictTo.Scope.LIBRARY_GROUP;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.core.util.Preconditions;
import androidx.versionedparcelable.ParcelField;
import androidx.versionedparcelable.VersionedParcelable;
import androidx.versionedparcelable.VersionedParcelize;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * An {@link Enum} wrapper that implements {@link VersionedParcelable} and provides backwards and
 * forward compatibility by allowing the data producer to provide an optional set of "fallback"
 * values. If a value provided by the producer is not known by the data consumer (e.g.: a new value
 * was introduced, but the consumer still is using a older version of the API), then this
 * class would return the first "fallback" value that is known to the consumer.

 * @param <T> Enum type to be wrapped.
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
@VersionedParcelize
public class EnumWrapper<T extends Enum<T>> implements VersionedParcelable {
    @ParcelField(1)
    List<String> mValues = new ArrayList<>();

    /**
     * Used by {@link VersionedParcelable}
     */
    EnumWrapper() {
    }

    @SafeVarargs
    private EnumWrapper(@NonNull T value, @NonNull T ... fallbacks) {
        if (value != null) {
            mValues.add(value.name());
            for (T fallback : fallbacks) {
                mValues.add(fallback.name());
            }
        }
    }

    /**
     * Returns the first value wrapped by this {@link VersionedParcelable} that is known to this
     * consumer, or a default value  if none of the fallback alternatives is known.
     *
     * @param defaultValue Value to return if non of the reported values is known to the consumer.
     */
    public @NonNull T getValue(@NonNull T defaultValue) {
        for (String value : mValues) {
            T result = getEnumByName(defaultValue.getDeclaringClass(), value);
            if (result != null) {
                return result;
            }
        }
        return defaultValue;
    }

    private static <T extends Enum<T>> T getEnumByName(@NonNull Class<T> clazz,
            @NonNull String name) {
        try {
            return Enum.valueOf(clazz, name);
        } catch (IllegalArgumentException ex) {
            return null;
        }
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        EnumWrapper<?> that = (EnumWrapper<?>) o;
        return Objects.equals(mValues, that.mValues);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mValues);
    }

    @Override
    public String toString() {
        return String.format("{values: %s}", mValues);
    }

    /**
     * Convenient method to create an empty {@link EnumWrapper}. Its {@link #getValue(Enum)} will
     * just return the default value passed in.
     */
    public static @NonNull <T extends Enum<T>> EnumWrapper<T> empty() {
        return new EnumWrapper<>();
    }

    /**
     * Wraps the given value and an optional list of fallback values.
     *
     * @param value Value to be wrapped.
     * @param fallbacks An optional list of fallback values, in order of preference, to be used in
     *                  case the consumer of this API doesn't know the value provided. This will be
     *                  used only if {@code value} is not null.
     */
    @SafeVarargs @NonNull
    public static <T extends Enum<T>> EnumWrapper<T> of(@NonNull T value,
            @NonNull T ... fallbacks) {
        return new EnumWrapper<>(Preconditions.checkNotNull(value), fallbacks);
    }
}
