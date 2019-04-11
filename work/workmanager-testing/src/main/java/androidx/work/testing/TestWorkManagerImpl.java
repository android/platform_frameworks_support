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

package androidx.work.testing;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;
import androidx.work.Configuration;
import androidx.work.ListenableWorker;
import androidx.work.WorkManager;
import androidx.work.WorkerFactory;
import androidx.work.WorkerParameters;
import androidx.work.impl.Scheduler;
import androidx.work.impl.WorkManagerImpl;
import androidx.work.impl.utils.taskexecutor.TaskExecutor;

import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.Executor;

/**
 * A concrete implementation of {@link WorkManager} which can be used for testing. This
 * implementation makes it easy to swap Schedulers.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
class TestWorkManagerImpl extends WorkManagerImpl implements TestDriver {

    private TestScheduler mScheduler;

    TestWorkManagerImpl(
            @NonNull Context context,
            @NonNull Configuration configuration) {

        // Note: This implies that the call to ForceStopRunnable() actually does nothing.
        // This is okay when testing.
        super(
                context,
                configuration,
                new TaskExecutor() {

                    Executor mSynchronousExecutor = new SynchronousExecutor();

                    @Override
                    public void postToMainThread(Runnable runnable) {
                        runnable.run();
                    }

                    @Override
                    public Executor getMainThreadExecutor() {
                        return mSynchronousExecutor;
                    }

                    @Override
                    public void executeOnBackgroundThread(Runnable runnable) {
                        runnable.run();
                    }

                    @Override
                    public Executor getBackgroundExecutor() {
                        return mSynchronousExecutor;
                    }

                    @NonNull
                    @Override
                    public Thread getBackgroundExecutorThread() {
                        return Thread.currentThread();
                    }
                },
                true);

        // mScheduler is initialized in createSchedulers() called by super()
        getProcessor().addExecutionListener(mScheduler);
    }

    @NonNull
    @Override
    public List<Scheduler> createSchedulers(Context context) {
        mScheduler = new TestScheduler(context);
        return Collections.singletonList((Scheduler) mScheduler);
    }

    @Override
    public void setAllConstraintsMet(@NonNull UUID workSpecId) {
        mScheduler.setAllConstraintsMet(workSpecId);
    }

    @Override
    public void setInitialDelayMet(@NonNull UUID workSpecId) {
        mScheduler.setInitialDelayMet(workSpecId);
    }

    @Override
    public void setPeriodDelayMet(@NonNull UUID workSpecId) {
        mScheduler.setPeriodDelayMet(workSpecId);
    }

    @NonNull
    @Override
    public WorkerParametersBuilder newWorkerParametersBuilder() {
        WorkerParametersBuilder builder = new WorkerParametersBuilder();
        Configuration configuration = getConfiguration();
        return builder.setBackgroundExector(configuration.getExecutor())
                .setWorkerFactory(configuration.getWorkerFactory())
                .setTaskExecutor(getWorkTaskExecutor());
    }

    @NonNull
    @Override
    @SuppressWarnings("unchecked")
    public <T extends ListenableWorker> T newWorker(
            @NonNull Class<T> workerKlass,
            @NonNull WorkerParameters workerParameters) {

        WorkerFactory factory = workerParameters.getWorkerFactory();

        ListenableWorker worker = factory.createWorkerWithDefaultFallback(
                getApplicationContext(),
                workerKlass.getName(),
                workerParameters);

        if (worker == null) {
            throw new IllegalArgumentException(
                    String.format("Worker %s could not be instantiated.", workerKlass.getName()));
        } else if (!workerKlass.isInstance(worker)) {
            throw new IllegalArgumentException(
                    String.format("Worker cannot be cast to %s", workerKlass.getName()));
        }
        return (T) worker;
    }
}
