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
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.contrib.RecyclerViewActions.actionOnItemAtPosition;
import static androidx.test.espresso.contrib.RecyclerViewActions.scrollToPosition;
import static androidx.test.espresso.matcher.ViewMatchers.withId;

import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.lessThan;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.hamcrest.number.IsCloseTo.closeTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.text.InputFilter;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;

import androidx.car.utils.CarUxRestrictionsTestUtils;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.test.InstrumentationRegistry;
import androidx.test.espresso.UiController;
import androidx.test.espresso.ViewAction;
import androidx.test.filters.SmallTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.hamcrest.Matcher;
import org.junit.Assume;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * Tests the layout configuration in {@link ActionButtonListItem}.
 */
@RunWith(AndroidJUnit4.class)
@SmallTest
public class ActionButtonListItemTest {
    @Rule
    public ActivityTestRule<PagedListViewTestActivity> mActivityRule =
            new ActivityTestRule<>(PagedListViewTestActivity.class);

    private PagedListViewTestActivity mActivity;
    private PagedListView mPagedListView;
    private ListItemAdapter mAdapter;

    @Before
    public void setUp() {
        Assume.assumeTrue(isAutoDevice());

        mActivity = mActivityRule.getActivity();
        mPagedListView = mActivity.findViewById(androidx.car.test.R.id.paged_list_view);
    }

    @Test
    public void testPrimaryActionVisible() {
        ActionButtonListItem largeIcon = new ActionButtonListItem(mActivity);
        largeIcon.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                ActionButtonListItem.PRIMARY_ACTION_ICON_SIZE_LARGE);

        ActionButtonListItem mediumIcon = new ActionButtonListItem(mActivity);
        mediumIcon.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                ActionButtonListItem.PRIMARY_ACTION_ICON_SIZE_MEDIUM);

        ActionButtonListItem smallIcon = new ActionButtonListItem(mActivity);
        smallIcon.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                ActionButtonListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);

        List<ActionButtonListItem> items = Arrays.asList(largeIcon, mediumIcon, smallIcon);
        setupPagedListView(items);

        assertThat(getViewHolderAtPosition(0).getPrimaryIcon().getVisibility(),
                is(equalTo(View.VISIBLE)));
        assertThat(getViewHolderAtPosition(1).getPrimaryIcon().getVisibility(),
                is(equalTo(View.VISIBLE)));
        assertThat(getViewHolderAtPosition(2).getPrimaryIcon().getVisibility(),
                is(equalTo(View.VISIBLE)));
    }

    @Test
    public void testTextVisible() {
        ActionButtonListItem item0 = new ActionButtonListItem(mActivity);
        item0.setTitle("title");

        ActionButtonListItem item1 = new ActionButtonListItem(mActivity);
        item1.setBody("body");

        List<ActionButtonListItem> items = Arrays.asList(item0, item1);
        setupPagedListView(items);

        assertThat(getViewHolderAtPosition(0).getTitle().getVisibility(),
                is(equalTo(View.VISIBLE)));
        assertThat(getViewHolderAtPosition(1).getBody().getVisibility(),
                is(equalTo(View.VISIBLE)));
    }

    @Test
    public void testTextStartMarginMatchesPrimaryActionType() {
        ActionButtonListItem largeIcon = new ActionButtonListItem(mActivity);
        largeIcon.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                ActionButtonListItem.PRIMARY_ACTION_ICON_SIZE_LARGE);

        ActionButtonListItem mediumIcon = new ActionButtonListItem(mActivity);
        mediumIcon.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                ActionButtonListItem.PRIMARY_ACTION_ICON_SIZE_MEDIUM);

        ActionButtonListItem smallIcon = new ActionButtonListItem(mActivity);
        smallIcon.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                ActionButtonListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);

        ActionButtonListItem emptyIcon = new ActionButtonListItem(mActivity);
        emptyIcon.setPrimaryActionEmptyIcon();

        ActionButtonListItem noIcon = new ActionButtonListItem(mActivity);
        noIcon.setPrimaryActionNoIcon();

        List<ActionButtonListItem> items = Arrays.asList(
                largeIcon, mediumIcon, smallIcon, emptyIcon, noIcon);
        List<Integer> expectedStartMargin = Arrays.asList(
                androidx.car.test.R.dimen.car_keyline_4,  // Large icon.
                androidx.car.test.R.dimen.car_keyline_3,  // Medium icon.
                androidx.car.test.R.dimen.car_keyline_3,  // Small icon.
                androidx.car.test.R.dimen.car_keyline_3,  // Empty icon.
                androidx.car.test.R.dimen.car_keyline_1); // No icon.
        setupPagedListView(items);

        for (int i = 0; i < items.size(); i++) {
            ActionButtonListItem.ViewHolder viewHolder = getViewHolderAtPosition(i);

            int expected = InstrumentationRegistry.getContext().getResources()
                    .getDimensionPixelSize(expectedStartMargin.get(i));
            assertThat(((ViewGroup.MarginLayoutParams) viewHolder.getTitle().getLayoutParams())
                    .getMarginStart(), is(equalTo(expected)));
            assertThat(((ViewGroup.MarginLayoutParams) viewHolder.getBody().getLayoutParams())
                    .getMarginStart(), is(equalTo(expected)));
        }
    }

    @Test
    public void testSingleActionButtonVisibility_withDividers() {
        ActionButtonListItem item = new ActionButtonListItem(mActivity);
        item.setAction("text", true, v -> { /* Do nothing. */ });

        List<ActionButtonListItem> items = Arrays.asList(item);
        setupPagedListView(items);

        ActionButtonListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(viewHolder.getAction1().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getAction1Divider().getVisibility(), is(equalTo(View.VISIBLE)));
    }

    @Test
    public void testTwoActionButtonsVisibility_withDividers() {
        ActionButtonListItem item = new ActionButtonListItem(mActivity);
        item.setActions("text", true, v -> { /* Do nothing. */ },
                "text", true, v -> { /* Do nothing. */ });

        List<ActionButtonListItem> items = Arrays.asList(item);
        setupPagedListView(items);

        ActionButtonListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(viewHolder.getAction1().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getAction1Divider().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getAction2().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getAction2Divider().getVisibility(), is(equalTo(View.VISIBLE)));
    }

    @Test
    public void testSingleActionButtonVisibility_noDividers() {
        ActionButtonListItem item = new ActionButtonListItem(mActivity);
        item.setAction("text", false, v -> { /* Do nothing. */ });

        List<ActionButtonListItem> items = Arrays.asList(item);
        setupPagedListView(items);

        ActionButtonListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(viewHolder.getAction1().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getAction1Divider().getVisibility(), is(equalTo(View.GONE)));
    }

    @Test
    public void testTwoActionButtonsVisibility_noDividers() {
        ActionButtonListItem item = new ActionButtonListItem(mActivity);
        item.setActions("text", false, v -> { /* Do nothing. */ },
                "text", false, v -> { /* Do nothing. */ });

        List<ActionButtonListItem> items = Arrays.asList(item);
        setupPagedListView(items);

        ActionButtonListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(viewHolder.getAction1().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getAction1Divider().getVisibility(), is(equalTo(View.GONE)));
        assertThat(viewHolder.getAction2().getVisibility(), is(equalTo(View.VISIBLE)));
        assertThat(viewHolder.getAction2Divider().getVisibility(), is(equalTo(View.GONE)));
    }

    @Test
    public void testClickInterceptor_ClickableIfOneActionSet() {
        ActionButtonListItem item = new ActionButtonListItem(mActivity);
        item.setEnabled(true);
        item.setAction("text", /* showDivider= */ true, v -> { });

        setupPagedListView(Arrays.asList(item));

        ActionButtonListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertTrue(viewHolder.getClickInterceptView().isClickable());
    }

    @Test
    public void testClickInterceptor_VisibleIfOneActionSet() {
        ActionButtonListItem item = new ActionButtonListItem(mActivity);
        item.setEnabled(true);
        item.setAction("text", /* showDivider= */ true, v -> { });

        setupPagedListView(Arrays.asList(item));

        ActionButtonListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertTrue(viewHolder.getClickInterceptView().getVisibility() == View.VISIBLE);
    }

    @Test
    public void testClickInterceptor_ClickableIfTwoActionsSet() {
        ActionButtonListItem item = new ActionButtonListItem(mActivity);
        item.setEnabled(true);
        item.setActions("text", /* showDivider= */ true, v -> { },
                "text", /* showDivider= */ true, v -> { });

        setupPagedListView(Arrays.asList(item));

        ActionButtonListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertTrue(viewHolder.getClickInterceptView().isClickable());
    }

    @Test
    public void testClickInterceptor_VisibleIfTwoActionsSet() {
        ActionButtonListItem item = new ActionButtonListItem(mActivity);
        item.setEnabled(true);
        item.setActions("text", /* showDivider= */ true, v -> { },
                "text", /* showDivider= */ true, v -> { });

        setupPagedListView(Arrays.asList(item));

        ActionButtonListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertTrue(viewHolder.getClickInterceptView().getVisibility() == View.VISIBLE);
    }



    @Test
    public void testItemWithOnlyTitleIsSingleLine() {
        // Only space.
        ActionButtonListItem item0 = new ActionButtonListItem(mActivity);
        item0.setTitle(" ");

        // Underscore.
        ActionButtonListItem item1 = new ActionButtonListItem(mActivity);
        item1.setTitle("______");

        ActionButtonListItem item2 = new ActionButtonListItem(mActivity);
        item2.setTitle("ALL UPPER CASE");

        // String wouldn't fit in one line.
        ActionButtonListItem item3 = new ActionButtonListItem(mActivity);
        item3.setTitle(InstrumentationRegistry.getContext().getResources().getString(
                androidx.car.test.R.string.over_uxr_text_length_limit));

        List<ActionButtonListItem> items = Arrays.asList(item0, item1, item2, item3);
        setupPagedListView(items);

        double singleLineHeight = InstrumentationRegistry.getContext().getResources().getDimension(
                androidx.car.test.R.dimen.car_single_line_list_item_height);

        LinearLayoutManager layoutManager =
                (LinearLayoutManager) mPagedListView.getRecyclerView().getLayoutManager();
        for (int i = 0; i < items.size(); i++) {
            assertThat((double) layoutManager.findViewByPosition(i).getHeight(),
                    is(closeTo(singleLineHeight, 1.0d)));
        }
    }

    @Test
    public void testItemWithBodyTextIsAtLeastDoubleLine() {
        // Only space.
        ActionButtonListItem item0 = new ActionButtonListItem(mActivity);
        item0.setBody(" ");

        // Underscore.
        ActionButtonListItem item1 = new ActionButtonListItem(mActivity);
        item1.setBody("____");

        // String wouldn't fit in one line.
        ActionButtonListItem item2 = new ActionButtonListItem(mActivity);
        item2.setBody(InstrumentationRegistry.getContext().getResources().getString(
                androidx.car.test.R.string.over_uxr_text_length_limit));

        List<ActionButtonListItem> items = Arrays.asList(item0, item1, item2);
        setupPagedListView(items);

        final int doubleLineHeight =
                (int) InstrumentationRegistry.getContext().getResources().getDimension(
                        androidx.car.test.R.dimen.car_double_line_list_item_height);

        LinearLayoutManager layoutManager =
                (LinearLayoutManager) mPagedListView.getRecyclerView().getLayoutManager();
        for (int i = 0; i < items.size(); i++) {
            assertThat(layoutManager.findViewByPosition(i).getHeight(),
                    is(greaterThanOrEqualTo(doubleLineHeight)));
        }
    }

    @Test
    public void testPrimaryIconDrawable() {
        Drawable drawable = InstrumentationRegistry.getContext().getResources().getDrawable(
                android.R.drawable.sym_def_app_icon, null);

        ActionButtonListItem item0 = new ActionButtonListItem(mActivity);
        item0.setPrimaryActionIcon(drawable,
                ActionButtonListItem.PRIMARY_ACTION_ICON_SIZE_LARGE);

        List<ActionButtonListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        assertTrue(getViewHolderAtPosition(0).getPrimaryIcon().getDrawable().getConstantState()
                .equals(drawable.getConstantState()));
    }

    @Test
    public void testPrimaryIconSizesInIncreasingOrder() {
        ActionButtonListItem small = new ActionButtonListItem(mActivity);
        small.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                ActionButtonListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);

        ActionButtonListItem medium = new ActionButtonListItem(mActivity);
        medium.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                ActionButtonListItem.PRIMARY_ACTION_ICON_SIZE_MEDIUM);

        ActionButtonListItem large = new ActionButtonListItem(mActivity);
        large.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                ActionButtonListItem.PRIMARY_ACTION_ICON_SIZE_LARGE);

        List<ActionButtonListItem> items = Arrays.asList(small, medium, large);
        setupPagedListView(items);

        ActionButtonListItem.ViewHolder smallVH = getViewHolderAtPosition(0);
        ActionButtonListItem.ViewHolder mediumVH = getViewHolderAtPosition(1);
        ActionButtonListItem.ViewHolder largeVH = getViewHolderAtPosition(2);

        assertThat(largeVH.getPrimaryIcon().getHeight(), is(greaterThan(
                mediumVH.getPrimaryIcon().getHeight())));
        assertThat(mediumVH.getPrimaryIcon().getHeight(), is(greaterThan(
                smallVH.getPrimaryIcon().getHeight())));
    }

    @Test
    public void testLargePrimaryIconHasNoStartMargin() {
        ActionButtonListItem item0 = new ActionButtonListItem(mActivity);
        item0.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                ActionButtonListItem.PRIMARY_ACTION_ICON_SIZE_LARGE);

        List<ActionButtonListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        ActionButtonListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(((ViewGroup.MarginLayoutParams) viewHolder.getPrimaryIcon().getLayoutParams())
                .getMarginStart(), is(equalTo(0)));
    }

    @Test
    public void testSmallAndMediumPrimaryIconStartMargin() {
        ActionButtonListItem item0 = new ActionButtonListItem(mActivity);
        item0.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                ActionButtonListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);

        ActionButtonListItem item1 = new ActionButtonListItem(mActivity);
        item1.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                ActionButtonListItem.PRIMARY_ACTION_ICON_SIZE_MEDIUM);

        List<ActionButtonListItem> items = Arrays.asList(item0, item1);
        setupPagedListView(items);

        int expected = InstrumentationRegistry.getContext().getResources().getDimensionPixelSize(
                androidx.car.test.R.dimen.car_keyline_1);

        ActionButtonListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(((ViewGroup.MarginLayoutParams) viewHolder.getPrimaryIcon().getLayoutParams())
                .getMarginStart(), is(equalTo(expected)));

        viewHolder = getViewHolderAtPosition(1);
        assertThat(((ViewGroup.MarginLayoutParams) viewHolder.getPrimaryIcon().getLayoutParams())
                .getMarginStart(), is(equalTo(expected)));
    }

    @Test
    public void testSmallPrimaryIconTopMarginRemainsTheSameRegardlessOfTextLength() {
        final String longText = InstrumentationRegistry.getContext().getResources().getString(
                androidx.car.test.R.string.over_uxr_text_length_limit);

        // Single line item.
        ActionButtonListItem item0 = new ActionButtonListItem(mActivity);
        item0.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                ActionButtonListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);
        item0.setTitle("one line text");

        // Double line item with one line text.
        ActionButtonListItem item1 = new ActionButtonListItem(mActivity);
        item1.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                ActionButtonListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);
        item1.setTitle("one line text");
        item1.setBody("one line text");

        // Double line item with long text.
        ActionButtonListItem item2 = new ActionButtonListItem(mActivity);
        item2.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                ActionButtonListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);
        item2.setTitle("one line text");
        item2.setBody(longText);

        // Body text only - long text.
        ActionButtonListItem item3 = new ActionButtonListItem(mActivity);
        item3.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                ActionButtonListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);
        item3.setBody(longText);

        // Body text only - one line text.
        ActionButtonListItem item4 = new ActionButtonListItem(mActivity);
        item4.setPrimaryActionIcon(android.R.drawable.sym_def_app_icon,
                ActionButtonListItem.PRIMARY_ACTION_ICON_SIZE_SMALL);
        item4.setBody("one line text");

        List<ActionButtonListItem> items = Arrays.asList(item0, item1, item2, item3, item4);
        setupPagedListView(items);

        for (int i = 1; i < items.size(); i++) {
            onView(withId(androidx.car.test.R.id.recycler_view)).perform(scrollToPosition(i));
            // Implementation uses integer division so it may be off by 1 vs centered vertically.
            assertThat((double) getViewHolderAtPosition(i - 1).getPrimaryIcon().getTop(),
                    is(closeTo(
                            (double) getViewHolderAtPosition(i).getPrimaryIcon().getTop(), 1.0d)));
        }
    }

    @Test
    public void testItemNotClickableWithNullOnClickListener() {
        ActionButtonListItem item = new ActionButtonListItem(mActivity);
        item.setOnClickListener(null);

        setupPagedListView(Arrays.asList(item));

        ActionButtonListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertFalse(viewHolder.itemView.isClickable());
    }



    @Test
    public void testClickingSupplementalAction() {
        final boolean[] clicked = {false};

        ActionButtonListItem item0 = new ActionButtonListItem(mActivity);
        item0.setAction("action", true, v -> clicked[0] = true);

        List<ActionButtonListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        onView(withId(androidx.car.test.R.id.recycler_view)).perform(
                actionOnItemAtPosition(0, clickChildViewWithId(androidx.car.test.R.id.action1)));
        assertTrue(clicked[0]);
    }

    @Test
    public void testClickingBothSupplementalActions() {
        final boolean[] clicked = {false, false};

        ActionButtonListItem item0 = new ActionButtonListItem(mActivity);
        item0.setActions("action 1", true, v -> clicked[0] = true,
                "action 2", true, v -> clicked[1] = true);

        List<ActionButtonListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        onView(withId(androidx.car.test.R.id.recycler_view)).perform(
                actionOnItemAtPosition(0, clickChildViewWithId(androidx.car.test.R.id.action1)));
        assertTrue(clicked[0]);
        assertFalse(clicked[1]);

        onView(withId(androidx.car.test.R.id.recycler_view)).perform(
                actionOnItemAtPosition(0, clickChildViewWithId(androidx.car.test.R.id.action2)));
        assertTrue(clicked[1]);
    }

    @Test
    public void testCustomViewBinderBindsLast() {
        final String updatedTitle = "updated title";

        ActionButtonListItem item0 = new ActionButtonListItem(mActivity);
        item0.setTitle("original title");
        item0.addViewBinder((viewHolder) -> viewHolder.getTitle().setText(updatedTitle));

        List<ActionButtonListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        ActionButtonListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(viewHolder.getTitle().getText(), is(equalTo(updatedTitle)));
    }

    @Test
    public void testCustomViewBinderOnUnusedViewsHasNoEffect() {
        ActionButtonListItem item0 = new ActionButtonListItem(mActivity);
        item0.addViewBinder((viewHolder) -> viewHolder.getBody().setText("text"));

        List<ActionButtonListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        ActionButtonListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(viewHolder.getBody().getVisibility(), is(equalTo(View.GONE)));
        // Custom binder interacts with body but has no effect.
        // Expect card height to remain single line.
        assertThat((double) viewHolder.itemView.getHeight(), is(closeTo(
                InstrumentationRegistry.getContext().getResources().getDimension(
                        androidx.car.test.R.dimen.car_single_line_list_item_height), 1.0d)));
    }

    @Test
    public void testRevertingViewBinder() throws Throwable {
        ActionButtonListItem item0 = new ActionButtonListItem(mActivity);
        item0.setBody("one item");
        item0.addViewBinder(
                (viewHolder) -> viewHolder.getBody().setEllipsize(TextUtils.TruncateAt.END),
                (viewHolder -> viewHolder.getBody().setEllipsize(null)));

        List<ActionButtonListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        ActionButtonListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);

        // Bind view holder to a new item - the customization made by item0 should be reverted.
        ActionButtonListItem item1 = new ActionButtonListItem(mActivity);
        item1.setBody("new item");
        mActivityRule.runOnUiThread(() -> item1.bind(viewHolder));

        assertThat(viewHolder.getBody().getEllipsize(), is(equalTo(null)));
    }

    @Test
    public void testRemovingViewBinder() {
        ActionButtonListItem item0 = new ActionButtonListItem(mActivity);
        item0.setBody("one item");
        ListItem.ViewBinder<ActionButtonListItem.ViewHolder> binder =
                (viewHolder) -> viewHolder.getTitle().setEllipsize(TextUtils.TruncateAt.END);
        item0.addViewBinder(binder);

        assertTrue(item0.removeViewBinder(binder));

        List<ActionButtonListItem> items = Arrays.asList(item0);
        setupPagedListView(items);

        assertThat(getViewHolderAtPosition(0).getBody().getEllipsize(), is(equalTo(null)));
    }

    @Test
    public void testSettingTitleOrBodyAsPrimaryText() {
        // Create 2 items, one with Title as primary (default) and one with Body.
        // The primary text, regardless of view, should have consistent look (as primary).
        ActionButtonListItem item0 = new ActionButtonListItem(mActivity);
        item0.setTitle("title");
        item0.setBody("body");

        ActionButtonListItem item1 = new ActionButtonListItem(mActivity);
        item1.setTitle("title");
        item1.setBody("body", true);

        List<ActionButtonListItem> items = Arrays.asList(item0, item1);
        setupPagedListView(items);

        ActionButtonListItem.ViewHolder titlePrimary = getViewHolderAtPosition(0);
        ActionButtonListItem.ViewHolder bodyPrimary = getViewHolderAtPosition(1);
        assertThat(titlePrimary.getTitle().getTextSize(),
                is(equalTo(bodyPrimary.getBody().getTextSize())));
        assertThat(titlePrimary.getTitle().getTextColors(),
                is(equalTo(bodyPrimary.getBody().getTextColors())));
    }

    @Test
    public void testNoCarriedOverOnClickListener() throws Throwable {
        boolean[] clicked = new boolean[] {false};
        ActionButtonListItem item0 = new ActionButtonListItem(mActivity);
        item0.setOnClickListener(v -> clicked[0] = true);

        setupPagedListView(Arrays.asList(item0));

        onView(withId(androidx.car.test.R.id.recycler_view)).perform(
                actionOnItemAtPosition(0, click()));
        assertTrue(clicked[0]);

        // item1 does not have onClickListener.
        ActionButtonListItem item1 = new ActionButtonListItem(mActivity);
        ActionButtonListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        // Manually rebind the view holder.
        mActivityRule.runOnUiThread(() -> item1.bind(viewHolder));

        // Reset for testing.
        clicked[0] = false;
        onView(withId(androidx.car.test.R.id.recycler_view)).perform(
                actionOnItemAtPosition(0, click()));
        assertFalse(clicked[0]);
    }

    @Test
    public void testUpdateItem() {
        ActionButtonListItem item = new ActionButtonListItem(mActivity);
        setupPagedListView(Arrays.asList(item));

        String title = "updated title";
        item.setTitle(title);

        refreshUi();

        ActionButtonListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertThat(viewHolder.getTitle().getText(), is(equalTo(title)));
    }

    @Test
    public void testUxRestrictionsChange() {
        String longText = mActivity.getString(
                androidx.car.test.R.string.over_uxr_text_length_limit);
        ActionButtonListItem item = new ActionButtonListItem(mActivity);
        item.setBody(longText);

        setupPagedListView(Arrays.asList(item));

        ActionButtonListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        // Default behavior without UXR is unrestricted.
        assertThat(viewHolder.getBody().getText(), is(equalTo(longText)));

        viewHolder.applyUxRestrictions(CarUxRestrictionsTestUtils.getFullyRestricted());
        refreshUi();

        // Verify that the body text length is limited.
        assertThat(viewHolder.getBody().getText().length(), is(lessThan(longText.length())));
    }

    @Test
    public void testUxRestrictionsChangesDoNotAlterExistingInputFilters() {
        InputFilter filter = new InputFilter.AllCaps(Locale.US);
        String bodyText = "bodytext";
        ActionButtonListItem item = new ActionButtonListItem(mActivity);
        item.setBody(bodyText);
        item.addViewBinder(vh -> vh.getBody().setFilters(new InputFilter[] {filter}));

        setupPagedListView(Arrays.asList(item));

        ActionButtonListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);

        // Toggle UX restrictions between fully restricted and unrestricted should not affect
        // existing filters.
        viewHolder.applyUxRestrictions(CarUxRestrictionsTestUtils.getFullyRestricted());
        refreshUi();
        assertTrue(Arrays.asList(viewHolder.getBody().getFilters()).contains(filter));

        viewHolder.applyUxRestrictions(CarUxRestrictionsTestUtils.getBaseline());
        refreshUi();
        assertTrue(Arrays.asList(viewHolder.getBody().getFilters()).contains(filter));
    }

    @Test
    public void testDisabledItemDisablesViewHolder() {
        ActionButtonListItem item = new ActionButtonListItem(mActivity);
        item.setOnClickListener(v -> { });
        item.setTitle("title");
        item.setBody("body");
        item.setAction("action", false, v -> { });
        item.setEnabled(false);

        setupPagedListView(Arrays.asList(item));

        ActionButtonListItem.ViewHolder viewHolder = getViewHolderAtPosition(0);
        assertFalse(viewHolder.getTitle().isEnabled());
        assertFalse(viewHolder.getBody().isEnabled());
        assertFalse(viewHolder.getAction1().isEnabled());
    }

    @Test
    public void testDisabledItemDoesNotRespondToClick() {
        // Disabled view will not respond to touch event.
        // Current test setup makes it hard to test, since clickChildViewWithId() directly calls
        // performClick() on a view, bypassing the way UI handles disabled state.

        // We are explicitly setting itemView so test it here.
        boolean[] clicked = new boolean[]{false};
        ActionButtonListItem item = new ActionButtonListItem(mActivity);
        item.setOnClickListener(v -> clicked[0] = true);
        item.setEnabled(false);

        setupPagedListView(Arrays.asList(item));

        onView(withId(androidx.car.test.R.id.recycler_view)).perform(
                actionOnItemAtPosition(0, click()));

        assertFalse(clicked[0]);
    }

    private boolean isAutoDevice() {
        PackageManager packageManager = mActivityRule.getActivity().getPackageManager();
        return packageManager.hasSystemFeature(PackageManager.FEATURE_AUTOMOTIVE);
    }

    private void refreshUi() {
        try {
            mActivityRule.runOnUiThread(() -> {
                mAdapter.notifyDataSetChanged();
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException(throwable);
        }
        // Wait for paged list view to layout by using espresso to scroll to a position.
        onView(withId(androidx.car.test.R.id.recycler_view)).perform(scrollToPosition(0));
    }

    private void setupPagedListView(List<ActionButtonListItem> items) {
        ListItemProvider provider = new ListItemProvider.ListProvider(
                new ArrayList<>(items));
        try {
            mAdapter = new ListItemAdapter(mActivity, provider);
            mActivityRule.runOnUiThread(() -> {
                mPagedListView.setAdapter(mAdapter);
            });
        } catch (Throwable throwable) {
            throwable.printStackTrace();
            throw new RuntimeException(throwable);
        }

        refreshUi();
    }

    private ActionButtonListItem.ViewHolder getViewHolderAtPosition(int position) {
        return (ActionButtonListItem.ViewHolder) mPagedListView.getRecyclerView()
                .findViewHolderForAdapterPosition(position);
    }

    private static ViewAction clickChildViewWithId(final int id) {
        return new ViewAction() {
            @Override
            public Matcher<View> getConstraints() {
                return null;
            }

            @Override
            public String getDescription() {
                return "Click on a child view with specific id.";
            }

            @Override
            public void perform(UiController uiController, View view) {
                View v = view.findViewById(id);
                v.performClick();
            }
        };
    }
}
