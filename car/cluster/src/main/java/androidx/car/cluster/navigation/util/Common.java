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
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Supplier;

/**
 * Helper methods
 *
 * @hide
 */
@RestrictTo(LIBRARY_GROUP)
public class Common {
    /**
     * Returns the given string, or an empty string if the value is null.
     */
    public static String nonNullOrEmpty(@Nullable String value) {
        return nonNullOrDefault(value, String::new);
    }

    /**
     * Returns the given list, or an empty one if the list is null.
     */
    public static <T> List<T> nonNullOrEmpty(@Nullable List<T> value) {
        return nonNullOrDefault(value, ArrayList<T>::new);
    }

    /**
     * Returns the given value, or a default value obtained from the provided {@link Supplier} if
     * the value is null.
     */
    @NonNull
    public static <T> T nonNullOrDefault(@Nullable T value, @NonNull Supplier<T> defaultValue) {
        return value != null ? value : defaultValue.get();
    }
}
