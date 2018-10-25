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

package androidx.paging;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.concurrent.futures.ResolvableFuture;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

public abstract class ListenablePageKeyedDataSource<Key, Value> extends DataSource<Key, Value> {
    ListenablePageKeyedDataSource() {
        super(KeyType.PAGE_KEYED);
    }

    @Override
    final ListenableFuture<? extends BaseResult<Value>> load(
            @NonNull Params<Key> params) {
        if (params.type == LoadType.INITIAL) {
            PageKeyedDataSource.LoadInitialParams<Key> initParams =
                    new PageKeyedDataSource.LoadInitialParams<>(
                            params.initialLoadSize, params.placeholdersEnabled);
            return loadInitial(initParams);
        } else {
            if (params.key == null) {
                // null key, immediately return empty data
                ResolvableFuture<BaseResult<Value>> future = ResolvableFuture.create();
                future.set(BaseResult.<Value>empty());
                return future;
            }

            //noinspection ConstantConditions
            PageKeyedDataSource.LoadParams<Key> loadMoreParams =
                    new PageKeyedDataSource.LoadParams<>(params.key, params.pageSize);

            if (params.type == LoadType.START) {
                return loadBefore(loadMoreParams);
            } else if (params.type == LoadType.END) {
                return loadAfter(loadMoreParams);
            }
        }
        throw new IllegalArgumentException("Unsupported type " + params.type.toString());
    }

    /**
     * Load initial data.
     * <p>
     * This method is called first to initialize a PagedList with data. If it's possible to count
     * the items that can be loaded by the DataSource, it's recommended to pass the position and
     * count to the
     * {@link InitialResult#InitialResult(List, int, int, Key, Key) InitialResult constructor}. This
     * enables PagedLists presenting data from this source to display placeholders to represent
     * unloaded items.
     * <p>
     * {@link PageKeyedDataSource.LoadInitialParams#requestedLoadSize} is a hint, not a requirement,
     * so it may be may be altered or ignored.
     *
     * @param params Parameters for initial load, including requested load size.
     * @return ListenableFuture of the loaded data.
     */
    @NonNull
    public abstract ListenableFuture<InitialResult<Key, Value>> loadInitial(
            @NonNull PageKeyedDataSource.LoadInitialParams<Key> params);
    @NonNull
    public abstract ListenableFuture<Result<Key, Value>> loadBefore(
            @NonNull PageKeyedDataSource.LoadParams<Key> params);
    @NonNull
    public abstract ListenableFuture<Result<Key, Value>> loadAfter(
            @NonNull PageKeyedDataSource.LoadParams<Key> params);

    @Nullable
    @Override
    Key getKey(@NonNull Value item) {
        return null;
    }

    @Override
    boolean supportsPageDropping() {
        /* To support page dropping when PageKeyed, we'll need to:
         *    - Stash keys for every page we have loaded (can id by index relative to loadInitial)
         *    - Drop keys for any page not adjacent to loaded content
         *    - And either:
         *        - Allow impl to signal previous page key: onResult(data, nextPageKey, prevPageKey)
         *        - Re-trigger loadInitial, and break assumption it will only occur once.
         */
        return false;
    }

    public static class InitialResult<Key, Value> extends BaseResult<Value> {
        public InitialResult(@NonNull List<Value> data, int position, int totalCount,
                @Nullable Key previousPageKey, @Nullable Key nextPageKey) {
            super(data, previousPageKey, nextPageKey,
                    position, totalCount - data.size() - position, position, true);
        }

        public InitialResult(@NonNull List<Value> data, @Nullable Key previousPageKey,
                @Nullable Key nextPageKey) {
            super(data, previousPageKey, nextPageKey, 0, 0, 0, false);
        }
    }

    public static class Result<Key, Value> extends BaseResult<Value> {
        public Result(@NonNull List<Value> data, @Nullable Key adjacentPageKey) {
            super(data, adjacentPageKey, adjacentPageKey, 0, 0, 0, false);
        }
    }
}
