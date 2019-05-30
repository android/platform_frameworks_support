/*
 * Copyright (C) 2017 The Android Open Source Project
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

package androidx.navigation.fragment;

import android.content.Context;
import android.content.res.TypedArray;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;

import androidx.annotation.CallSuper;
import androidx.annotation.NavigationRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavController;
import androidx.navigation.NavGraph;
import androidx.navigation.NavHost;
import androidx.navigation.NavHostController;
import androidx.navigation.Navigation;
import androidx.navigation.Navigator;

/**
 * NavHostFragment provides an area within your layout for self-contained navigation to occur.
 *
 * <p>NavHostFragment is intended to be used as the content area within a layout resource
 * defining your app's chrome around it, e.g.:</p>
 *
 * <pre class="prettyprint">
 * &lt;androidx.drawerlayout.widget.DrawerLayout
 *        xmlns:android="http://schemas.android.com/apk/res/android"
 *        xmlns:app="http://schemas.android.com/apk/res-auto"
 *        android:layout_width="match_parent"
 *        android:layout_height="match_parent"&gt;
 *    &lt;fragment
 *            android:layout_width="match_parent"
 *            android:layout_height="match_parent"
 *            android:id="@+id/my_nav_host_fragment"
 *            android:name="androidx.navigation.fragment.NavHostFragment"
 *            app:navGraph="@navigation/nav_sample"
 *            app:defaultNavHost="true" /&gt;
 *    &lt;android.support.design.widget.NavigationView
 *            android:layout_width="wrap_content"
 *            android:layout_height="match_parent"
 *            android:layout_gravity="start"/&gt;
 * &lt;/androidx.drawerlayout.widget.DrawerLayout&gt;
 * </pre>
 *
 * <p>Each NavHostFragment has a {@link NavController} that defines valid navigation within
 * the navigation host. This includes the {@link NavGraph navigation graph} as well as navigation
 * state such as current location and back stack that will be saved and restored along with the
 * NavHostFragment itself.</p>
 *
 * <p>NavHostFragments register their navigation controller at the root of their view subtree
 * such that any descendant can obtain the controller instance through the {@link Navigation}
 * helper class's methods such as {@link Navigation#findNavController(View)}. View event listener
 * implementations such as {@link android.view.View.OnClickListener} within navigation destination
 * fragments can use these helpers to navigate based on user interaction without creating a tight
 * coupling to the navigation host.</p>
 */
public class NavHostFragment extends Fragment implements NavHost {
    private static final String KEY_GRAPH_ID = "android-support-nav:fragment:graphId";
    private static final String KEY_START_DESTINATION_ARGS =
            "android-support-nav:fragment:startDestinationArgs";
    private static final String KEY_NAV_CONTROLLER_STATE =
            "android-support-nav:fragment:navControllerState";
    private static final String KEY_DEFAULT_NAV_HOST = "android-support-nav:fragment:defaultHost";

    /**
     * Find a {@link NavController} given a local {@link Fragment}.
     *
     * <p>This method will locate the {@link NavController} associated with this Fragment,
     * looking first for a {@link NavHostFragment} along the given Fragment's parent chain.
     * If a {@link NavController} is not found, this method will look for one along this
     * Fragment's {@link Fragment#getView() view hierarchy} as specified by
     * {@link Navigation#findNavController(View)}.</p>
     *
     * @param fragment the locally scoped Fragment for navigation
     * @return the locally scoped {@link NavController} for navigating from this {@link Fragment}
     * @throws IllegalStateException if the given Fragment does not correspond with a
     * {@link NavHost} or is not within a NavHost.
     */
    @NonNull
    public static NavController findNavController(@NonNull Fragment fragment) {
        Fragment findFragment = fragment;
        while (findFragment != null) {
            if (findFragment instanceof NavHostFragment) {
                return ((NavHostFragment) findFragment).getNavController();
            }
            Fragment primaryNavFragment = findFragment.requireFragmentManager()
                    .getPrimaryNavigationFragment();
            if (primaryNavFragment instanceof NavHostFragment) {
                return ((NavHostFragment) primaryNavFragment).getNavController();
            }
            findFragment = findFragment.getParentFragment();
        }

        // Try looking for one associated with the view instead, if applicable
        View view = fragment.getView();
        if (view != null) {
            return Navigation.findNavController(view);
        }
        throw new IllegalStateException("Fragment " + fragment
                + " does not have a NavController set");
    }

    private NavHostController mNavController;
    private Boolean mIsPrimaryBeforeOnCreate = null;

    // State that will be saved and restored
    private int mGraphId;
    private boolean mDefaultNavHost;

    /**
     * Create a new NavHostFragment instance with an inflated {@link NavGraph} resource.
     *
     * @param graphResId resource id of the navigation graph to inflate
     * @return a new NavHostFragment instance
     */
    @NonNull
    public static NavHostFragment create(@NavigationRes int graphResId) {
        return create(graphResId, null);
    }

    /**
     * Create a new NavHostFragment instance with an inflated {@link NavGraph} resource.
     *
     * @param graphResId resource id of the navigation graph to inflate
     * @param startDestinationArgs arguments to send to the start destination of the graph
     * @return a new NavHostFragment instance
     */
    @NonNull
    public static NavHostFragment create(@NavigationRes int graphResId,
            @Nullable Bundle startDestinationArgs) {
        Bundle b = null;
        if (graphResId != 0) {
            b = new Bundle();
            b.putInt(KEY_GRAPH_ID, graphResId);
        }
        if (startDestinationArgs != null) {
            if (b == null) {
                b = new Bundle();
            }
            b.putBundle(KEY_START_DESTINATION_ARGS, startDestinationArgs);
        }

        final NavHostFragment result = new NavHostFragment();
        if (b != null) {
            result.setArguments(b);
        }
        return result;
    }

    /**
     * Returns the {@link NavController navigation controller} for this navigation host.
     * This method will return null until this host fragment's {@link #onCreate(Bundle)}
     * has been called and it has had an opportunity to restore from a previous instance state.
     *
     * @return this host's navigation controller
     * @throws IllegalStateException if called before {@link #onCreate(Bundle)}
     */
    @NonNull
    @Override
    public final NavController getNavController() {
        if (mNavController == null) {
            throw new IllegalStateException("NavController is not available before onCreate()");
        }
        return mNavController;
    }

    @CallSuper
    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        // TODO This feature should probably be a first-class feature of the Fragment system,
        // but it can stay here until we can add the necessary attr resources to
        // the fragment lib.
        if (mDefaultNavHost) {
            requireFragmentManager().beginTransaction()
                    .setPrimaryNavigationFragment(this)
                    .commit();
        }
    }

    @CallSuper
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        final Context context = requireContext();

        mNavController = new NavHostController(context);
        mNavController.setLifecycleOwner(this);
        mNavController.setOnBackPressedDispatcher(requireActivity().getOnBackPressedDispatcher());
        // Set the default state - this will be updated whenever
        // onPrimaryNavigationFragmentChanged() is called
        mNavController.enableOnBackPressed(
                mIsPrimaryBeforeOnCreate != null && mIsPrimaryBeforeOnCreate);
        mIsPrimaryBeforeOnCreate = null;
        mNavController.setViewModelStore(getViewModelStore());
        onCreateNavController(mNavController);

        Bundle navState = null;
        if (savedInstanceState != null) {
            navState = savedInstanceState.getBundle(KEY_NAV_CONTROLLER_STATE);
            if (savedInstanceState.getBoolean(KEY_DEFAULT_NAV_HOST, false)) {
                mDefaultNavHost = true;
                requireFragmentManager().beginTransaction()
                        .setPrimaryNavigationFragment(this)
                        .commit();
            }
        }

        if (navState != null) {
            // Navigation controller state overrides arguments
            mNavController.restoreState(navState);
        }
        if (mGraphId != 0) {
            // Set from onInflate()
            mNavController.setGraph(mGraphId);
        } else {
            // See if it was set by NavHostFragment.create()
            final Bundle args = getArguments();
            final int graphId = args != null ? args.getInt(KEY_GRAPH_ID) : 0;
            final Bundle startDestinationArgs = args != null
                    ? args.getBundle(KEY_START_DESTINATION_ARGS)
                    : null;
            if (graphId != 0) {
                mNavController.setGraph(graphId, startDestinationArgs);
            }
        }
    }

    /**
     * Callback for when the {@link #getNavController() NavController} is created. If you
     * support any custom destination types, their {@link Navigator} should be added here to
     * ensure it is available before the navigation graph is inflated / set.
     * <p>
     * By default, this adds a {@link FragmentNavigator}.
     * <p>
     * This is only called once in {@link #onCreate(Bundle)} and should not be called directly by
     * subclasses.
     *
     * @param navController The newly created {@link NavController}.
     */
    @SuppressWarnings({"WeakerAccess", "deprecation"})
    @CallSuper
    protected void onCreateNavController(@NonNull NavController navController) {
        navController.getNavigatorProvider().addNavigator(
                new DialogFragmentNavigator(requireContext(), getChildFragmentManager()));
        navController.getNavigatorProvider().addNavigator(createFragmentNavigator());
    }

    @CallSuper
    @Override
    public void onPrimaryNavigationFragmentChanged(boolean isPrimaryNavigationFragment) {
        if (mNavController != null) {
            mNavController.enableOnBackPressed(isPrimaryNavigationFragment);
        } else {
            mIsPrimaryBeforeOnCreate = isPrimaryNavigationFragment;
        }
    }

    /**
     * Create the FragmentNavigator that this NavHostFragment will use. By default, this uses
     * {@link FragmentNavigator}, which replaces the entire contents of the NavHostFragment.
     * <p>
     * This is only called once in {@link #onCreate(Bundle)} and should not be called directly by
     * subclasses.
     * @return a new instance of a FragmentNavigator
     * @deprecated Use {@link #onCreateNavController(NavController)}
     */
    @SuppressWarnings("DeprecatedIsStillUsed")
    @Deprecated
    @NonNull
    protected Navigator<? extends FragmentNavigator.Destination> createFragmentNavigator() {
        return new FragmentNavigator(requireContext(), getChildFragmentManager(), getId());
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        FrameLayout frameLayout = new FrameLayout(inflater.getContext());
        // When added via XML, this has no effect (since this FrameLayout is given the ID
        // automatically), but this ensures that the View exists as part of this Fragment's View
        // hierarchy in cases where the NavHostFragment is added programmatically as is required
        // for child fragment transactions
        frameLayout.setId(getId());
        return frameLayout;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        if (!(view instanceof ViewGroup)) {
            throw new IllegalStateException("created host view " + view + " is not a ViewGroup");
        }
        // When added via XML, the parent is null and our view is the root of the NavHostFragment
        // but when added programmatically, we need to set the NavController on the parent - i.e.,
        // the View that has the ID matching this NavHostFragment.
        View rootView = view.getParent() != null ? (View) view.getParent() : view;
        Navigation.setViewNavController(rootView, mNavController);
    }

    @CallSuper
    @Override
    public void onInflate(@NonNull Context context, @NonNull AttributeSet attrs,
            @Nullable Bundle savedInstanceState) {
        super.onInflate(context, attrs, savedInstanceState);

        final TypedArray a = context.obtainStyledAttributes(attrs, R.styleable.NavHostFragment);
        final int graphId = a.getResourceId(R.styleable.NavHostFragment_navGraph, 0);
        final boolean defaultHost = a.getBoolean(R.styleable.NavHostFragment_defaultNavHost, false);

        if (graphId != 0) {
            mGraphId = graphId;
        }
        if (defaultHost) {
            mDefaultNavHost = true;
        }
        a.recycle();
    }

    @CallSuper
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        Bundle navState = mNavController.saveState();
        if (navState != null) {
            outState.putBundle(KEY_NAV_CONTROLLER_STATE, navState);
        }
        if (mDefaultNavHost) {
            outState.putBoolean(KEY_DEFAULT_NAV_HOST, true);
        }
    }
}
