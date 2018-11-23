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

package androidx.navigation;

import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.io.Serializable;

/**
 * NavType denotes the type that can be used in a {@link NavArgument}.
 *
 * <p>There are NavTypes for primitive types, such as int, long, boolean, float, as well as
 * string and parcelable.</p>
 * @param <T> the type of the data that is supported by this NavType
 */
public abstract class NavType<T> {
    private final boolean mAllowsNullable;

    NavType(boolean allowsNullable) {
        this.mAllowsNullable = allowsNullable;
    }

    boolean isAllowsNullable() {
        return mAllowsNullable;
    }

    abstract void put(Bundle bundle, String key, T value);

    /**
     * Get a value of this type from the {@code bundle}
     * @param bundle bundle to get value from
     * @param key bundle key
     * @return value value of this type
     */
    abstract T get(Bundle bundle, String key);

    abstract T parseValue(String value);

    public T parseAndPut(Bundle bundle, String key, String value){
        T parsedValue = parseValue(value);
        put(bundle, key, parsedValue);
        return parsedValue;
    }

    abstract String getName();

    @Override
    public String toString() {
        return getName();
    }

    //TODO: make identical to safeargs version
    @SuppressWarnings("unchecked")
    static NavType<?> fromArgType(@Nullable String type, @Nullable String packageName) {
        if (IntType.getName().equals(type)) {
            return IntType;
        } else if (LongType.getName().equals(type)) {
            return LongType;
        } else if (BoolType.getName().equals(type)) {
            return BoolType;
        } else if (StringType.getName().equals(type)) {
            return StringType;
        } else if (IntType.getName().equals(type) || "reference".equals(type)) {
            return IntType;
        } else if (FloatType.getName().equals(type)) {
            return FloatType;
        } else if (type != null && !type.isEmpty()) {
            try {
                String className;
                if (type.startsWith(".") && packageName != null) {
                    className = packageName + type;
                } else {
                    className = type;
                }
                return new ObjectType(Class.forName(className));
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
            }
        } else if (type == null){
            return StringType;
        }
        return null;
    }

    //TODO: make identical to safeargs version
    static NavType inferFromValue(String value) {
        Long longValue = LongType.parseValue(value);
        if (longValue != null) {
            return LongType;
        }
        Integer intValue = IntType.parseValue(value);
        if (intValue != null) {
            return IntType;
        }
        Float floatValue = FloatType.parseValue(value);
        if (floatValue != null) {
            return FloatType;
        }
        Boolean boolValue = BoolType.parseValue(value);
        if (boolValue != null) {
            return BoolType;
        }
        return StringType;
    }

    //TODO: make identical to safeargs version
    public static final NavType<Integer> IntType = new NavType<Integer>(false) {
        @Override
        void put(Bundle bundle, String key, Integer value) {
            bundle.putInt(key, value);
        }

        @Override
        Integer get(Bundle bundle, String key) {
            return (Integer) bundle.get(key);
        }

        @Override
        Integer parseValue(String value) {
            try {
                return Integer.parseInt(value);
            } catch (NumberFormatException e) {
                return null;
            }
        }

        @Override
        String getName() {
            return "integer";
        }
    };

    //TODO: make identical to safeargs version
    public static final NavType<Long> LongType = new NavType<Long>(false) {
        @Override
        void put(Bundle bundle, String key, Long value) {
            bundle.putLong(key, value);
        }

        @Override
        Long get(Bundle bundle, String key) {
            return (Long) bundle.get(key);
        }

        @Nullable
        Long parseValue(String value) {
            if (!value.endsWith("L")) {
                return null;
            }
            try {
                value = value.substring(0, value.length() - 1);
                if (value.startsWith("0x")) {
                    return Long.parseLong(value.substring(2), 16);
                } else {
                    return Long.parseLong(value);
                }
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        @Override
        String getName() {
            return "long";
        }
    };

    //TODO: make identical to safeargs version
    public static final NavType<Float> FloatType = new NavType<Float>(false) {
        @Override
        void put(Bundle bundle, String key, Float value) {
            bundle.putFloat(key, value);
        }

        @Override
        Float get(Bundle bundle, String key) {
            return (Float) bundle.get(key);
        }

        @Nullable
        Float parseValue(String value) {
            try {
                return Float.parseFloat(value);
            } catch (NumberFormatException ex) {
                return null;
            }
        }

        @Override
        String getName() {
            return "float";
        }
    };

    public static final NavType<Boolean> BoolType = new NavType<Boolean>(false) {
        @Override
        void put(Bundle bundle, String key, Boolean value) {
            bundle.putBoolean(key, value);
        }

        @Override
        Boolean get(Bundle bundle, String key) {
            return (Boolean) bundle.get(key);
        }

        @Override
        Boolean parseValue(String value) {
            if ("true".equals(value)) {
                return true;
            } else if ("false".equals(value)) {
                return false;
            }
            return null;
        }

        @Override
        String getName() {
            return "boolean";
        }
    };

    public static final NavType<String> StringType = new NavType<String>(true) {
        @Override
        void put(Bundle bundle, String key, String value) {
            bundle.putString(key, value);
        }

        @Override
        String get(Bundle bundle, String key) {
            return (String) bundle.get(key);
        }

        @Override
        String parseValue(String value) {
            return value;
        }

        @Override
        String getName() {
            return "string";
        }
    };

    /**
     * Create an instance of ParcelableType for the specific type that you want to use with
     * a @{link NavArgument}.
     * @param <D> the parcelable class
     */
    public static final class ObjectType<D> extends NavType<D> {

        private final String mType;

        public ObjectType(Class<D> type) {
            super(true);
            if (!Parcelable.class.isAssignableFrom(type) || !Serializable.class.isAssignableFrom(type)) {
                throw new IllegalArgumentException(type + " does not implement Parcelable or Serializable.");
            }
            this.mType = type.getName();
        }

        String getType() {
            return mType;
        }

        @Override
        void put(Bundle bundle, String key, D value) {
            if (value instanceof Parcelable) {
                bundle.putParcelable(key, (Parcelable) value);
            } else if (value instanceof Serializable){
                bundle.putSerializable(key, (Serializable) value);
            }
        }

        @SuppressWarnings("unchecked")
        @Override
        D get(Bundle bundle, String key) {
            return (D) bundle.get(key);
        }

        @SuppressWarnings("unchecked")
        @Override
        D parseValue(String value) {
            Class cls = null;
            try {
                cls = Class.forName(mType);
            } catch (ClassNotFoundException e) {
                e.printStackTrace();
                return null;
            }

            if (!cls.isEnum()) {
                return null;
            }

            for (Object constant : cls.getEnumConstants()) {
                if (((Enum) constant).name().equals(value)) {
                    return (D) constant;
                }
            }

            return null;
        }

        @Override
        String getName() {
            return mType;
        }
    }
}
