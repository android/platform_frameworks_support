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
import static android.app.slice.Slice.SUBTYPE_COLOR;
import static android.app.slice.SliceItem.FORMAT_INT;

import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.res.TypedArray;
import android.graphics.drawable.ColorDrawable;
import androidx.annotation.IntDef;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RestrictTo;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;

import java.util.List;

import androidx.slice.Slice;
import androidx.slice.SliceItem;
import androidx.slice.SliceUtils;
import androidx.slice.core.SliceQuery;
import androidx.slice.view.R;

/**
 * A view for displaying a {@link Slice} which is a piece of app content and actions. SliceView is
 * able to present slice content in a templated format outside of the associated app. The way this
 * content is displayed depends on the structure of the slice, the hints associated with the
 * content, and the mode that SliceView is configured for. The modes that SliceView supports are:
 * <ul>
 * <li><b>Shortcut</b>: A shortcut is presented as an icon and a text label representing the main
 * content or action associated with the slice.</li>
 * <li><b>Small</b>: The small format has a restricted height and can present a single
 * {@link SliceItem} or a limited collection of items.</li>
 * <li><b>Large</b>: The large format displays multiple small templates in a list, if scrolling is
 * not enabled (see {@link #setScrollable(boolean)}) the view will show as many items as it can
 * comfortably fit.</li>
 * </ul>
 * <p>
 * When constructing a slice, the contents of it can be annotated with hints, these provide the OS
 * with some information on how the content should be displayed. For example, text annotated with
 * {@link android.app.slice.Slice#HINT_TITLE} would be placed in the title position of a template.
 * A slice annotated with {@link android.app.slice.Slice#HINT_LIST} would present the child items
 * of that slice in a list.
 * <p>
 * Example usage:
 *
 * <pre class="prettyprint">
 * SliceView v = new SliceView(getContext());
 * v.setMode(desiredMode);
 * LiveData<Slice> liveData = SliceLiveData.fromUri(sliceUri);
 * liveData.observe(lifecycleOwner, v);
 * </pre>
 * @see SliceLiveData
 */
public class SliceView extends ViewGroup implements Observer<Slice> {

    private static final String TAG = "SliceView";

    /**
     * Implement this interface to be notified of interactions with the slice displayed
     * in this view.
     * @see EventInfo
     */
    public interface OnSliceActionListener {
        /**
         * Called when an interaction has occurred with an element in this view.
         * @param info the type of event that occurred.
         * @param item the specific item within the {@link Slice} that was interacted with.
         */
        void onSliceAction(@NonNull EventInfo info, @NonNull SliceItem item);
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    @IntDef({
            MODE_SMALL, MODE_LARGE, MODE_SHORTCUT
    })
    public @interface SliceMode {}

    /**
     * Mode indicating this slice should be presented in small template format.
     */
    public static final int MODE_SMALL       = 1;
    /**
     * Mode indicating this slice should be presented in large template format.
     */
    public static final int MODE_LARGE       = 2;
    /**
     * Mode indicating this slice should be presented as an icon. A shortcut requires an intent,
     * icon, and label. This can be indicated by using {@link android.app.slice.Slice#HINT_TITLE}
     * on an action in a slice.
     */
    public static final int MODE_SHORTCUT    = 3;

    private int mMode = MODE_LARGE;
    private Slice mCurrentSlice;
    private SliceChildView mCurrentView;
    private List<SliceItem> mActions;
    private final ActionRow mActionRow;

    private boolean mShowActions = false;
    private boolean mIsScrollable = true;

    private final int mShortcutSize;
    private final int mMinLargeHeight;
    private final int mMaxLargeHeight;
    private final int mActionRowHeight;

    private AttributeSet mAttrs;
    private int mThemeTintColor = -1;

    private OnSliceActionListener mSliceObserver;

    public SliceView(Context context) {
        this(context, null);
    }

    public SliceView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, R.attr.sliceViewStyle);
    }

    public SliceView(Context context, @Nullable AttributeSet attrs, int defStyleAttr) {
        this(context, attrs, defStyleAttr, R.style.Widget_SliceView);
    }

    public SliceView(Context context, AttributeSet attrs, int defStyleAttr, int defStyleRes) {
        super(context, attrs, defStyleAttr, defStyleRes);
        mAttrs = attrs;
        TypedArray a = context.getTheme().obtainStyledAttributes(attrs, R.styleable.SliceView,
                defStyleAttr, defStyleRes);
        try {
            mThemeTintColor = a.getColor(R.styleable.SliceView_tintColor, -1);
        } finally {
            a.recycle();
        }
        // TODO: action row background should support light / dark / maybe presenter customization
        mActionRow = new ActionRow(getContext(), true);
        mActionRow.setBackground(new ColorDrawable(0xffeeeeee));
        mCurrentView = new LargeTemplateView(getContext());
        mCurrentView.setMode(getMode());
        addView(mCurrentView.getView(), getChildLp(mCurrentView.getView()));
        addView(mActionRow, getChildLp(mActionRow));
        mShortcutSize = getContext().getResources()
                .getDimensionPixelSize(R.dimen.abc_slice_shortcut_size);
        mMinLargeHeight = getResources().getDimensionPixelSize(R.dimen.abc_slice_large_height);
        mMaxLargeHeight = getResources().getDimensionPixelSize(R.dimen.abc_slice_max_large_height);
        mActionRowHeight = getResources().getDimensionPixelSize(
                R.dimen.abc_slice_action_row_height);
    }

    private int getHeightForMode() {
        int mode = getMode();
        if (mode == MODE_SHORTCUT) {
            return mShortcutSize;
        }
        return mode == MODE_LARGE
                ? mCurrentView.getActualHeight()
                : mCurrentView.getSmallHeight();
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int width = MeasureSpec.getSize(widthMeasureSpec);
        int childWidth = MeasureSpec.getSize(widthMeasureSpec);
        if (MODE_SHORTCUT == mMode) {
            // TODO: consider scaling the shortcut to fit if too small
            childWidth = mShortcutSize;
            width = mShortcutSize + getPaddingLeft() + getPaddingRight();
        }
        final int actionHeight = mActionRow.getVisibility() != View.GONE
                ? mActionRowHeight
                : 0;
        final int sliceHeight = getHeightForMode();
        final int heightAvailable = MeasureSpec.getSize(heightMeasureSpec);
        final int heightMode = MeasureSpec.getMode(heightMeasureSpec);
        // Remove the padding from our available height
        int height = heightAvailable - getPaddingTop() - getPaddingBottom();
        if (heightAvailable >= sliceHeight + actionHeight
                || heightMode == MeasureSpec.UNSPECIFIED) {
            // Available space is larger than the slice or we be what we want
            if (heightMode != MeasureSpec.EXACTLY) {
                if (!mIsScrollable) {
                    height = Math.min(mMaxLargeHeight, sliceHeight);
                } else {
                    // If we want to be bigger than max, then we can be a good scrollable at min
                    // large height, if it's not larger lets just use its desired height
                    height = sliceHeight > mMaxLargeHeight ? mMinLargeHeight : sliceHeight;
                }
            }
        } else {
            // Not enough space available for slice in current mode
            if (getMode() == MODE_LARGE && heightAvailable >= mMinLargeHeight + actionHeight) {
                // It's just a slice with scrolling content; cap it to height available.
                height = Math.min(mMinLargeHeight, heightAvailable);
            } else if (getMode() == MODE_SHORTCUT) {
                // TODO: consider scaling the shortcut to fit if too small
                height = mShortcutSize;
            }
        }

        int childHeight = height + getPaddingTop() + getPaddingBottom();
        int childWidthMeasureSpec = MeasureSpec.makeMeasureSpec(childWidth, MeasureSpec.EXACTLY);
        int childHeightMeasureSpec = MeasureSpec.makeMeasureSpec(childHeight, MeasureSpec.EXACTLY);
        measureChild(mCurrentView, childWidthMeasureSpec, childHeightMeasureSpec);

        int actionPaddedHeight = actionHeight + getPaddingTop() + getPaddingBottom();
        int actionHeightSpec = MeasureSpec.makeMeasureSpec(actionPaddedHeight, MeasureSpec.EXACTLY);
        measureChild(mActionRow, childWidthMeasureSpec, actionHeightSpec);

        // Total height should include action row and our padding
        height += actionHeight + getPaddingTop() + getPaddingBottom();
        setMeasuredDimension(width, height);
    }

    @Override
    protected void onLayout(boolean changed, int l, int t, int r, int b) {
        View v = mCurrentView.getView();
        final int left = getPaddingLeft();
        final int top = getPaddingTop();
        v.layout(left, top, left + v.getMeasuredWidth(), top + v.getMeasuredHeight());
        if (mActionRow.getVisibility() != View.GONE) {
            mActionRow.layout(left,
                    top + v.getMeasuredHeight(),
                    left + mActionRow.getMeasuredWidth(),
                    top + v.getMeasuredHeight() + mActionRow.getMeasuredHeight());
        }
    }

    @Override
    public void onChanged(@Nullable Slice slice) {
        setSlice(slice);
    }

    /**
     * Populates this view to the provided {@link Slice}.
     *
     * This will not update automatically if the slice content changes, for live
     * content see {@link SliceLiveData}.
     */
    public void setSlice(@Nullable Slice slice) {
        if (slice != null) {
            if (mCurrentSlice == null || mCurrentSlice.getUri() != slice.getUri()) {
                // New slice, new actions
                mActions = SliceUtils.getSliceActions(slice);
                mCurrentView.resetView();
            }
        } else {
            // No slice, no actions
            mActions = null;
        }
        mCurrentSlice = slice;
        reinflate();
    }

    /**
     * Returns the slice actions presented in this view.
     */
    @Nullable
    public List<SliceItem> getSliceActions() {
        return mActions;
    }

    /**
     * Sets the slice actions to display for the slice contained in this view. Normally SliceView
     * will automatically show actions, however, it is possible to reorder or omit actions on the
     * view using this method. This is generally discouraged.
     * <p>
     * It is required that the slice be set on this view before actions can be set, otherwise
     * this will throw {@link IllegalStateException}. If any of the actions supplied are not
     * available for the slice set on this view (i.e. the action is not returned by
     * {@link SliceUtils#getSliceActions(Slice)} this will throw {@link IllegalArgumentException}.
     */
    public void setSliceActions(@Nullable List<SliceItem> newActions) {
        // Check that these actions are part of available set
        if (mCurrentSlice == null) {
            throw new IllegalStateException("Trying to set actions on a view without a slice");
        }
        List<SliceItem> availableActions = SliceUtils.getSliceActions(mCurrentSlice);
        if (availableActions != null && newActions != null) {
            for (int i = 0; i < newActions.size(); i++) {
                if (!availableActions.contains(newActions.get(i))) {
                    throw new IllegalArgumentException(
                            "Trying to set an action that isn't available: " + newActions.get(i));
                }
            }
        }
        mActions = newActions;
        updateActions();
    }

    /**
     * Set the mode this view should present in.
     */
    public void setMode(@SliceMode int mode) {
        setMode(mode, false /* animate */);
    }

    /**
     * Set whether this view should allow scrollable content when presenting in {@link #MODE_LARGE}.
     */
    public void setScrollable(boolean isScrollable) {
        mIsScrollable = isScrollable;
        reinflate();
    }

    /**
     * Sets the listener to notify when an interaction events occur on the view.
     * @see EventInfo
     */
    public void setOnSliceActionListener(@Nullable OnSliceActionListener observer) {
        mSliceObserver = observer;
        mCurrentView.setSliceActionListener(mSliceObserver);
    }

    /**
     * Contents of a slice such as icons, text, and controls (e.g. toggle) can be tinted. Normally
     * a color for tinting will be provided by the slice. Using this method will override
     * this color information and instead tint elements with the provided color.
     *
     * @param tintColor the color to use for tinting contents of this view.
     */
    public void setTint(int tintColor) {
        mThemeTintColor = tintColor;
        mCurrentView.setTint(tintColor);
    }

    /**
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setMode(@SliceMode int mode, boolean animate) {
        if (animate) {
            Log.e(TAG, "Animation not supported yet");
        }
        if (mMode == mode) {
            return;
        }
        mMode = mode;
        reinflate();
    }

    /**
     * @return the mode this view is presenting in.
     */
    public @SliceMode int getMode() {
        return mMode;
    }

    /**
     * @hide
     *
     * Whether this view should show a row of actions with it.
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public void setShowActionRow(boolean show) {
        mShowActions = show;
        updateActions();
    }

    /**
     * @return whether this view is showing a row of actions.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public boolean isShowingActionRow() {
        return mShowActions;
    }

    private SliceChildView createView(int mode, boolean isGrid) {
        switch (mode) {
            case MODE_SHORTCUT:
                return new ShortcutView(getContext());
            case MODE_SMALL:
                return isGrid ? new GridRowView(getContext()) : new RowView(getContext());
        }
        return new LargeTemplateView(getContext());
    }

    private void reinflate() {
        if (mCurrentSlice == null) {
            mCurrentView.resetView();
            return;
        }
        ListContent lc = new ListContent(getContext(), mCurrentSlice);
        if (!lc.isValid()) {
            mCurrentView.resetView();
            mCurrentView.setVisibility(View.GONE);
            return;
        }
        // TODO: Smarter mapping here from one state to the next.
        int mode = getMode();
        boolean reuseView = mode == mCurrentView.getMode();
        SliceItem header = lc.getHeaderItem();
        boolean isSmallGrid = header != null && SliceQuery.hasHints(header, HINT_HORIZONTAL);
        if (reuseView && mode == MODE_SMALL) {
            reuseView = (mCurrentView instanceof GridRowView) == isSmallGrid;
        }
        if (!reuseView) {
            removeAllViews();
            mCurrentView = createView(mode, isSmallGrid);
            if (mSliceObserver != null) {
                mCurrentView.setSliceActionListener(mSliceObserver);
            }
            addView(mCurrentView.getView(), getChildLp(mCurrentView.getView()));
            addView(mActionRow, getChildLp(mActionRow));
            mCurrentView.setMode(mode);
        }
        // Scrolling
        if (mode == MODE_LARGE && (mCurrentView instanceof LargeTemplateView)) {
            ((LargeTemplateView) mCurrentView).setScrollable(mIsScrollable);
        }
        // Styles
        mCurrentView.setStyle(mAttrs);
        mCurrentView.setTint(getTintColor());
        mCurrentView.setVisibility(lc.isValid() ? View.VISIBLE : View.GONE);
        // Set the slice
        mCurrentView.setSlice(mCurrentSlice);
        updateActions();
    }

    private void updateActions() {
        if (mActions == null || mActions.isEmpty()) {
            // No actions, hide the row, clear out the view
            mActionRow.setVisibility(View.GONE);
            mCurrentView.setSliceActions(null);
            return;
        }

        // TODO: take priority attached to actions into account
        if (mShowActions && mMode != MODE_SHORTCUT && mActions.size() >= 2) {
            // Show in action row if available
            mActionRow.setActions(mActions, getTintColor());
            mActionRow.setVisibility(View.VISIBLE);
            // Hide them on the template
            mCurrentView.setSliceActions(null);
        } else if (mActions.size() > 0) {
            // Otherwise set them on the template
            mCurrentView.setSliceActions(mActions);
            mActionRow.setVisibility(View.GONE);
        }
    }

    private int getTintColor() {
        if (mThemeTintColor != -1) {
            // Theme has specified a color, use that
            return mThemeTintColor;
        } else {
            final SliceItem colorItem = SliceQuery.findSubtype(
                    mCurrentSlice, FORMAT_INT, SUBTYPE_COLOR);
            return colorItem != null
                    ? colorItem.getInt()
                    : SliceViewUtil.getColorAccent(getContext());
        }
    }

    private LayoutParams getChildLp(View child) {
        if (child instanceof ShortcutView) {
            return new LayoutParams(mShortcutSize, mShortcutSize);
        } else {
            return new LayoutParams(LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT);
        }
    }

    /**
     * @return String representation of the provided mode.
     * @hide
     */
    @RestrictTo(RestrictTo.Scope.LIBRARY)
    public static String modeToString(@SliceMode int mode) {
        switch(mode) {
            case MODE_SHORTCUT:
                return "MODE SHORTCUT";
            case MODE_SMALL:
                return "MODE SMALL";
            case MODE_LARGE:
                return "MODE LARGE";
            default:
                return "unknown mode: " + mode;
        }
    }
}
