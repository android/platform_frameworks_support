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

package androidx.lifecycle

import android.os.Handler
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext

/**
 * Runs the given block when the [LifecycleOwner]'s [Lifecycle] is at least in
 * [Lifecycle.State.CREATED] state.
 *
 * @see Lifecycle.whenStateAtLeast for details
 */
suspend fun <T> LifecycleOwner.whenCreated(block: suspend CoroutineScope.() -> T): T =
    lifecycle.whenCreated(block)

/**
 * Runs the given block when the [Lifecycle] is at least in [Lifecycle.State.CREATED] state.
 *
 * @see Lifecycle.whenStateAtLeast for details
 */
suspend fun <T> Lifecycle.whenCreated(block: suspend CoroutineScope.() -> T): T {
    return whenStateAtLeast(Lifecycle.State.CREATED, block)
}

/**
 * Runs the given block when the [LifecycleOwner]'s [Lifecycle] is at least in
 * [Lifecycle.State.STARTED] state.
 *
 * @see Lifecycle.whenStateAtLeast for details
 */
suspend fun <T> LifecycleOwner.whenStarted(block: suspend CoroutineScope.() -> T): T =
    lifecycle.whenStarted(block)

/**
 * Runs the given block when the [Lifecycle] is at least in [Lifecycle.State.STARTED] state.
 *
 * @see Lifecycle.whenStateAtLeast for details
 */
suspend fun <T> Lifecycle.whenStarted(block: suspend CoroutineScope.() -> T): T {
    return whenStateAtLeast(Lifecycle.State.STARTED, block)
}

/**
 * Runs the given block when the [LifecycleOwner]'s [Lifecycle] is at least in
 * [Lifecycle.State.RESUMED] state.
 *
 * @see Lifecycle.whenStateAtLeast for details
 */
suspend fun <T> LifecycleOwner.whenResumed(block: suspend CoroutineScope.() -> T): T =
    lifecycle.whenResumed(block)

/**
 * Runs the given block when the [Lifecycle] is at least in [Lifecycle.State.RESUMED] state.
 *
 * @see Lifecycle.whenStateAtLeast for details
 */
suspend fun <T> Lifecycle.whenResumed(block: suspend CoroutineScope.() -> T): T {
    return whenStateAtLeast(Lifecycle.State.RESUMED, block)
}

/**
 * Runs the given [block] on a [CoroutineDispatcher] that executes the [block] on the main thread
 * and suspends the execution unless the [Lifecycle]'s state is at least [minState].
 *
 * If the [Lifecycle] moves to a lesser state while the [block] is running, the [block] will
 * be suspended until the [Lifecycle] reaches to a state greater or equal to [minState].
 *
 * Note that this won't effect any sub coroutine if they use a different [CoroutineDispatcher].
 * However, the [block] will not resume execution when the sub coroutine finishes unless the
 * [Lifecycle] is at least in [minState].
 *
 * If the [Lifecycle] is destroyed while the [block] is suspended, the [block] will be cancelled.
 *
 * ```
 * // running a block only if lifecycle is STARTED
 * viewLifecycle.whenStateAtLeast(Lifecycle.State.STARTED) {
 *     // here, we are on the main thread and view lifecycle is guaranteed to be STARTED or RESUMED.
 *     // We can safely access our views.
 *     loadingBar.visibility = View.VISIBLE
 *     try {
 *         // we can call any suspend function
 *         val data = withContext(Dispatchers.IO) {
 *             // this will run in IO thread pool. It will keep running as long as Lifecycle
 *             // is not DESTROYED. If it is destroyed, this coroutine will be cancelled as well.
 *             // However, we CANNOT access Views here.
 *
 *             // NOTE that you should never ever explicitly use an IO dispatcher in your UI code.
 *             // We are using withContext(Dispatchers.IO) here for demonstration purposes. Such
 *             // code should live in your business logic classes and your UI should use a ViewModel
 *             // to access it.
 *             api.getUser()
 *         }
 *         // this line will execute on the main thread and only if the lifecycle is in at least
 *         // STARTED state (STARTED is the parameter we've passed to whenStateAtLeast)
 *         // Because of this guarantee, we can safely access the UI again.
 *         loadingBar.visibility = View.GONE
 *         nameTextView.text = user.name
 *         lastNameTextView.text = user.lastName
 *     } catch(ex : Throwable) {
 *         // same as above, this code can safely access UI elements because it only runs if
 *         // view lifecycle is at least STARTED
 *         loadingBar.visibility = View.GONE
 *         showErrorDialog(ex)
 *     } finally {
 *         // in case of cancellation, the line might run even if the Lifecycle is not DESTROYED.
 *         // You cannot access Views here.
 *     }
 * }
 * ```
 *
 * @param minState The desired minimum state to run the [block].
 * @param block The block to run when the lifecycle is at least in [minState].
 * @return <T> The return value of the [block]
 */
suspend fun <T> Lifecycle.whenStateAtLeast(
    minState: Lifecycle.State,
    block: suspend CoroutineScope.() -> T
) = withContext(Dispatchers.Main) {
    val job = coroutineContext[Job] ?: throw IllegalStateException("must have a parent job")
    val dispatcher = PausingDispatcher(MAIN_HANDLER)
    val controller =
        LifecycleController(this@whenStateAtLeast, minState, dispatcher.dispatchQueue, job)
    try {
        withContext(dispatcher, block)
    } finally {
        controller.finish()
    }
}

/**
 * A [CoroutineDispatcher] implementation that also has [pause] and [finish] methods.
 *
 * When the dispatcher is paused, it does not run resume any continuation until it is resumed.
 * If finished, it runs all pending continuations to be able to run the finally blocks.
 */
internal class PausingDispatcher(
    /**
     * The handler into which messages are posted
     */
    handler: Handler
) : CoroutineDispatcher() {
    /**
     * helper class to maintain state and enqueued continuations.
     */
    @JvmField
    internal val dispatchQueue = DispatchQueue(handler)

    override fun dispatch(context: CoroutineContext, block: Runnable) {
        dispatchQueue.runOrEnqueue(maybeWrap(block))
    }

    /**
     * Wraps the given runnable into a [Wrapper] so that we can reference back to this dispatcher
     * when it is time to run it.
     */
    private fun maybeWrap(runnable: Runnable): Wrapper =
        if (runnable is Wrapper && runnable.dispatchQueue === this.dispatchQueue) {
            runnable
        } else {
            Wrapper(dispatchQueue, runnable)
        }

    /**
     * A wrapper for the enqueued runnable that keeps a reference back to the [PausingDispatcher].
     */
    internal class Wrapper(
        val dispatchQueue: DispatchQueue,
        val real: Runnable
    ) : Runnable {
        override fun run() {
            real.run()
        }
    }
}