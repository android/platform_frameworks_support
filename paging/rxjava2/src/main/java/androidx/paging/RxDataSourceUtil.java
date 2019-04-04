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

import android.annotation.SuppressLint;

import androidx.annotation.NonNull;
import androidx.concurrent.futures.ResolvableFuture;

import com.google.common.util.concurrent.ListenableFuture;

import io.reactivex.Single;
import io.reactivex.disposables.CompositeDisposable;
import io.reactivex.disposables.Disposable;
import io.reactivex.functions.Action;
import io.reactivex.functions.BiConsumer;
import io.reactivex.functions.Consumer;

class RxDataSourceUtil {
    private static class ListenableFutureDisposeAction<T> implements Action {
        private ResolvableFuture<T> mFuture;

        ListenableFutureDisposeAction(ResolvableFuture<T> future) {
            mFuture = future;
        }

        @Override
        public void run() {
            mFuture.cancel(true);
        }
    }

    private static class ListenableFutureBiConsumer<T> implements BiConsumer<T, Throwable> {
        private ResolvableFuture<T> mFuture;

        ListenableFutureBiConsumer(ResolvableFuture<T> future) {
            mFuture = future;
        }

        @Override
        public void accept(T data, Throwable throwable) {
            if (throwable != null) {
                mFuture.setException(throwable);
            }

            mFuture.set(data);
        }
    }

    @SuppressLint("CheckResult")
    @SuppressWarnings("ResultOfMethodCallIgnored")
    @NonNull
    static <T> ListenableFuture<T> singleToListenableFuture(@NonNull Single<T> single,
            @NonNull ScheduledExecutor scheduledExecutor) {
        final CompositeDisposable compositeDisposable = new CompositeDisposable();
        final ResolvableFuture<T> future = ResolvableFuture.create();
        future.addListener(new Runnable() {
            @Override
            public void run() {
                if (future.isCancelled()) {
                    compositeDisposable.dispose();
                }
            }
        }, scheduledExecutor.getExecutor());
        single.subscribeOn(scheduledExecutor.getScheduler())
                .observeOn(scheduledExecutor.getScheduler())
                .doOnSubscribe(new Consumer<Disposable>() {
                    @Override
                    public void accept(Disposable disposable) {
                        compositeDisposable.add(disposable);
                    }
                })
                .doOnDispose(new ListenableFutureDisposeAction<>(future))
                .subscribe(new ListenableFutureBiConsumer<>(future));
        return future;
    }
}
