/*
 * Copyright (C) 2016 The Android Open Source Project
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

package androidx.room;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a field in an {@link Entity} as the primary key.
 * <p>
 * If you would like to define a composite primary key, you should use {@link Entity#primaryKeys()}
 * method.
 * <p>
 * Each {@link Entity} must declare a primary key unless one of its super classes declares a
 * primary key. If both an {@link Entity} and its super class defines a {@code PrimaryKey}, the
 * child's {@code PrimaryKey} definition will override the parent's {@code PrimaryKey}.
 * <p>
 * If {@code PrimaryKey} annotation is used on a {@link Embedded}d field, all columns inherited
 * from that embedded field becomes the composite primary key (including its grand children
 * fields).
 */
@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.CLASS) // For Room to be incremental, this can't be SOURCE.
public @interface PrimaryKey {
    /**
     * Set to true to let SQLite generate the unique id.
     * <p>
     * When set to {@code true}, the SQLite type affinity for the field should be {@code INTEGER}.
     * <p>
     * If the field type is {@code long} or {@code int} (or its TypeConverter converts it to a
     * {@code long} or {@code int}), {@link Insert} methods treat {@code 0} as not-set while
     * inserting the item.
     * <p>
     * If the field's type is {@link Integer} or {@link Long} (or its TypeConverter converts it to
     * an {@link Integer} or a {@link Long}), {@link Insert} methods treat {@code null} as
     * not-set while inserting the item.
     *
     * @return Whether the primary key should be auto-generated by SQLite or not. Defaults
     * to false.
     */
    boolean autoGenerate() default false;
}
