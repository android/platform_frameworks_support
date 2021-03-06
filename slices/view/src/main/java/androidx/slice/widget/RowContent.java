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

package androidx.slice.widget;

import static android.app.slice.Slice.HINT_ACTIONS;
import static android.app.slice.Slice.HINT_PARTIAL;
import static android.app.slice.Slice.HINT_SEE_MORE;
import static android.app.slice.Slice.HINT_SHORTCUT;
import static android.app.slice.Slice.HINT_SUMMARY;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.Slice.SUBTYPE_CONTENT_DESCRIPTION;
import static android.app.slice.Slice.SUBTYPE_RANGE;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_INT;
import static android.app.slice.SliceItem.FORMAT_REMOTE_INPUT;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;
import static android.app.slice.SliceItem.FORMAT_TIMESTAMP;

import static androidx.slice.core.SliceHints.HINT_KEYWORDS;
import static androidx.slice.core.SliceHints.HINT_LAST_UPDATED;
import static androidx.slice.core.SliceHints.HINT_TTL;

import android.content.Context;
import android.text.TextUtils;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import androidx.slice.SliceItem;
import androidx.slice.core.SliceAction;
import androidx.slice.core.SliceActionImpl;
import androidx.slice.core.SliceQuery;
import androidx.slice.view.R;

import java.util.ArrayList;
import java.util.List;

/**
 * Extracts information required to present content in a row format from a slice.
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY_GROUP)
public class RowContent {
    private static final String TAG = "RowContent";

    private SliceItem mPrimaryAction;
    private SliceItem mRowSlice;
    private SliceItem mStartItem;
    private SliceItem mTitleItem;
    private SliceItem mSubtitleItem;
    private SliceItem mSummaryItem;
    private ArrayList<SliceItem> mEndItems = new ArrayList<>();
    private ArrayList<SliceAction> mToggleItems = new ArrayList<>();
    private SliceItem mRange;
    private SliceItem mContentDescr;
    private boolean mEndItemsContainAction;
    private boolean mIsHeader;
    private int mLineCount = 0;
    private int mMaxHeight;
    private int mMinHeight;
    private int mRangeHeight;

    public RowContent(Context context, SliceItem rowSlice, boolean isHeader) {
        populate(rowSlice, isHeader);
        mMaxHeight = context.getResources().getDimensionPixelSize(R.dimen.abc_slice_row_max_height);
        mMinHeight = context.getResources().getDimensionPixelSize(R.dimen.abc_slice_row_min_height);
        mRangeHeight = context.getResources().getDimensionPixelSize(
                R.dimen.abc_slice_row_range_height);
    }

    /**
     * @return whether this row has content that is valid to display.
     */
    private boolean populate(SliceItem rowSlice, boolean isHeader) {
        mIsHeader = isHeader;
        mRowSlice = rowSlice;
        if (!isValidRow(rowSlice)) {
            Log.w(TAG, "Provided SliceItem is invalid for RowContent");
            return false;
        }
        // Find primary action first (otherwise filtered out of valid row items)
        String[] hints = new String[] {HINT_SHORTCUT, HINT_TITLE};
        mPrimaryAction = SliceQuery.find(rowSlice, FORMAT_SLICE, hints,
                new String[] { HINT_ACTIONS, HINT_KEYWORDS} /* nonHints */);

        if (mPrimaryAction == null && FORMAT_ACTION.equals(rowSlice.getFormat())
                && rowSlice.getSlice().getItems().size() == 1) {
            mPrimaryAction = rowSlice;
        }

        mContentDescr = SliceQuery.findSubtype(rowSlice, FORMAT_TEXT, SUBTYPE_CONTENT_DESCRIPTION);

        // Filter anything not viable for displaying in a row
        ArrayList<SliceItem> rowItems = filterInvalidItems(rowSlice);
        // If we've only got one item that's a slice / action use those items instead
        if (rowItems.size() == 1 && (FORMAT_ACTION.equals(rowItems.get(0).getFormat())
                || FORMAT_SLICE.equals(rowItems.get(0).getFormat()))
                && !rowItems.get(0).hasHint(HINT_SHORTCUT)) {
            if (isValidRow(rowItems.get(0))) {
                rowSlice = rowItems.get(0);
                rowItems = filterInvalidItems(rowSlice);
            }
        }
        if (SUBTYPE_RANGE.equals(rowSlice.getSubType())) {
            mRange = rowSlice;
        }
        if (rowItems.size() > 0) {
            // Start item
            SliceItem firstItem = rowItems.get(0);
            if (FORMAT_SLICE.equals(firstItem.getFormat())) {
                SliceItem unwrappedItem = firstItem.getSlice().getItems().get(0);
                if (isStartType(unwrappedItem)) {
                    mStartItem = unwrappedItem;
                    rowItems.remove(0);
                }
            }

            // Text + end items
            ArrayList<SliceItem> endItems = new ArrayList<>();
            for (int i = 0; i < rowItems.size(); i++) {
                final SliceItem item = rowItems.get(i);
                if (FORMAT_TEXT.equals(item.getFormat())) {
                    if ((mTitleItem == null || !mTitleItem.hasHint(HINT_TITLE))
                            && item.hasHint(HINT_TITLE) && !item.hasHint(HINT_SUMMARY)) {
                        mTitleItem = item;
                    } else if (mSubtitleItem == null && !item.hasHint(HINT_SUMMARY)) {
                        mSubtitleItem = item;
                    } else if (mSummaryItem == null && item.hasHint(HINT_SUMMARY)) {
                        mSummaryItem = item;
                    }
                } else {
                    endItems.add(item);
                }
            }
            if (hasText(mTitleItem)) {
                mLineCount++;
            }
            if (hasText(mSubtitleItem)) {
                mLineCount++;
            }
            // Special rules for end items: only one timestamp
            boolean hasTimestamp = mStartItem != null
                    && FORMAT_TIMESTAMP.equals(mStartItem.getFormat());
            for (int i = 0; i < endItems.size(); i++) {
                final SliceItem item = endItems.get(i);
                boolean isAction = SliceQuery.find(item, FORMAT_ACTION) != null;
                if (FORMAT_TIMESTAMP.equals(item.getFormat())) {
                    if (!hasTimestamp) {
                        hasTimestamp = true;
                        mEndItems.add(item);
                    }
                } else {
                    processContent(item, isAction);
                }
            }
        }
        return isValid();
    }

    private void processContent(@NonNull SliceItem item, boolean isAction) {
        if (isAction) {
            SliceAction ac = new SliceActionImpl(item);
            if (ac.isToggle()) {
                mToggleItems.add(ac);
            }
        }
        mEndItems.add(item);
        mEndItemsContainAction |= isAction;
    }

    /**
     * @return the {@link SliceItem} used to populate this row.
     */
    @NonNull
    public SliceItem getSlice() {
        return mRowSlice;
    }

    /**
     * @return the {@link SliceItem} representing the range in the row; can be null.
     */
    @Nullable
    public SliceItem getRange() {
        return mRange;
    }

    /**
     * @return the {@link SliceItem} used for the main intent for this row; can be null.
     */
    @Nullable
    public SliceItem getPrimaryAction() {
        return mPrimaryAction;
    }

    /**
     * @return the {@link SliceItem} to display at the start of this row; can be null.
     */
    @Nullable
    public SliceItem getStartItem() {
        return mIsHeader ? null : mStartItem;
    }

    /**
     * @return the {@link SliceItem} representing the title text for this row; can be null.
     */
    @Nullable
    public SliceItem getTitleItem() {
        return mTitleItem;
    }

    /**
     * @return the {@link SliceItem} representing the subtitle text for this row; can be null.
     */
    @Nullable
    public SliceItem getSubtitleItem() {
        return mSubtitleItem;
    }

    @Nullable
    public SliceItem getSummaryItem() {
        return mSummaryItem == null ? mSubtitleItem : mSummaryItem;
    }

    /**
     * @return the list of {@link SliceItem} that can be shown as items at the end of the row.
     */
    public ArrayList<SliceItem> getEndItems() {
        return mEndItems;
    }

    /**
     * @return a list of toggles associated with this row.
     */
    public ArrayList<SliceAction> getToggleItems() {
        return mToggleItems;
    }

    /**
     * @return the content description to use for this row.
     */
    @Nullable
    public CharSequence getContentDescription() {
        return mContentDescr != null ? mContentDescr.getText() : null;
    }

    /**
     * @return whether {@link #getEndItems()} contains a SliceItem with FORMAT_SLICE, HINT_SHORTCUT
     */
    public boolean endItemsContainAction() {
        return mEndItemsContainAction;
    }

    /**
     * @return the number of lines of text contained in this row.
     */
    public int getLineCount() {
        return mLineCount;
    }

    /**
     * @return the height to display a row at when it is used as a small template.
     */
    public int getSmallHeight() {
        return getRange() != null
                ? getActualHeight()
                : mMaxHeight;
    }

    /**
     * @return the height the content in this template requires to be displayed.
     */
    public int getActualHeight() {
        if (!isValid()) {
            return 0;
        }
        int rowHeight = (getLineCount() > 1 || mIsHeader) ? mMaxHeight : mMinHeight;
        if (getRange() != null) {
            if (getLineCount() > 0) {
                rowHeight += mRangeHeight;
            } else {
                rowHeight = mIsHeader ? mMaxHeight : mRangeHeight;
            }
        }
        return rowHeight;
    }

    private static boolean hasText(SliceItem textSlice) {
        return textSlice != null
                && (textSlice.hasHint(HINT_PARTIAL)
                    || !TextUtils.isEmpty(textSlice.getText()));
    }

    /**
     * @return whether this row content represents a default see more item.
     */
    public boolean isDefaultSeeMore() {
        return FORMAT_ACTION.equals(mRowSlice.getFormat())
                && mRowSlice.getSlice().hasHint(HINT_SEE_MORE)
                && mRowSlice.getSlice().getItems().isEmpty();
    }

    /**
     * @return whether this row has content that is valid to display.
     */
    public boolean isValid() {
        return mStartItem != null
                || mTitleItem != null
                || mSubtitleItem != null
                || mEndItems.size() > 0
                || mRange != null
                || isDefaultSeeMore();
    }

    /**
     * @return whether this is a valid item to use to populate a row of content.
     */
    private static boolean isValidRow(SliceItem rowSlice) {
        if (rowSlice == null) {
            return false;
        }
        // Must be slice or action
        if (FORMAT_SLICE.equals(rowSlice.getFormat())
                || FORMAT_ACTION.equals(rowSlice.getFormat())) {
            // Must have at least one legitimate child
            List<SliceItem> rowItems = rowSlice.getSlice().getItems();
            for (int i = 0; i < rowItems.size(); i++) {
                if (isValidRowContent(rowSlice, rowItems.get(i))) {
                    return true;
                }
            }
            // Special case: default see more just has an action but no other items
            if (rowSlice.hasHint(HINT_SEE_MORE) && rowItems.isEmpty()) {
                return true;
            }
        }
        return false;
    }

    /**
     * @return list of {@link SliceItem}s that are valid to display in a row according
     * to {@link #isValidRowContent(SliceItem, SliceItem)}.
     */
    private static ArrayList<SliceItem> filterInvalidItems(SliceItem rowSlice) {
        ArrayList<SliceItem> filteredList = new ArrayList<>();
        for (SliceItem i : rowSlice.getSlice().getItems()) {
            if (isValidRowContent(rowSlice, i)) {
                filteredList.add(i);
            }
        }
        return filteredList;
    }

    /**
     * @return whether this item is valid content to display in a row.
     */
    private static boolean isValidRowContent(SliceItem slice, SliceItem item) {
        if (item.hasAnyHints(HINT_KEYWORDS, HINT_TTL, HINT_LAST_UPDATED)) {
            return false;
        }
        if (FORMAT_SLICE.equals(item.getFormat()) && !item.hasHint(HINT_SHORTCUT)) {
            // Unpack contents of slice
            item = item.getSlice().getItems().get(0);
        }
        final String itemFormat = item.getFormat();
        return (FORMAT_TEXT.equals(itemFormat)
                && !SUBTYPE_CONTENT_DESCRIPTION.equals(item.getSubType()))
                || FORMAT_IMAGE.equals(itemFormat)
                || FORMAT_TIMESTAMP.equals(itemFormat)
                || FORMAT_REMOTE_INPUT.equals(itemFormat)
                || (FORMAT_SLICE.equals(itemFormat) && item.hasHint(HINT_TITLE)
                && !item.hasHint(HINT_SHORTCUT))
                || (FORMAT_SLICE.equals(itemFormat) && item.hasHint(HINT_SHORTCUT)
                && !item.hasHint(HINT_TITLE))
                || FORMAT_ACTION.equals(itemFormat)
                || (FORMAT_INT.equals(itemFormat) && SUBTYPE_RANGE.equals(slice.getSubType()));
    }

    /**
     * @return Whether this item is appropriate to be considered a "start" item, i.e. go in the
     *         front slot of a row.
     */
    private static boolean isStartType(SliceItem item) {
        final String type = item.getFormat();
        return (FORMAT_ACTION.equals(type) && (SliceQuery.find(item, FORMAT_IMAGE) != null))
                    || FORMAT_IMAGE.equals(type)
                    || (FORMAT_TIMESTAMP.equals(type)
                && !item.hasAnyHints(HINT_TTL, HINT_LAST_UPDATED));
    }
}
