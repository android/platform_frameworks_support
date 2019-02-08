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
import androidx.paging.futures.FutureCallback;
import androidx.paging.futures.Futures;

import com.google.common.util.concurrent.ListenableFuture;

import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

public class Pager<K, V> {

    @SuppressWarnings({"WeakerAccess", "unchecked"}) /* synthetic accessor */
    Pager(@NonNull PagedList.Config config,
            @NonNull DataSource<K, V> source,
            @NonNull Executor notifyExecutor,
            @NonNull PageConsumer<V> pageConsumer,
            @Nullable AdjacentProvider<V> adjacentProvider,
            @NonNull DataSource.BaseResult<V> result) {
        mConfig = config;
        mSource = source;
        mNotifyExecutor = notifyExecutor;
        mPageConsumer = pageConsumer;
        if (adjacentProvider == null) {
            adjacentProvider = new SimpleAdjacentProvider<>();
        }
        mAdjacentProvider = adjacentProvider;
        System.out.println("initial prev " + result.prevKey + ", next " + result.nextKey);
        mPrevKey = (K) result.prevKey;
        mNextKey = (K) result.nextKey;
        /*
        if (mSource.mType == DataSource.KeyType.PAGE_KEYED) {
            mLoadStateManager.setState(PagedList.LoadType.START,
                    mPrevKey == null ? PagedList.LoadState.DONE : PagedList.LoadState.IDLE, null);
            mLoadStateManager.setState(PagedList.LoadType.END,
                    mNextKey == null ? PagedList.LoadState.DONE : PagedList.LoadState.IDLE, null);
        }*/
    }

    @NonNull
    private final PagedList.Config mConfig;
    @NonNull
    private final DataSource<K, V> mSource;
    @NonNull
    private final Executor mNotifyExecutor;
    @NonNull
    private final PageConsumer<V> mPageConsumer;
    @NonNull
    private final AdjacentProvider<V> mAdjacentProvider;
    @Nullable
    private K mPrevKey;
    @Nullable
    private K mNextKey;

    private final AtomicBoolean mDetached = new AtomicBoolean(false);

    PagedList.LoadStateManager mLoadStateManager = new PagedList.LoadStateManager() {
        @Override
        protected void onStateChanged(@NonNull PagedList.LoadType type,
                @NonNull PagedList.LoadState state, @Nullable Throwable error) {
            System.out.println("onStateChanged " + type + ", state " + state);
            mPageConsumer.onStateChanged(type, state, error);
        }
    };

    private void listenTo(@NonNull final PagedList.LoadType type,
            @NonNull ListenableFuture<? extends DataSource.BaseResult<V>> future) {
        // TODO: check if is invalid on BG executor, if it is, detach()
        Futures.addCallback(future, new FutureCallback<DataSource.BaseResult<V>>() {
            @Override
            public void onSuccess(DataSource.BaseResult<V> value) {
                onLoadSuccess(type, value);
            }

            @Override
            public void onError(@NonNull Throwable throwable) {
                onLoadError(type, throwable);
            }
        }, mNotifyExecutor);
    }

    interface PageConsumer<V> {
        // return true if we need to fetch more
        boolean onPageResult(
                @NonNull PagedList.LoadType type,
                @NonNull DataSource.BaseResult<V> pageResult);

        void onStateChanged(@NonNull PagedList.LoadType type,
                @NonNull PagedList.LoadState state, @Nullable Throwable error);
    }

    interface AdjacentProvider<V> {
        V getFirstLoadedItem();

        V getLastLoadedItem();

        int getFirstLoadedItemIndex();

        int getLastLoadedItemIndex();

        // TODO: use this for non-contiguouspagedlist usecase
        void onPageResultResolution(
                @NonNull PagedList.LoadType type,
                @NonNull PageResult<V> pageResult);
    }

    private void onLoadSuccess(PagedList.LoadType type, DataSource.BaseResult<V> value) {
        if (isDetached()) {
            // abort!
            return;
        }

        if (mPageConsumer.onPageResult(type, value)) {
            if (type.equals(PagedList.LoadType.START)) {
                System.out.println("---- scheduling prepend, key " + mPrevKey);
                //noinspection unchecked
                mPrevKey = (K) value.prevKey;
                schedulePrepend();
            } else if (type.equals(PagedList.LoadType.END)) {
                System.out.println("---- scheduling append,  key " + mNextKey);
                //noinspection unchecked
                mNextKey = (K) value.nextKey;
                scheduleAppend();
            } else {
                throw new IllegalStateException("Can only fetch more during append/prepend");
            }
        } else {
            boolean isDone = value.data.isEmpty()
                    || (mSource.mType == DataSource.KeyType.PAGE_KEYED
                        && (type.equals(PagedList.LoadType.START) && mPrevKey == null)
                            || type.equals(PagedList.LoadType.END) && mNextKey == null);
            mLoadStateManager.setState(type,
                    isDone ? PagedList.LoadState.DONE : PagedList.LoadState.IDLE, null);
        }
    }

    private void onLoadError(PagedList.LoadType type, Throwable throwable) {
        if (isDetached()) {
            // abort!
            return;
        }
        // TODO: handle nesting
        PagedList.LoadState state = mSource.isRetryableError(throwable)
                ? PagedList.LoadState.RETRYABLE_ERROR : PagedList.LoadState.ERROR;
        mLoadStateManager.setState(type, state, throwable);
    }

    public void trySchedulePrepend() {
        if (mLoadStateManager.getStart().equals(PagedList.LoadState.IDLE)) {
            schedulePrepend();
        }
    }

    public void tryScheduleAppend() {
        if (mLoadStateManager.getEnd().equals(PagedList.LoadState.IDLE)) {
            scheduleAppend();
        }
    }

    private void schedulePrepend() {
        K key;
        switch(mSource.mType) {
            case POSITIONAL:
                //noinspection unchecked
                key = (K) ((Integer) (mAdjacentProvider.getFirstLoadedItemIndex() - 1));
                break;
            case PAGE_KEYED:
                key = mPrevKey;
                System.out.println("schedule prepend, key " + key);
                break;
            case ITEM_KEYED:
                key = mSource.getKey(mAdjacentProvider.getFirstLoadedItem());
                break;
            default:
                throw new IllegalArgumentException("unknown source type");
        }
        mLoadStateManager.setState(PagedList.LoadType.START, PagedList.LoadState.LOADING,null);
        listenTo(PagedList.LoadType.START, mSource.load(new DataSource.Params<>(
                DataSource.LoadType.START,
                key,
                mConfig.initialLoadSizeHint,
                mConfig.enablePlaceholders,
                mConfig.pageSize)));
    }

    private void scheduleAppend() {
        K key;
        switch(mSource.mType) {
            case POSITIONAL:
                //noinspection unchecked
                key = (K) ((Integer) (mAdjacentProvider.getLastLoadedItemIndex() + 1));
                break;
            case PAGE_KEYED:
                key = mNextKey;
                System.out.println("scheduleAppend, next " + key);
                break;
            case ITEM_KEYED:
                key = mSource.getKey(mAdjacentProvider.getLastLoadedItem());
                break;
            default:
                throw new IllegalArgumentException("unknown source type");
        }
        System.out.println("schedule append, loading...");
        mLoadStateManager.setState(PagedList.LoadType.END, PagedList.LoadState.LOADING,null);
        listenTo(PagedList.LoadType.END, mSource.load(new DataSource.Params<>(
                DataSource.LoadType.END,
                key,
                mConfig.initialLoadSizeHint,
                mConfig.enablePlaceholders,
                mConfig.pageSize)));
    }

    void retry() {
        if (mLoadStateManager.getStart().equals(PagedList.LoadState.RETRYABLE_ERROR)) {
            schedulePrepend();
        }
        if (mLoadStateManager.getEnd().equals(PagedList.LoadState.RETRYABLE_ERROR)) {
            scheduleAppend();
        }
    }


    public boolean isDetached() {
        return mDetached.get();
    }

    public void detach() {
        mDetached.set(true);
    }

    static class SimpleAdjacentProvider<V> implements AdjacentProvider<V> {
        private int mFirstIndex;
        private int mLastIndex;

        private V mFirstItem;
        private V mLastItem;

        @Override
        public V getFirstLoadedItem() {
            return mFirstItem;
        }

        @Override
        public V getLastLoadedItem() {
            return mLastItem;
        }

        @Override
        public int getFirstLoadedItemIndex() {
            return mFirstIndex;
        }

        @Override
        public int getLastLoadedItemIndex() {
            return mLastIndex;
        }

        @Override
        public void onPageResultResolution(@NonNull PagedList.LoadType type, @NonNull PageResult<V> pageResult) {
            if (type == PagedList.LoadType.START) {
                mFirstIndex -= pageResult.page.size();
                mFirstItem = pageResult.page.get(0);
            } else if (type == PagedList.LoadType.END) {
                mLastIndex += pageResult.page.size();
                mLastItem = pageResult.page.get(pageResult.page.size() - 1);
            } else {
                mFirstIndex = pageResult.leadingNulls;
                mLastIndex = pageResult.leadingNulls + pageResult.page.size();
                mFirstItem = pageResult.page.get(0);
                mLastItem = pageResult.page.get(pageResult.page.size() - 1);
            }
        }
    }
}
