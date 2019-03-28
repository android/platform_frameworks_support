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

package androidx.lifecycle;

import android.app.Application;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.savedstate.SavedStateRegistry;

/**
 * Skeleton of androidx.lifecycle.ViewModelProvider.KeyedFactory
 * that creates {@link SavedStateHandle} for every requested {@link androidx.lifecycle.ViewModel}. The subclasses
 * implement {@link #create(String, Class, SavedStateHandle)} to actually instantiate
 * {@code androidx.lifecycle.ViewModel}s.
 */
public abstract class AbstractSavedStateVMFactory implements ViewModelProvider.KeyedFactory {
    static final String TAG_SAVED_STATE_HANDLE = "androidx.lifecycle.savedstate.vm.tag";

    private final SavedStateRegistry mSavedStateRegistry;
    private final Bundle mDefaultArgs;

    /**
     * Constructs this factory.
<<<<<<< HEAD   (60b11c Merge "Merge empty history for sparse-5338950-L0630000027955)
=======
     *
     * @param owner {@link SavedStateRegistryOwner} that will provide restored state for created
     * {@link androidx.lifecycle.ViewModel ViewModels}
     * @param defaultArgs values from this {@code Bundle} will be used as defaults by
     *                    {@link SavedStateHandle} passed in {@link ViewModel ViewModels}
     *                    if there is no previously saved state
     *                    or previously saved state misses a value by such key
>>>>>>> BRANCH (e95ebf Merge "Merge cherrypicks of [936611, 936612] into sparse-541)
     */
    public AbstractSavedStateVMFactory(
            @NonNull Application application,
            @NonNull SavedStateRegistry savedStateRegistry,
            @Nullable Bundle defaultArgs) {
        mSavedStateRegistry = savedStateRegistry;
        mDefaultArgs = defaultArgs;
        VMSavedStateInitializer.initializeIfNeeded(application);
    }

    // TODO: make KeyedFactory#create(String, Class) package private
    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @NonNull
    @Override
    public final <T extends ViewModel> T create(@NonNull String key, @NonNull Class<T> modelClass) {
        Bundle restoredState = mSavedStateRegistry.consumeRestoredStateForKey(key);
        SavedStateHandle handle = SavedStateHandle.createHandle(restoredState, mDefaultArgs);
        mSavedStateRegistry.registerSavedStateProvider(key, handle.savedStateProvider());
        T viewmodel = create(key, modelClass, handle);
        viewmodel.setTagIfAbsent(TAG_SAVED_STATE_HANDLE, handle);
        return viewmodel;
    }

<<<<<<< HEAD   (60b11c Merge "Merge empty history for sparse-5338950-L0630000027955)
=======
    @NonNull
    @Override
    public final <T extends ViewModel> T create(@NonNull Class<T> modelClass) {
        // ViewModelProvider calls correct create that support same modelClass with different keys
        // If a developer manually calls this method, there is no "key" in picture, so factory
        // simply uses classname internally as as key.
        String canonicalName = modelClass.getCanonicalName();
        if (canonicalName == null) {
            throw new IllegalArgumentException("Local and anonymous classes can not be ViewModels");
        }
        return create(canonicalName, modelClass);
    }

>>>>>>> BRANCH (e95ebf Merge "Merge cherrypicks of [936611, 936612] into sparse-541)
    /**
     * Creates a new instance of the given {@code Class}.
     * <p>
     *
     * @param key a key associated with the requested ViewModel
     * @param modelClass a {@code Class} whose instance is requested
     * @param handle a handle to saved state associated with the requested ViewModel
     * @param <T>        The type parameter for the ViewModel.
     * @return a newly created ViewModels
     */
    @NonNull
    protected abstract <T extends ViewModel> T create(@NonNull String key,
            @NonNull Class<T> modelClass, @NonNull SavedStateHandle handle);
}

