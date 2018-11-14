/*
 * Copyright 2018 The Android Open Source Project
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

package androidx.car.widget;

import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.hamcrest.Matchers.not;

import android.content.pm.PackageManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.car.test.R;
import androidx.recyclerview.widget.RecyclerView;
import androidx.test.filters.MediumTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

/** Unit tests for {@link PagedListView} with alpha jump button. */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class AlphaJumpPagedListViewTest {
    @Rule
    public ActivityTestRule<PagedListViewTestActivity> mActivityRule =
            new ActivityTestRule<>(PagedListViewTestActivity.class);
    private PagedListViewTestActivity mActivity;
    private PagedListView mPagedListView;

    /** Returns {@code true} if the testing device has the automotive feature flag. */
    private boolean isAutoDevice() {
        PackageManager packageManager = mActivityRule.getActivity().getPackageManager();
        return packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE);
    }

    @Before
    public void setUp() {
        Assume.assumeTrue(isAutoDevice());
        mActivity = mActivityRule.getActivity();
        mPagedListView = mActivity.findViewById(R.id.paged_list_view);
        setUpPagedListViewWithAlphaJump();
    }

    /** Sets up {@link #mPagedListView} with the alpha jump adapter. */
    private void setUpPagedListViewWithAlphaJump() {
        try {
            mActivityRule.runOnUiThread(() -> {
                mPagedListView.setMaxPages(PagedListView.ItemCap.UNLIMITED);
                mPagedListView.setAdapter(new AlphaJumpTestAdapter());
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException(throwable);
        }
        // Wait for paged list view to layout.
        onView(withId(R.id.page_up)).check(matches(isDisplayed()));
    }

    @Test
    public void testScrollBarViewDoesNotShowButtonWhenLimitedSpace() {
        setLayoutRestrictions(/* allowUpDownButtons= */false, /* allowScrollThumb= */false,
                /* allowAlphaJump= */false);
        onView(withId(R.id.page_up)).check(matches(not(isDisplayed())));
        onView(withId(R.id.scrollbar_thumb)).check(matches(not(isDisplayed())));
        onView(withId(R.id.alpha_jump)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testScrollBarViewDoesNotShowThumbWhenLimitedSpace() {
        setLayoutRestrictions(/* allowUpDownButtons= */true, /* allowScrollThumb= */false,
                /* allowAlphaJump= */false);
        onView(withId(R.id.page_up)).check(matches(isDisplayed()));
        onView(withId(R.id.scrollbar_thumb)).check(matches(not(isDisplayed())));
        onView(withId(R.id.alpha_jump)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testScrollBarViewDoesNotShowAlphaWhenEnoughSpace() {
        setLayoutRestrictions(/* allowUpDownButtons= */true, /* allowScrollThumb= */true,
                /* allowAlphaJump= */false);
        onView(withId(R.id.page_up)).check(matches(isDisplayed()));
        onView(withId(R.id.scrollbar_thumb)).check(matches(isDisplayed()));
        onView(withId(R.id.alpha_jump)).check(matches(not(isDisplayed())));
    }

    @Test
    public void testScrollBarViewShowAllWhenLimitedSpace() {
        setLayoutRestrictions(/* allowUpDownButtons= */true, /* allowScrollThumb= */true,
                /* allowAlphaJump= */true);
        onView(withId(R.id.page_up)).check(matches(isDisplayed()));
        onView(withId(R.id.scrollbar_thumb)).check(matches(isDisplayed()));
        onView(withId(R.id.alpha_jump)).check(matches(isDisplayed()));
    }

    /**
     * Apply layout height restrictions to scroll bar view
     *
     * @param allowUpDownButtons whether allow up and down buttons or not
     * @param allowScrollThumb whether allow thumb or not
     * @param allowAlphaJump whether allow alpha jump button or not
     *
     */
    private void setLayoutRestrictions(boolean allowUpDownButtons, boolean allowScrollThumb,
            boolean allowAlphaJump) {
        ImageView upButton = mPagedListView.findViewById(R.id.page_up);
        ImageView downButton = mPagedListView.findViewById(R.id.page_down);
        Button alphaJump = mPagedListView.findViewById(R.id.alpha_jump);
        View scrollThumb = mPagedListView.findViewById(R.id.scrollbar_thumb);
        View scrollBarView = mPagedListView.findViewById(R.id.paged_scroll_view);
        ViewGroup.LayoutParams params = scrollBarView.getLayoutParams();
        int seperateMargin = mPagedListView.getContext().getResources()
                .getDimensionPixelSize(androidx.car.R.dimen.car_padding_4);
        int restrictedHeight = mPagedListView.getPaddingTop()
                + upButton.getHeight() + downButton.getHeight()
                + mPagedListView.getPaddingBottom()
                + seperateMargin;
        if (allowUpDownButtons) {
            restrictedHeight += scrollThumb.getHeight() + seperateMargin;
        }
        if (allowScrollThumb) {
            restrictedHeight += alphaJump.getHeight() + seperateMargin;
        }
        // Only when the three variables are all true, we do not need to deduct one pixel off.
        if (!(allowUpDownButtons & allowScrollThumb & allowAlphaJump)) {
            restrictedHeight += -1;
        }
        params.height = restrictedHeight;
        try {
            mActivityRule.runOnUiThread(() -> scrollBarView.requestLayout());
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException(throwable);
        }
    }

    /**
     * An Alpha Jump Adapter used to construct a PagedListView with alpha jump button
     */
    private static class AlphaJumpTestAdapter
            extends RecyclerView.Adapter<AlphaJumpTestAdapter.ViewHolder>
            implements AlphaJumpAdapter {
        private static final int SIZE = 100;
        private final String[] mStrings = new String[SIZE];

        AlphaJumpTestAdapter() {
            for (int i = 0; i < mStrings.length; i++) {
                mStrings[i] = "Dummy" + i;
            }
        }

        @Override
        public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(parent.getContext());
            View view = inflater.inflate(R.layout.paged_list_item_column_card,
                    parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            holder.mTextView.setText(mStrings[position]);
        }

        @Override
        public int getItemCount() {
            return mStrings.length;
        }

        @Override
        public List<AlphaJumpBucket> getAlphaJumpBuckets() {
            AlphaJumpBucketer bucketer = new AlphaJumpBucketer();
            return bucketer.createBuckets(mStrings);
        }

        @Override
        public void onAlphaJumpEnter() {
        }

        @Override
        public void onAlphaJumpLeave(AlphaJumpBucket bucket) {
        }

        /**
         * ViewHolder for AlphaJumpTestAdapter.
         */
        public static class ViewHolder extends RecyclerView.ViewHolder {
            private final TextView mTextView;

            ViewHolder(View itemView) {
                super(itemView);
                mTextView = itemView.findViewById(R.id.text_view);
            }
        }
    }
}
