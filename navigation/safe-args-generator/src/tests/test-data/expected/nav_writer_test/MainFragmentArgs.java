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

package a.b;

import android.content.pm.ActivityInfo;
import android.os.Bundle;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import java.io.Serializable;
import java.lang.IllegalArgumentException;
import java.lang.Object;
import java.lang.Override;
import java.lang.String;
import java.nio.file.AccessMode;

public class MainFragmentArgs {
    private Bundle bundle = new Bundle();

    private MainFragmentArgs() {
    }

    private MainFragmentArgs(Bundle bundle) {
        this.bundle.putAll(bundle);
    }

    @NonNull
    public static MainFragmentArgs fromBundle(Bundle bundle) {
        MainFragmentArgs result = new MainFragmentArgs();
        bundle.setClassLoader(MainFragmentArgs.class.getClassLoader());
        if (bundle.containsKey("main")) {
            String main;
            main = bundle.getString("main");
            if (main == null) {
                throw new IllegalArgumentException("Argument \"main\" is marked as non-null but was passed a null value.");
            }
            result.bundle.putString("main", main);
        } else {
            throw new IllegalArgumentException("Required argument \"main\" is missing and does not have an android:defaultValue");
        }
        if (bundle.containsKey("optional")) {
            int optional;
            optional = bundle.getInt("optional");
            result.bundle.putInt("optional", optional);
        }
        if (bundle.containsKey("reference")) {
            int reference;
            reference = bundle.getInt("reference");
            result.bundle.putInt("reference", reference);
        }
        if (bundle.containsKey("floatArg")) {
            float floatArg;
            floatArg = bundle.getFloat("floatArg");
            result.bundle.putFloat("floatArg", floatArg);
        }
        if (bundle.containsKey("floatArrayArg")) {
            float[] floatArrayArg;
            floatArrayArg = bundle.getFloatArray("floatArrayArg");
            if (floatArrayArg == null) {
                throw new IllegalArgumentException("Argument \"floatArrayArg\" is marked as non-null but was passed a null value.");
            }
            result.bundle.putFloatArray("floatArrayArg", floatArrayArg);
        } else {
            throw new IllegalArgumentException("Required argument \"floatArrayArg\" is missing and does not have an android:defaultValue");
        }
        if (bundle.containsKey("objectArrayArg")) {
            ActivityInfo[] objectArrayArg;
            objectArrayArg = (ActivityInfo[]) bundle.getParcelableArray("objectArrayArg");
            if (objectArrayArg == null) {
                throw new IllegalArgumentException("Argument \"objectArrayArg\" is marked as non-null but was passed a null value.");
            }
            result.bundle.putParcelableArray("objectArrayArg", objectArrayArg);
        } else {
            throw new IllegalArgumentException("Required argument \"objectArrayArg\" is missing and does not have an android:defaultValue");
        }
        if (bundle.containsKey("boolArg")) {
            boolean boolArg;
            boolArg = bundle.getBoolean("boolArg");
            result.bundle.putBoolean("boolArg", boolArg);
        }
        if (bundle.containsKey("optionalParcelable")) {
            ActivityInfo optionalParcelable;
            if (Parcelable.class.isAssignableFrom(ActivityInfo.class) || Serializable.class.isAssignableFrom(ActivityInfo.class)) {
                optionalParcelable = (ActivityInfo) bundle.get("optionalParcelable");
            } else {
                throw new UnsupportedOperationException(ActivityInfo.class.getName() + " must implement Parcelable or Serializable or must be an Enum.");
            }
            if (Parcelable.class.isAssignableFrom(ActivityInfo.class) || optionalParcelable == null) {
                result.bundle.putParcelable("optionalParcelable", Parcelable.class.cast(optionalParcelable));
            } else if (Serializable.class.isAssignableFrom(ActivityInfo.class)) {
                result.bundle.putSerializable("optionalParcelable", Serializable.class.cast(optionalParcelable));
            } else {
                throw new UnsupportedOperationException(ActivityInfo.class.getName() + " must implement Parcelable or Serializable or must be an Enum.");
            }
        }
        if (bundle.containsKey("enumArg")) {
            AccessMode enumArg;
            if (Parcelable.class.isAssignableFrom(AccessMode.class) || Serializable.class.isAssignableFrom(AccessMode.class)) {
                enumArg = (AccessMode) bundle.get("enumArg");
            } else {
                throw new UnsupportedOperationException(AccessMode.class.getName() + " must implement Parcelable or Serializable or must be an Enum.");
            }
            if (enumArg == null) {
                throw new IllegalArgumentException("Argument \"enumArg\" is marked as non-null but was passed a null value.");
            }
            if (Parcelable.class.isAssignableFrom(AccessMode.class) || enumArg == null) {
                result.bundle.putParcelable("enumArg", Parcelable.class.cast(enumArg));
            } else if (Serializable.class.isAssignableFrom(AccessMode.class)) {
                result.bundle.putSerializable("enumArg", Serializable.class.cast(enumArg));
            } else {
                throw new UnsupportedOperationException(AccessMode.class.getName() + " must implement Parcelable or Serializable or must be an Enum.");
            }
        }
        return result;
    }

    @NonNull
    public String getMain() {
        return bundle.getString("main");
    }

    public int getOptional() {
        return bundle.getInt("optional");
    }

    public int getReference() {
        return bundle.getInt("reference");
    }

    public float getFloatArg() {
        return bundle.getFloat("floatArg");
    }

    @NonNull
    public float[] getFloatArrayArg() {
        return bundle.getFloatArray("floatArrayArg");
    }

    @NonNull
    public ActivityInfo[] getObjectArrayArg() {
        return (ActivityInfo[]) bundle.getParcelableArray("objectArrayArg");
    }

    public boolean getBoolArg() {
        return bundle.getBoolean("boolArg");
    }

    @Nullable
    public ActivityInfo getOptionalParcelable() {
        if (Parcelable.class.isAssignableFrom(ActivityInfo.class) || Serializable.class.isAssignableFrom(ActivityInfo.class)) {
            return (ActivityInfo) bundle.get("optionalParcelable");
        } else {
            throw new UnsupportedOperationException(ActivityInfo.class.getName() + " must implement Parcelable or Serializable or must be an Enum.");
        }
    }

    @NonNull
    public AccessMode getEnumArg() {
        if (Parcelable.class.isAssignableFrom(AccessMode.class) || Serializable.class.isAssignableFrom(AccessMode.class)) {
            return (AccessMode) bundle.get("enumArg");
        } else {
            throw new UnsupportedOperationException(AccessMode.class.getName() + " must implement Parcelable or Serializable or must be an Enum.");
        }
    }

    @NonNull
    public Bundle toBundle() {
        return bundle;
    }

    @Override
    public boolean equals(Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        MainFragmentArgs that = (MainFragmentArgs) object;
        if (bundle.containsKey("main") != that.bundle.containsKey("main")) {
            return false;
        }
        if (getMain() != null ? !getMain().equals(that.getMain()) : that.getMain() != null) {
            return false;
        }
        if (bundle.containsKey("optional") != that.bundle.containsKey("optional")) {
            return false;
        }
        if (getOptional() != that.getOptional()) {
            return false;
        }
        if (bundle.containsKey("reference") != that.bundle.containsKey("reference")) {
            return false;
        }
        if (getReference() != that.getReference()) {
            return false;
        }
        if (bundle.containsKey("floatArg") != that.bundle.containsKey("floatArg")) {
            return false;
        }
        if (Float.compare(that.getFloatArg(), getFloatArg()) != 0) {
            return false;
        }
        if (bundle.containsKey("floatArrayArg") != that.bundle.containsKey("floatArrayArg")) {
            return false;
        }
        if (getFloatArrayArg() != null ? !getFloatArrayArg().equals(that.getFloatArrayArg()) : that.getFloatArrayArg() != null) {
            return false;
        }
        if (bundle.containsKey("objectArrayArg") != that.bundle.containsKey("objectArrayArg")) {
            return false;
        }
        if (getObjectArrayArg() != null ? !getObjectArrayArg().equals(that.getObjectArrayArg()) : that.getObjectArrayArg() != null) {
            return false;
        }
        if (bundle.containsKey("boolArg") != that.bundle.containsKey("boolArg")) {
            return false;
        }
        if (getBoolArg() != that.getBoolArg()) {
            return false;
        }
        if (bundle.containsKey("optionalParcelable") != that.bundle.containsKey("optionalParcelable")) {
            return false;
        }
        if (getOptionalParcelable() != null ? !getOptionalParcelable().equals(that.getOptionalParcelable()) : that.getOptionalParcelable() != null) {
            return false;
        }
        if (bundle.containsKey("enumArg") != that.bundle.containsKey("enumArg")) {
            return false;
        }
        if (getEnumArg() != null ? !getEnumArg().equals(that.getEnumArg()) : that.getEnumArg() != null) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (getMain() != null ? getMain().hashCode() : 0);
        result = 31 * result + getOptional();
        result = 31 * result + getReference();
        result = 31 * result + Float.floatToIntBits(getFloatArg());
        result = 31 * result + java.util.Arrays.hashCode(getFloatArrayArg());
        result = 31 * result + java.util.Arrays.hashCode(getObjectArrayArg());
        result = 31 * result + (getBoolArg() ? 1 : 0);
        result = 31 * result + (getOptionalParcelable() != null ? getOptionalParcelable().hashCode() : 0);
        result = 31 * result + (getEnumArg() != null ? getEnumArg().hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "MainFragmentArgs{"
                + "main=" + getMain()
                + ", optional=" + getOptional()
                + ", reference=" + getReference()
                + ", floatArg=" + getFloatArg()
                + ", floatArrayArg=" + getFloatArrayArg()
                + ", objectArrayArg=" + getObjectArrayArg()
                + ", boolArg=" + getBoolArg()
                + ", optionalParcelable=" + getOptionalParcelable()
                + ", enumArg=" + getEnumArg()
                + "}";
    }

    public static class Builder {
        private Bundle bundle = new Bundle();

        public Builder(MainFragmentArgs original) {
            this.bundle.putAll(original.bundle);
        }

        public Builder(@NonNull String main, @NonNull float[] floatArrayArg,
                @NonNull ActivityInfo[] objectArrayArg) {
            if (main == null) {
                throw new IllegalArgumentException("Argument \"main\" is marked as non-null but was passed a null value.");
            }
            bundle.putString("main", main);
            if (floatArrayArg == null) {
                throw new IllegalArgumentException("Argument \"floatArrayArg\" is marked as non-null but was passed a null value.");
            }
            bundle.putFloatArray("floatArrayArg", floatArrayArg);
            if (objectArrayArg == null) {
                throw new IllegalArgumentException("Argument \"objectArrayArg\" is marked as non-null but was passed a null value.");
            }
            bundle.putParcelableArray("objectArrayArg", objectArrayArg);
        }

        @NonNull
        public MainFragmentArgs build() {
            MainFragmentArgs result = new MainFragmentArgs(bundle);
            return result;
        }

        @NonNull
        public Builder setMain(@NonNull String main) {
            if (main == null) {
                throw new IllegalArgumentException("Argument \"main\" is marked as non-null but was passed a null value.");
            }
            this.bundle.putString("main", main);
            return this;
        }

        @NonNull
        public Builder setOptional(int optional) {
            this.bundle.putInt("optional", optional);
            return this;
        }

        @NonNull
        public Builder setReference(int reference) {
            this.bundle.putInt("reference", reference);
            return this;
        }

        @NonNull
        public Builder setFloatArg(float floatArg) {
            this.bundle.putFloat("floatArg", floatArg);
            return this;
        }

        @NonNull
        public Builder setFloatArrayArg(@NonNull float[] floatArrayArg) {
            if (floatArrayArg == null) {
                throw new IllegalArgumentException("Argument \"floatArrayArg\" is marked as non-null but was passed a null value.");
            }
            this.bundle.putFloatArray("floatArrayArg", floatArrayArg);
            return this;
        }

        @NonNull
        public Builder setObjectArrayArg(@NonNull ActivityInfo[] objectArrayArg) {
            if (objectArrayArg == null) {
                throw new IllegalArgumentException("Argument \"objectArrayArg\" is marked as non-null but was passed a null value.");
            }
            this.bundle.putParcelableArray("objectArrayArg", objectArrayArg);
            return this;
        }

        @NonNull
        public Builder setBoolArg(boolean boolArg) {
            this.bundle.putBoolean("boolArg", boolArg);
            return this;
        }

        @NonNull
        public Builder setOptionalParcelable(@Nullable ActivityInfo optionalParcelable) {
            if (Parcelable.class.isAssignableFrom(ActivityInfo.class) || optionalParcelable == null) {
                this.bundle.putParcelable("optionalParcelable", Parcelable.class.cast(optionalParcelable));
            } else if (Serializable.class.isAssignableFrom(ActivityInfo.class)) {
                this.bundle.putSerializable("optionalParcelable", Serializable.class.cast(optionalParcelable));
            } else {
                throw new UnsupportedOperationException(ActivityInfo.class.getName() + " must implement Parcelable or Serializable or must be an Enum.");
            }
            return this;
        }

        @NonNull
        public Builder setEnumArg(@NonNull AccessMode enumArg) {
            if (enumArg == null) {
                throw new IllegalArgumentException("Argument \"enumArg\" is marked as non-null but was passed a null value.");
            }
            if (Parcelable.class.isAssignableFrom(AccessMode.class) || enumArg == null) {
                this.bundle.putParcelable("enumArg", Parcelable.class.cast(enumArg));
            } else if (Serializable.class.isAssignableFrom(AccessMode.class)) {
                this.bundle.putSerializable("enumArg", Serializable.class.cast(enumArg));
            } else {
                throw new UnsupportedOperationException(AccessMode.class.getName() + " must implement Parcelable or Serializable or must be an Enum.");
            }
            return this;
        }

        @NonNull
        public String getMain() {
            return bundle.getString("main");
        }

        public int getOptional() {
            return bundle.getInt("optional");
        }

        public int getReference() {
            return bundle.getInt("reference");
        }

        public float getFloatArg() {
            return bundle.getFloat("floatArg");
        }

        @NonNull
        public float[] getFloatArrayArg() {
            return bundle.getFloatArray("floatArrayArg");
        }

        @NonNull
        public ActivityInfo[] getObjectArrayArg() {
            return (ActivityInfo[]) bundle.getParcelableArray("objectArrayArg");
        }

        public boolean getBoolArg() {
            return bundle.getBoolean("boolArg");
        }

        @Nullable
        public ActivityInfo getOptionalParcelable() {
            if (Parcelable.class.isAssignableFrom(ActivityInfo.class) || Serializable.class.isAssignableFrom(ActivityInfo.class)) {
                return (ActivityInfo) bundle.get("optionalParcelable");
            } else {
                throw new UnsupportedOperationException(ActivityInfo.class.getName() + " must implement Parcelable or Serializable or must be an Enum.");
            }
        }

        @NonNull
        public AccessMode getEnumArg() {
            if (Parcelable.class.isAssignableFrom(AccessMode.class) || Serializable.class.isAssignableFrom(AccessMode.class)) {
                return (AccessMode) bundle.get("enumArg");
            } else {
                throw new UnsupportedOperationException(AccessMode.class.getName() + " must implement Parcelable or Serializable or must be an Enum.");
            }
        }
    }
}