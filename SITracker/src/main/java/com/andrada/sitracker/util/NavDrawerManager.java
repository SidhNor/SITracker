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

package com.andrada.sitracker.util;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.animation.TypeEvaluator;
import android.animation.ValueAnimator;
import android.graphics.Color;
import android.graphics.Rect;
import android.os.Handler;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.widget.RecyclerView;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.andrada.sitracker.R;
import com.andrada.sitracker.ui.BaseActivity;
import com.andrada.sitracker.ui.widget.ScrimInsetsScrollView;

import java.util.ArrayList;

public class NavDrawerManager {

    // symbols for navdrawer items (indices must correspond to array below). This is
    // not a list of items that are necessarily *present* in the Nav Drawer; rather,
    // it's a list of all possible items.
    public static final int NAVDRAWER_ITEM_MY_AUTHORS = 0;
    public static final int NAVDRAWER_ITEM_EXPLORE = 1;
    public static final int NAVDRAWER_ITEM_NEW_PUBS = 2;
    public static final int NAVDRAWER_ITEM_EXPORT = 3;
    public static final int NAVDRAWER_ITEM_IMPORT = 4;
    public static final int NAVDRAWER_ITEM_SETTINGS = 5;
    public static final int NAVDRAWER_ITEM_INVALID = -1;
    public static final int NAVDRAWER_ITEM_SEPARATOR = -2;
    public static final int NAVDRAWER_ITEM_SEPARATOR_SPECIAL = -3;
    // fade in and fade out durations for the main content when switching between
    // different Activities of the app through the Nav Drawer
    private static final int MAIN_CONTENT_FADEOUT_DURATION = 150;
    private static final int MAIN_CONTENT_FADEIN_DURATION = 250;
    private static final int[] NAVDRAWER_TITLE_RES_ID = new int[]{
            R.string.navdrawer_item_my_authors,
            R.string.navdrawer_item_explore,
            R.string.navdrawer_item_new_pubs,
            R.string.navdrawer_item_export,
            R.string.navdrawer_item_import,
            R.string.navdrawer_item_settings
    };
    // icons for navdrawer items (indices must correspond to above array)
    private static final int[] NAVDRAWER_ICON_RES_ID = new int[]{
            R.drawable.ic_drawer_my_authors,  // My Authors
            R.drawable.ic_drawer_explore,  // Explore
            R.drawable.ic_drawer_new_pubs, // Map
            R.drawable.ic_drawer_export, //Export
            R.drawable.ic_drawer_import, //Import
            R.drawable.ic_drawer_settings //Settings
    };
    // delay to launch nav drawer item, to allow close animation to play
    private static final int NAVDRAWER_LAUNCH_DELAY = 250;
    private static final TypeEvaluator ARGB_EVALUATOR = new ArgbEvaluator();
    BaseActivity mActivity;
    // list of navdrawer items that were actually added to the navdrawer, in order
    private ArrayList<Integer> mNavDrawerItems = new ArrayList<Integer>();
    // views that correspond to each navdrawer item, null if not yet created
    private View[] mNavDrawerItemViews = null;
    // Navigation drawer:
    private DrawerLayout mDrawerLayout;
    private Handler mHandler;

    /**
     * Status bar color related
     */
    // Helper methods for L APIs
    private LUtils mLUtils;
    private ObjectAnimator mStatusBarColorAnimator;
    private int mThemedStatusBarColor;
    private int mNormalStatusBarColor;

    public NavDrawerManager(BaseActivity activity) {
        mActivity = activity;
        mLUtils = LUtils.getInstance(activity);
        mThemedStatusBarColor = mActivity.getResources().getColor(R.color.theme_primary_dark);
        mNormalStatusBarColor = mThemedStatusBarColor;
        setupNavDrawer();
        mHandler = new Handler();
    }

    public void adjustStatusBarBasedOnActionBarVisibility(boolean shown) {
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

    /**
     * Sets up the given navdrawer item's appearance to the selected state. Note: this could
     * also be accomplished (perhaps more cleanly) with state-based layouts.
     */
    public void setSelectedNavDrawerItem(int itemId) {
        if (mNavDrawerItemViews != null) {
            for (int i = 0; i < mNavDrawerItemViews.length; i++) {
                if (i < mNavDrawerItems.size()) {
                    int thisItemId = mNavDrawerItems.get(i);
                    formatNavDrawerItem(mNavDrawerItemViews[i], thisItemId, itemId == thisItemId);
                }
            }
        }
    }


    protected void onNavDrawerSlide(float offset) {

    }

    private void setupNavDrawer() {
        // What nav drawer item should be selected?
        int selfItem = mActivity.getSelfNavDrawerItem();

        mDrawerLayout = (DrawerLayout) mActivity.findViewById(R.id.drawer_layout);
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

        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(mActivity, mDrawerLayout,
                mActivity.getActionBarToolbar(), R.string.drawer_open, R.string.drawer_close) {
            @Override
            public void onDrawerClosed(View drawerView) {
                super.onDrawerClosed(drawerView);
                mActivity.onNavDrawerStateChanged(false, false);
            }

            @Override
            public void onDrawerOpened(View drawerView) {
                super.onDrawerOpened(drawerView);
                mActivity.onNavDrawerStateChanged(true, false);
            }

            @Override
            public void onDrawerStateChanged(int newState) {
                super.onDrawerStateChanged(newState);
                mActivity.onNavDrawerStateChanged(isNavDrawerOpen(), newState != DrawerLayout.STATE_IDLE);
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

    public boolean isNavDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(Gravity.START);
    }

    public void closeNavDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(Gravity.START);
        }
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
        mNavDrawerItems.add(NAVDRAWER_ITEM_EXPORT);
        mNavDrawerItems.add(NAVDRAWER_ITEM_IMPORT);
        mNavDrawerItems.add(NAVDRAWER_ITEM_SETTINGS);

        createNavDrawerItems();
    }

    private void createNavDrawerItems() {
        ViewGroup mDrawerItemsListContainer = (ViewGroup) mActivity.findViewById(R.id.navdrawer_items_list);
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

    private View makeNavDrawerItem(final int itemId, ViewGroup container) {
        boolean selected = mActivity.getSelfNavDrawerItem() == itemId;
        int layoutToInflate = 0;
        if (itemId == NAVDRAWER_ITEM_SEPARATOR) {
            layoutToInflate = R.layout.navdrawer_separator;
        } else if (itemId == NAVDRAWER_ITEM_SEPARATOR_SPECIAL) {
            layoutToInflate = R.layout.navdrawer_separator;
        } else {
            layoutToInflate = R.layout.navdrawer_item;
        }
        View view = mActivity.getLayoutInflater().inflate(layoutToInflate, container, false);

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
        titleView.setText(mActivity.getString(titleId));

        formatNavDrawerItem(view, itemId, selected);

        view.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onNavDrawerItemClicked(itemId);
            }
        });

        return view;
    }

    private boolean isSpecialItem(int itemId) {
        return itemId == NavDrawerManager.NAVDRAWER_ITEM_SETTINGS;
    }

    private void onNavDrawerItemClicked(final int itemId) {
        if (mActivity == null) {
            return;
        }
        if (itemId == mActivity.getSelfNavDrawerItem()) {
            mDrawerLayout.closeDrawer(Gravity.START);
            return;
        }
        if (isSpecialItem(itemId)) {
            mActivity.goToNavDrawerItem(itemId);
        } else {
            // launch the target Activity after a short delay, to allow the close animation to play
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    mActivity.goToNavDrawerItem(itemId);
                }
            }, NAVDRAWER_LAUNCH_DELAY);

            // change the active item on the list so the user can see the item changed
            setSelectedNavDrawerItem(itemId);
        }
        mDrawerLayout.closeDrawer(Gravity.START);
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
                mActivity.getResources().getColor(R.color.navdrawer_text_color_selected) :
                mActivity.getResources().getColor(R.color.navdrawer_text_color));
        iconView.setColorFilter(selected ?
                mActivity.getResources().getColor(R.color.navdrawer_icon_tint_selected) :
                mActivity.getResources().getColor(R.color.navdrawer_icon_tint));
    }

    public interface NavDrawerListener {

        void goToNavDrawerItem(final int itemId);

        int getSelfNavDrawerItem();

        void onNavDrawerStateChanged(boolean isOpen, boolean isAnimating);
    }

    public interface NavDrawerItemAware {
        int getSelfNavDrawerItem();
        void setContentTopClearance(int top);
        boolean canCollectionViewScrollUp();
        RecyclerView getRecyclerView();
    }

}
