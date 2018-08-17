/*
 * Copyright (C) 2018 The Android Open Source Project
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
import static androidx.test.espresso.assertion.PositionAssertions.isCompletelyRightOf;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

import static junit.framework.TestCase.assertTrue;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.junit.Assert.assertEquals;

import android.view.View;
import android.widget.ImageButton;

import androidx.car.R;
import androidx.test.filters.MediumTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.hamcrest.TypeSafeMatcher;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

/** Unit tests for {@link CarToolbar}. */
@RunWith(AndroidJUnit4.class)
@MediumTest
public class CarToolbarTest {
    @Rule
    public ActivityTestRule<CarToolbarTestActivity> mActivityRule =
            new ActivityTestRule<>(CarToolbarTestActivity.class);
    private CarToolbarTestActivity mActivity;
    private CarToolbar mToolbar;

    @Before
    public void setUp() {
        mActivity = mActivityRule.getActivity();
        mToolbar = mActivity.findViewById(R.id.car_toolbar);
    }

    @Test
    public void testConstructor() {
        new CarToolbar(mActivity);

        new CarToolbar(mActivity, /* attrs= */ null);

        new CarToolbar(mActivity, /* attrs= */ null, R.attr.carToolbarStyle);

        new CarToolbar(mActivity, /* attrs= */ null, R.attr.carToolbarStyle,
                R.style.Widget_Car_CarToolbar);
    }

    @Test
    public void testMinimumHeight() throws Throwable {
        int minHeight = mActivity.getResources().getDimensionPixelSize(R.dimen.car_app_bar_height);
        // Set all widgets to null - Toolbar should still be minimum-height tall.
        mActivityRule.runOnUiThread(() -> {
            mToolbar.setTitle(null);
            mToolbar.setNavigationIcon(null);
        });

        assertThat(mToolbar.getHeight(), is(greaterThanOrEqualTo(minHeight)));
    }

    @Test
    public void testTitleContent() throws Throwable {
        mActivityRule.runOnUiThread(() -> mToolbar.setTitle("title"));

        // Verify view is updated, and getTitle() returns expected value.
        onView(withId(R.id.title)).check(matches(withText("title")));
        assertEquals("title", mToolbar.getTitle());

        mActivityRule.runOnUiThread(() -> mToolbar.setTitle("new title"));
        onView(withId(R.id.title)).check(matches(withText("new title")));
        assertEquals("new title", mToolbar.getTitle());
    }

    @Test
    public void testSetTitleTextAppearance() throws Throwable {
        // Since there are no APIs to get reference to the underlying implementation of
        // title, here we are testing that calling the relevant APIs doesn't crash.
        mActivityRule.runOnUiThread(() ->
                mToolbar.setTitleTextAppearance(R.style.TextAppearance_Car_Body1));
    }

    @Test
    public void testSetTitle_NullValueHidesToolbar() throws Throwable {
        mActivityRule.runOnUiThread(() -> mToolbar.setTitle(null));

        onView(withId(R.id.nav_button)).check(matches(isDisplayed()));
    }

    @Test
    public void testSetNavigationIcon() throws Throwable {
        // Since there is no easy way to compare drawable, here we are testing that calling the
        // relevant APIs doesn't crash.
        mActivityRule.runOnUiThread(() ->
                mToolbar.setNavigationIcon(android.R.drawable.sym_def_app_icon));
    }

    @Test
    public void testSetNavigationIconContainerWidth() throws Throwable {
        mActivityRule.runOnUiThread(() -> {
            mToolbar.setNavigationIcon(R.drawable.ic_nav_arrow_back);
            // Set title to verify icon space on right.
            mToolbar.setTitle("title");
        });

        int sideWidth = 10;
        // Container width is icon width plus |sideWidth| on both ends.
        int containerWidth = getNavigationIconView().getWidth() + (sideWidth * 2);
        mActivityRule.runOnUiThread(() ->
                mToolbar.setNavigationIconContainerWidth(containerWidth));

        onView(withId(R.id.nav_button)).check(matches(withLeft(sideWidth)));
        onView(withId(R.id.title)).check(matches(withLeft(containerWidth)));
    }

    @Test
    public void testSetNavigationIconContainerWidth_NoContainerKeepsIconCompletelyVisible()
            throws Throwable {
        mActivityRule.runOnUiThread(() -> {
            mToolbar.setNavigationIcon(R.drawable.ic_nav_arrow_back);
            // Set title to verify icon space on right.
            mToolbar.setTitle("title");
        });

        int containerWidth = 0;
        mActivityRule.runOnUiThread(() ->
                mToolbar.setNavigationIconContainerWidth(containerWidth));

        onView(withId(R.id.nav_button)).check(matches(withLeft(0)));
        onView(withId(R.id.title)).check(isCompletelyRightOf(withId(R.id.nav_button)));
    }

    @Test
    public void testSetNavigationIconBackground() throws Throwable {
        // Since there is no easy way to compare drawable, here we are testing that calling the
        // relevant APIs doesn't crash.
        mActivityRule.runOnUiThread(() ->
                mToolbar.setNavigationIconBackground(R.drawable.car_card_background));

        mActivityRule.runOnUiThread(() ->
                mToolbar.setNavigationIconBackground(
                        mActivity.getDrawable(android.R.drawable.sym_def_app_icon)));
    }

    @Test
    public void testSetNavigationIconOnClickListener() throws Throwable {
        boolean[] clicked = new boolean[] {false};
        mActivityRule.runOnUiThread(() ->
                mToolbar.setNavigationIconOnClickListener(v -> clicked[0] = true));

        onView(withId(R.id.nav_button)).perform(click());
        assertTrue(clicked[0]);
    }

    private ImageButton getNavigationIconView() {
        return mActivity.findViewById(R.id.nav_button);
    }

    private static Matcher<View> withLeft(int expected) {
        return new TypeSafeMatcher<View>() {
            @Override
            protected boolean matchesSafely(View item) {
                return item.getLeft() == expected;
            }

            @Override
            public void describeTo(Description description) {
                description.appendText("is " + expected + " pixel to its parent");
            }
        };
    }
}
