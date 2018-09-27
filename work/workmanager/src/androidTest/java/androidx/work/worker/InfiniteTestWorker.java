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

package androidx.work.worker;

import android.support.annotation.NonNull;

import androidx.work.Logger;
import androidx.work.Worker;

/**
 * Test Worker that loops until Thread is interrupted.
 */
public class InfiniteTestWorker extends Worker {

    private static final String TAG = "InfiniteTestWorker";

    @Override
    public @NonNull Result doWork() {
        // Make this interruption aware.
        while (!isStopped()) {
            Logger.info(TAG, "Working.");
        }
        return Result.SUCCESS;
    }
}
