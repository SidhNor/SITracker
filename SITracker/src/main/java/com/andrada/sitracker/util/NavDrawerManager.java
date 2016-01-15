/*
 * Copyright 2016 Gleb Godonoga.
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

import android.graphics.PorterDuff;
import android.os.Handler;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.widget.ImageView;

import com.andrada.sitracker.R;
import com.andrada.sitracker.ui.BaseActivity;
import com.mikepenz.materialdrawer.Drawer;
import com.mikepenz.materialdrawer.DrawerBuilder;
import com.mikepenz.materialdrawer.model.DividerDrawerItem;
import com.mikepenz.materialdrawer.model.PrimaryDrawerItem;
import com.mikepenz.materialdrawer.model.SecondaryDrawerItem;
import com.mikepenz.materialdrawer.model.interfaces.IDrawerItem;

public class NavDrawerManager {

    // delay to launch nav drawer item, to allow close animation to play
    private static final int NAVDRAWER_LAUNCH_DELAY = 250;

    // fade in and fade out durations for the main content when switching between
    // different Activities of the app through the Nav Drawer
    private static final int MAIN_CONTENT_FADEOUT_DURATION = 150;

    private static final int MAIN_CONTENT_FADEIN_DURATION = 250;

    BaseActivity mActivity;

    // Navigation drawer:
    //private DrawerLayout mDrawerLayout;
    //private NavigationView mNavigationView;
    private Handler mHandler;

    private Drawer result;

    private int mCurrentNavId = -1;

    public NavDrawerManager(BaseActivity activity) {
        mActivity = activity;
        setupNavDrawer();
        mHandler = new Handler();
    }

    private void setupNavDrawer() {
        // What nav drawer item should be selected?

        /*
        mDrawerLayout = (DrawerLayout) mActivity.findViewById(R.id.drawer_layout);
        if (mDrawerLayout == null) {
            return;
        }

        mDrawerLayout.setStatusBarBackground(R.color.theme_primary_dark);


        mNavigationView = (NavigationView) mActivity.findViewById(R.id.navigation_view);
        if (mNavigationView == null) {
            return;
        }

        ImageView drawerImage = (ImageView) mNavigationView.getHeaderView(0).findViewById(R.id.navdrawer_image);
        if (drawerImage != null) {
            drawerImage.setColorFilter(ContextCompat.getColor(mActivity, R.color.theme_primary),
                    PorterDuff.Mode.MULTIPLY);
        }

        */

        Drawer.OnDrawerItemClickListener listener = new Drawer.OnDrawerItemClickListener() {
            @Override
            public boolean onItemClick(View view, int position, IDrawerItem drawerItem) {
                if (mActivity == null) {
                    return false;
                }
                final int itemId = drawerItem.getIdentifier();
                if (itemId == mCurrentNavId && !isSpecialItem(itemId)) {
                    result.closeDrawer();
                    return true;
                }
                if (isSpecialItem(itemId)) {
                    mCurrentNavId = itemId;
                    mActivity.goToNavDrawerItem(itemId);
                    result.closeDrawer();
                    return false;
                } else {
                    // launch the target Activity/Fragment after a short delay, to allow the close animation to play
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mActivity.goToNavDrawerItem(itemId);
                        }
                    }, NAVDRAWER_LAUNCH_DELAY);
                    mCurrentNavId = itemId;
                    result.closeDrawer();
                    // fade out the main content
                    View mainContent = mActivity.findViewById(R.id.fragment_holder);
                    if (mainContent != null) {
                        mainContent.animate().alpha(0).setDuration(MAIN_CONTENT_FADEOUT_DURATION);
                    }
                    return true;
                }
            }
        };

        /*
        mNavigationView.setNavigationItemSelectedListener(new NavigationView.OnNavigationItemSelectedListener() {
            @Override
            public boolean onNavigationItemSelected(final MenuItem item) {
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
                    // launch the target Activity/Fragment after a short delay, to allow the close animation to play
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mActivity.goToNavDrawerItem(itemId);
                        }
                    }, NAVDRAWER_LAUNCH_DELAY);
                    mCurrentNavId = itemId;
                    mDrawerLayout.closeDrawers();
                    // fade out the main content
                    View mainContent = mActivity.findViewById(R.id.fragment_holder);
                    if (mainContent != null) {
                        mainContent.animate().alpha(0).setDuration(MAIN_CONTENT_FADEOUT_DURATION);
                    }
                    return true;
                }
            }
        });
        */

        final Toolbar actionBarToolbar = mActivity.getActionBarToolbar();
        result = new DrawerBuilder()
                .withActivity(mActivity)
                .withToolbar(actionBarToolbar)
                .withFullscreen(false)
                .withHeader(R.layout.navdrawer_header)
                .withActionBarDrawerToggle(true)
                .withHasStableIds(true)
                .withOnDrawerItemClickListener(listener)
                .addDrawerItems(
                        new PrimaryDrawerItem().withName(R.string.navdrawer_item_my_authors).withIcon(R.drawable.ic_drawer_my_authors).withTextColorRes(R.color.body_text_1).withIdentifier(R.id.navigation_item_my_authors).withIconTintingEnabled(true).withSelectedIconColorRes(R.color.theme_primary).withSelectedTextColorRes(R.color.theme_primary),
                        new PrimaryDrawerItem().withName(R.string.navdrawer_item_new_pubs).withIcon(R.drawable.ic_drawer_new_pubs).withTextColorRes(R.color.body_text_1).withIdentifier(R.id.navigation_item_new_pubs).withIconTintingEnabled(true).withSelectedIconColorRes(R.color.theme_primary).withSelectedTextColorRes(R.color.theme_primary),
                        new PrimaryDrawerItem().withName(R.string.navdrawer_item_export).withIcon(R.drawable.ic_drawer_export).withTextColorRes(R.color.body_text_1).withIdentifier(R.id.navigation_item_export).withIconTintingEnabled(true).withSelectable(false).withSelectedIconColorRes(R.color.theme_primary).withSelectedTextColorRes(R.color.theme_primary),
                        new PrimaryDrawerItem().withName(R.string.navdrawer_item_import).withIcon(R.drawable.ic_drawer_import).withTextColorRes(R.color.body_text_1).withIdentifier(R.id.navigation_item_import).withIconTintingEnabled(true).withSelectable(false).withSelectedIconColorRes(R.color.theme_primary).withSelectedTextColorRes(R.color.theme_primary),
                        new DividerDrawerItem().withSelectable(false),
                        new PrimaryDrawerItem().withName(R.string.navdrawer_item_settings).withIcon(R.drawable.ic_drawer_settings).withTextColorRes(R.color.body_text_1).withIdentifier(R.id.navigation_item_settings).withIconTintingEnabled(true).withSelectedIconColorRes(R.color.theme_primary).withSelectedTextColorRes(R.color.theme_primary),
                        new PrimaryDrawerItem().withName(R.string.action_about).withIcon(R.drawable.ic_drawer_info).withTextColorRes(R.color.body_text_1).withIdentifier(R.id.navigation_item_about).withIconTintingEnabled(true).withSelectedIconColorRes(R.color.theme_primary).withSelectedTextColorRes(R.color.theme_primary)
                )
                .build();

        ImageView drawerImage = (ImageView) result.getHeader().findViewById(R.id.navdrawer_image);
        if (drawerImage != null) {
            drawerImage.setColorFilter(ContextCompat.getColor(mActivity, R.color.theme_primary),
                    PorterDuff.Mode.MULTIPLY);
        }
        /*
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
        */
    }

    public void tryFadeInMainContent() {
        View mainContent = mActivity.findViewById(R.id.fragment_holder);
        if (mainContent != null) {
            mainContent.setAlpha(0);
            mainContent.animate().alpha(1).setDuration(MAIN_CONTENT_FADEIN_DURATION);
        }
    }

    public boolean isNavDrawerOpen() {
        return result != null && result.isDrawerOpen();
        //return mDrawerLayout != null && mDrawerLayout.isDrawerOpen(GravityCompat.START);
    }

    public void closeNavDrawer() {
        if (result != null) {
            result.closeDrawer();
        }
        /*if (mDrawerLayout != null) {
            mDrawerLayout.closeDrawer(GravityCompat.START);
        }*/
    }

    public void openNavDrawer() {
        if (result != null) {
            result.openDrawer();
        }
        /*if (mDrawerLayout != null) {
            mDrawerLayout.openDrawer(GravityCompat.START);
        }*/
    }


    private boolean isSpecialItem(int itemId) {
        return itemId == R.id.navigation_item_settings ||
                itemId == R.id.navigation_item_export ||
                itemId == R.id.navigation_item_import;
    }

    public interface NavDrawerListener {
        void goToNavDrawerItem(final int itemId);
    }

}
