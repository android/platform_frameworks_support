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

package androidx.work.integration.testapp;

import android.app.Application;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.work.Configuration;
import androidx.work.tracing.TracingExecutor;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/**
 * An Application class that initializes WorkManager.
 */
public class TestApplication extends Application implements Configuration.Provider {

    private TracingExecutor mTracingExecutor;

    @NonNull
    @Override
    public Configuration getWorkManagerConfiguration() {
        if (mTracingExecutor == null) {
            mTracingExecutor = new TracingExecutor(this, Executors.newCachedThreadPool());
        }
        return new Configuration.Builder()
                .setExecutor(mTracingExecutor)
                .setTaskExecutorDelegate(mTracingExecutor)
                .setMinimumLoggingLevel(Log.VERBOSE).build();
    }
}
