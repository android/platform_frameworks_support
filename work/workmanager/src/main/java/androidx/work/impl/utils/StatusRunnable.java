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

package androidx.work.impl.utils;

import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;
import android.support.annotation.WorkerThread;

import androidx.work.WorkStatus;
import androidx.work.impl.WorkDatabase;
import androidx.work.impl.WorkManagerEngine;
import androidx.work.impl.model.WorkSpec;
import androidx.work.impl.utils.futures.SettableFuture;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;
import java.util.UUID;

/**
 * A {@link Runnable} to get {@link WorkStatus}es.
 *
 * @param <T> The expected return type for the {@link ListenableFuture}.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public abstract class StatusRunnable<T> implements Runnable {
    private final SettableFuture<T> mFuture = SettableFuture.create();

    @Override
    public void run() {
        try {
            final T value = runInternal();
            mFuture.set(value);
        } catch (Throwable throwable) {
            mFuture.setException(throwable);
        }
    }

    @WorkerThread
    abstract T runInternal();

    public ListenableFuture<T> getFuture() {
        return mFuture;
    }

    /**
     * Creates a {@link StatusRunnable} which can get statuses for a given {@link List} of
     * {@link String} workSpec ids.
     *
     * @param workManager The {@link WorkManagerEngine} to use
     * @param ids         The {@link List} of {@link String} ids
     * @return an instance of {@link StatusRunnable}
     */
    public static StatusRunnable<List<WorkStatus>> forStringIds(
            @NonNull final WorkManagerEngine workManager,
            @NonNull final List<String> ids) {

        return new StatusRunnable<List<WorkStatus>>() {
            @Override
            public List<WorkStatus> runInternal() {
                WorkDatabase workDatabase = workManager.getWorkDatabase();
                List<WorkSpec.WorkStatusPojo> workStatusPojos =
                        workDatabase.workSpecDao().getWorkStatusPojoForIds(ids);

                return WorkSpec.WORK_STATUS_MAPPER.apply(workStatusPojos);
            }
        };
    }

    /**
     * Creates a {@link StatusRunnable} which can get statuses for a specific {@link UUID}
     * workSpec id.
     *
     * @param workManager The {@link WorkManagerEngine} to use
     * @param id          The workSpec {@link UUID}
     * @return an instance of {@link StatusRunnable}
     */
    public static StatusRunnable<WorkStatus> forUUID(
            @NonNull final WorkManagerEngine workManager,
            @NonNull final UUID id) {

        return new StatusRunnable<WorkStatus>() {
            @Override
            WorkStatus runInternal() {
                WorkDatabase workDatabase = workManager.getWorkDatabase();
                WorkSpec.WorkStatusPojo workStatusPojo =
                        workDatabase.workSpecDao().getWorkStatusPojoForId(id.toString());

                return workStatusPojo != null ? workStatusPojo.toWorkStatus() : null;
            }
        };
    }

    /**
     * Creates a {@link StatusRunnable} which can get statuses for {@link WorkSpec}s annotated with
     * the given {@link String} tag.
     *
     * @param workManager The {@link WorkManagerEngine} to use
     * @param tag The {@link String} tag
     * @return an instance of {@link StatusRunnable}
     */
    public static StatusRunnable<List<WorkStatus>> forTag(
            @NonNull final WorkManagerEngine workManager,
            @NonNull final String tag) {

        return new StatusRunnable<List<WorkStatus>>() {
            @Override
            List<WorkStatus> runInternal() {
                WorkDatabase workDatabase = workManager.getWorkDatabase();
                List<WorkSpec.WorkStatusPojo> workStatusPojos =
                        workDatabase.workSpecDao().getWorkStatusPojoForTag(tag);

                return WorkSpec.WORK_STATUS_MAPPER.apply(workStatusPojos);
            }
        };
    }

    /**
     * Creates a {@link StatusRunnable} which can get statuses for {@link WorkSpec}s annotated with
     * the given {@link String} unique name.
     *
     * @param workManager The {@link WorkManagerEngine} to use
     * @param name The {@link String} unique name
     * @return an instance of {@link StatusRunnable}
     */
    public static StatusRunnable<List<WorkStatus>> forUniqueWork(
            @NonNull final WorkManagerEngine workManager,
            @NonNull final String name) {

        return new StatusRunnable<List<WorkStatus>>() {
            @Override
            List<WorkStatus> runInternal() {
                WorkDatabase workDatabase = workManager.getWorkDatabase();
                List<WorkSpec.WorkStatusPojo> workStatusPojos =
                        workDatabase.workSpecDao().getWorkStatusPojoForName(name);

                return WorkSpec.WORK_STATUS_MAPPER.apply(workStatusPojos);
            }
        };
    }
}
