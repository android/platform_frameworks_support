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

import android.arch.persistence.room.TypeConverter;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.annotation.RestrictTo;
import android.support.annotation.VisibleForTesting;
import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * A persistable set of key/value pairs which are used as inputs and outputs for
 * {@link ListenableWorker}s.  Keys are Strings, and values can be Strings, primitive types, or
 * their array variants.
 * <p>
 * This is a lightweight container, and should not be considered your data store.  As such, there is
 * an enforced {@link #MAX_DATA_BYTES} limit on the serialized (byte array) size of the payloads.
 * This class will throw {@link IllegalStateException}s if you try to serialize or deserialize past
 * this limit.
 */

public final class Data {

    private static final String TAG = Logger.tagWithPrefix("Data");

    /**
     * An empty Data object with no elements.
     */
    public static final Data EMPTY = new Data.Builder().build();

    /**
     * The maximum number of bytes for Data when it is serialized (converted to a byte array).
     * Please see the class-level Javadoc for more information.
     */
    public static final int MAX_DATA_BYTES = 10 * 1024;    // 10KB

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    Map<String, Object> mValues;

    Data() {    // stub required for room
    }

    public Data(@NonNull Data other) {
        mValues = new HashMap<>(other.mValues);
    }

    Data(@NonNull Map<String, ?> values) {
        mValues = new HashMap<>(values);
    }

    /**
     * Gets the boolean value for the given key.
     *
     * @param key The key for the argument
     * @param defaultValue The default value to return if the key is not found
     * @return The value specified by the key if it exists; the default value otherwise
     */
    public boolean getBoolean(@NonNull String key, boolean defaultValue) {
        Object value = mValues.get(key);
        if (value instanceof Boolean) {
            return (boolean) value;
        } else {
            return defaultValue;
        }
    }

    /**
     * Gets the boolean array value for the given key.
     *
     * @param key The key for the argument
     * @return The value specified by the key if it exists; {@code null} otherwise
     */
    public @Nullable boolean[] getBooleanArray(@NonNull String key) {
        Object value = mValues.get(key);
        if (value instanceof Boolean[]) {
            Boolean[] array = (Boolean[]) value;
            boolean[] returnArray = new boolean[array.length];
            for (int i = 0; i < array.length; ++i) {
                returnArray[i] = array[i];
            }
            return returnArray;
        } else {
            return null;
        }
    }

    /**
     * Gets the byte value for the given key.
     *
     * @param key The key for the argument
     * @param defaultValue The default value to return if the key is not found
     * @return The value specified by the key if it exists; the default value otherwise
     */
    public byte getByte(@NonNull String key, byte defaultValue) {
        Object value = mValues.get(key);
        if (value instanceof Byte) {
            return (byte) value;
        } else {
            return defaultValue;
        }
    }

    /**
     * Gets the byte array value for the given key.
     *
     * @param key The key for the argument
     * @return The value specified by the key if it exists; {@code null} otherwise
     */
    public @Nullable byte[] getByteArray(@NonNull String key) {
        Object value = mValues.get(key);
        if (value instanceof Byte[]) {
            Byte[] array = (Byte[]) value;
            byte[] returnArray = new byte[array.length];
            for (int i = 0; i < array.length; ++i) {
                returnArray[i] = array[i];
            }
            return returnArray;
        } else {
            return null;
        }
    }

    /**
     * Gets the integer value for the given key.
     *
     * @param key The key for the argument
     * @param defaultValue The default value to return if the key is not found
     * @return The value specified by the key if it exists; the default value otherwise
     */
    public int getInt(@NonNull String key, int defaultValue) {
        Object value = mValues.get(key);
        if (value instanceof Integer) {
            return (int) value;
        } else {
            return defaultValue;
        }
    }

    /**
     * Gets the integer array value for the given key.
     *
     * @param key The key for the argument
     * @return The value specified by the key if it exists; {@code null} otherwise
     */
    public @Nullable int[] getIntArray(@NonNull String key) {
        Object value = mValues.get(key);
        if (value instanceof Integer[]) {
            Integer[] array = (Integer[]) value;
            int[] returnArray = new int[array.length];
            for (int i = 0; i < array.length; ++i) {
                returnArray[i] = array[i];
            }
            return returnArray;
        } else {
            return null;
        }
    }

    /**
     * Gets the long value for the given key.
     *
     * @param key The key for the argument
     * @param defaultValue The default value to return if the key is not found
     * @return The value specified by the key if it exists; the default value otherwise
     */
    public long getLong(@NonNull String key, long defaultValue) {
        Object value = mValues.get(key);
        if (value instanceof Long) {
            return (long) value;
        } else {
            return defaultValue;
        }
    }

    /**
     * Gets the long array value for the given key.
     *
     * @param key The key for the argument
     * @return The value specified by the key if it exists; {@code null} otherwise
     */
    public @Nullable long[] getLongArray(@NonNull String key) {
        Object value = mValues.get(key);
        if (value instanceof Long[]) {
            Long[] array = (Long[]) value;
            long[] returnArray = new long[array.length];
            for (int i = 0; i < array.length; ++i) {
                returnArray[i] = array[i];
            }
            return returnArray;
        } else {
            return null;
        }
    }

    /**
     * Gets the float value for the given key.
     *
     * @param key The key for the argument
     * @param defaultValue The default value to return if the key is not found
     * @return The value specified by the key if it exists; the default value otherwise
     */
    public float getFloat(@NonNull String key, float defaultValue) {
        Object value = mValues.get(key);
        if (value instanceof Float) {
            return (float) value;
        } else {
            return defaultValue;
        }
    }

    /**
     * Gets the float array value for the given key.
     *
     * @param key The key for the argument
     * @return The value specified by the key if it exists; {@code null} otherwise
     */
    public @Nullable float[] getFloatArray(@NonNull String key) {
        Object value = mValues.get(key);
        if (value instanceof Float[]) {
            Float[] array = (Float[]) value;
            float[] returnArray = new float[array.length];
            for (int i = 0; i < array.length; ++i) {
                returnArray[i] = array[i];
            }
            return returnArray;
        } else {
            return null;
        }
    }

    /**
     * Gets the double value for the given key.
     *
     * @param key The key for the argument
     * @param defaultValue The default value to return if the key is not found
     * @return The value specified by the key if it exists; the default value otherwise
     */
    public double getDouble(@NonNull String key, double defaultValue) {
        Object value = mValues.get(key);
        if (value instanceof Double) {
            return (double) value;
        } else {
            return defaultValue;
        }
    }

    /**
     * Gets the double array value for the given key.
     *
     * @param key The key for the argument
     * @return The value specified by the key if it exists; {@code null} otherwise
     */
    public @Nullable double[] getDoubleArray(@NonNull String key) {
        Object value = mValues.get(key);
        if (value instanceof Double[]) {
            Double[] array = (Double[]) value;
            double[] returnArray = new double[array.length];
            for (int i = 0; i < array.length; ++i) {
                returnArray[i] = array[i];
            }
            return returnArray;
        } else {
            return null;
        }
    }

    /**
     * Gets the String value for the given key.
     *
     * @param key The key for the argument
     * @return The value specified by the key if it exists; the default value otherwise
     */
    public @Nullable String getString(@NonNull String key) {
        Object value = mValues.get(key);
        if (value instanceof String) {
            return (String) value;
        } else {
            return null;
        }
    }

    /**
     * Gets the String array value for the given key.
     *
     * @param key The key for the argument
     * @return The value specified by the key if it exists; {@code null} otherwise
     */
    public @Nullable String[] getStringArray(@NonNull String key) {
        Object value = mValues.get(key);
        if (value instanceof String[]) {
            return (String[]) value;
        } else {
            return null;
        }
    }

    /**
     * Gets all the values in this Data object.
     *
     * @return A {@link Map} of key-value pairs for this object; this Map is unmodifiable and should
     * be used for reads only.
     */
    public @NonNull Map<String, Object> getKeyValueMap() {
        return Collections.unmodifiableMap(mValues);
    }

    /**
     * @return The number of elements in this Data object.
     * @hide
     */
    @VisibleForTesting
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public int size() {
        return mValues.size();
    }

    /**
     * Converts {@link Data} to a byte array for persistent storage.
     *
     * @param data The {@link Data} object to convert
     * @return The byte array representation of the input
     * @throws IllegalStateException if the serialized payload is bigger than
     *         {@link #MAX_DATA_BYTES}
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @TypeConverter
    public static @NonNull byte[] toByteArray(@NonNull Data data) throws IllegalStateException {
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
        ObjectOutputStream objectOutputStream = null;
        try {
            objectOutputStream = new ObjectOutputStream(outputStream);
            objectOutputStream.writeInt(data.size());
            for (Map.Entry<String, Object> entry : data.mValues.entrySet()) {
                objectOutputStream.writeUTF(entry.getKey());
                objectOutputStream.writeObject(entry.getValue());
            }
        } catch (IOException e) {
            Log.e(TAG, "Error in Data#toByteArray: ", e);
            return outputStream.toByteArray();
        } finally {
            if (objectOutputStream != null) {
                try {
                    // NOTE: this writes something to the output stream for bookkeeping purposes.
                    // Don't get the byteArray before we do this!
                    objectOutputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error in Data#toByteArray: ", e);
                }
            }
            try {
                outputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Error in Data#toByteArray: ", e);
            }
        }

        if (outputStream.size() > MAX_DATA_BYTES) {
            throw new IllegalStateException(
                    "Data cannot occupy more than " + MAX_DATA_BYTES
                            + " bytes when serialized");
        }
        return outputStream.toByteArray();
    }

    /**
     * Converts a byte array to {@link Data}.
     *
     * @param bytes The byte array representation to convert
     * @return An {@link Data} object built from the input
     * @throws IllegalStateException if bytes is bigger than {@link #MAX_DATA_BYTES}
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @TypeConverter
    public static @NonNull Data fromByteArray(@NonNull byte[] bytes) throws IllegalStateException {
        if (bytes.length > MAX_DATA_BYTES) {
            throw new IllegalStateException(
                    "Data cannot occupy more than " + MAX_DATA_BYTES + " bytes when serialized");
        }

        Map<String, Object> map = new HashMap<>();
        ByteArrayInputStream inputStream = new ByteArrayInputStream(bytes);
        ObjectInputStream objectInputStream = null;
        try {
            objectInputStream = new ObjectInputStream(inputStream);
            for (int i = objectInputStream.readInt(); i > 0; i--) {
                map.put(objectInputStream.readUTF(), objectInputStream.readObject());
            }
        } catch (IOException | ClassNotFoundException e) {
            Log.e(TAG, "Error in Data#fromByteArray: ", e);
        } finally {
            if (objectInputStream != null) {
                try {
                    objectInputStream.close();
                } catch (IOException e) {
                    Log.e(TAG, "Error in Data#fromByteArray: ", e);
                }
            }
            try {
                inputStream.close();
            } catch (IOException e) {
                Log.e(TAG, "Error in Data#fromByteArray: ", e);
            }
        }
        return new Data(map);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Data other = (Data) o;
        return mValues.equals(other.mValues);
    }

    @Override
    public int hashCode() {
        return 31 * mValues.hashCode();
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static @NonNull Boolean[] convertPrimitiveBooleanArray(@NonNull boolean[] value) {
        Boolean[] returnValue = new Boolean[value.length];
        for (int i = 0; i < value.length; ++i) {
            returnValue[i] = value[i];
        }
        return returnValue;
    }

    static @NonNull Byte[] convertPrimitiveByteArray(@NonNull byte[] value) {
        Byte[] returnValue = new Byte[value.length];
        for (int i = 0; i < value.length; ++i) {
            returnValue[i] = value[i];
        }
        return returnValue;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static @NonNull Integer[] convertPrimitiveIntArray(@NonNull int[] value) {
        Integer[] returnValue = new Integer[value.length];
        for (int i = 0; i < value.length; ++i) {
            returnValue[i] = value[i];
        }
        return returnValue;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static @NonNull Long[] convertPrimitiveLongArray(@NonNull long[] value) {
        Long[] returnValue = new Long[value.length];
        for (int i = 0; i < value.length; ++i) {
            returnValue[i] = value[i];
        }
        return returnValue;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static @NonNull Float[] convertPrimitiveFloatArray(@NonNull float[] value) {
        Float[] returnValue = new Float[value.length];
        for (int i = 0; i < value.length; ++i) {
            returnValue[i] = value[i];
        }
        return returnValue;
    }

    @SuppressWarnings("WeakerAccess") /* synthetic access */
    static @NonNull Double[] convertPrimitiveDoubleArray(@NonNull double[] value) {
        Double[] returnValue = new Double[value.length];
        for (int i = 0; i < value.length; ++i) {
            returnValue[i] = value[i];
        }
        return returnValue;
    }

    /**
     * A builder for {@link Data} objects.
     */
    public static final class Builder {

        private Map<String, Object> mValues = new HashMap<>();

        /**
         * Puts a boolean into the arguments.
         *
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The {@link Builder}
         */
        public @NonNull Builder putBoolean(@NonNull String key, boolean value) {
            mValues.put(key, value);
            return this;
        }

        /**
         * Puts a boolean array into the arguments.
         *
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The {@link Builder}
         */
        public @NonNull Builder putBooleanArray(@NonNull String key, @NonNull boolean[] value) {
            mValues.put(key, convertPrimitiveBooleanArray(value));
            return this;
        }

        /**
         * Puts an byte into the arguments.
         *
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The {@link Builder}
         */
        public @NonNull Builder putByte(@NonNull String key, byte value) {
            mValues.put(key, value);
            return this;
        }

        /**
         * Puts an integer array into the arguments.
         *
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The {@link Builder}
         */
        public @NonNull Builder putByteArray(@NonNull String key, @NonNull byte[] value) {
            mValues.put(key, convertPrimitiveByteArray(value));
            return this;
        }

        /**
         * Puts an integer into the arguments.
         *
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The {@link Builder}
         */
        public @NonNull Builder putInt(@NonNull String key, int value) {
            mValues.put(key, value);
            return this;
        }

        /**
         * Puts an integer array into the arguments.
         *
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The {@link Builder}
         */
        public @NonNull Builder putIntArray(@NonNull String key, @NonNull int[] value) {
            mValues.put(key, convertPrimitiveIntArray(value));
            return this;
        }

        /**
         * Puts a long into the arguments.
         *
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The {@link Builder}
         */
        public @NonNull Builder putLong(@NonNull String key, long value) {
            mValues.put(key, value);
            return this;
        }

        /**
         * Puts a long array into the arguments.
         *
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The {@link Builder}
         */
        public @NonNull Builder putLongArray(@NonNull String key, @NonNull long[] value) {
            mValues.put(key, convertPrimitiveLongArray(value));
            return this;
        }

        /**
         * Puts a float into the arguments.
         *
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The {@link Builder}
         */
        public @NonNull Builder putFloat(@NonNull String key, float value) {
            mValues.put(key, value);
            return this;
        }

        /**
         * Puts a float array into the arguments.
         *
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The {@link Builder}
         */
        public @NonNull Builder putFloatArray(@NonNull String key, @NonNull float[] value) {
            mValues.put(key, convertPrimitiveFloatArray(value));
            return this;
        }

        /**
         * Puts a double into the arguments.
         *
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The {@link Builder}
         */
        public @NonNull Builder putDouble(@NonNull String key, double value) {
            mValues.put(key, value);
            return this;
        }

        /**
         * Puts a double array into the arguments.
         *
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The {@link Builder}
         */
        public @NonNull Builder putDoubleArray(@NonNull String key, @NonNull double[] value) {
            mValues.put(key, convertPrimitiveDoubleArray(value));
            return this;
        }

        /**
         * Puts a String into the arguments.
         *
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The {@link Builder}
         */
        public @NonNull Builder putString(@NonNull String key, @Nullable String value) {
            mValues.put(key, value);
            return this;
        }

        /**
         * Puts a String array into the arguments.
         *
         * @param key The key for this argument
         * @param value The value for this argument
         * @return The {@link Builder}
         */
        public @NonNull Builder putStringArray(@NonNull String key, @NonNull String[] value) {
            mValues.put(key, value);
            return this;
        }

        /**
         * Puts all input key-value pairs from a {@link Data} into the Builder.
         * <p>
         * Valid value types are: Boolean, Integer, Long, Float, Double, String, and their array
         * versions.  Invalid types will throw an {@link IllegalArgumentException}.
         *
         * @param data {@link Data} containing key-value pairs to add
         * @return The {@link Builder}
         */
        public @NonNull Builder putAll(@NonNull Data data) {
            putAll(data.mValues);
            return this;
        }

        /**
         * Puts all input key-value pairs from a {@link Map} into the Builder.
         * <p>
         * Valid value types are: Boolean, Integer, Long, Float, Double, String, and their array
         * versions.  Invalid types will throw an {@link IllegalArgumentException}.
         *
         * @param values A {@link Map} of key-value pairs to add
         * @return The {@link Builder}
         */
        public @NonNull Builder putAll(@NonNull Map<String, Object> values) {
            for (Map.Entry<String, Object> entry : values.entrySet()) {
                String key = entry.getKey();
                Object value = entry.getValue();
                put(key, value);
            }
            return this;
        }

        /**
         * Puts an input key-value pair into the Builder. Valid types are: Boolean, Integer,
         * Long, Float, Double, String, and array versions of each of those types.
         * Invalid types throw an {@link IllegalArgumentException}.
         *
         * @param key A {@link String} key to add
         * @param value A nullable {@link Object} value to add of the valid types
         * @return The {@link Builder}
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        public @NonNull Builder put(@NonNull String key, @Nullable Object value) {
            if (value == null) {
                mValues.put(key, null);
            } else {
                Class valueType = value.getClass();
                if (valueType == Boolean.class
                        || valueType == Byte.class
                        || valueType == Integer.class
                        || valueType == Long.class
                        || valueType == Float.class
                        || valueType == Double.class
                        || valueType == String.class
                        || valueType == Boolean[].class
                        || valueType == Byte[].class
                        || valueType == Integer[].class
                        || valueType == Long[].class
                        || valueType == Float[].class
                        || valueType == Double[].class
                        || valueType == String[].class) {
                    mValues.put(key, value);
                } else if (valueType == boolean[].class) {
                    mValues.put(key, convertPrimitiveBooleanArray((boolean[]) value));
                } else if (valueType == byte[].class) {
                    mValues.put(key, convertPrimitiveByteArray((byte[]) value));
                } else if (valueType == int[].class) {
                    mValues.put(key, convertPrimitiveIntArray((int[]) value));
                } else if (valueType == long[].class) {
                    mValues.put(key, convertPrimitiveLongArray((long[]) value));
                } else if (valueType == float[].class) {
                    mValues.put(key, convertPrimitiveFloatArray((float[]) value));
                } else if (valueType == double[].class) {
                    mValues.put(key, convertPrimitiveDoubleArray((double[]) value));
                } else {
                    throw new IllegalArgumentException(
                            String.format("Key %s has invalid type %s", key, valueType));
                }
            }
            return this;
        }

        /**
         * Builds a {@link Data} object.
         *
         * @return The {@link Data} object containing all key-value pairs specified by this
         *         {@link Builder}.
         */
        public @NonNull Data build() {
            Data data = new Data(mValues);
            // Make sure we catch Data objects that are too large at build() instead of later.  This
            // method will throw an exception if data is too big.
            Data.toByteArray(data);
            return data;
        }
    }
}
