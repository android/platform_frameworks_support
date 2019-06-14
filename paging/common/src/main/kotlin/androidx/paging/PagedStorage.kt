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

import androidx.annotation.RestrictTo
import java.util.AbstractList

/**
 * Class holding the pages of data backing a [PagedList], presenting sparse loaded data as a List.
 *
 * It has two modes of operation: contiguous and non-contiguous (tiled). This class only holds
 * data, and does not have any notion of the ideas of async loads, or prefetching.
 *
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
class PagedStorage<T : Any> : AbstractList<T>, Pager.AdjacentProvider<T> {
    /**
     * List of pages in storage.
     *
     * Two storage modes:
     *
     * Contiguous - all content in pages is valid and loaded, but may return `false` from [isTiled].
     * Safe to access any item in any page.
     *
     * Non-contiguous - pages may have nulls or a placeholder page, [isTiled] always returns `true`.
     * pages may have nulls, or placeholder (empty) pages while content is loading.
     */
    private val pages: ArrayList<List<T>?>

    var leadingNullCount: Int = 0
        private set

    var trailingNullCount: Int = 0
        private set

    var positionOffset: Int = 0
        private set
    /**
     * Number of loaded items held by [pages]. When tiling, doesn't count unloaded pages in [pages].
     * If tiling is disabled, same as [storageCount].
     *
     * This count is the one used for trimming.
     */
    var storageCount: Int = 0
        private set

    /**
     *If pageSize > 0, tiling is enabled, 'pages' may have gaps, and leadingPages is set
     */
    private var pageSize: Int = 0

    var numberPrepended: Int = 0
        private set
    var numberAppended: Int = 0
        private set

    val middleOfLoadedRange
        get() = leadingNullCount + positionOffset + storageCount / 2

    // ------------- Adjacent Provider interface (contiguous-only) ------------------

    override val firstLoadedItem
        // Safe to access first page's first item here
        // If contiguous, pages can't be empty, can't hold null Pages, and items can't be empty
        get() = pages[0]?.first()

    override val lastLoadedItem
        // Safe to access last page's last item here:
        // If contiguous, pages can't be empty, can't hold null Pages, and items can't be empty
        get() = pages.last()?.last()

    override val firstLoadedItemIndex
        get() = leadingNullCount + positionOffset

    override val lastLoadedItemIndex
        get() = leadingNullCount + storageCount - 1 + positionOffset

    constructor() {
        leadingNullCount = 0
        pages = ArrayList()
        trailingNullCount = 0
        positionOffset = 0
        storageCount = 0
        pageSize = 1
        numberPrepended = 0
        numberAppended = 0
    }

    constructor(leadingNulls: Int, page: List<T>, trailingNulls: Int) : this() {
        init(leadingNulls, page, trailingNulls, 0)
    }

    private constructor(other: PagedStorage<T>) {
        leadingNullCount = other.leadingNullCount
        pages = ArrayList(other.pages)
        trailingNullCount = other.trailingNullCount
        positionOffset = other.positionOffset
        storageCount = other.storageCount
        pageSize = other.pageSize
        numberPrepended = other.numberPrepended
        numberAppended = other.numberAppended
    }

    fun snapshot() = PagedStorage(this)

    private fun init(leadingNulls: Int, page: List<T>, trailingNulls: Int, positionOffset: Int) {
        leadingNullCount = leadingNulls
        pages.clear()
        pages.add(page)
        trailingNullCount = trailingNulls

        this.positionOffset = positionOffset
        storageCount = page.size

        // initialized as tiled. There may be 3 nulls, 2 items, but we still call this tiled
        // even if it will break if nulls convert.
        pageSize = page.size

        numberPrepended = 0
        numberAppended = 0
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    fun init(
        leadingNulls: Int,
        page: List<T>,
        trailingNulls: Int,
        positionOffset: Int,
        callback: Callback
    ) {
        init(leadingNulls, page, trailingNulls, positionOffset)
        callback.onInitialized(size)
    }

    override fun get(index: Int): T? {
        // is it definitely outside 'pages'?
        val localIndex = index - leadingNullCount

        when {
            index < 0 || index >= size ->
                throw IndexOutOfBoundsException("Index: $index, Size: $size")
            localIndex < 0 || localIndex >= storageCount -> return null
        }

        var localPageIndex = 0
        var pageInternalIndex: Int

        // it's inside pages, but page sizes aren't regular. Walk to correct tile.
        // Pages can only be null while tiled, so accessing page count is safe.
        pageInternalIndex = localIndex
        val localPageCount = pages.size
        while (localPageIndex < localPageCount) {
            val pageSize = pages[localPageIndex]!!.size
            if (pageSize > pageInternalIndex) {
                // stop, found the page
                break
            }
            pageInternalIndex -= pageSize
            localPageIndex++
        }

        val page = pages[localPageIndex]
        return when {
            // can only occur in tiled case, with untouched inner/placeholder pages
            page == null || page.isEmpty() -> null
            else -> page[pageInternalIndex]
        }
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    interface Callback {
        fun onInitialized(count: Int)
        fun onPagePrepended(leadingNulls: Int, changed: Int, added: Int)
        fun onPageAppended(endPosition: Int, changed: Int, added: Int)
        fun onPagePlaceholderInserted(pageIndex: Int)
        fun onPageInserted(start: Int, count: Int)
        fun onPagesRemoved(startOfDrops: Int, count: Int)
        fun onPagesSwappedToPlaceholder(startOfDrops: Int, count: Int)
    }

    override val size
        get() = leadingNullCount + storageCount + trailingNullCount

    fun computeLeadingNulls(): Int {
        var total = leadingNullCount
        val pageCount = pages.size
        for (i in 0 until pageCount) {
            val page = pages[i]
            if (page != null && page !is PlaceholderList) {
                break
            }
            total += pageSize
        }
        return total
    }

    fun computeTrailingNulls(): Int {
        var total = trailingNullCount
        for (i in pages.indices.reversed()) {
            val page = pages[i]
            if (page != null && page !is PlaceholderList) {
                break
            }
            total += pageSize
        }
        return total
    }

    // ---------------- Trimming API -------------------
    // Trimming is always done at the beginning or end of the list, as content is loaded.
    // In addition to trimming pages in the storage, we also support pre-trimming pages (dropping
    // them just before they're added) to avoid dispatching an add followed immediately by a trim.
    //
    // Note - we avoid trimming down to a single page to reduce chances of dropping page in
    // viewport, since we don't strictly know the viewport. If trim is aggressively set to size of a
    // single page, trimming while the user can see a page boundary is dangerous. To be safe, we
    // just avoid trimming in these cases entirely.

    private fun needsTrim(maxSize: Int, requiredRemaining: Int, localPageIndex: Int): Boolean {
        val page = pages[localPageIndex]
        return page == null || (storageCount > maxSize &&
                pages.size > 2 &&
                page !is PlaceholderList &&
                storageCount - page.size >= requiredRemaining)
    }

    fun needsTrimFromFront(maxSize: Int, requiredRemaining: Int) =
        needsTrim(maxSize, requiredRemaining, 0)

    fun needsTrimFromEnd(maxSize: Int, requiredRemaining: Int) =
        needsTrim(maxSize, requiredRemaining, pages.size - 1)

    fun shouldPreTrimNewPage(maxSize: Int, requiredRemaining: Int, countToBeAdded: Int) =
        storageCount + countToBeAdded > maxSize &&
                pages.size > 1 &&
                storageCount >= requiredRemaining

    internal fun trimFromFront(
        insertNulls: Boolean,
        maxSize: Int,
        requiredRemaining: Int,
        callback: Callback
    ): Boolean {
        var totalRemoved = 0
        while (needsTrimFromFront(maxSize, requiredRemaining)) {
            val page = pages.removeAt(0)
            val removed = page?.size ?: pageSize
            totalRemoved += removed
            storageCount -= removed
        }

        if (totalRemoved > 0) {
            if (insertNulls) {
                // replace removed items with nulls
                val previousLeadingNulls = leadingNullCount
                leadingNullCount += totalRemoved
                callback.onPagesSwappedToPlaceholder(previousLeadingNulls, totalRemoved)
            } else {
                // simply remove, and handle offset
                positionOffset += totalRemoved
                callback.onPagesRemoved(leadingNullCount, totalRemoved)
            }
        }
        return totalRemoved > 0
    }

    internal fun trimFromEnd(
        insertNulls: Boolean,
        maxSize: Int,
        requiredRemaining: Int,
        callback: Callback
    ): Boolean {
        var totalRemoved = 0
        while (needsTrimFromEnd(maxSize, requiredRemaining)) {
            val page = pages.removeAt(pages.size - 1)
            val removed = page?.size ?: pageSize
            totalRemoved += removed
            storageCount -= removed
        }

        if (totalRemoved > 0) {
            val newEndPosition = leadingNullCount + storageCount
            if (insertNulls) {
                // replace removed items with nulls
                trailingNullCount += totalRemoved
                callback.onPagesSwappedToPlaceholder(newEndPosition, totalRemoved)
            } else {
                // items were just removed, signal
                callback.onPagesRemoved(newEndPosition, totalRemoved)
            }
        }
        return totalRemoved > 0
    }

    // ---------------- Contiguous API -------------------

    internal fun prependPage(page: List<T>, callback: Callback) {
        val count = page.size
        if (count == 0) {
            // Nothing returned from source, nothing to do
            return
        }
        if (pageSize > 0 && count != pageSize) {
            if (pages.size == 1 && count > pageSize) {
                // prepending to a single item - update current page size to that of 'inner' page
                pageSize = count
            } else {
                // no longer tiled
                pageSize = -1
            }
        }

        pages.add(0, page)
        storageCount += count

        val changedCount = minOf(leadingNullCount, count)
        val addedCount = count - changedCount

        if (changedCount != 0) {
            leadingNullCount -= changedCount
        }
        positionOffset -= addedCount
        numberPrepended += count

        callback.onPagePrepended(leadingNullCount, changedCount, addedCount)
    }

    internal fun appendPage(page: List<T>, callback: Callback) {
        val count = page.size
        if (count == 0) {
            // Nothing returned from source, nothing to do
            return
        }

        if (pageSize > 0) {
            // if the previous page was smaller than pageSize,
            // or if this page is larger than the previous, disable tiling
            if (pages[pages.size - 1]!!.size != pageSize || count > pageSize) {
                pageSize = -1
            }
        }

        pages.add(page)
        storageCount += count

        val changedCount = minOf(trailingNullCount, count)
        val addedCount = count - changedCount

        if (changedCount != 0) {
            trailingNullCount -= changedCount
        }
        numberAppended += count
        callback.onPageAppended(
            leadingNullCount + storageCount - count,
            changedCount, addedCount
        )
    }

    override fun onPageResultResolution(
        type: PagedList.LoadType,
        result: DataSource.BaseResult<T>
    ) {
        // ignored
    }

    fun hasPage(pageSize: Int, index: Int): Boolean {
        // NOTE: we pass pageSize here to avoid in case pageSize not fully initialized (when last
        // page only one loaded).
        val leadingNullPages = leadingNullCount / pageSize

        if (index < leadingNullPages || index >= leadingNullPages + pages.size) {
            return false
        }

        val page = pages[index - leadingNullPages]

        return page != null && page !is PlaceholderList
    }

    override fun toString(): String {
        var ret = "leading $leadingNullCount, storage $storageCount, trailing $trailingNullCount"
        if (pages.isNotEmpty()) {
            ret += " ${pages.joinToString(" ")}"
        }
        return ret
    }

    /**
     * Lists instances are compared (with instance equality) to [placeholderList] to check if an
     * item in that position is already loading. We use a singleton placeholder list that is
     * distinct from `Collections.emptyList()` for safety.
     */
    private class PlaceholderList<T> : ArrayList<T>()

    private val placeholderList = PlaceholderList<T>()
}
