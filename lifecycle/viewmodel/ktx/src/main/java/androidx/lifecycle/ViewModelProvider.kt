<<<<<<< HEAD   (810747 Merge "Merge empty history for sparse-5626174-L1780000033228)
/*
 * Copyright 2017 The Android Open Source Project
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
import kotlin.reflect.KClass

/**
 * Returns an existing ViewModel or creates a new one in the scope (usually, a fragment or
 * an activity), associated with this `ViewModelProvider`.
 *
 * @see ViewModelProvider.get(Class)
 */
@MainThread
inline fun <reified VM : ViewModel> ViewModelProvider.get() = get(VM::class.java)

/**
 * An implementation of [Lazy] used by [androidx.fragment.app.Fragment.viewModels] and
 * [androidx.activity.ComponentActivity.viewmodels].
 *
 * [ownerProducer] is a lambda that will be called during initialization, [VM] will be created
 * in the scope of returned [ViewModelStoreOwner].
 *
 * [factoryProducer] is a lambda that will be called during initialization,
 * returned [ViewModelProvider.Factory] will be used for creation of [VM]
 */
class ViewModelLazy<VM : ViewModel>(
    private val viewModelClass: KClass<VM>,
    private val ownerProducer: () -> ViewModelStoreOwner,
    private val factoryProducer: () -> ViewModelProvider.Factory
) : Lazy<VM> {
    private var cached: VM? = null

    override val value: VM
        get() {
            val viewModel = cached
            return if (viewModel == null) {
                val factory = factoryProducer()
                val owner = ownerProducer()
                ViewModelProvider(owner, factory).get(viewModelClass.java).also { cached = it }
            } else {
                viewModel
            }
        }

    override fun isInitialized() = cached != null
}
=======
>>>>>>> BRANCH (2c954e Merge "Merge cherrypicks of [988730] into sparse-5676727-L53)
