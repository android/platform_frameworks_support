/*
 * Copyright 2019 The Android Open Source Project
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

package androidx.security.crypto;

import static androidx.security.crypto.MasterKey.KEYSTORE_PATH_URI;

import static java.nio.charset.StandardCharsets.UTF_8;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.ArraySet;
import android.util.Pair;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.google.crypto.tink.Aead;
import com.google.crypto.tink.DeterministicAead;
import com.google.crypto.tink.KeysetHandle;
import com.google.crypto.tink.aead.AeadFactory;
import com.google.crypto.tink.aead.AeadKeyTemplates;
import com.google.crypto.tink.config.TinkConfig;
import com.google.crypto.tink.daead.DeterministicAeadFactory;
import com.google.crypto.tink.daead.DeterministicAeadKeyTemplates;
import com.google.crypto.tink.integration.android.AndroidKeysetManager;
import com.google.crypto.tink.proto.KeyTemplate;
import com.google.crypto.tink.subtle.Base64;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * A class that provides an encrypted implementation for all SharedPreferences data.
 */
public final class EncryptedSharedPreferences implements SharedPreferences {

    static final String TAG = "EncryptedSharedPrefs";

    private static final String KEYSET_PREF_NAME =
            "__androidx_security__crypto_encrypted_prefs__";
    private static final String KEY_KEYSET_ALIAS =
            "__androidx_security_crypto_encrypted_prefs_key_keyset__";
    private static final String VALUE_KEYSET_ALIAS =
            "__androidx_security_crypto_encrypted_prefs_value_keyset__";

    private static final String NULL_VALUE = "__NULL__";

    final SharedPreferences mSharedPreferences;
    final Context mContext;
    final List<OnSharedPreferenceChangeListener> mListeners;
    final String mFileName;
    final String mMasterKeyAlias;

    final Aead mValueAead;
    final DeterministicAead mKeyDeterministicAead;

    EncryptedSharedPreferences(@NonNull String name,
            @NonNull String masterKeyAlias,
            @NonNull Context context,
            @NonNull Aead aead,
            @NonNull DeterministicAead deterministicAead) {
        mContext = context;
        mFileName = name;
        mSharedPreferences = mContext.getSharedPreferences(mFileName, Context.MODE_PRIVATE);
        mMasterKeyAlias = masterKeyAlias;
        mValueAead = aead;
        mKeyDeterministicAead = deterministicAead;
        mListeners = new ArrayList<>();
    }

    /**
     * Opens an instance of encrypted SharedPreferences
     *
     * @param fileName The name of the file to open; can not contain path separators.
     * @return The SharedPreferences instance that encrypts all data.
     * @throws GeneralSecurityException when a bad master key or keyset has been attempted
     * @throws IOException              when fileName can not be used
     */
    @NonNull
    public static SharedPreferences create(@NonNull String fileName,
            @NonNull String masterKeyAlias,
            @NonNull Context context,
            @NonNull PrefKeyEncryptionScheme prefKeyEncryptionScheme,
            @NonNull PrefValueEncryptionScheme prefValueEncryptionScheme)
            throws GeneralSecurityException, IOException {
        TinkConfig.register();

        KeysetHandle daeadKeysetHandle = new AndroidKeysetManager.Builder()
                .withKeyTemplate(prefKeyEncryptionScheme.getKeyTemplate())
                .withSharedPref(context, KEY_KEYSET_ALIAS, KEYSET_PREF_NAME)
                .withMasterKeyUri(KEYSTORE_PATH_URI + masterKeyAlias)
                .build().getKeysetHandle();
        KeysetHandle aeadKeysetHandle = new AndroidKeysetManager.Builder()
                .withKeyTemplate(prefValueEncryptionScheme.getKeyTemplate())
                .withSharedPref(context, VALUE_KEYSET_ALIAS, KEYSET_PREF_NAME)
                .withMasterKeyUri(KEYSTORE_PATH_URI + masterKeyAlias)
                .build().getKeysetHandle();

        DeterministicAead daead = DeterministicAeadFactory.getPrimitive(daeadKeysetHandle);
        Aead aead = AeadFactory.getPrimitive(aeadKeysetHandle);

        return new EncryptedSharedPreferences(fileName, masterKeyAlias, context, aead, daead);
    }

    /**
     * The encryption scheme to encrypt keys.
     */
    public enum PrefKeyEncryptionScheme {
        /**
         * Uses Deterministic Aead encryption to ensure that the key is encrypted the same way such
         * that the key can be used as a lookup mechanism.
         *
         * For more information please see the Tink documentation:
         *
         * @link com.google.crypto.tink.daead.DeterministicAeadKeyTemplates
         */
        AES256_SIV(DeterministicAeadKeyTemplates.AES256_SIV);

        KeyTemplate mDeterministicAeadKeyTemplate;

        PrefKeyEncryptionScheme(KeyTemplate keyTemplate) {
            mDeterministicAeadKeyTemplate = keyTemplate;
        }

        KeyTemplate getKeyTemplate() {
            return mDeterministicAeadKeyTemplate;
        }
    }

    /**
     * The encryption scheme to encrypt values.
     */
    public enum PrefValueEncryptionScheme {
        /**
         * Uses Aead encryption to ensure that the value is encrypted. The AAD provided is the key.
         *
         * For more information please see the Tink documentation:
         *
         * @link com.google.crypto.tink.aead.AeadKeyTemplates
         */
        AES256_GCM(AeadKeyTemplates.AES256_GCM);

        KeyTemplate mAeadKeyTemplate;

        PrefValueEncryptionScheme(KeyTemplate keyTemplates) {
            mAeadKeyTemplate = keyTemplates;
        }

        KeyTemplate getKeyTemplate() {
            return mAeadKeyTemplate;
        }
    }

    private static final class Editor implements SharedPreferences.Editor {
        private final EncryptedSharedPreferences mEncryptedSharedPreferences;
        private final SharedPreferences.Editor mEditor;
        private final List<String> mKeysChanged;

        Editor(EncryptedSharedPreferences encryptedSharedPreferences,
                SharedPreferences.Editor editor) {
            mEncryptedSharedPreferences = encryptedSharedPreferences;
            mEditor = editor;
            mKeysChanged = new CopyOnWriteArrayList<>();
        }

        @Override
        @NonNull
        public SharedPreferences.Editor putString(@Nullable String key, @Nullable String value) {
            if (value == null) {
                value = NULL_VALUE;
            }
            byte[] stringBytes = value.getBytes(UTF_8);
            int stringByteLength = stringBytes.length;
            ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + Integer.BYTES
                    + stringByteLength);
            buffer.putInt(EncryptedType.STRING.getId());
            buffer.putInt(stringByteLength);
            buffer.put(stringBytes);
            putEncryptedObject(key, buffer.array());
            return this;
        }

        @Override
        @NonNull
        public SharedPreferences.Editor putStringSet(@Nullable String key,
                @Nullable Set<String> values) {
            if (values == null) {
                values = new ArraySet<>();
                values.add(NULL_VALUE);
            }
            List<byte[]> byteValues = new ArrayList<>(values.size());
            int totalBytes = values.size() * Integer.BYTES;
            for (String strValue : values) {
                byte[] byteValue = strValue.getBytes(UTF_8);
                byteValues.add(byteValue);
                totalBytes += byteValue.length;
            }
            totalBytes += Integer.BYTES;
            ByteBuffer buffer = ByteBuffer.allocate(totalBytes);
            buffer.putInt(EncryptedType.STRING_SET.getId());
            for (byte[] bytes : byteValues) {
                buffer.putInt(bytes.length);
                buffer.put(bytes);
            }
            putEncryptedObject(key, buffer.array());
            return this;
        }

        @Override
        @NonNull
        public SharedPreferences.Editor putInt(@Nullable String key, int value) {
            ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + Integer.BYTES);
            buffer.putInt(EncryptedType.INT.getId());
            buffer.putInt(value);
            putEncryptedObject(key, buffer.array());
            return this;
        }

        @Override
        @NonNull
        public SharedPreferences.Editor putLong(@Nullable String key, long value) {
            ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + Long.BYTES);
            buffer.putInt(EncryptedType.LONG.getId());
            buffer.putLong(value);
            putEncryptedObject(key, buffer.array());
            return this;
        }

        @Override
        @NonNull
        public SharedPreferences.Editor putFloat(@Nullable String key, float value) {
            ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + Float.BYTES);
            buffer.putInt(EncryptedType.FLOAT.getId());
            buffer.putFloat(value);
            putEncryptedObject(key, buffer.array());
            return this;
        }

        @Override
        @NonNull
        public SharedPreferences.Editor putBoolean(@Nullable String key, boolean value) {
            ByteBuffer buffer = ByteBuffer.allocate(Integer.BYTES + Byte.BYTES);
            buffer.putInt(EncryptedType.BOOLEAN.getId());
            buffer.put(value ? (byte) 1 : (byte) 0);
            putEncryptedObject(key, buffer.array());
            return this;
        }

        @Override
        @NonNull
        public SharedPreferences.Editor remove(@Nullable String key) {
            mEditor.remove(mEncryptedSharedPreferences.encryptKey(key));
            mKeysChanged.remove(key);
            return this;
        }

        @Override
        @NonNull
        public SharedPreferences.Editor clear() {
            mEditor.clear();
            mKeysChanged.clear();
            return this;
        }

        @Override
        public boolean commit() {
            try {
                return mEditor.commit();
            } finally {
                notifyListeners();
            }
        }

        @Override
        public void apply() {
            mEditor.apply();
            notifyListeners();
        }

        private void putEncryptedObject(String key, byte[] value) {
            mKeysChanged.add(key);
            if (key == null) {
                key = NULL_VALUE;
            }
            try {
                Pair<String, String> encryptedPair = mEncryptedSharedPreferences
                        .encryptKeyValuePair(key, value);
                mEditor.putString(encryptedPair.first, encryptedPair.second);
            } catch (GeneralSecurityException ex) {
                throw new SecurityException("Could not encrypt data: " + ex.getMessage(), ex);
            }
        }

        private void notifyListeners() {
            for (OnSharedPreferenceChangeListener listener :
                    mEncryptedSharedPreferences.mListeners) {
                for (String key : mKeysChanged) {
                    listener.onSharedPreferenceChanged(mEncryptedSharedPreferences, key);
                }
            }
        }
    }

    // SharedPreferences methods

    @Override
    @NonNull
    public Map<String, ?> getAll() {
        Map<String, ? super Object> allEntries = new HashMap<>();
        for (Map.Entry<String, ?> entry : mSharedPreferences.getAll().entrySet()) {
            String decryptedKey = decryptKey(entry.getKey());

            allEntries.put(decryptedKey,
                    getDecryptedObject(decryptedKey));
        }
        return allEntries;
    }

    @Nullable
    @Override
    public String getString(@Nullable String key, @Nullable String defValue) {
        Object value = getDecryptedObject(key);
        return (value != null && value instanceof String ? (String) value : defValue);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public Set<String> getStringSet(@Nullable String key, @Nullable Set<String> defValues) {
        Set<String> returnValues;
        Object value = getDecryptedObject(key);
        if (value instanceof Set) {
            returnValues = (Set<String>) value;
        } else {
            returnValues = new ArraySet<>();
        }
        return returnValues.size() > 0 ? returnValues : defValues;
    }

    @Override
    public int getInt(@Nullable String key, int defValue) {
        Object value = getDecryptedObject(key);
        return (value != null && value instanceof Integer ? (Integer) value : defValue);
    }

    @Override
    public long getLong(@Nullable String key, long defValue) {
        Object value = getDecryptedObject(key);
        return (value != null && value instanceof Long ? (Long) value : defValue);
    }

    @Override
    public float getFloat(@Nullable String key, float defValue) {
        Object value = getDecryptedObject(key);
        return (value != null && value instanceof Float ? (Float) value : defValue);
    }

    @Override
    public boolean getBoolean(@Nullable String key, boolean defValue) {
        Object value = getDecryptedObject(key);
        return (value != null && value instanceof Boolean ? (Boolean) value : defValue);
    }

    @Override
    public boolean contains(@Nullable String key) {
        String encryptedKey = encryptKey(key);
        return mSharedPreferences.contains(encryptedKey);
    }

    @Override
    @NonNull
    public SharedPreferences.Editor edit() {
        return new Editor(this, mSharedPreferences.edit());
    }

    @Override
    public void registerOnSharedPreferenceChangeListener(
            @NonNull OnSharedPreferenceChangeListener listener) {
        mListeners.add(listener);
    }

    @Override
    public void unregisterOnSharedPreferenceChangeListener(
            @NonNull OnSharedPreferenceChangeListener listener) {
        mListeners.remove(listener);
    }

    /**
     * Internal enum to set the type of encrypted data.
     */
    private enum EncryptedType {
        STRING(0),
        STRING_SET(1),
        INT(2),
        LONG(3),
        FLOAT(4),
        BOOLEAN(5);

        int mId;

        EncryptedType(int id) {
            mId = id;
        }

        public int getId() {
            return mId;
        }

        public static EncryptedType fromId(int id) {
            switch (id) {
                case 0:
                    return STRING;
                case 1:
                    return STRING_SET;
                case 2:
                    return INT;
                case 3:
                    return LONG;
                case 4:
                    return FLOAT;
                case 5:
                    return BOOLEAN;
            }
            return null;
        }
    }

    private Object getDecryptedObject(String key) {
        if (key == null) {
            key = NULL_VALUE;
        }
        Object returnValue = null;
        try {
            String encryptedKey = encryptKey(key);
            String encryptedValue = mSharedPreferences.getString(encryptedKey, null);
            if (encryptedValue != null) {
                byte[] cipherText = Base64.decode(encryptedValue, Base64.DEFAULT);
                byte[] value = mValueAead.decrypt(cipherText, encryptedKey.getBytes(UTF_8));
                ByteBuffer buffer = ByteBuffer.wrap(value);
                buffer.position(0);
                int typeId = buffer.getInt();
                EncryptedType type = EncryptedType.fromId(typeId);
                switch (type) {
                    case STRING:
                        int stringLength = buffer.getInt();
                        ByteBuffer stringSlice = buffer.slice();
                        buffer.limit(stringLength);
                        String stringValue = UTF_8.decode(stringSlice).toString();
                        if (stringValue.equals(NULL_VALUE)) {
                            returnValue = null;
                        } else {
                            returnValue = stringValue;
                        }
                        break;
                    case INT:
                        returnValue = buffer.getInt();
                        break;
                    case LONG:
                        returnValue = buffer.getLong();
                        break;
                    case FLOAT:
                        returnValue = buffer.getFloat();
                        break;
                    case BOOLEAN:
                        returnValue = buffer.get() != (byte) 0;
                        break;
                    case STRING_SET:
                        ArraySet<String> stringSet = new ArraySet<>();
                        while (buffer.hasRemaining()) {
                            int subStringLength = buffer.getInt();
                            ByteBuffer subStringSlice = buffer.slice();
                            subStringSlice.limit(subStringLength);
                            buffer.position(buffer.position() + subStringLength);
                            stringSet.add(UTF_8.decode(subStringSlice).toString());
                        }
                        if (stringSet.size() == 1 && NULL_VALUE.equals(stringSet.valueAt(0))) {
                            returnValue = null;
                        } else {
                            returnValue = stringSet;
                        }
                        break;
                }
            }
        } catch (GeneralSecurityException ex) {
            throw new SecurityException("Could not decrypt value. " + ex.getMessage(), ex);
        }
        return returnValue;
    }

    String encryptKey(String key) {
        if (key == null) {
            key = NULL_VALUE;
        }
        String encryptedKey = null;
        try {
            byte[] encryptedKeyBytes = mKeyDeterministicAead.encryptDeterministically(
                    key.getBytes(UTF_8),
                    mFileName.getBytes());
            encryptedKey = Base64.encode(encryptedKeyBytes);
        } catch (GeneralSecurityException ex) {
            throw new SecurityException("Could not encrypt key. " + ex.getMessage(), ex);
        }
        return encryptedKey;
    }

    String decryptKey(String encryptedKey) {
        String key = null;
        try {
            byte[] clearText = mKeyDeterministicAead.decryptDeterministically(
                    Base64.decode(encryptedKey, Base64.DEFAULT),
                    mFileName.getBytes());
            key = new String(clearText, UTF_8);
            if (key.equals(NULL_VALUE)) {
                key = null;
            }
        } catch (GeneralSecurityException ex) {
            throw new SecurityException("Could not decrypt key. " + ex.getMessage(), ex);
        }
        return key;
    }

    Pair<String, String> encryptKeyValuePair(String key, byte[] value)
            throws GeneralSecurityException {
        String encryptedKey = encryptKey(key);
        byte[] cipherText = mValueAead.encrypt(value, encryptedKey.getBytes(UTF_8));
        return new Pair<>(encryptedKey, Base64.encode(cipherText));
    }

}
