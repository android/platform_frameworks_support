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

import com.google.common.util.concurrent.ListenableFuture;

import java.util.List;

public abstract class ListenablePositionalDataSource<Value> extends DataSource<Integer, Value> {
    ListenablePositionalDataSource() {
        super(KeyType.POSITIONAL);
    }

    @Override
    final ListenableFuture<? extends BaseResult<Value>> load(@NonNull Params<Integer> params) {
        if (params.type == LoadType.INITIAL) {
            int initialPosition = 0;
            int initialLoadSize = params.initialLoadSize;
            if (params.key != null) {
                initialPosition = params.key;

                if (params.placeholdersEnabled) {
                    // snap load size to page multiple (minimum two)
                    initialLoadSize = Math.max(initialLoadSize / params.pageSize, 2)
                            * params.pageSize;

                    // move start so the load is centered around the key, not starting at it
                    final int idealStart = initialPosition - initialLoadSize / 2;
                    initialPosition = Math.max(0, idealStart / params.pageSize * params.pageSize);
                } else {
                    // not tiled, so don't try to snap or force multiple of a page size
                    initialPosition = initialPosition - initialLoadSize / 2;
                }

            }
            PositionalDataSource.LoadInitialParams initParams =
                    new PositionalDataSource.LoadInitialParams(
                            initialPosition,
                            initialLoadSize,
                            params.pageSize,
                            params.placeholdersEnabled);
            return loadInitial(initParams);
        } else {
            int startIndex = params.key;
            int loadSize = params.pageSize;
            if (params.type == LoadType.START) {
                loadSize = Math.min(loadSize, startIndex + 1);
                startIndex = startIndex - loadSize + 1;
            }
            return loadRange(new PositionalDataSource.LoadRangeParams(startIndex, loadSize));
        }
    }

    /**
     * Load initial list data.
     * <p>
     * This method is called to load the initial page(s) from the DataSource.
     * <p>
     * Result list must be a multiple of pageSize to enable efficient tiling.
     *
     * @param params Parameters for initial load, including requested start position, load size, and
     *               page size.
     * @return ListenableFuture of the loaded data.
     */
    @NonNull
    public abstract ListenableFuture<InitialResult<Value>> loadInitial(
            @NonNull PositionalDataSource.LoadInitialParams params);


    /**
     * Called to load a range of data from the DataSource.
     * <p>
     * This method is called to load additional pages from the DataSource after the
     * LoadInitialCallback passed to dispatchLoadInitial has initialized a PagedList.
     * <p>
     * Unlike {@link #loadInitial(PositionalDataSource.LoadInitialParams)}, this method must return
     * the number of items requested, at the position requested.
     *
     * @param params Parameters for load, including start position and load size.
     * @return ListenableFuture of the loaded data.
     */
    @NonNull
    public abstract ListenableFuture<RangeResult<Value>> loadRange(
            @NonNull PositionalDataSource.LoadRangeParams params);

    @Nullable
    @Override
    final Integer getKey(@NonNull Value item) {
        return null;
    }

    public static class InitialResult<V> extends BaseResult<V> {
        public InitialResult(@NonNull List<V> data, int position, int totalCount) {
            super(data, null, null, position, totalCount - data.size() - position, 0, true);
            if (data.isEmpty() && position != 0) {
                throw new IllegalArgumentException(
                        "Initial result cannot be empty if items are present in data set.");
            }
        }

        public InitialResult(@NonNull List<V> data, int position) {
            super(data, null, null, 0, 0, position, false);
            if (data.isEmpty() && position != 0) {
                throw new IllegalArgumentException(
                        "Initial result cannot be empty if items are present in data set.");
            }
        }
    }

    public static class RangeResult<V> extends BaseResult<V> {
        public RangeResult(@NonNull List<V> data) {
            super(data, null, null, 0, 0, 0, false);
        }
    }
}
