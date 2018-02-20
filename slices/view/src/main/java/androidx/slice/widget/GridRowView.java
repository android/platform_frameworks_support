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

import static android.app.slice.Slice.HINT_LARGE;
import static android.app.slice.Slice.HINT_NO_TINT;
import static android.app.slice.Slice.HINT_TITLE;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_IMAGE;
import static android.app.slice.SliceItem.FORMAT_SLICE;
import static android.app.slice.SliceItem.FORMAT_TEXT;
import static android.app.slice.SliceItem.FORMAT_TIMESTAMP;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static android.view.ViewGroup.LayoutParams.WRAP_CONTENT;

import static androidx.slice.widget.SliceView.MODE_SMALL;

import android.app.PendingIntent;
import android.content.Context;
import android.content.res.Resources;
import androidx.annotation.ColorInt;
import androidx.annotation.RestrictTo;
import android.util.AttributeSet;
import android.util.Log;
import android.util.Pair;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.core.SliceQuery;
import androidx.slice.view.R;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
public class GridRowView extends SliceChildView implements View.OnClickListener {

    private static final String TAG = "GridView";

    private static final int TITLE_TEXT_LAYOUT = R.layout.abc_slice_title;
    private static final int TEXT_LAYOUT = R.layout.abc_slice_secondary_text;

    // Max number of normal cell items that can be shown in a row
    private static final int MAX_CELLS = 5;

    // Max number of text items that can show in a cell
    private static final int MAX_CELL_TEXT = 2;
    // Max number of text items that can show in a cell if the mode is small
    private static final int MAX_CELL_TEXT_SMALL = 1;
    // Max number of images that can show in a cell
    private static final int MAX_CELL_IMAGES = 1;

    private int mRowIndex;
    private int mSmallImageSize;
    private int mIconSize;
    private int mGutter;

    private GridContent mGridContent;
    private LinearLayout mViewContainer;

    public GridRowView(Context context) {
        this(context, null);
    }

    public GridRowView(Context context, AttributeSet attrs) {
        super(context, attrs);
        final Resources res = getContext().getResources();
        mViewContainer = new LinearLayout(getContext());
        mViewContainer.setOrientation(LinearLayout.HORIZONTAL);
        addView(mViewContainer, new LayoutParams(MATCH_PARENT, MATCH_PARENT));
        mViewContainer.setGravity(Gravity.CENTER_VERTICAL);
        mIconSize = res.getDimensionPixelSize(R.dimen.abc_slice_icon_size);
        mSmallImageSize = res.getDimensionPixelSize(R.dimen.abc_slice_small_image_size);
        mGutter = res.getDimensionPixelSize(R.dimen.abc_slice_grid_gutter);
    }

    @Override
    public int getSmallHeight() {
        // GridRow is small if its the first element in a list without a header presented in small
        return mGridContent != null ? mGridContent.getSmallHeight() : 0;
    }

    @Override
    public int getActualHeight() {
        return mGridContent != null ? mGridContent.getActualHeight() : 0;
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = getMode() == MODE_SMALL ? getSmallHeight() : getActualHeight();
        heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY);
        mViewContainer.getLayoutParams().height = height;
        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }

    @Override
    public void setTint(@ColorInt int tintColor) {
        super.setTint(tintColor);
        if (mGridContent != null) {
            GridContent gc = mGridContent;
            // TODO -- could be smarter about this
            resetView();
            populateViews(gc);
        }
    }

    /**
     * This is called when GridView is presented in small format.
     */
    @Override
    public void setSlice(Slice slice) {
        resetView();
        mRowIndex = 0;
        mGridContent = new GridContent(getContext(), slice.getItems().get(0));
        populateViews(mGridContent);
    }

    /**
     * This is called when GridView is being used as a component in a larger template.
     */
    @Override
    public void setSliceItem(SliceItem slice, boolean isHeader, int index,
            SliceView.OnSliceActionListener observer) {
        resetView();
        setSliceActionListener(observer);
        mRowIndex = index;
        mGridContent = new GridContent(getContext(), slice);
        populateViews(mGridContent);
    }

    private void populateViews(GridContent gc) {
        if (gc.getContentIntent() != null) {
            EventInfo info = new EventInfo(getMode(), EventInfo.ACTION_TYPE_CONTENT,
                    EventInfo.ROW_TYPE_GRID, mRowIndex);
            Pair<SliceItem, EventInfo> tagItem = new Pair<>(gc.getContentIntent(), info);
            mViewContainer.setTag(tagItem);
            makeClickable(mViewContainer);
        }
        ArrayList<GridContent.CellContent> cells = gc.getGridContent();
        boolean hasSeeMore = gc.getSeeMoreItem() != null;
        for (int i = 0; i < cells.size(); i++) {
            if (mViewContainer.getChildCount() >= MAX_CELLS) {
                if (hasSeeMore) {
                    addSeeMoreCount(cells.size() - MAX_CELLS);
                }
                break;
            }
            addCell(cells.get(i), i, Math.min(cells.size(), MAX_CELLS));
        }
    }

    private void addSeeMoreCount(int numExtra) {
        // Remove last element
        View last = mViewContainer.getChildAt(mViewContainer.getChildCount() - 1);
        mViewContainer.removeView(last);

        SliceItem seeMoreItem = mGridContent.getSeeMoreItem();
        int index = mViewContainer.getChildCount();
        int total = MAX_CELLS;
        if ((FORMAT_SLICE.equals(seeMoreItem.getFormat())
                || FORMAT_ACTION.equals(seeMoreItem.getFormat()))
                && seeMoreItem.getSlice().getItems().size() > 0) {
            // It's a custom see more cell, add it
            addCell(new GridContent.CellContent(seeMoreItem), index, total);
            return;
        }

        // Default see more, create it
        LayoutInflater inflater = LayoutInflater.from(getContext());
        TextView extraText;
        ViewGroup seeMoreView;
        if (mGridContent.isAllImages()) {
            seeMoreView = (FrameLayout) inflater.inflate(R.layout.abc_slice_grid_see_more_overlay,
                    mViewContainer, false);
            seeMoreView.addView(last, 0, new LayoutParams(MATCH_PARENT, MATCH_PARENT));
            extraText = seeMoreView.findViewById(R.id.text_see_more_count);
        } else {
            seeMoreView = (LinearLayout) inflater.inflate(
                    R.layout.abc_slice_grid_see_more, mViewContainer, false);
            extraText = seeMoreView.findViewById(R.id.text_see_more_count);
        }
        mViewContainer.addView(seeMoreView, new LinearLayout.LayoutParams(0, MATCH_PARENT, 1));
        extraText.setText(getResources().getString(R.string.abc_slice_more_content, numExtra));

        // Make it clickable
        EventInfo info = new EventInfo(getMode(), EventInfo.ACTION_TYPE_BUTTON,
                EventInfo.ROW_TYPE_GRID, mRowIndex);
        info.setPosition(EventInfo.POSITION_CELL, index, total);
        Pair<SliceItem, EventInfo> tagItem = new Pair<>(seeMoreItem, info);
        seeMoreView.setTag(tagItem);
        makeClickable(seeMoreView);
    }

    /**
     * Adds a cell to the grid view based on the provided {@link SliceItem}.
     */
    private void addCell(GridContent.CellContent cell, int index, int total) {
        final int maxCellText = getMode() == MODE_SMALL
                ? MAX_CELL_TEXT_SMALL
                : MAX_CELL_TEXT;
        LinearLayout cellContainer = new LinearLayout(getContext());
        cellContainer.setOrientation(LinearLayout.VERTICAL);
        cellContainer.setGravity(Gravity.CENTER_HORIZONTAL);

        ArrayList<SliceItem> cellItems = cell.getCellItems();
        SliceItem contentIntentItem = cell.getContentIntent();

        int textCount = 0;
        int imageCount = 0;
        boolean added = false;
        boolean singleItem = cellItems.size() == 1;
        List<SliceItem> textItems = null;
        // In small format we display one text item and prefer titles
        if (!singleItem && getMode() == MODE_SMALL) {
            // Get all our text items
            textItems = new ArrayList<>();
            for (SliceItem cellItem : cellItems) {
                if (FORMAT_TEXT.equals(cellItem.getFormat())) {
                    textItems.add(cellItem);
                }
            }
            // If we have more than 1 remove non-titles
            Iterator<SliceItem> iterator = textItems.iterator();
            while (textItems.size() > 1) {
                SliceItem item = iterator.next();
                if (!item.hasHint(HINT_TITLE)) {
                    iterator.remove();
                }
            }
        }
        for (int i = 0; i < cellItems.size(); i++) {
            SliceItem item = cellItems.get(i);
            final String itemFormat = item.getFormat();
            if (textCount < maxCellText && (FORMAT_TEXT.equals(itemFormat)
                    || FORMAT_TIMESTAMP.equals(itemFormat))) {
                if (textItems != null && !textItems.contains(item)) {
                    continue;
                }
                if (addItem(item, mTintColor, cellContainer, singleItem)) {
                    textCount++;
                    added = true;
                }
            } else if (imageCount < MAX_CELL_IMAGES && FORMAT_IMAGE.equals(item.getFormat())) {
                if (addItem(item, mTintColor, cellContainer, singleItem)) {
                    imageCount++;
                    added = true;
                }
            }
        }
        if (added) {
            mViewContainer.addView(cellContainer,
                    new LinearLayout.LayoutParams(0, WRAP_CONTENT, 1));
            if (index != total - 1) {
                // If we're not the last or only element add space between items
                MarginLayoutParams lp =
                        (LinearLayout.MarginLayoutParams) cellContainer.getLayoutParams();
                lp.setMarginEnd(mGutter);
                cellContainer.setLayoutParams(lp);
            }
            if (contentIntentItem != null) {
                EventInfo info = new EventInfo(getMode(), EventInfo.ACTION_TYPE_BUTTON,
                        EventInfo.ROW_TYPE_GRID, mRowIndex);
                info.setPosition(EventInfo.POSITION_CELL, index, total);
                Pair<SliceItem, EventInfo> tagItem = new Pair<>(contentIntentItem, info);
                cellContainer.setTag(tagItem);
                makeClickable(cellContainer);
            }
        }
    }

    /**
     * Adds simple items to a container. Simple items include icons, text, and timestamps.
     * @return Whether an item was added.
     */
    private boolean addItem(SliceItem item, int color, ViewGroup container, boolean singleItem) {
        final String format = item.getFormat();
        View addedView = null;
        if (FORMAT_TEXT.equals(format) || FORMAT_TIMESTAMP.equals(format)) {
            boolean title = SliceQuery.hasAnyHints(item, HINT_LARGE, HINT_TITLE);
            TextView tv = (TextView) LayoutInflater.from(getContext()).inflate(title
                    ? TITLE_TEXT_LAYOUT : TEXT_LAYOUT, null);
            tv.setTextSize(TypedValue.COMPLEX_UNIT_PX, title ? mTitleSize : mSubtitleSize);
            tv.setTextColor(title ? mTitleColor : mSubtitleColor);
            CharSequence text = FORMAT_TIMESTAMP.equals(format)
                    ? SliceViewUtil.getRelativeTimeString(item.getTimestamp())
                    : item.getText();
            tv.setText(text);
            container.addView(tv);
            addedView = tv;
        } else if (FORMAT_IMAGE.equals(format)) {
            ImageView iv = new ImageView(getContext());
            iv.setImageIcon(item.getIcon());
            LinearLayout.LayoutParams lp;
            if (item.hasHint(HINT_LARGE)) {
                iv.setScaleType(ScaleType.CENTER_CROP);
                lp = new LinearLayout.LayoutParams(MATCH_PARENT, MATCH_PARENT);
            } else {
                boolean isIcon = !item.hasHint(HINT_NO_TINT);
                int size = isIcon ? mIconSize : mSmallImageSize;
                iv.setScaleType(isIcon ? ScaleType.CENTER_INSIDE : ScaleType.CENTER_CROP);
                lp = new LinearLayout.LayoutParams(size, size);
            }
            if (color != -1 && !item.hasHint(HINT_NO_TINT)) {
                iv.setColorFilter(color);
            }
            container.addView(iv, lp);
            addedView = iv;
        }
        return addedView != null;
    }

    private void makeClickable(View layout) {
        layout.setOnClickListener(this);
        layout.setForeground(SliceViewUtil.getDrawable(getContext(),
                android.R.attr.selectableItemBackground));
    }

    @Override
    public void onClick(View view) {
        Pair<SliceItem, EventInfo> tagItem = (Pair<SliceItem, EventInfo>) view.getTag();
        final SliceItem actionItem = tagItem.first;
        final EventInfo info = tagItem.second;
        if (actionItem != null && FORMAT_ACTION.equals(actionItem.getFormat())) {
            try {
                actionItem.getAction().send();
                if (mObserver != null) {
                    mObserver.onSliceAction(info, actionItem);
                }
            } catch (PendingIntent.CanceledException e) {
                Log.w(TAG, "PendingIntent for slice cannot be sent", e);
            }
        }
    }

    @Override
    public void resetView() {
        mViewContainer.removeAllViews();
    }
}
