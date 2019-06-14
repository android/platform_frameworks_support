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

package androidx.loader.app.test

import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner

class LoaderOwner : LifecycleOwner, ViewModelStoreOwner {

    private val lifecycle = LifecycleRegistry(this)
    private val viewModelStore = ViewModelStore()

    init {
        lifecycle.handleLifecycleEvent(Lifecycle.Event.ON_START)
    }

    override fun getLifecycle() = lifecycle

    override fun getViewModelStore() = viewModelStore
}
