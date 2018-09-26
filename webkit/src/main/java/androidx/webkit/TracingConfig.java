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

package androidx.webkit;

import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.RestrictTo;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

/**
 * Holds tracing configuration information and predefined settings
 * for {@link TracingController}.
 *
 * This class is functionally equivalent to {@link android.webkit.TracingConfig}.
 */
public class TracingConfig {
    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @IntDef(flag = true, value = {CATEGORIES_NONE, CATEGORIES_ALL, CATEGORIES_ANDROID_WEBVIEW,
            CATEGORIES_WEB_DEVELOPER, CATEGORIES_INPUT_LATENCY, CATEGORIES_RENDERING,
            CATEGORIES_JAVASCRIPT_AND_RENDERING, CATEGORIES_FRAME_VIEWER})
    @Retention(RetentionPolicy.SOURCE)
    public @interface PredefinedCategories {}

    /**
     * Indicates that there are no predefined categories.
     *
     * This should remain equal to {@link android.webkit.TracingConfig#CATEGORIES_NONE}.
     */
    public static final int CATEGORIES_NONE = 0;

    /**
     * Predefined set of categories, includes all categories enabled by default in chromium.
     * Use with caution: this setting may produce large trace output.
     *
     * This should remain equal to {@link android.webkit.TracingConfig#CATEGORIES_ALL}.
     */
    public static final int CATEGORIES_ALL = 1 << 0;

    /**
     * Predefined set of categories typically useful for analyzing WebViews.
     * Typically includes "android_webview" and "Java" categories.
     *
     * This should remain equal to {@link android.webkit.TracingConfig#CATEGORIES_ANDROID_WEBVIEW}.
     */
    public static final int CATEGORIES_ANDROID_WEBVIEW = 1 << 1;

    /**
     * Predefined set of categories typically useful for web developers.
     * Typically includes "blink", "compositor", "renderer.scheduler" and "v8" categories.
     *
     * This should remain equal to {@link android.webkit.TracingConfig#CATEGORIES_WEB_DEVELOPER}.
     */
    public static final int CATEGORIES_WEB_DEVELOPER = 1 << 2;

    /**
     * Predefined set of categories for analyzing input latency issues.
     * Typically includes "input", "renderer.scheduler" categories.
     *
     * This should remain equal to {@link android.webkit.TracingConfig#CATEGORIES_INPUT_LATENCY}.
     */
    public static final int CATEGORIES_INPUT_LATENCY = 1 << 3;

    /**
     * Predefined set of categories for analyzing rendering issues.
     * Typically includes "blink", "compositor" and "gpu" categories.
     *
     * This should remain equal to {@link android.webkit.TracingConfig#CATEGORIES_RENDERING}.
     */
    public static final int CATEGORIES_RENDERING = 1 << 4;

    /**
     * Predefined set of categories for analyzing javascript and rendering issues.
     * Typically includes "blink", "compositor", "gpu", "renderer.scheduler" and "v8" categories.
     * This should remain equal to
     * {@link android.webkit.TracingConfig#CATEGORIES_JAVASCRIPT_AND_RENDERING}.
     */
    public static final int CATEGORIES_JAVASCRIPT_AND_RENDERING = 1 << 5;

    /**
     * Predefined set of categories for studying difficult rendering performance problems.
     * Typically includes "blink", "compositor", "gpu", "renderer.scheduler", "v8" and
     * some other compositor categories which are disabled by default.
     *
     * This should remain equal to {@link android.webkit.TracingConfig#CATEGORIES_FRAME_VIEWER}.
     */
    public static final int CATEGORIES_FRAME_VIEWER = 1 << 6;

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    @IntDef({RECORD_UNTIL_FULL, RECORD_CONTINUOUSLY})
    @Retention(RetentionPolicy.SOURCE)
    public @interface TracingMode {}

    /**
     * Record trace events until the internal tracing buffer is full.
     *
     * Typically the buffer memory usage is larger than {@link #RECORD_CONTINUOUSLY}.
     * Depending on the implementation typically allows up to 256k events to be stored.
     *
     * This should remain equal to {@link android.webkit.TracingConfig#RECORD_UNTIL_FULL}.
     */
    public static final int RECORD_UNTIL_FULL = 0;

    /**
     * Record trace events continuously using an internal ring buffer. Default tracing mode.
     *
     * Overwrites old events if they exceed buffer capacity. Uses less memory than the
     * {@link #RECORD_UNTIL_FULL} mode. Depending on the implementation typically allows
     * up to 64k events to be stored.
     *
     * This should remain equal to {@link android.webkit.TracingConfig#RECORD_CONTINUOUSLY}.
     */
    public static final int RECORD_CONTINUOUSLY = 1;

    private @PredefinedCategories int mPredefinedCategories;
    private final List<String> mCustomIncludedCategories = new ArrayList<>();
    private @TracingMode int mTracingMode;

    /** @hide */
    @RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
    public TracingConfig(@PredefinedCategories int predefinedCategories,
                         List<String> customIncludedCategories, @TracingMode int tracingMode) {
        mPredefinedCategories = predefinedCategories;
        mCustomIncludedCategories.addAll(customIncludedCategories);
        mTracingMode = tracingMode;
    }

    /**
     * Returns a bitmask of the predefined category sets of this configuration.
     *
     * @return Bitmask of predefined category sets.
     */
    @PredefinedCategories
    public int getPredefinedCategories() {
        return mPredefinedCategories;
    }

    /**
     * Returns the list of included custom category patterns for this configuration.
     *
     * @return Empty list if no custom category patterns are specified.
     */
    @NonNull
    public List<String> getCustomIncludedCategories() {
        return mCustomIncludedCategories;
    }

    /**
     * Returns the tracing mode of this configuration.
     *
     * @return The tracing mode of this configuration.
     */
    @TracingMode
    public int getTracingMode() {
        return mTracingMode;
    }

    /**
     * Builder used to create {@link TracingConfig} objects.
     */
    public static class Builder {
        private @PredefinedCategories int mPredefinedCategories = CATEGORIES_NONE;
        private final List<String> mCustomIncludedCategories = new ArrayList<>();
        private @TracingMode int mTracingMode = RECORD_CONTINUOUSLY;

        /**
         * Default constructor for Builder.
         */
        public Builder() {
        }

        /**
         * Build {@link TracingConfig} using the current settings.
         *
         * @return The {@link TracingConfig} with the current settings.
         */
        @NonNull
        public TracingConfig build() {
            return new TracingConfig(mPredefinedCategories, mCustomIncludedCategories,
                    mTracingMode);
        }

        /**
         * Adds predefined sets of categories to be included in the trace output.
         *
         * A predefined category set can be one of
         * {@link TracingConfig#CATEGORIES_NONE},
         * {@link TracingConfig#CATEGORIES_ALL},
         * {@link TracingConfig#CATEGORIES_ANDROID_WEBVIEW},
         * {@link TracingConfig#CATEGORIES_WEB_DEVELOPER},
         * {@link TracingConfig#CATEGORIES_INPUT_LATENCY},
         * {@link TracingConfig#CATEGORIES_RENDERING},
         * {@link TracingConfig#CATEGORIES_JAVASCRIPT_AND_RENDERING} or
         * {@link TracingConfig#CATEGORIES_FRAME_VIEWER}.
         *
         * @param predefinedCategories A list or bitmask of predefined category sets.
         * @return The builder to facilitate chaining.
         */
        @NonNull
        public Builder addCategories(@PredefinedCategories int... predefinedCategories) {
            for (int categorySet : predefinedCategories) {
                mPredefinedCategories |= categorySet;
            }
            return this;
        }

        /**
         * Adds custom categories to be included in trace output.
         *
         * Note that the categories are defined by the currently-in-use version of WebView. They
         * live in chromium code and are not part of the Android API.
         * See <a href="https://www.chromium.org/developers/how-tos/trace-event-profiling-tool">
         * chromium documentation on tracing</a> for more details.
         *
         * @param categories A list of category patterns. A category pattern can contain wildcards,
         *        e.g. "blink*" or full category name e.g. "renderer.scheduler".
         * @return The builder to facilitate chaining.
         */
        @NonNull
        public Builder addCategories(String... categories) {
            mCustomIncludedCategories.addAll(Arrays.asList(categories));
            return this;
        }

        /**
         * Adds custom categories to be included in trace output.
         *
         * Same as {@link #addCategories(String...)} but allows to pass a Collection as a parameter.
         *
         * @param categories A list of category patterns.
         * @return The builder to facilitate chaining.
         */
        @NonNull
        public Builder addCategories(@NonNull Collection<String> categories) {
            mCustomIncludedCategories.addAll(categories);
            return this;
        }

        /**
         * Sets the tracing mode for this configuration.
         * When tracingMode is not set explicitly,
         * the default is {@link android.webkit.TracingConfig#RECORD_CONTINUOUSLY}.
         *
         * @param tracingMode The tracing mode to use, one of
         *                    {@link TracingConfig#RECORD_UNTIL_FULL} or
         *                    {@link TracingConfig#RECORD_CONTINUOUSLY}.
         * @return The builder to facilitate chaining.
         */
        @NonNull
        public Builder setTracingMode(@TracingMode int tracingMode) {
            mTracingMode = tracingMode;
            return this;
        }
    }
}
