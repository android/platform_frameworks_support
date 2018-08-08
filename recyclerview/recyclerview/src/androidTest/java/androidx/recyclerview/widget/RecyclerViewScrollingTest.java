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

package androidx.recyclerview.widget;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyFloat;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;

import android.content.Context;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewConfiguration;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.NestedScrollingChild3;
import androidx.core.view.NestedScrollingParent3;
import androidx.core.view.ViewCompat;
import androidx.test.InstrumentationRegistry;
import androidx.test.filters.LargeTest;
import androidx.test.rule.ActivityTestRule;
import androidx.test.runner.AndroidJUnit4;

import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * Large integration tests that verify that a {@link RecyclerView} interacts with nested scrolling
 * correctly.
 */
@RunWith(AndroidJUnit4.class)
@LargeTest
public class RecyclerViewScrollingTest {

    private static final int CHILD_HEIGHT = 800;
    private static final int NSV_HEIGHT = 400;
    private static final int PARENT_HEIGHT = 400;
    private static final int WIDTH = 400;
    private static final int TOTAL_SCROLL_DISTANCE = CHILD_HEIGHT - NSV_HEIGHT;
    private static final int PARTIAL_SCROLL_DISTANCE = TOTAL_SCROLL_DISTANCE / 10;

    private RecyclerView mRecyclerView;
    private NestedScrollingSpyView mParent;

    @Rule
    public final ActivityTestRule<TestContentViewActivity> mActivityTestRule;

    public RecyclerViewScrollingTest() {
        mActivityTestRule = new ActivityTestRule<>(TestContentViewActivity.class);
    }

    @Test
    public void uiFlings_dispatchNestedPreFlingReturnsFalse_scrolls() throws Throwable {
        uiFlings_parentPreFlingReturnDeterminesNestedScrollViewsScroll(false, true);
    }

    @Test
    public void uiFlings_dispatchNestedPreFlingReturnsTrue_doesNotScroll() throws Throwable {
        uiFlings_parentPreFlingReturnDeterminesNestedScrollViewsScroll(true, false);
    }

    private void uiFlings_parentPreFlingReturnDeterminesNestedScrollViewsScroll(
            final boolean returnValue, final boolean scrolls) throws Throwable {
        setup();
        attachToActivity();

        final Context context = InstrumentationRegistry.getContext();

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                doReturn(true).when(mParent).onStartNestedScroll(any(View.class), any(View.class),
                        anyInt(), anyInt());
                doReturn(returnValue).when(mParent).onNestedPreFling(any(View.class), anyFloat(),
                        anyFloat());

                mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    int mTotalScrolled = 0;
                    @Override
                    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {

                    }

                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        mTotalScrolled += dy;
                        if (mTotalScrolled > PARTIAL_SCROLL_DISTANCE) {
                            countDownLatch.countDown();
                        }
                    }
                });

                NestedScrollViewTestUtils.simulateFlingDown(context, mRecyclerView);
            }
        });
        assertThat(countDownLatch.await(1, TimeUnit.SECONDS), is(scrolls));
    }

    @Test
    public void uiFling_fullyParticipatesInNestedScrolling() throws Throwable {
        setup();
        attachToActivity();

        final Context context = InstrumentationRegistry.getContext();

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                doReturn(true).when(mParent).onStartNestedScroll(any(View.class), any(View.class),
                        anyInt(), anyInt());

                mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    int mTotalScrolled = 0;
                    @Override
                    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {

                    }

                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        mTotalScrolled += dy;
                        if (mTotalScrolled == TOTAL_SCROLL_DISTANCE) {
                            countDownLatch.countDown();
                        }
                    }
                });

                NestedScrollViewTestUtils.simulateFlingDown(context, mRecyclerView);
            }
        });
        assertThat(countDownLatch.await(2, TimeUnit.SECONDS), is(true));

        // Verify all of the following TYPE_TOUCH nested scrolling methods are called.
        verify(mParent, atLeastOnce()).onStartNestedScroll(mRecyclerView, mRecyclerView,
                ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
        verify(mParent, atLeastOnce()).onNestedScrollAccepted(mRecyclerView, mRecyclerView,
                ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_TOUCH);
        verify(mParent, atLeastOnce()).onNestedPreScroll(eq(mRecyclerView), anyInt(), anyInt(),
                any(int[].class), eq(ViewCompat.TYPE_TOUCH));
        verify(mParent, atLeastOnce()).onNestedScroll(eq(mRecyclerView),  anyInt(), anyInt(),
                anyInt(), anyInt(), eq(ViewCompat.TYPE_TOUCH), any(int[].class));
        verify(mParent, atLeastOnce()).onNestedPreFling(eq(mRecyclerView),  anyFloat(),
                anyFloat());
        verify(mParent, atLeastOnce()).onNestedFling(eq(mRecyclerView),  anyFloat(), anyFloat(),
                eq(true));
        verify(mParent, atLeastOnce()).onStopNestedScroll(mRecyclerView, ViewCompat.TYPE_TOUCH);

        // Verify all of the following TYPE_NON_TOUCH nested scrolling methods are called
        verify(mParent, atLeastOnce()).onStartNestedScroll(mRecyclerView, mRecyclerView,
                ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);
        verify(mParent, atLeastOnce()).onNestedScrollAccepted(mRecyclerView, mRecyclerView,
                ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);
        verify(mParent, atLeastOnce()).onNestedPreScroll(eq(mRecyclerView), anyInt(), anyInt(),
                any(int[].class), eq(ViewCompat.TYPE_NON_TOUCH));
        verify(mParent, atLeastOnce()).onNestedScroll(eq(mRecyclerView),  anyInt(), anyInt(),
                anyInt(),
                anyInt(), eq(ViewCompat.TYPE_NON_TOUCH), any(int[].class));
        verify(mParent, atLeastOnce()).onStopNestedScroll(mRecyclerView,
                ViewCompat.TYPE_NON_TOUCH);
    }

    @Test
    public void fling_fullyParticipatesInNestedScrolling() throws Throwable {
        setup();
        attachToActivity();

        final Context context = InstrumentationRegistry.getContext();
        final int targetVelocity =
                NestedScrollViewTestUtils.getTargetFlingVelocityTimeAndDistance(context)[0];

        final CountDownLatch countDownLatch = new CountDownLatch(1);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                doReturn(true).when(mParent).onStartNestedScroll(any(View.class), any(View.class),
                        anyInt(), anyInt());

                mRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
                    int mTotalScrolled = 0;
                    @Override
                    public void onScrollStateChanged(RecyclerView recyclerView, int newState) {

                    }

                    @Override
                    public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                        mTotalScrolled += dy;
                        if (mTotalScrolled == TOTAL_SCROLL_DISTANCE) {
                            countDownLatch.countDown();
                        }
                    }
                });

                mRecyclerView.fling(0, targetVelocity);
            }
        });
        assertThat(countDownLatch.await(1, TimeUnit.SECONDS), is(true));

        // Verify all of the following TYPE_NON_TOUCH nested scrolling methods are called
        verify(mParent, atLeastOnce()).onStartNestedScroll(mRecyclerView, mRecyclerView,
                ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);
        verify(mParent, atLeastOnce()).onNestedScrollAccepted(mRecyclerView, mRecyclerView,
                ViewCompat.SCROLL_AXIS_VERTICAL, ViewCompat.TYPE_NON_TOUCH);
        verify(mParent, atLeastOnce()).onNestedPreScroll(eq(mRecyclerView), anyInt(), anyInt(),
                any(int[].class), eq(ViewCompat.TYPE_NON_TOUCH));
        verify(mParent, atLeastOnce()).onNestedScroll(eq(mRecyclerView),  anyInt(), anyInt(),
                anyInt(),
                anyInt(), eq(ViewCompat.TYPE_NON_TOUCH), any(int[].class));
        verify(mParent, atLeastOnce()).onStopNestedScroll(mRecyclerView,
                ViewCompat.TYPE_NON_TOUCH);

        // Verify all of the following TYPE_TOUCH nested scrolling methods are not called.
        verify(mParent, never()).onStartNestedScroll(any(View.class), any(View.class),
                anyInt(), eq(ViewCompat.TYPE_TOUCH));
        verify(mParent, never()).onNestedScrollAccepted(any(View.class), any(View.class),
                anyInt(), eq(ViewCompat.TYPE_TOUCH));
        verify(mParent, never()).onNestedPreScroll(any(View.class), anyInt(), anyInt(),
                any(int[].class), eq(ViewCompat.TYPE_TOUCH));
        verify(mParent, never()).onNestedScroll(any(View.class),  anyInt(), anyInt(), anyInt(),
                anyInt(), eq(ViewCompat.TYPE_TOUCH), any(int[].class));
        verify(mParent, never()).onNestedPreFling(any(View.class),  anyFloat(), anyFloat());
        verify(mParent, never()).onNestedFling(any(View.class),  anyFloat(), anyFloat(),
                anyBoolean());
        verify(mParent, never()).onStopNestedScroll(any(View.class), eq(ViewCompat.TYPE_TOUCH));
    }

    private void setup() {
        Context context = mActivityTestRule.getActivity();

        mRecyclerView = new RecyclerView(context);
        mRecyclerView.setLayoutParams(new ViewGroup.LayoutParams(WIDTH, NSV_HEIGHT));
        mRecyclerView.setBackgroundColor(0xFF0000FF);
        mRecyclerView.setLayoutManager(new LinearLayoutManager(context));
        mRecyclerView.setAdapter(new TestAdapter(context, CHILD_HEIGHT, 100, true));

        mParent = spy(new NestedScrollingSpyView(context));
        mParent.setLayoutParams(new ViewGroup.LayoutParams(WIDTH, PARENT_HEIGHT));
        mParent.setBackgroundColor(0xFF0000FF);
        mParent.addView(mRecyclerView);
    }

    private void attachToActivity() throws Throwable {
        final TestContentView testContentView = mActivityTestRule.getActivity().getContentView();
        testContentView.expectLayouts(1);
        mActivityTestRule.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                testContentView.addView(mParent);
            }
        });
        testContentView.awaitLayouts(2);
    }

    public class NestedScrollingSpyView extends FrameLayout implements NestedScrollingChild3,
            NestedScrollingParent3 {

        public NestedScrollingSpyView(Context context) {
            super(context);
        }

        @Override
        public boolean onStartNestedScroll(@NonNull View child, @NonNull View target, int axes,
                int type) {
            return false;
        }

        @Override
        public void onNestedScrollAccepted(@NonNull View child, @NonNull View target, int axes,
                int type) {

        }

        @Override
        public void onStopNestedScroll(@NonNull View target, int type) {

        }

        @Override
        public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
                int dxUnconsumed, int dyUnconsumed, int type) {

        }

        @Override
        public void onNestedPreScroll(@NonNull View target, int dx, int dy, @NonNull int[] consumed,
                int type) {

        }

        @Override
        public boolean startNestedScroll(int axes, int type) {
            return false;
        }

        @Override
        public void stopNestedScroll(int type) {

        }

        @Override
        public boolean hasNestedScrollingParent(int type) {
            return false;
        }

        @Override
        public boolean dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                int dyUnconsumed, @Nullable int[] offsetInWindow, int type) {
            return false;
        }

        @Override
        public boolean dispatchNestedPreScroll(int dx, int dy, @Nullable int[] consumed,
                @Nullable int[] offsetInWindow, int type) {
            return false;
        }

        @Override
        public void onNestedScroll(@NonNull View target, int dxConsumed, int dyConsumed,
                int dxUnconsumed, int dyUnconsumed, int type, @Nullable int[] consumed) {
        }

        @Override
        public void dispatchNestedScroll(int dxConsumed, int dyConsumed, int dxUnconsumed,
                int dyUnconsumed, @Nullable int[] offsetInWindow, int type,
                @Nullable int[] consumed) {
        }
    }

    private class TestAdapter extends RecyclerView.Adapter<TestViewHolder> {

        private Context mContext;
        private int mOrientationSize;
        private int mItemCount;
        private boolean mVertical;

        TestAdapter(Context context, int orientationSize, int itemCount, boolean vertical) {
            mContext = context;
            mOrientationSize = orientationSize / itemCount;
            mItemCount = itemCount;
            mVertical = vertical;
        }

        @Override
        public TestViewHolder onCreateViewHolder(ViewGroup parent,
                int viewType) {
            View view = new View(mContext);

            int width;
            int height;
            if (mVertical) {
                width = ViewGroup.LayoutParams.MATCH_PARENT;
                height = mOrientationSize;
            } else {
                width = mOrientationSize;
                height = ViewGroup.LayoutParams.MATCH_PARENT;
            }

            view.setLayoutParams(new ViewGroup.LayoutParams(width, height));
            view.setMinimumHeight(mOrientationSize);
            return new TestViewHolder(view);
        }

        @Override
        public void onBindViewHolder(TestViewHolder holder, int position) {

        }

        @Override
        public int getItemCount() {
            return mItemCount;
        }
    }

    private class TestViewHolder extends RecyclerView.ViewHolder {

        TestViewHolder(View itemView) {
            super(itemView);
        }
    }

    private static class NestedScrollViewTestUtils {

        static int[] getTargetFlingVelocityTimeAndDistance(Context context) {
            ViewConfiguration configuration =
                    ViewConfiguration.get(context);
            int touchSlop = configuration.getScaledTouchSlop();
            int mMinimumVelocity = configuration.getScaledMinimumFlingVelocity();
            int mMaximumVelocity = configuration.getScaledMaximumFlingVelocity();

            int targetVelocitySeconds = ((mMaximumVelocity - mMinimumVelocity) / 2)
                    + mMinimumVelocity;
            int targetDistanceTraveled = touchSlop * 2;
            int targetTimePassed = (targetDistanceTraveled * 1000) / targetVelocitySeconds;

            return new int[]{targetVelocitySeconds, targetTimePassed, targetDistanceTraveled};
        }

        static MotionEvent[] generateMotionEvents(int[] targetFlingVelocityTimeAndDistance) {
            int targetTimePassed = targetFlingVelocityTimeAndDistance[1];
            int targetDistanceTraveled = targetFlingVelocityTimeAndDistance[2];
            targetDistanceTraveled *= -1;

            MotionEvent down = MotionEvent.obtain(
                    0,
                    0,
                    MotionEvent.ACTION_DOWN,
                    500,
                    500,
                    0);
            MotionEvent move = MotionEvent.obtain(
                    0,
                    targetTimePassed,
                    MotionEvent.ACTION_MOVE,
                    500,
                    500 + targetDistanceTraveled,
                    0);
            MotionEvent up = MotionEvent.obtain(
                    0,
                    targetTimePassed,
                    MotionEvent.ACTION_UP,
                    500,
                    500 + targetDistanceTraveled,
                    0);

            return new MotionEvent[]{down, move, up};
        }

        static void dispatchMotionEventsToView(View view, MotionEvent[] motionEvents) {
            for (MotionEvent motionEvent : motionEvents) {
                view.dispatchTouchEvent(motionEvent);
            }
        }

        static void simulateFlingDown(Context context, View view) {
            int[] targetFlingTimeAndDistance =
                    NestedScrollViewTestUtils.getTargetFlingVelocityTimeAndDistance(context);
            MotionEvent[] motionEvents =
                    NestedScrollViewTestUtils.generateMotionEvents(targetFlingTimeAndDistance);
            NestedScrollViewTestUtils.dispatchMotionEventsToView(view, motionEvents);
        }
    }
}
