<<<<<<< HEAD   (810747 Merge "Merge empty history for sparse-5626174-L1780000033228)
/*
 * Copyright (C) 2017 The Android Open Source Project
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


import androidx.annotation.RestrictTo;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP_PREFIX)
public class CompositeGeneratedAdaptersObserver implements LifecycleEventObserver {

    private final GeneratedAdapter[] mGeneratedAdapters;

    CompositeGeneratedAdaptersObserver(GeneratedAdapter[] generatedAdapters) {
        mGeneratedAdapters = generatedAdapters;
    }

    @Override
    public void onStateChanged(LifecycleOwner source, Lifecycle.Event event) {
        MethodCallsLogger logger = new MethodCallsLogger();
        for (GeneratedAdapter mGenerated: mGeneratedAdapters) {
            mGenerated.callMethods(source, event, false, logger);
        }
        for (GeneratedAdapter mGenerated: mGeneratedAdapters) {
            mGenerated.callMethods(source, event, true, logger);
        }
    }
}
=======
>>>>>>> BRANCH (2c954e Merge "Merge cherrypicks of [988730] into sparse-5676727-L53)
