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

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import com.andrada.sitracker.BuildConfig;
import com.andrada.sitracker.R;
import com.andrada.sitracker.ui.debug.DebugActionRunnerActivity;
import com.andrada.sitracker.ui.widget.DrawShadowFrameLayout;
import com.andrada.sitracker.ui.widget.MultiSwipeRefreshLayout;
import com.andrada.sitracker.util.ActionBarUtil;
import com.andrada.sitracker.util.NavDrawerManager;
import com.andrada.sitracker.util.PlayServicesUtils;
import com.andrada.sitracker.util.UIUtils;
import com.google.android.gms.analytics.GoogleAnalytics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.andrada.sitracker.util.LogUtils.LOGW;
import static com.andrada.sitracker.util.LogUtils.makeLogTag;

/**
 * A base activity that handles common functionality in the app.
 */
public abstract class BaseActivity extends ActionBarActivity implements
        MultiSwipeRefreshLayout.CanChildScrollUpCallback,
        ActionBarUtil.ActionBarShowHideListener,
        NavDrawerManager.NavDrawerListener {

    private static final String TAG = makeLogTag(BaseActivity.class);

    // fade in and fade out durations for the main content when switching between
    // different Activities of the app through the Nav Drawer
    private static final int MAIN_CONTENT_FADEIN_DURATION = 250;
    private ActionBarUtil mABUtil;
    private NavDrawerManager mDrawerManager;

    // Navigation drawer:
    private DrawerLayout mDrawerLayout;
    // Primary toolbar and drawer toggle
    private Toolbar mActionBarToolbar;
    private int mProgressBarTopWhenActionBarShown;
    // SwipeRefreshLayout allows the user to swipe the screen down to trigger a manual refresh
    private SwipeRefreshLayout mSwipeRefreshLayout;
    //ShadowFrameLayout for setting toolbar shadow
    private DrawShadowFrameLayout mDrawShadowFrameLayout;

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

        // Enable or disable each Activity depending on the form factor. This is necessary
        // because this app uses many implicit intents where we don't name the exact Activity
        // in the Intent, so there should only be one enabled Activity that handles each
        // Intent in the app.
        UIUtils.enableDisableActivitiesByFormFactor(this);


        ActionBar ab = getSupportActionBar();
        if (ab != null) {
            ab.setDisplayHomeAsUpEnabled(true);
        }
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

        mABUtil = new ActionBarUtil(this, this);
        mDrawerManager = new NavDrawerManager(this);

        trySetupSwipeRefresh();
        updateSwipeRefreshProgressBarTop();

        mDrawShadowFrameLayout = (DrawShadowFrameLayout) findViewById(R.id.main_content);
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

    /**
     * Returns the navigation drawer item that corresponds to this Activity. Subclasses
     * of BaseActivity override this to indicate what nav drawer item corresponds to them
     * Return NAVDRAWER_ITEM_INVALID to mean that this Activity should not have a Nav Drawer.
     */
    @Override
    public int getSelfNavDrawerItem() {
        return NavDrawerManager.NAVDRAWER_ITEM_INVALID;
    }

    private void trySetupSwipeRefresh() {
        mSwipeRefreshLayout = (SwipeRefreshLayout) findViewById(R.id.swipe_refresh_layout);
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setColorSchemeResources(
                    R.color.refresh_progress_1,
                    R.color.refresh_progress_2,
                    R.color.refresh_progress_3);
            mSwipeRefreshLayout.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
                @Override
                public void onRefresh() {
                    requestDataRefresh();
                }
            });
            if (mSwipeRefreshLayout instanceof MultiSwipeRefreshLayout) {
                MultiSwipeRefreshLayout mswrl = (MultiSwipeRefreshLayout) mSwipeRefreshLayout;
                mswrl.setCanChildScrollUpCallback(this);
            }
        }
    }

    protected void requestDataRefresh() {
        //Stub - should be implemented in subclass
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        getActionBarToolbar();
    }

    // Subclasses can override this for custom behavior
    @Override
    public void onNavDrawerStateChanged(boolean isOpen, boolean isAnimating) {
        if (mABUtil.isActionBarAutoHideEnabled() && isOpen) {
            mABUtil.autoShowOrHideActionBar(true);
        }
    }

    @Override
    public void goToNavDrawerItem(int item) {
        /*
        //TODO this should probably go to com.andrada.sitracker.util.ActivityFragmentNavigator
        // fade out the main content
        View mainContent = mActivity.findViewById(R.id.main_content);
        if (mainContent != null) {
            mainContent.animate().alpha(0).setDuration(MAIN_CONTENT_FADEOUT_DURATION);
        }*/

        Intent intent;
        switch (item) {
            case NavDrawerManager.NAVDRAWER_ITEM_MY_AUTHORS:
                //TODO Switch fragment with com.andrada.sitracker.util.ActivityFragmentNavigator
                break;
            case NavDrawerManager.NAVDRAWER_ITEM_EXPLORE:
                //TODO Switch fragment with com.andrada.sitracker.util.ActivityFragmentNavigator
                break;
            case NavDrawerManager.NAVDRAWER_ITEM_NEW_PUBS:
                //TODO Switch fragment with com.andrada.sitracker.util.ActivityFragmentNavigator
                break;
            case NavDrawerManager.NAVDRAWER_ITEM_SETTINGS:
                intent = new Intent(this, SettingsActivity_.class);
                startActivity(intent);
                break;
        }
    }

    protected void setProgressBarTopWhenActionBarShown(int progressBarTopWhenActionBarShown) {
        mProgressBarTopWhenActionBarShown = progressBarTopWhenActionBarShown;
        if (mDrawShadowFrameLayout != null) {
            mDrawShadowFrameLayout.setShadowTopOffset(progressBarTopWhenActionBarShown);
        }
        updateSwipeRefreshProgressBarTop();
    }

    @Override
    public void actionBarVisibilityChanged(boolean shown) {
        mDrawerManager.adjustStatusBarBasedOnActionBarVisibility(shown);
        mDrawShadowFrameLayout.setShadowVisible(shown, shown);
        updateSwipeRefreshProgressBarTop();
    }

    protected void updateSwipeRefreshProgressBarTop() {
        if (mSwipeRefreshLayout == null) {
            return;
        }
        int progressBarStartMargin = getResources().getDimensionPixelSize(
                R.dimen.swipe_refresh_progress_bar_start_margin);
        int progressBarEndMargin = getResources().getDimensionPixelSize(
                R.dimen.swipe_refresh_progress_bar_end_margin);
        int top = mABUtil.isActionBarShown() ? mProgressBarTopWhenActionBarShown : 0;
        mSwipeRefreshLayout.setProgressViewOffset(false,
                top + progressBarStartMargin, top + progressBarEndMargin);
    }

    protected void onRefreshingStateChanged(boolean refreshing) {
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setRefreshing(refreshing);
        }
    }

    protected void enableDisableSwipeRefresh(boolean enable) {
        if (mSwipeRefreshLayout != null) {
            mSwipeRefreshLayout.setEnabled(enable);
        }
    }

    @Override
    public boolean canSwipeRefreshChildScrollUp() {
        return false;
    }

    public Toolbar getActionBarToolbar() {
        if (mActionBarToolbar == null) {
            mActionBarToolbar = (Toolbar) findViewById(R.id.toolbar_actionbar);
            if (mActionBarToolbar != null) {
                setSupportActionBar(mActionBarToolbar);
            }
        }
        return mActionBarToolbar;
    }

    public ActionBarUtil getActionBarUtil() {
        return mABUtil;
    }


}
