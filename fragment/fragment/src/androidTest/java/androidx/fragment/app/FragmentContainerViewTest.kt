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

package androidx.fragment.app

import android.animation.LayoutTransition
import androidx.fragment.app.test.FragmentTestActivity
import androidx.fragment.test.R
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.MediumTest
import androidx.test.rule.ActivityTestRule
import com.google.common.truth.Truth.assertThat
import com.google.common.truth.Truth.assertWithMessage
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.lang.IllegalArgumentException

@MediumTest
@RunWith(AndroidJUnit4::class)
class FragmentContainerViewTest {
    @get:Rule
    var activityRule = ActivityTestRule(FragmentTestActivity::class.java)

    @Before
    fun setupContainer() {
        activityRule.setContentView(R.layout.fragment_container_view)
    }

    @Test
    fun setLayoutTransitionUnsupported() {
        val activity = activityRule.activity
        val layout = FragmentContainerView(activity.applicationContext)

        try {
            layout.layoutTransition = LayoutTransition()
        } catch (e: UnsupportedOperationException) {
            assertThat(e)
                .hasMessageThat()
                .contains("FragmentContainerView does not support Layout Transitions or " +
                        "animateLayoutChanges=\"true\".")
        }
    }

    // If view sets animateLayoutChanges to true, throw UnsupportedOperationException
    @Test
    fun animateLayoutChangesTrueUnsupported() {
        try {
            StrictViewFragment(R.layout.fragment_container_view_unsupported_operation)
        } catch (e: UnsupportedOperationException) {
            assertThat(e)
                .hasMessageThat()
                .contains("FragmentContainerView does not support Layout Transitions or " +
                        "animateLayoutChanges=\"true\".")
        }
    }

    @Test
    fun createFragmentWithFragmentContainerView() {
        val activity = activityRule.activity
        val fm = activity.supportFragmentManager

        val fragment = StrictViewFragment(R.layout.fragment_container_view)
        fm.beginTransaction()
            .add(R.id.fragment_container_view, fragment)
            .commit()
        activityRule.runOnUiThread { fm.executePendingTransactions() }

        assertWithMessage("Fragment View should be a FragmentContainerView")
            .that(fragment.view)
            .isInstanceOf(FragmentContainerView::class.java)
    }

    @Test
    fun removeViewAt() {
        val context = activityRule.activity.applicationContext
        val view = FragmentContainerView(context)

        val childView1 = FragmentContainerView(context)
        val childView2 = FragmentContainerView(context)

        view.addView(childView1)
        view.addView(childView2)

        assertThat(view.childCount).isEqualTo(3)
        assertThat(view.getChildAt(2)).isEqualTo(childView2)

        view.removeViewAt(1)

        assertThat(view.childCount).isEqualTo(2)
        assertThat(view.getChildAt(1)).isEqualTo(childView2)
    }

    @Test
    fun removeViewAtIndex0() {
        val context = activityRule.activity.applicationContext
        val view = FragmentContainerView(context)

        val childView1 = FragmentContainerView(context)
        val childView2 = FragmentContainerView(context)

        view.addView(childView1)
        view.addView(childView2)

        assertThat(view.childCount).isEqualTo(3)

        view.removeViewAt(0)

        assertThat(view.childCount).isEqualTo(3)
    }

    @Test
    fun removeViewInLayout() {
        val context = activityRule.activity.applicationContext
        val view = FragmentContainerView(context)

        val childView1 = FragmentContainerView(context)
        val childView2 = FragmentContainerView(context)

        view.addView(childView1)
        view.addView(childView2)

        assertThat(view.childCount).isEqualTo(3)
        assertThat(view.getChildAt(2)).isEqualTo(childView2)

        view.removeViewInLayout(childView1)

        assertThat(view.childCount).isEqualTo(2)
        assertThat(view.getChildAt(1)).isEqualTo(childView2)
    }

    @Test
    fun removeViewInLayoutFirstChild() {
        val context = activityRule.activity.applicationContext
        val view = FragmentContainerView(context)

        val childView1 = FragmentContainerView(context)
        val childView2 = FragmentContainerView(context)

        view.addView(childView1)
        view.addView(childView2)

        assertThat(view.childCount).isEqualTo(3)

        view.removeViewInLayout(view.getChildAt(0))

        assertThat(view.childCount).isEqualTo(3)
    }

    @Test
    fun removeView() {
        val context = activityRule.activity.applicationContext
        val view = FragmentContainerView(context)

        val childView1 = FragmentContainerView(context)
        val childView2 = FragmentContainerView(context)

        view.addView(childView1)
        view.addView(childView2)

        assertThat(view.childCount).isEqualTo(3)
        assertThat(view.getChildAt(2)).isEqualTo(childView2)
        view.removeView(childView1)
        assertThat(view.getChildAt(1)).isEqualTo(childView2)
    }

    @Test
    fun removeViewFirstChild() {
        val context = activityRule.activity.applicationContext
        val view = FragmentContainerView(context)

        val childView1 = FragmentContainerView(context)
        val childView2 = FragmentContainerView(context)

        view.addView(childView1)
        view.addView(childView2)

        assertThat(view.childCount).isEqualTo(3)

        view.removeView(view.getChildAt(0))

        assertThat(view.childCount).isEqualTo(3)
    }

    @Test
    fun removeViews() {
        val context = activityRule.activity.applicationContext
        val view = FragmentContainerView(context)

        val childView1 = FragmentContainerView(context)
        val childView2 = FragmentContainerView(context)

        view.addView(childView1)
        view.addView(childView2)

        assertThat(view.childCount).isEqualTo(3)
        assertThat(view.getChildAt(2)).isEqualTo(childView2)
        view.removeViews(1, 2)
        assertThat(view.childCount).isEqualTo(1)
    }

    @Test
    fun removeViewsStartIndex0() {
        val context = activityRule.activity.applicationContext
        val view = FragmentContainerView(context)

        try {
            view.removeViews(0, 1)
        } catch (e: IllegalArgumentException) {
            assertThat(e)
                .hasMessageThat()
                .contains("Cannot remove the first child view in a FragmentContainerView. Start " +
                        "index must be greater than 0.")
        }
    }

    @Test
    fun removeViewsInLayout() {
        val context = activityRule.activity.applicationContext
        val view = FragmentContainerView(context)

        val childView1 = FragmentContainerView(context)
        val childView2 = FragmentContainerView(context)

        view.addView(childView1)
        view.addView(childView2)

        assertThat(view.childCount).isEqualTo(3)
        assertThat(view.getChildAt(2)).isEqualTo(childView2)
        view.removeViewsInLayout(1, 2)
        assertThat(view.childCount).isEqualTo(1)
    }

    @Test
    fun removeViewsInLayoutStartIndex0() {
        val context = activityRule.activity.applicationContext
        val view = FragmentContainerView(context)

        try {
            view.removeViewsInLayout(0, 1)
        } catch (e: IllegalArgumentException) {
            assertThat(e)
                .hasMessageThat()
                .contains("Cannot remove the first child view in a FragmentContainerView. Start " +
                        "index must be greater than 0.")
        }
    }

    @Test
    fun removeAllViewsInLayout() {
        val context = activityRule.activity.applicationContext
        val view = FragmentContainerView(context)

        val childView = FragmentContainerView(context)

        view.addView(childView)

        assertThat(view.childCount).isEqualTo(2)
        assertThat(view.getChildAt(1)).isEqualTo(childView)
        view.removeAllViewsInLayout()
        assertThat(view.childCount).isEqualTo(1)
    }

    // removeDetachedView should not actually remove the view
    @Test
    fun removeDetachedView() {
        val context = activityRule.activity.applicationContext
        val view = FragmentContainerView(context)

        val childView1 = FragmentContainerView(context)
        val childView2 = FragmentContainerView(context)

        view.addView(childView1)
        view.addView(childView2)

        assertThat(view.childCount).isEqualTo(3)
        assertThat(view.getChildAt(2)).isEqualTo(childView2)

        view.removeDetachedView(childView1, false)

        assertThat(view.childCount).isEqualTo(3)
        assertThat(view.getChildAt(2)).isEqualTo(childView2)
    }
}