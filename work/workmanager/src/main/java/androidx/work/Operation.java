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

import android.arch.lifecycle.LiveData;
import android.support.annotation.NonNull;
import android.support.annotation.RestrictTo;

import com.google.common.util.concurrent.ListenableFuture;

/**
 * Information about an operation being performed by {@link WorkManager}.
 */
public interface Operation {

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    State.SUCCESS SUCCESS = new State.SUCCESS();

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    State.IN_PROGRESS IN_PROGRESS = new State.IN_PROGRESS();

    /**
     * Gets a {@link LiveData} of the Operation {@link State}.
     *
     * @return A {@link LiveData} of the Operation {@link State}.
     */
    @NonNull
    LiveData<State> getState();

    /**
     * Gets a {@link ListenableFuture} which will only resolve with a {@link State.SUCCESS}.
     * FAILURE {@link Operation}'s will come through as {@link Throwable}'s on the
     * {@link ListenableFuture}.
     *
     * Call {@link ListenableFuture#get()} to block until the {@link Operation} reaches a
     * terminal state.
     *
     * @return a {@link ListenableFuture} with information about {@link Operation}'s
     * {@link State.SUCCESS} state.
     */
    @NonNull
    ListenableFuture<State.SUCCESS> getResult();

    /**
     * The {@link Operation} state.
     */
    abstract class State {

        /**
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        State() {
            // Restricting access to the constructor, to give Operation.State a sealed class
            // like behavior.
        }

        /**
         * This represents an {@link Operation} which is successful.
         */
        public static final class SUCCESS extends Operation.State {
            private SUCCESS() {
                super();
            }

            @Override
            @NonNull
            public String toString() {
                return "SUCCESS";
            }
        }

        /**
         * This represents an {@link Operation} which is in progress.
         */
        public static final class IN_PROGRESS extends Operation.State {
            private IN_PROGRESS() {
                super();
            }

            @Override
            @NonNull
            public String toString() {
                return "IN_PROGRESS";
            }
        }

        /**
         * This represents an {@link Operation} which has failed.
         */
        public static final class FAILURE extends Operation.State {

            private final Throwable mThrowable;

            public FAILURE(@NonNull Throwable exception) {
                super();
                mThrowable = exception;
            }

            /**
             * @return The {@link Throwable} which caused the {@link Operation} to fail.
             */
            @NonNull
            public Throwable getException() {
                return mThrowable;
            }

            @Override
            @NonNull
            public String toString() {
                return String.format("FAILURE (%s)", mThrowable.getMessage());
            }
        }
    }
}
