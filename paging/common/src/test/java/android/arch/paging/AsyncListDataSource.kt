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

package android.arch.paging

class AsyncListDataSource<T>(list: List<T>)
    : PositionalDataSource<T>() {
    val workItems: MutableList<() -> Unit> = ArrayList()
    private val listDataSource = ListDataSource(list)

    override fun loadInitial(requestedStartPosition: Int, requestedLoadSize: Int, pageSize: Int,
            callback: InitialLoadCallback<T>) {
        workItems.add {
            listDataSource.loadInitial(requestedStartPosition, requestedLoadSize, pageSize, callback)
        }
    }

    override fun loadRange(startPosition: Int, count: Int, callback: LoadCallback<T>) {
        workItems.add {
            listDataSource.loadRange(startPosition, count, callback)
        }
    }

    fun flush() {
        workItems.map { it() }
        workItems.clear()
    }
}
