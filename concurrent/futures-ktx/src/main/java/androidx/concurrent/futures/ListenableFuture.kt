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

package androidx.concurrent.futures

import com.google.common.util.concurrent.ListenableFuture
import kotlinx.coroutines.suspendCancellableCoroutine
import java.util.concurrent.CancellationException
import java.util.concurrent.ExecutionException
import java.util.concurrent.Executor
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Awaits for the completion of the [ListenableFuture] without blocking a thread.
 */
suspend fun <R> ListenableFuture<R>.await(): R {
    return await(DirectExecutor.INSTANCE)
}

/**
 * Awaits for the completion of the [ListenableFuture] without blocking a thread.
 */
suspend inline fun <R> ListenableFuture<R>.await(executor: Executor): R {
    // Fast path
    if (isDone) {
        try {
            return get()
        } catch (e: ExecutionException) {
            throw e.cause ?: e
        }
    }

    return suspendCancellableCoroutine { cancellableContinuation ->
        addListener(Runnable {
            try {
                cancellableContinuation.resume(get())
            } catch (throwable: Throwable) {
                val cause = throwable.cause ?: throwable
                when (throwable) {
                    is CancellationException -> cancellableContinuation.cancel(cause)
                    else -> cancellableContinuation.resumeWithException(cause)
                }
            }
        }, executor)
    }
}
