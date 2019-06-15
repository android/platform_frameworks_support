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

package androidx.paging

import androidx.paging.futures.DirectExecutor
import androidx.paging.futures.transform
import com.google.common.util.concurrent.ListenableFuture

class PagedSourceWrapper<Key : Any, Value : Any>(
    val dataSource: DataSource<Key, Value>
) : PagedSource<Key, Value>() {
    override val keyProvider: KeyProvider<Key, Value> = when (dataSource.type) {
        DataSource.KeyType.POSITIONAL -> {
            @Suppress("UNCHECKED_CAST") // TODO: Suspicious cast?
            KeyProvider.Positional<Value>() as KeyProvider<Key, Value>
        }
        DataSource.KeyType.PAGE_KEYED -> KeyProvider.PageKey()
        DataSource.KeyType.ITEM_KEYED -> object : KeyProvider.ItemKey<Key, Value>() {
            override fun getKey(item: Value) = dataSource.getKeyInternal(item)
        }
    }

    override fun load(params: LoadParams<Key>): ListenableFuture<LoadResult<Key, Value>> {
        val loadType = when (params.loadType) {
            LoadType.INITIAL -> DataSource.LoadType.INITIAL
            LoadType.START -> DataSource.LoadType.START
            LoadType.END -> DataSource.LoadType.END
        }

        val dataSourceParams = DataSource.Params(
            loadType,
            params.key,
            params.loadSize,
            params.placeholdersEnabled,
            params.pageSize
        )
        return dataSource.load(dataSourceParams)
            .transform(
                // TODO: Real result for itemsBefore and itemsAfter.
                androidx.arch.core.util.Function { result: DataSource.BaseResult<Value> ->
                    @Suppress("UNCHECKED_CAST") // TODO: Suspicious cast?
                    LoadResult(
                        result.leadingNulls,
                        result.trailingNulls,
                        result.nextKey as Key?,
                        result.prevKey as Key?,
                        result.data
                    )
                },
                DirectExecutor
            )
    }

    override fun isRetryableError(error: Throwable) = dataSource.isRetryableError(error)
}