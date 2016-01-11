/*
 *
 * Copyright 2016 Gleb Godonoga.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.andrada.sitracker.util;

import android.os.Handler;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.view.View;

import com.andrada.sitracker.R;
import com.andrada.sitracker.ui.BaseActivity;

public class NavDrawerManager {

    // delay to launch nav drawer item, to allow close animation to play
    private static final int NAVDRAWER_LAUNCH_DELAY = 250;
    BaseActivity mActivity;

    // Navigation drawer:
    private DrawerLayout mDrawerLayout;
    private NavigationView mNavigationView;
    private Handler mHandler;

    private int mCurrentNavId = -1;

    public NavDrawerManager(BaseActivity activity) {
        mActivity = activity;
        setupNavDrawer();
        mHandler = new Handler();
    }

    private void setupNavDrawer() {
        // What nav drawer item should be selected?

        mDrawerLayout = (DrawerLayout) mActivity.findViewById(R.id.drawer_layout);
        if (mDrawerLayout == null) {
            return;
        }

        mDrawerLayout.setStatusBarBackground(R.color.theme_primary_dark);


        mNavigationView = (NavigationView) mActivity.findViewById(R.id.navigation_view);
        if (mNavigationView == null) {
            return;
        }

        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(MenuItem item) {
                if (mActivity == null) {
                    return false;
                }
                final int itemId = item.getItemId();

                if (itemId == mCurrentNavId && !isSpecialItem(itemId)) {
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                }

                if (isSpecialItem(itemId)) {
                    mCurrentNavId = itemId;
                    mActivity.goToNavDrawerItem(itemId);
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                    return false;
                } else {
                    // launch the target Activity after a short delay, to allow the close animation to play
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mActivity.goToNavDrawerItem(itemId);
                        }
                    }, NAVDRAWER_LAUNCH_DELAY);
                    mCurrentNavId = itemId;
                    mDrawerLayout.closeDrawer(GravityCompat.START);
                    return true;
                }
            }
        });

        final Toolbar mActionBarToolbar = mActivity.getActionBarToolbar();

        if (mActionBarToolbar != null) {
            mActionBarToolbar.setNavigationIcon(R.drawable.ic_drawer);
            mActionBarToolbar.setNavigationOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    mDrawerLayout.openDrawer(GravityCompat.START);
                }
            });
        }

        mDrawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

    }

    public boolean isNavDrawerOpen() {
        return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START);
    }

    public void closeNavDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        }
    }

    public void openNavDrawer() {
        if (mDrawerLayout != null) {
            mDrawerLayout.openDrawer(GravityCompat.START);
        }
    }


    private boolean isSpecialItem(int itemId) {
        return itemId == R.id.navigation_item_settings ||
                itemId == R.id.navigation_item_export ||
                itemId == R.id.navigation_item_import ||
                itemId == R.id.navigation_item_about;
    }

    public interface NavDrawerListener {
        void goToNavDrawerItem(final int itemId);
    }

}
