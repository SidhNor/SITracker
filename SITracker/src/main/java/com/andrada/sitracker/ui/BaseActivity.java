/*
 * Copyright 2014 Gleb Godonoga.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andrada.sitracker.ui;


import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Rect;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.DecelerateInterpolator;
import android.widget.AbsListView;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.andrada.sitracker.BuildConfig;
import com.andrada.sitracker.Constants;
import com.andrada.sitracker.R;
import com.andrada.sitracker.ui.debug.DebugActionRunnerActivity;
import com.andrada.sitracker.ui.fragment.AboutDialog;
import com.andrada.sitracker.ui.widget.ScrimInsetsScrollView;
import com.andrada.sitracker.util.AnalyticsHelper;
import com.andrada.sitracker.util.LUtils;
import com.andrada.sitracker.util.PlayServicesUtils;
import com.andrada.sitracker.util.UIUtils;
import com.google.android.gms.analytics.GoogleAnalytics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;

import static com.andrada.sitracker.util.LogUtils.LOGW;
import static com.andrada.sitracker.util.LogUtils.makeLogTag;

/**
 * A base activity that handles common functionality in the app.
 */
public abstract class BaseActivity extends ActionBarActivity {

    // symbols for navdrawer items (indices must correspond to array below). This is
    // not a list of items that are necessarily *present* in the Nav Drawer; rather,
    // it's a list of all possible items.
    protected static final int NAVDRAWER_ITEM_MY_AUTHORS = 0;
    protected static final int NAVDRAWER_ITEM_EXPLORE = 1;
    protected static final int NAVDRAWER_ITEM_NEW_PUBS = 2;
    protected static final int NAVDRAWER_ITEM_SETTINGS = 3;
    protected static final int NAVDRAWER_ITEM_INVALID = -1;
    protected static final int NAVDRAWER_ITEM_SEPARATOR = -2;
    protected static final int NAVDRAWER_ITEM_SEPARATOR_SPECIAL = -3;
    private static final String TAG = makeLogTag(BaseActivity.class);
    private static final int HEADER_HIDE_ANIM_DURATION = 300;
    // titles for navdrawer items (indices must correspond to the above)
    private static final int[] NAVDRAWER_TITLE_RES_ID = new int[]{
            R.string.navdrawer_item_my_authors,
            R.string.navdrawer_item_explore,
            R.string.navdrawer_item_new_pubs,
            R.string.navdrawer_item_settings
    };
    // icons for navdrawer items (indices must correspond to above array)
    private static final int[] NAVDRAWER_ICON_RES_ID = new int[]{
            R.drawable.ic_drawer_my_authors,  // My Authors
            R.drawable.ic_drawer_explore,  // Explore
            R.drawable.ic_drawer_new_pubs, // Map
            R.drawable.ic_drawer_settings
    };
    // delay to launch nav drawer item, to allow close animation to play
    private static final int NAVDRAWER_LAUNCH_DELAY = 250;
    // fade in and fade out durations for the main content when switching between
    // different Activities of the app through the Nav Drawer
    private static final int MAIN_CONTENT_FADEOUT_DURATION = 150;
    private static final int MAIN_CONTENT_FADEIN_DURATION = 250;
    private static final TypeEvaluator ARGB_EVALUATOR = new ArgbEvaluator();
    // Helper methods for L APIs
    private LUtils mLUtils;
    private Handler mHandler;
    // When set, these components will be shown/hidden in sync with the action bar
    // to implement the "quick recall" effect (the Action Bar and the header views disappear
    // when you scroll down a list, and reappear quickly when you scroll up).
    private ArrayList<View> mHideableHeaderViews = new ArrayList<View>();
    private ObjectAnimator mStatusBarColorAnimator;
    // variables that control the Action Bar auto hide behavior (aka "quick recall")
    private boolean mActionBarAutoHideEnabled = false;
    private int mActionBarAutoHideSensivity = 0;
    private int mActionBarAutoHideMinY = 0;
    private int mActionBarAutoHideSignal = 0;
    private boolean mActionBarShown = true;
    // list of navdrawer items that were actually added to the navdrawer, in order
    private ArrayList<Integer> mNavDrawerItems = new ArrayList<Integer>();
    // views that correspond to each navdrawer item, null if not yet created
    private View[] mNavDrawerItemViews = null;
    // Navigation drawer:
    private DrawerLayout mDrawerLayout;
    private ViewGroup mDrawerItemsListContainer;
    // Primary toolbar and drawer toggle
    private Toolbar mActionBarToolbar;
    // A Runnable that we should execute when the navigation drawer finishes its closing animation
    private Runnable mDeferredOnDrawerClosedRunnable;
    private int mThemedStatusBarColor;
    private int mNormalStatusBarColor;
    private int mProgressBarTopWhenActionBarShown;

    /**
     * Converts an intent into a {@link Bundle} suitable for use as fragment arguments.
     */
    @NotNull
    public static Bundle intentToFragmentArguments(@Nullable Intent intent) {
        Bundle arguments = new Bundle();
        if (intent == null) {
            return arguments;
        }

        final Uri data = intent.getData();
        if (data != null) {
            arguments.putParcelable("_uri", data);
        }

        final Bundle extras = intent.getExtras();
        if (extras != null) {
            arguments.putAll(extras);
        }

        return arguments;
    }

    /**
     * Converts a fragment arguments bundle into an intent.
     */
    @NotNull
    public static Intent fragmentArgumentsToIntent(@Nullable Bundle arguments) {
        Intent intent = new Intent();
        if (arguments == null) {
            return intent;
        }

        final Uri data = arguments.getParcelable("_uri");
        if (data != null) {
            intent.setData(data);
        }

        intent.putExtras(arguments);
        intent.removeExtra("_uri");
        return intent;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mHandler = new Handler();

        // Enable or disable each Activity depending on the form factor. This is necessary
        // because this app uses many implicit intents where we don't name the exact Activity
        // in the Intent, so there should only be one enabled Activity that handles each
        // Intent in the app.
        UIUtils.enableDisableActivitiesByFormFactor(this);


        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }

        mLUtils = LUtils.getInstance(this);
        mThemedStatusBarColor = getResources().getColor(R.color.theme_primary_dark);
        mNormalStatusBarColor = mThemedStatusBarColor;

    }

    @Override
    public void onStart() {
        super.onStart();
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!BuildConfig.DEBUG) {
            // Verifies the proper version of Google Play Services exists on the device.
            PlayServicesUtils.checkGooglePlaySevices(this);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        GoogleAnalytics.getInstance(this).reportActivityStart(this);
    }

    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        setupNavDrawer();

        //trySetupSwipeRefresh();
        //updateSwipeRefreshProgressBarTop();

        View mainContent = findViewById(R.id.fragment_container);
        if (mainContent != null) {
            mainContent.setAlpha(0);
            mainContent.animate().alpha(1).setDuration(MAIN_CONTENT_FADEIN_DURATION);
        } else {
            LOGW(TAG, "No view with ID main_content to fade in.");
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case R.id.action_about:
                AnalyticsHelper.getInstance().sendView(Constants.GA_SCREEN_ABOUT_DIALOG);
                FragmentManager fm = this.getFragmentManager();
                FragmentTransaction ft = fm.beginTransaction();
                Fragment prev = fm.findFragmentByTag(AboutDialog.FRAGMENT_TAG);
                if (prev != null) {
                    ft.remove(prev);
                }
                ft.addToBackStack(null);
                new AboutDialog().show(ft, AboutDialog.FRAGMENT_TAG);
                return true;
            case R.id.menu_debug:
                if (BuildConfig.DEBUG) {
                    startActivity(new Intent(this, DebugActionRunnerActivity.class));
                }
                return true;
        }
        //Handle default options
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        boolean result = super.onCreateOptionsMenu(menu);
        MenuItem debugItem = menu.findItem(R.id.menu_debug);
        if (debugItem != null) {
            debugItem.setVisible(BuildConfig.DEBUG);
        }
        return result;
    }

    protected void registerHideableHeaderView(View hideableHeaderView) {
        if (!mHideableHeaderViews.contains(hideableHeaderView)) {
            mHideableHeaderViews.add(hideableHeaderView);
        }
    }

    protected void deregisterHideableHeaderView(View hideableHeaderView) {
        if (mHideableHeaderViews.contains(hideableHeaderView)) {
            mHideableHeaderViews.remove(hideableHeaderView);
        }
    }

    /**
     * Returns the navigation drawer item that corresponds to this Activity. Subclasses
     * of BaseActivity override this to indicate what nav drawer item corresponds to them
     * Return NAVDRAWER_ITEM_INVALID to mean that this Activity should not have a Nav Drawer.
     */
    protected int getSelfNavDrawerItem() {
        return NAVDRAWER_ITEM_INVALID;
    }

    private void setupNavDrawer() {
        // What nav drawer item should be selected?
        int selfItem = getSelfNavDrawerItem();

        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (mDrawerLayout == null) {
            return;
        }
        mDrawerLayout.setStatusBarBackground(R.color.theme_primary_dark);
        ScrimInsetsScrollView navDrawer = (ScrimInsetsScrollView)
                mDrawerLayout.findViewById(R.id.navdrawer);
        if (selfItem == NAVDRAWER_ITEM_INVALID) {
            // do not show a nav drawer
            if (navDrawer != null) {
                ((ViewGroup) navDrawer.getParent()).removeView(navDrawer);
            }
            mDrawerLayout = null;
            return;
        }

        if (navDrawer != null) {
            navDrawer.setOnInsetsCallback(new ScrimInsetsScrollView.OnInsetsCallback() {
                @Override
                public void onInsetsChanged(Rect insets) {

                }
            });
        }

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(this, mDrawerLayout,
                mActionBarToolbar, R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                // run deferred action, if we have one
                if (mDeferredOnDrawerClosedRunnable != null) {
                    mDeferredOnDrawerClosedRunnable.run();
                    mDeferredOnDrawerClosedRunnable = null;
                }
                onNavDrawerStateChanged(false, false);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                onNavDrawerStateChanged(true, false);
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                super.onDrawerStateChanged(newState);
                onNavDrawerStateChanged(isNavDrawerOpen(), newState != DrawerLayout.STATE_IDLE);
            }

            @Override
            public void onDrawerSlide(View drawerView, float slideOffset) {
                super.onDrawerSlide(drawerView, slideOffset);
                onNavDrawerSlide(slideOffset);
            }
        };

        mDrawerLayout.setDrawerListener(toggle);
        toggle.syncState();

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, Gravity.START);

        // populate the nav drawer with the correct items
        populateNavDrawer();
        /*
        // When the user runs the app for the first time, we want to land them with the
        // navigation drawer open. But just the first time.
        if (!PrefUtils.isWelcomeDone(this)) {
            // first run of the app starts with the nav drawer open
            PrefUtils.markWelcomeDone(this);
            mDrawerLayout.openDrawer(Gravity.START);
        }*/
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        getActionBarToolbar();
    }

    // Subclasses can override this for custom behavior
    protected void onNavDrawerStateChanged(boolean isOpen, boolean isAnimating) {
        if (mActionBarAutoHideEnabled && isOpen) {
            autoShowOrHideActionBar(true);
        }
    }

    protected void onNavDrawerSlide(float offset) {
    }

    protected boolean isNavDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(Gravity.START);
    }

    /**
     * Populates the navigation drawer with the appropriate items.
     */
    private void populateNavDrawer() {
        mNavDrawerItems.clear();

        // decide which items will appear in the nav drawer

        // My authors always shown
        mNavDrawerItems.add(NAVDRAWER_ITEM_MY_AUTHORS);
        // Explore is always shown
        mNavDrawerItems.add(NAVDRAWER_ITEM_EXPLORE);

        mNavDrawerItems.add(NAVDRAWER_ITEM_NEW_PUBS);

        // Other items that are always in the nav drawer
        mNavDrawerItems.add(NAVDRAWER_ITEM_SEPARATOR_SPECIAL);
        mNavDrawerItems.add(NAVDRAWER_ITEM_SETTINGS);

        createNavDrawerItems();
    }

    private void createNavDrawerItems() {
        mDrawerItemsListContainer = (ViewGroup) findViewById(R.id.navdrawer_items_list);
        if (mDrawerItemsListContainer == null) {
            return;
        }

        mNavDrawerItemViews = new View[mNavDrawerItems.size()];
        mDrawerItemsListContainer.removeAllViews();
        int i = 0;
        for (int itemId : mNavDrawerItems) {
            mNavDrawerItemViews[i] = makeNavDrawerItem(itemId, mDrawerItemsListContainer);
            mDrawerItemsListContainer.addView(mNavDrawerItemViews[i]);
            ++i;
        }
    }

    /**
     * Sets up the given navdrawer item's appearance to the selected state. Note: this could
     * also be accomplished (perhaps more cleanly) with state-based layouts.
     */
    private void setSelectedNavDrawerItem(int itemId) {
        if (mNavDrawerItemViews != null) {
            for (int i = 0; i < mNavDrawerItemViews.length; i++) {
                if (i < mNavDrawerItems.size()) {
                    int thisItemId = mNavDrawerItems.get(i);
                    formatNavDrawerItem(mNavDrawerItemViews[i], thisItemId, itemId == thisItemId);
                }
            }
        }
    }

    private View makeNavDrawerItem(final int itemId, ViewGroup container) {
        boolean selected = getSelfNavDrawerItem() == itemId;
        int layoutToInflate = 0;
        if (itemId == NAVDRAWER_ITEM_SEPARATOR) {
            layoutToInflate = R.layout.navdrawer_separator;
        } else if (itemId == NAVDRAWER_ITEM_SEPARATOR_SPECIAL) {
            layoutToInflate = R.layout.navdrawer_separator;
        } else {
            layoutToInflate = R.layout.navdrawer_item;
        }
        View view = getLayoutInflater().inflate(layoutToInflate, container, false);

        if (isSeparator(itemId)) {
            // we are done
            return view;
        }

        ImageView iconView = (ImageView) view.findViewById(R.id.icon);
        TextView titleView = (TextView) view.findViewById(R.id.title);
        int iconId = itemId >= 0 && itemId < NAVDRAWER_ICON_RES_ID.length ?
                NAVDRAWER_ICON_RES_ID[itemId] : 0;
        int titleId = itemId >= 0 && itemId < NAVDRAWER_TITLE_RES_ID.length ?
                NAVDRAWER_TITLE_RES_ID[itemId] : 0;

        // set icon and text
        iconView.setVisibility(iconId > 0 ? View.VISIBLE : View.GONE);
        if (iconId > 0) {
            iconView.setImageResource(iconId);
        }
        titleView.setText(getString(titleId));

        formatNavDrawerItem(view, itemId, selected);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNavDrawerItemClicked(itemId);
            }
        });

        return view;
    }

    private void onNavDrawerItemClicked(final int itemId) {
        if (itemId == getSelfNavDrawerItem()) {
            mDrawerLayout.closeDrawer(Gravity.START);
            return;
        }

        if (isSpecialItem(itemId)) {
            goToNavDrawerItem(itemId);
        } else {
            // launch the target Activity after a short delay, to allow the close animation to play
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    goToNavDrawerItem(itemId);
                }
            }, NAVDRAWER_LAUNCH_DELAY);

            // change the active item on the list so the user can see the item changed
            setSelectedNavDrawerItem(itemId);
            // fade out the main content
            View mainContent = findViewById(R.id.main_content);
            if (mainContent != null) {
                mainContent.animate().alpha(0).setDuration(MAIN_CONTENT_FADEOUT_DURATION);
            }
        }

        mDrawerLayout.closeDrawer(Gravity.START);
    }

    private void goToNavDrawerItem(int item) {
        Intent intent;
        switch (item) {
            case NAVDRAWER_ITEM_MY_AUTHORS:
                intent = new Intent(this, UIUtils.getMyAuthorsActivityClass(this));
                startActivity(intent);
                finish();
                break;
            case NAVDRAWER_ITEM_EXPLORE:
                intent = new Intent(this, ExploreAuthorsActivity_.class);
                startActivity(intent);
                finish();
                break;
            case NAVDRAWER_ITEM_NEW_PUBS:
                intent = new Intent(this, NewPublicationsActivity_.class);
                startActivity(intent);
                finish();
                break;
            case NAVDRAWER_ITEM_SETTINGS:
                intent = new Intent(this, SettingsActivity_.class);
                startActivity(intent);
                break;
        }
    }

    private boolean isSpecialItem(int itemId) {
        return itemId == NAVDRAWER_ITEM_SETTINGS;
    }

    private boolean isSeparator(int itemId) {
        return itemId == NAVDRAWER_ITEM_SEPARATOR || itemId == NAVDRAWER_ITEM_SEPARATOR_SPECIAL;
    }

    private void formatNavDrawerItem(View view, int itemId, boolean selected) {
        if (isSeparator(itemId)) {
            // not applicable
            return;
        }

        ImageView iconView = (ImageView) view.findViewById(R.id.icon);
        TextView titleView = (TextView) view.findViewById(R.id.title);

        // configure its appearance according to whether or not it's selected
        titleView.setTextColor(selected ?
                getResources().getColor(R.color.navdrawer_text_color_selected) :
                getResources().getColor(R.color.navdrawer_text_color));
        iconView.setColorFilter(selected ?
                getResources().getColor(R.color.navdrawer_icon_tint_selected) :
                getResources().getColor(R.color.navdrawer_icon_tint));
    }

    /**
     * Initializes the Action Bar auto-hide (aka Quick Recall) effect.
     */
    private void initActionBarAutoHide() {
        mActionBarAutoHideEnabled = true;
        mActionBarAutoHideMinY = getResources().getDimensionPixelSize(
                R.dimen.action_bar_auto_hide_min_y);
        mActionBarAutoHideSensivity = getResources().getDimensionPixelSize(
                R.dimen.action_bar_auto_hide_sensivity);
    }

    /**
     * Indicates that the main content has scrolled (for the purposes of showing/hiding
     * the action bar for the "action bar auto hide" effect). currentY and deltaY may be exact
     * (if the underlying view supports it) or may be approximate indications:
     * deltaY may be INT_MAX to mean "scrolled forward indeterminately" and INT_MIN to mean
     * "scrolled backward indeterminately".  currentY may be 0 to mean "somewhere close to the
     * start of the list" and INT_MAX to mean "we don't know, but not at the start of the list"
     */
    private void onMainContentScrolled(int currentY, int deltaY) {
        if (deltaY > mActionBarAutoHideSensivity) {
            deltaY = mActionBarAutoHideSensivity;
        } else if (deltaY < -mActionBarAutoHideSensivity) {
            deltaY = -mActionBarAutoHideSensivity;
        }

        if (Math.signum(deltaY) * Math.signum(mActionBarAutoHideSignal) < 0) {
            // deltaY is a motion opposite to the accumulated signal, so reset signal
            mActionBarAutoHideSignal = deltaY;
        } else {
            // add to accumulated signal
            mActionBarAutoHideSignal += deltaY;
        }

        boolean shouldShow = currentY < mActionBarAutoHideMinY ||
                (mActionBarAutoHideSignal <= -mActionBarAutoHideSensivity);
        autoShowOrHideActionBar(shouldShow);
    }


    protected Toolbar getActionBarToolbar() {
        if (mActionBarToolbar == null) {
            mActionBarToolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
            if (mActionBarToolbar != null) {
                setSupportActionBar(mActionBarToolbar);
            }
        }
        return mActionBarToolbar;
    }

    protected void autoShowOrHideActionBar(boolean show) {
        if (show == mActionBarShown) {
            return;
        }
        mActionBarShown = show;
        onActionBarAutoShowOrHide(show);
    }

    protected void enableActionBarAutoHide(final ListView listView) {
        initActionBarAutoHide();
        listView.setOnScrollListener(new AbsListView.OnScrollListener() {
            final static int ITEMS_THRESHOLD = 3;
            int lastFvi = 0;

            @Override
            public void onScrollStateChanged(AbsListView view, int scrollState) {
            }

            @Override
            public void onScroll(AbsListView view, int firstVisibleItem, int visibleItemCount, int totalItemCount) {
                onMainContentScrolled(firstVisibleItem <= ITEMS_THRESHOLD ? 0 : Integer.MAX_VALUE,
                        lastFvi - firstVisibleItem > 0 ? Integer.MIN_VALUE :
                                lastFvi == firstVisibleItem ? 0 : Integer.MAX_VALUE
                );
                lastFvi = firstVisibleItem;
            }
        });
    }

    public LUtils getLUtils() {
        return mLUtils;
    }

    public int getThemedStatusBarColor() {
        return mThemedStatusBarColor;
    }

    public void setNormalStatusBarColor(int color) {
        mNormalStatusBarColor = color;
        if (mDrawerLayout != null) {
            mDrawerLayout.setStatusBarBackgroundColor(mNormalStatusBarColor);
        }
    }

    protected void onActionBarAutoShowOrHide(boolean shown) {
        if (mStatusBarColorAnimator != null) {
            mStatusBarColorAnimator.cancel();
        }
        mStatusBarColorAnimator = ObjectAnimator.ofInt(
                (mDrawerLayout != null) ? mDrawerLayout : mLUtils,
                (mDrawerLayout != null) ? "statusBarBackgroundColor" : "statusBarColor",
                shown ? Color.BLACK : mNormalStatusBarColor,
                shown ? mNormalStatusBarColor : Color.BLACK)
                .setDuration(250);
        if (mDrawerLayout != null) {
            mStatusBarColorAnimator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(ValueAnimator valueAnimator) {
                    ViewCompat.postInvalidateOnAnimation(mDrawerLayout);
                }
            });
        }
        mStatusBarColorAnimator.setEvaluator(ARGB_EVALUATOR);
        mStatusBarColorAnimator.start();

        //updateSwipeRefreshProgressBarTop();

        for (View view : mHideableHeaderViews) {
            if (shown) {
                view.animate()
                        .translationY(0)
                        .alpha(1)
                        .setDuration(HEADER_HIDE_ANIM_DURATION)
                        .setInterpolator(new DecelerateInterpolator());
            } else {
                view.animate()
                        .translationY(-view.getBottom())
                        .alpha(0)
                        .setDuration(HEADER_HIDE_ANIM_DURATION)
                        .setInterpolator(new DecelerateInterpolator());
            }
        }
    }

}
