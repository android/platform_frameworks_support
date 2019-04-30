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

package androidx.camera.core;

import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;

/** Configuration containing options pertaining to SessionEventListener object. */
public interface SessionEventConfig {

    /**
     * Option: camerax.core.sessionEventListener
     *
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    Config.Option<SessionEventListener> OPTION_SESSION_EVENT_LISTENER =
            Config.Option.create("camerax.core.sessionEventListener", SessionEventListener.class);

    /**
     * Returns the SessionEventListener.
     *
     * @param valueIfMissing The value to return if this configuration option has not been set.
     * @return The stored value or <code>valueIfMissing</code> if the value does not exist in this
     * configuration.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Nullable
    SessionEventListener getSessionEventListener(@Nullable SessionEventListener valueIfMissing);

    /**
     * Returns the SessionEventListener.
     *
     * @return The stored value, if it exists in this configuration.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @Nullable
    SessionEventListener getSessionEventListener();

    // Option Declarations:
    // *********************************************************************************************

    /**
     * Builder for a {@link SessionEventConfig}.
     *
     * @param <B> The top level builder type for which this builder is composed with.
     */
    interface Builder<B> {

        /**
         * Sets the SessionEventListener.
         *
         * @param sessionEventListener The SessionEventListener.
         * @return the current Builder.
         * @hide
         */
        @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
        B setSessionEventListener(SessionEventListener sessionEventListener);
    }
}
