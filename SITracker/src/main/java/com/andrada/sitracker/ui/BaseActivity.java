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

import android.app.Fragment;
import android.app.FragmentManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

import com.andrada.sitracker.BuildConfig;
import com.andrada.sitracker.R;
import com.andrada.sitracker.ui.debug.DebugActionRunnerActivity;
import com.andrada.sitracker.ui.fragment.AuthorsFragment;
import com.andrada.sitracker.ui.fragment.AuthorsFragment_;
import com.andrada.sitracker.ui.fragment.ExploreAuthorsFragment;
import com.andrada.sitracker.ui.fragment.ExploreAuthorsFragment_;
import com.andrada.sitracker.ui.widget.DrawShadowFrameLayout;
import com.andrada.sitracker.util.ActionBarUtil;
import com.andrada.sitracker.util.ActivityFragmentNavigator;
import com.andrada.sitracker.util.NavDrawerManager;
import com.andrada.sitracker.util.PlayServicesUtils;
import com.andrada.sitracker.util.UIUtils;
import com.google.android.gms.analytics.GoogleAnalytics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static com.andrada.sitracker.util.LogUtils.makeLogTag;

/**
 * A base activity that handles common functionality in the app.
 */
public abstract class BaseActivity extends ActionBarActivity implements
        ActionBarUtil.ActionBarShowHideListener,
        NavDrawerManager.NavDrawerListener/*,
        NavDrawerManager.NavDrawerItemAware*/ {

    private static final String TAG = makeLogTag(BaseActivity.class);

    // fade in and fade out durations for the main content when switching between
    // different Activities of the app through the Nav Drawer
    private static final int MAIN_CONTENT_FADEIN_DURATION = 250;
    protected NavDrawerManager.NavDrawerItemAware mCurrentNavigationElement;
    private ActionBarUtil mABUtil;
    private NavDrawerManager mDrawerManager;
    // Navigation drawer:
    private DrawerLayout mDrawerLayout;
    // Primary toolbar and drawer toggle
    private Toolbar mActionBarToolbar;

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

        getFragmentManager().addOnBackStackChangedListener(new FragmentManager.OnBackStackChangedListener() {
            @Override
            public void onBackStackChanged() {
                Fragment frag = getFragmentManager().findFragmentById(R.id.fragment_holder);
                if (frag instanceof NavDrawerManager.NavDrawerItemAware && mDrawerManager != null) {
                    mCurrentNavigationElement = (NavDrawerManager.NavDrawerItemAware) frag;
                    setContentTopClearance();
                    mDrawerManager.setSelectedNavDrawerItem(mCurrentNavigationElement.getSelfNavDrawerItem());
                }
            }
        });
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
        //Show the action bar back
        if (mABUtil != null) {
            mABUtil.autoShowOrHideActionBar(true);
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
        mDrawShadowFrameLayout = (DrawShadowFrameLayout) findViewById(R.id.main_content);
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
        if (mCurrentNavigationElement != null) {
            return mCurrentNavigationElement.getSelfNavDrawerItem();
        }
        return NavDrawerManager.NAVDRAWER_ITEM_INVALID;
    }

    public void setContentTopClearance() {
    }

    protected void setContentTopClearance(int top) {
        if (mDrawShadowFrameLayout != null) {
            mDrawShadowFrameLayout.setShadowTopOffset(top);
        }
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
        mABUtil.autoShowOrHideActionBar(true);
        switch (item) {
            case NavDrawerManager.NAVDRAWER_ITEM_MY_AUTHORS:
                AuthorsFragment authFrag = AuthorsFragment_.builder().build();
                ActivityFragmentNavigator.switchMainFragmentInMainActivity(this, authFrag);
                mCurrentNavigationElement = authFrag;
                break;
            case NavDrawerManager.NAVDRAWER_ITEM_EXPLORE:
                ExploreAuthorsFragment exploreFrag = ExploreAuthorsFragment_.builder().build();
                ActivityFragmentNavigator.switchMainFragmentInMainActivity(this, exploreFrag);
                mCurrentNavigationElement = exploreFrag;
                break;
            case NavDrawerManager.NAVDRAWER_ITEM_NEW_PUBS:
                //TODO Switch fragment with com.andrada.sitracker.util.ActivityFragmentNavigator
                break;
            case NavDrawerManager.NAVDRAWER_ITEM_SETTINGS:
                SettingsActivity_.intent(this).start();
                break;
        }
    }

    public void setProgressBarTopWhenActionBarShown(int progressBarTopWhenActionBarShown) {
        if (mDrawShadowFrameLayout != null) {
            mDrawShadowFrameLayout.setShadowTopOffset(progressBarTopWhenActionBarShown);
        }
        if (mCurrentNavigationElement != null) {
            mCurrentNavigationElement.updateSwipeRefreshProgressBarTop();
        }

    }

    @Override
    public void actionBarVisibilityChanged(boolean shown) {
        mDrawerManager.adjustStatusBarBasedOnActionBarVisibility(shown);
        mDrawShadowFrameLayout.setShadowVisible(shown, shown);
        if (mCurrentNavigationElement != null) {
            mCurrentNavigationElement.updateSwipeRefreshProgressBarTop();
        }
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

    public NavDrawerManager getDrawerManager() {
        return mDrawerManager;
    }

    @Override
    public void onBackPressed() {
        if (mDrawerManager.isNavDrawerOpen()) {
            mDrawerManager.closeNavDrawer();
        } else {
            mDrawerManager.popNavigationState();
            //As we are using SupportActivity and native FragmentManager -
            //we need to query it instead of default ActionBarActivity implementation
            if (!getFragmentManager().popBackStackImmediate()) {
                super.onBackPressed();
            }
        }
    }

}
