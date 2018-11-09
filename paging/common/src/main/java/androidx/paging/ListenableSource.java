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

package androidx.paging;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.arch.core.util.Function;

import com.google.common.util.concurrent.ListenableFuture;
import java.util.List;
import java.util.concurrent.Executor;

abstract class ListenableSource<K, V> extends DataSource<K, V> {
    enum LoadType {
        INITIAL,
        START,
        END,
        // TODO: only used for positional. could use 'inner' :P
        TILE,
    }

    static class Params<K> {
        @NonNull
        final LoadType type;
        // TODO: can be NULL for init, otherwise non-null :P
        // TODO: would like to avoid allocating here
        final K key;
        final int requestedLoadSize;
        final boolean placeholdersEnabled;

        // TODO: only needed for positional initial load :P
        final int pageSize;

        Params(@NonNull LoadType type, K key, int requestedLoadSize, boolean placeholdersEnabled,
                int pageSize) {
            this.type = type;
            this.key = key;
            this.requestedLoadSize = requestedLoadSize;
            this.placeholdersEnabled = placeholdersEnabled;
            this.pageSize = pageSize;
        }
    }

    // NOTE: keeping all data in subclasses for now, to enable public members
    // TODO: reconsider, only providing accessors to minimize subclass complexity
    static abstract class BaseResult<V> {
        final List<V> mData;
        final Object mNextKey;
        final Object mPrevKey;
        final int mLeadingNulls;
        final int mTrailingNulls;
        final int mOffset;

        protected BaseResult(List<V> data, Object nextKey, Object prevKey, int leadingNulls,
                int trailingNulls, int offset) {
            mData = data;
            mNextKey = nextKey;
            mPrevKey = prevKey;
            mLeadingNulls = leadingNulls;
            mTrailingNulls = trailingNulls;
            mOffset = offset;
        }
    }

    enum KeyType {
        // TODO: PAGE_INDEX?,
        POSITIONAL,
        PAGE_KEYED,
        ITEM_KEYED,
    }

    @NonNull
    final KeyType mType;

    ListenableSource(@NonNull KeyType type) {
        mType = type;
    }

    abstract ListenableFuture<? extends BaseResult<V>> load(@NonNull Params<K> params);

    @Nullable
    abstract K getKey(@NonNull V item);

    @Nullable
    abstract K getKey(int lastLoad, @Nullable V item);

    boolean isRetryableError(@NonNull Throwable error) {
        return false;
    }

    void initExecutor(@NonNull Executor executor) {
        mExecutor = executor;
    }

    /**
     * In reality, this is null until loadInitial is called
     */
    @Nullable
    private Executor mExecutor;

    @NonNull
    public Executor getExecutor() {
        if (mExecutor == null) {
            throw new IllegalStateException("too soon, executor!");
        }
        return mExecutor;
    }

    @NonNull
    @Override
    public <ToValue> DataSource<K, ToValue> mapByPage(
            @NonNull Function<List<V>, List<ToValue>> function) {
        throw new IllegalArgumentException("TODO");
    }

    @NonNull
    @Override
    public <ToValue> DataSource<K, ToValue> map(@NonNull Function<V, ToValue> function) {
        throw new IllegalArgumentException("TODO");
    }

    @Override
    boolean isContiguous() {
        return true;
    }

    boolean supportsPageDropping() {
        return true;
    }
}
