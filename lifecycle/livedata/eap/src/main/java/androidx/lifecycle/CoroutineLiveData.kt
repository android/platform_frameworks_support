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

import androidx.annotation.MainThread
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.EmptyCoroutineContext
import kotlin.experimental.ExperimentalTypeInference

/**
 * Interface that allows controlling a [LiveData] from a coroutine block.
 *
 * @see liveData
 */
interface LiveDataScope<T> {
    /**
     * Set's the [LiveData]'s value to the given [value]. If you've called [yieldSource] previously,
     * calling [yield] will remove that source.
     *
     * Note that this function suspends until the value is set on the [LiveData].
     *
     * @param value The new value for the [LiveData]
     *
     * @see yieldSource
     */
    suspend fun yield(value: T)

    /**
     * Add the given [LiveData] as a source, similar to [MediatorLiveData.addSource]. Calling this
     * method will remove any source that was yielded before via [yieldSource].
     *
     * @param source The [LiveData] instance whose values will be dispatched from the current
     * [LiveData].
     *
     * @see yield
     * @see MediatorLiveData.addSource
     * @see MediatorLiveData.removeSource
     */
    suspend fun yieldSource(source: LiveData<T>)

    /**
     * Denotes the value of the [LiveData] when this block is started.
     *
     * If it is the first time block is running, [initialValue] will be `null`. You can use this
     * value to check what was then latest value `yield`ed by your `block` before it got cancelled.
     *
     * Note that if the block called [yieldSource], then `initialValue` will be last value
     * dispatched by the `source` [LiveData].
     */
    val initialValue: T?
}

internal class LiveDataScopeImpl<T>(
    private var target: CoroutineLiveData<T>,
    context: CoroutineContext,
    override val initialValue: T? = target.value
) : LiveDataScope<T> {
    // use `liveData` provided context + main dispatcher to communicate with the target
    // LiveData. This gives us main thread safety as well as cancellation cooperation
    private val coroutineContext = context + Dispatchers.Main

    override suspend fun yieldSource(source: LiveData<T>) = withContext(coroutineContext) {
        target.yieldSource(source)
    }

    override suspend fun yield(value: T) = withContext(coroutineContext) {
        target.clearYieldedSource()
        target.value = value
    }
}

internal typealias Block<T> = suspend LiveDataScope<T>.() -> Unit

/**
 * Handles running a block at most once to completion.
 */
internal class BlockRunner<T>(
    private val liveData: CoroutineLiveData<T>,
    private val block: Block<T>,
    private val timeoutInMs: Long,
    private val scope: CoroutineScope,
    private val onDone: () -> Unit
) {
    // currently running block job.
    private var runningJob: Job? = null

    // cancelation job created in cancel.
    private var cancellationJob: Job? = null

    @MainThread
    fun maybeRun() {
        cancellationJob?.cancel()
        cancellationJob = null
        if (runningJob != null) {
            return
        }
        runningJob = scope.launch {
            val liveDataScope = LiveDataScopeImpl(liveData, coroutineContext)
            block(liveDataScope)
            onDone()
        }
    }

    @MainThread
    fun cancel() {
        if (cancellationJob != null) {
            error("Cancel call cannot happen without a maybeRun")
        }
        cancellationJob = scope.launch(Dispatchers.Main) {
            delay(timeoutInMs)
            if (!liveData.hasActiveObservers()) {
                // one last check on active observers to avoid any race condition between starting
                // a running coroutine and cancelation
                runningJob?.cancel()
                runningJob = null
            }
        }
    }
}

internal class CoroutineLiveData<T>(
    context: CoroutineContext = EmptyCoroutineContext,
    timeoutInMs: Long = 5000,
    block: Block<T>
) : MediatorLiveData<T>() {
    private var blockRunner: BlockRunner<T>?

    init {
        // use an intermediate supervisor job so that if we cancel individual block runs due to losing
        // observers, it won't cancel the given context as we only cancel w/ the intention of possibly
        // relaunching using the same parent context.
        val supervisorJob = SupervisorJob(context[Job])

        // The scope for this LiveData where we launch every block Job.
        // We default to Main dispatcher but developer can override it.
        // The supervisor job is added last to isolate block runs.
        val scope = CoroutineScope(Dispatchers.Main + context + supervisorJob)
        blockRunner = BlockRunner(
            liveData = this,
            block = block,
            timeoutInMs = timeoutInMs,
            scope = scope
        ) {
            blockRunner = null
        }
    }

    // The source we are delegated to, sent from LiveDataScope#yieldSource
    // TODO We track this specifically since [MediatorLiveData] does not provide access to it.
    // We should eventually get rid of this and provide the internal API from MediatorLiveData after
    // the EAP.
    private var yieldedSource: LiveData<T>? = null

    @MainThread
    internal fun yieldSource(source: LiveData<T>) {
        clearYieldedSource()
        yieldedSource = source
        addSource(source) {
            value = it
        }
    }

    @MainThread
    internal fun clearYieldedSource() {
        yieldedSource?.let {
            removeSource(it)
            yieldedSource = null
        }
    }

    override fun onActive() {
        super.onActive()
        blockRunner?.maybeRun()
    }

    override fun onInactive() {
        super.onInactive()
        blockRunner?.cancel()
    }
}

/**
 * Builds a LiveData that has values yielded from the given [block] that executes on a
 * [LiveDataScope].
 *
 * The [block] starts executing when the returned [LiveData] becomes active ([LiveData.onActive]).
 * If the [LiveData] becomes inactive ([LiveData.onInactive]) while the [block] is executing, it
 * will be cancelled after [timeoutInMs] milliseconds unless the [LiveData] becomes active again
 * before that timeout (to gracefully handle cases like Activity rotation). Any value
 * [LiveDataScope.yield]ed from a cancelled [block] will be ignored.
 *
 * After a cancellation, if the [LiveData] becomes active again, the [block] will be re-executed
 * from the beginning. If you would like to continue the operations based on where it was stopped
 * last, you can use the [LiveDataScope.currentValue] function to get the last
 * [LiveDataScope.yield]ed value.

 * If the [block] completes successfully *or* is cancelled due to reasons other than [LiveData]
 * becoming inactive, it *will not* be re-executed even after [LiveData] goes through active
 * inactive cycle.
 *
 * As a best practice, it is important for the [block] to cooperate in cancellation. See kotlin
 * coroutines documentation for details
 * https://kotlinlang.org/docs/reference/coroutines/cancellation-and-timeouts.html.
 *
 * ```
 * // a simple LiveData that receives value 3, 3 seconds after being observed for the first time.
 * val data : LiveData<Int> = liveData {
 *     delay(3000)
 *     yield(3)
 * }
 *
 *
 * // a LiveData that fetches a `User` object based on a `userId` and refreshes it every 30 seconds
 * // as long as it is observed
 * val userId : LiveData<String> = ...
 * val user = userId.switchMap { id ->
 *     liveData {
 *       while(true) {
 *         // note that `while(true)` is fine because the `delay(30_000)` below will cooperate in
 *         // cancellation if LiveData is not actively observed anymore
 *         val data = api.fetch(id) // errors are ignored for brevity
 *         yield(data)
 *         delay(30_000)
 *       }
 *     }
 * }
 *
 * // A retrying data fetcher with doubling back-off
 * val user = liveData {
 *     var backOffTime = 1_000
 *     var succeeded = false
 *     while(!succeeded) {
 *         try {
 *             yield(api.fetch(id))
 *             succeeded = true
 *         } catch(ioError : IOException) {
 *             delay(backOffTime)
 *             backOffTime *= minOf(backOffTime * 2, 60_000)
 *         }
 *     }
 * }
 *
 * // a LiveData that tries to load the `User` from local cache first and then tries to fetch
 * // from the server and also yields the updated value
 * val user = liveData {
 *     // dispatch loading first
 *     yield(LOADING(id))
 *     // check local storage
 *     val cached = cache.loadUser(id)
 *     if (cached != null) {
 *         yield(cached)
 *     }
 *     if (cached == null || cached.isStale()) {
 *         val fresh = api.fetch(id) // errors are ignored for brevity
 *         cache.save(fresh)
 *         yield(fresh)
 *     }
 * }
 *
 * // a LiveData that immediately receives a LiveData<User> from the database and yields it as a
 * // source but also tries to back-fill the database from the server
 * val user = liveData {
 *     val fromDb: LiveData<User> = roomDatabase.loadUser(id)
 *     yieldSource(fromDb)
 *     val updated = api.fetch(id) // errors are ignored for brevity
 *     // Since we are using Room here, updating the database will update the `fromDb` LiveData
 *     // that was obtained above. See Room's documentation for more details.
 *     // https://developer.android.com/training/data-storage/room/accessing-data#query-observable
 *     roomDatabase.insert(updated)
 * }
 * ```
 */
@UseExperimental(ExperimentalTypeInference::class)
fun <T> liveData(
    context: CoroutineContext = EmptyCoroutineContext,
    timeoutInMs: Long = 5000,
    @BuilderInference block: suspend LiveDataScope<T>.() -> Unit
): LiveData<T> = CoroutineLiveData(context, timeoutInMs, block)