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

import static android.app.slice.Slice.HINT_HORIZONTAL;
import static android.app.slice.Slice.HINT_LIST_ITEM;
import static android.app.slice.Slice.SUBTYPE_MESSAGE;
import static android.app.slice.Slice.SUBTYPE_SOURCE;
import static android.app.slice.SliceItem.FORMAT_ACTION;
import static android.app.slice.SliceItem.FORMAT_SLICE;

import android.app.slice.Slice;
import android.content.Context;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;

import androidx.annotation.RequiresApi;
import androidx.annotation.RestrictTo;
import androidx.collection.ArrayMap;
import androidx.recyclerview.widget.RecyclerView;
import androidx.slice.SliceItem;
import androidx.slice.core.SliceAction;
import androidx.slice.core.SliceQuery;
import androidx.slice.view.R;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * @hide
 */
@RestrictTo(RestrictTo.Scope.LIBRARY)
@RequiresApi(19)
public class LargeSliceAdapter extends RecyclerView.Adapter<LargeSliceAdapter.SliceViewHolder>
        implements SliceActionView.SliceActionLoadingListener {

    static final int TYPE_DEFAULT       = 1;
    static final int TYPE_HEADER        = 2; // TODO: headers shouldn't scroll off
    static final int TYPE_GRID          = 3;
    static final int TYPE_MESSAGE       = 4;
    static final int TYPE_MESSAGE_LOCAL = 5;

    static final int HEADER_INDEX = 0;

    private final IdGenerator mIdGen = new IdGenerator();
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    final Context mContext;
    private List<SliceWrapper> mSlices = new ArrayList<>();
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    SliceView.OnSliceActionListener mSliceObserver;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mColor;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    SliceStyle mSliceStyle;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    List<SliceAction> mSliceActions;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    boolean mShowLastUpdated;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    long mLastUpdated;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    SliceView mParent;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    LargeTemplateView mTemplateView;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mInsetStart;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mInsetTop;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mInsetEnd;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    int mInsetBottom;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    Set<SliceItem> mLoadingActions = new HashSet<>();
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    boolean mAllowTwoLines;
    @SuppressWarnings("WeakerAccess") /* synthetic access */
    SliceViewPolicy mPolicy;

    public LargeSliceAdapter(Context context) {
        mContext = context;
        setHasStableIds(true);
    }

    /**
     * Sets the SliceView parent and the template parent.
     */
    public void setParents(SliceView parent, LargeTemplateView templateView) {
        mParent = parent;
        mTemplateView = templateView;
    }

    /**
     * Sets the insets (padding) for slice view. LargeSliceAdapter will handle determining
     * if a child needs a particular padding, i.e. if it's the first row then the top inset
     * will be applied to it whereas subsequent rows would get a top inset of 0.
     */
    public void setInsets(int l, int t, int r, int b) {
        mInsetStart = l;
        mInsetTop = t;
        mInsetEnd = r;
        mInsetBottom = b;
    }

    /**
     * Sets the observer to pass down to child views.
     */
    public void setSliceObserver(SliceView.OnSliceActionListener observer) {
        mSliceObserver = observer;
    }

    /**
     * Sets the actions to display for this slice, this adjusts what's displayed in the header item.
     */
    public void setSliceActions(List<SliceAction> actions) {
        mSliceActions = actions;
        notifyHeaderChanged();
    }

    /**
     * Set the {@link SliceItem}'s to be displayed in the adapter and the accent color.
     */
    public void setSliceItems(List<SliceContent> slices, int color, int mode) {
        if (slices == null) {
            mLoadingActions.clear();
            mSlices.clear();
        } else {
            mIdGen.resetUsage();
            mSlices = new ArrayList<>(slices.size());
            for (SliceContent s : slices) {
                mSlices.add(new SliceWrapper(s, mIdGen, mode));
            }
        }
        mColor = color;
        notifyDataSetChanged();
    }

    /**
     * Sets the style information to use for views in this adapter.
     */
    public void setStyle(SliceStyle style) {
        mSliceStyle = style;
        notifyDataSetChanged();
    }

    /**
     * Sets the policy information to use for views in this adapter.
     */
    public void setPolicy(SliceViewPolicy p) {
        mPolicy = p;
    }

    /**
     * Sets whether the last updated time should be shown on the slice.
     */
    public void setShowLastUpdated(boolean showLastUpdated) {
        if (mShowLastUpdated != showLastUpdated) {
            mShowLastUpdated = showLastUpdated;
            notifyHeaderChanged();
        }
    }

    /**
     * Sets when the slice was last updated.
     */
    public void setLastUpdated(long lastUpdated) {
        if (mLastUpdated != lastUpdated) {
            mLastUpdated = lastUpdated;
            notifyHeaderChanged();
        }
    }

    /**
     * Indicates that no actions should be loading and updates the views.
     */
    public void setLoadingActions(Set<SliceItem> actions) {
        if (actions == null) {
            mLoadingActions.clear();
        } else {
            mLoadingActions = actions;
        }
        notifyDataSetChanged();
    }

    /**
     * Returns the currently loading actions.
     */
    public Set<SliceItem> getLoadingActions() {
        return mLoadingActions;
    }

    @Override
    public void onSliceActionLoading(SliceItem actionItem, int position) {
        mLoadingActions.add(actionItem);
        if (getItemCount() > position) {
            notifyItemChanged(position);
        } else {
            notifyDataSetChanged();
        }
    }

    /**
     * Sets whether this slice can have 2 lines of subtitle text in the first row.
     */
    public void setAllowTwoLines(boolean allowTwoLines) {
        mAllowTwoLines = allowTwoLines;
        notifyHeaderChanged();
    }

    /**
     * Notifies that content in the header of this adapter has changed.
     */
    public void notifyHeaderChanged() {
        if (getItemCount() > 0) {
            notifyItemChanged(HEADER_INDEX);
        }
    }

    @Override
    public SliceViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View v = inflateForType(viewType);
        v.setLayoutParams(new LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.WRAP_CONTENT));
        return new SliceViewHolder(v);
    }

    @Override
    public int getItemViewType(int position) {
        return mSlices.get(position).mType;
    }

    @Override
    public long getItemId(int position) {
        return mSlices.get(position).mId;
    }

    @Override
    public int getItemCount() {
        return mSlices.size();
    }

    @Override
    public void onBindViewHolder(SliceViewHolder holder, int position) {
        SliceWrapper slice = mSlices.get(position);
        holder.bind(slice.mItem, position);
    }

    private View inflateForType(int viewType) {
        View v = new RowView(mContext);
        switch (viewType) {
            case TYPE_GRID:
                v = LayoutInflater.from(mContext).inflate(R.layout.abc_slice_grid, null);
                break;
            case TYPE_MESSAGE:
                v = LayoutInflater.from(mContext).inflate(R.layout.abc_slice_message, null);
                break;
            case TYPE_MESSAGE_LOCAL:
                v = LayoutInflater.from(mContext).inflate(R.layout.abc_slice_message_local,
                        null);
                break;
        }
        return v;
    }

    protected static class SliceWrapper {
        final SliceContent mItem;
        final int mType;
        final long mId;

        public SliceWrapper(SliceContent item, IdGenerator idGen, int mode) {
            mItem = item;
            mType = getFormat(item.getSliceItem());
            mId = idGen.getId(item.getSliceItem());
        }

        public static int getFormat(SliceItem item) {
            if (SUBTYPE_MESSAGE.equals(item.getSubType())) {
                // TODO: Better way to determine me or not? Something more like Messaging style.
                if (SliceQuery.findSubtype(item, null, SUBTYPE_SOURCE) != null) {
                    return TYPE_MESSAGE;
                } else {
                    return TYPE_MESSAGE_LOCAL;
                }
            }
            if (item.hasHint(HINT_HORIZONTAL)) {
                return TYPE_GRID;
            }
            if (!item.hasHint(Slice.HINT_LIST_ITEM)) {
                return TYPE_HEADER;
            }
            return TYPE_DEFAULT;
        }
    }

    /**
     * A {@link RecyclerView.ViewHolder} for presenting slices in {@link LargeSliceAdapter}.
     */
    public class SliceViewHolder extends RecyclerView.ViewHolder implements View.OnTouchListener,
            View.OnClickListener {
        public final SliceChildView mSliceChildView;

        public SliceViewHolder(View itemView) {
            super(itemView);
            mSliceChildView = itemView instanceof SliceChildView ? (SliceChildView) itemView : null;
        }

        void bind(SliceContent item, int position) {
            if (mSliceChildView == null || item == null) {
                return;
            }
            // Click listener used to pipe click events to parent
            mSliceChildView.setOnClickListener(this);
            // Touch listener used to pipe events to touch feedback drawable
            mSliceChildView.setOnTouchListener(this);
            mSliceChildView.setSliceActionLoadingListener(LargeSliceAdapter.this);

            final boolean isFirstPosition = position == HEADER_INDEX;
            final boolean isRealHeader = !item.getSliceItem().hasAnyHints(HINT_LIST_ITEM);
            mSliceChildView.setLoadingActions(mLoadingActions);
            mSliceChildView.setPolicy(mPolicy);
            mSliceChildView.setTint(mColor);
            mSliceChildView.setStyle(mSliceStyle);
            mSliceChildView.setShowLastUpdated(isFirstPosition && mShowLastUpdated);
            mSliceChildView.setLastUpdated(isFirstPosition ? mLastUpdated : -1);
            // Only apply top / bottom insets to first / last rows
            int top = position == 0 ? mInsetTop : 0;
            int bottom = position == getItemCount() - 1 ? mInsetBottom : 0;
            mSliceChildView.setInsets(mInsetStart, top, mInsetEnd, bottom);
            mSliceChildView.setAllowTwoLines(mAllowTwoLines);
            mSliceChildView.setSliceActions(isFirstPosition ? mSliceActions : null);
            mSliceChildView.setSliceItem(item, isRealHeader, position, getItemCount(),
                    mSliceObserver);
            int[] info = new int[2];
            info[0] = ListContent.getRowType(item, isFirstPosition, mSliceActions);
            info[1] = position;
            mSliceChildView.setTag(info);
        }

        @Override
        public void onClick(View v) {
            if (mParent != null) {
                mParent.setClickInfo((int[]) v.getTag());
                mParent.performClick();
            }
        }

        @Override
        public boolean onTouch(View v, MotionEvent event) {
            if (mTemplateView != null) {
                mTemplateView.onForegroundActivated(event);
            }
            return false;
        }
    }

    private static class IdGenerator {
        private long mNextLong = 0;
        private final ArrayMap<String, Long> mCurrentIds = new ArrayMap<>();
        private final ArrayMap<String, Integer> mUsedIds = new ArrayMap<>();

        IdGenerator() {
        }

        public long getId(SliceItem item) {
            String str = genString(item);
            if (!mCurrentIds.containsKey(str)) {
                mCurrentIds.put(str, mNextLong++);
            }
            long id = mCurrentIds.get(str);
            Integer usedIdIndex = mUsedIds.get(str);
            int index = usedIdIndex != null ? usedIdIndex : 0;
            mUsedIds.put(str, index + 1);
            return id + index * 10000;
        }

        private String genString(SliceItem item) {
            if (FORMAT_SLICE.equals(item.getFormat())
                    || FORMAT_ACTION.equals(item.getFormat())) {
                return String.valueOf(item.getSlice().getItems().size());
            }
            return item.toString();
        }

        public void resetUsage() {
            mUsedIds.clear();
        }
    }
}
