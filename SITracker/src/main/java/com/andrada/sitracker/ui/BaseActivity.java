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

package com.andrada.sitracker.ui;

import android.app.Activity;
import android.app.Fragment;
import android.app.backup.BackupManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.support.design.widget.AppBarLayout;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NavUtils;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.ActionBar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.view.MenuItem;
import android.view.ViewGroup;

import com.andrada.sitracker.BuildConfig;
import com.andrada.sitracker.Constants;
import com.andrada.sitracker.R;
import com.andrada.sitracker.contracts.OnBackAware;
import com.andrada.sitracker.events.AuthorMarkedAsReadEvent;
import com.andrada.sitracker.events.AuthorsExported;
import com.andrada.sitracker.events.PublicationMarkedAsReadEvent;
import com.andrada.sitracker.ui.fragment.AboutFragment;
import com.andrada.sitracker.ui.fragment.AboutFragment_;
import com.andrada.sitracker.ui.fragment.AuthorsFragment;
import com.andrada.sitracker.ui.fragment.AuthorsFragment_;
import com.andrada.sitracker.ui.fragment.NewPublicationsFragment;
import com.andrada.sitracker.ui.fragment.NewPublicationsFragment_;
import com.andrada.sitracker.util.ActivityFragmentNavigator;
import com.andrada.sitracker.util.AnalyticsHelper;
import com.andrada.sitracker.util.NavDrawerManager;
import com.andrada.sitracker.util.PlayServicesUtils;
import com.andrada.sitracker.util.UIUtils;
import com.google.android.gms.analytics.GoogleAnalytics;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.Timer;
import java.util.TimerTask;

import static com.andrada.sitracker.util.LogUtils.makeLogTag;

/**
 * A base activity that handles common functionality in the app.
 */
public abstract class BaseActivity extends AppCompatActivity implements
        NavDrawerManager.NavDrawerListener {

    private static final String TAG = makeLogTag(BaseActivity.class);
    private static final long BACK_UP_DELAY = 30000L;

    private NavDrawerManager mDrawerManager;

    // Primary toolbar and drawer toggle
    private Toolbar mActionBarToolbar;

    private AppBarLayout appBarLayout;

    private ExportAuthorsController mExportCtrl;

    protected Fragment currentFragment;

    private ViewGroup cabContainer;

    private TimerTask backUpTask;
    @NotNull
    private final Timer backUpTimer = new Timer();

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

        mExportCtrl = new ExportAuthorsController(this);
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

    protected void afterViews() {
        mDrawerManager = new NavDrawerManager(this);
    }


    @Override
    protected void onPostCreate(Bundle savedInstanceState) {
        super.onPostCreate(savedInstanceState);
        /*if (mDrawerManager == null) {
            afterViews();
        }*/
        if (appBarLayout == null) {
            appBarLayout = (AppBarLayout) findViewById(R.id.appbar_layout);
        }

        if (cabContainer == null) {
            cabContainer = (ViewGroup) findViewById(R.id.si_cab_container);
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        switch (id) {
            case android.R.id.home:
                getDrawerManager().openNavDrawer();
                return true;
        }
        //Handle default options
        return super.onOptionsItemSelected(item);
    }

    @Override
    public void setContentView(int layoutResID) {
        super.setContentView(layoutResID);
        getActionBarToolbar();
    }

    public void trySetToolbarScrollable(boolean scrollable) {
        if (cabContainer != null) {
            AppBarLayout.LayoutParams params = (AppBarLayout.LayoutParams) cabContainer.getLayoutParams();
            if (scrollable) {
                params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP |
                        AppBarLayout.LayoutParams.SCROLL_FLAG_ENTER_ALWAYS |
                        AppBarLayout.LayoutParams.SCROLL_FLAG_SCROLL);
            } else {
                if (appBarLayout != null) {
                    appBarLayout.setExpanded(true, true);
                }
                params.setScrollFlags(AppBarLayout.LayoutParams.SCROLL_FLAG_SNAP);
            }

            cabContainer.setLayoutParams(params);
        }
    }


    @Override
    public void goToNavDrawerItem(int item) {
        currentFragment = null;
        switch (item) {
            case R.id.navigation_item_my_authors:
                AuthorsFragment authFrag = AuthorsFragment_.builder().build();
                if (appBarLayout != null) {
                    appBarLayout.setExpanded(true, false);
                }
                currentFragment = authFrag;
                ActivityFragmentNavigator.switchMainFragmentInMainActivity(this, authFrag);
                mDrawerManager.tryFadeInMainContent();
                break;
            case R.id.navigation_item_new_pubs:
                NewPublicationsFragment newPubsFrag = NewPublicationsFragment_.builder().build();
                if (appBarLayout != null) {
                    appBarLayout.setExpanded(true, false);
                }
                currentFragment = newPubsFrag;
                ActivityFragmentNavigator.switchMainFragmentInMainActivity(this, newPubsFrag);
                mDrawerManager.tryFadeInMainContent();
                break;
            case R.id.navigation_item_export:
                mExportCtrl.showDialog();
                break;
            case R.id.navigation_item_import:
                ImportAuthorsActivity_.intent(this).start();
                break;
            case R.id.navigation_item_settings:
                SettingsActivity.SettingsFragment settingsFragment = SettingsActivity_.SettingsFragment_.builder().build();
                if (appBarLayout != null) {
                    appBarLayout.setExpanded(true, false);
                }
                currentFragment = settingsFragment;
                ActivityFragmentNavigator.switchMainFragmentInMainActivity(this, settingsFragment);
                mDrawerManager.tryFadeInMainContent();
                //SettingsActivity_.intent(this).start();
                break;
            case R.id.navigation_item_about:
                AboutFragment aboutFrag = AboutFragment_.builder().build();
                if (appBarLayout != null) {
                    appBarLayout.setExpanded(true, false);
                }
                currentFragment = aboutFrag;
                ActivityFragmentNavigator.switchMainFragmentInMainActivity(this, aboutFrag);
                mDrawerManager.tryFadeInMainContent();
                break;
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

    public NavDrawerManager getDrawerManager() {
        return mDrawerManager;
    }

    @Override
    public void onBackPressed() {
        if (mDrawerManager != null && mDrawerManager.isNavDrawerOpen()) {
            mDrawerManager.closeNavDrawer();
            return;
        } else if (currentFragment != null && currentFragment instanceof OnBackAware) {
            boolean handled = ((OnBackAware) currentFragment).onBackPressed();
            if (handled) {
                return;
            }
        } else if (getFragmentManager().getBackStackEntryCount() > 0) {
            getFragmentManager().popBackStack();
            return;
        }
        super.onBackPressed();
    }

    public void onEvent(@NotNull AuthorsExported event) {
        String message = event.getMessage();

        SpannableStringBuilder snackbarText = new SpannableStringBuilder();
        if (message.length() == 0) {
            //This is success
            snackbarText.append(getResources().getString(R.string.author_export_success_crouton_message));
        } else {
            snackbarText.append(message);
            snackbarText.setSpan(new ForegroundColorSpan(Color.RED), 0, snackbarText.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        }

        Snackbar.make(findViewById(R.id.main_content), snackbarText, Snackbar.LENGTH_LONG).show();
    }

    public void onEvent(AuthorMarkedAsReadEvent event) {
        this.scheduleBackup();
    }

    public void onEvent(PublicationMarkedAsReadEvent event) {
        AnalyticsHelper.getInstance().sendEvent(
                Constants.GA_READ_CATEGORY,
                Constants.GA_EVENT_AUTHOR_MANUAL_READ,
                Constants.GA_EVENT_AUTHOR_MANUAL_READ);
        this.scheduleBackup();
    }

    private void scheduleBackup() {
        if (this.backUpTask != null) {
            this.backUpTask.cancel();
        }
        this.backUpTask = new TimerTask() {
            @Override
            public void run() {
                BackupManager bm = new BackupManager(getApplicationContext());
                bm.dataChanged();
            }
        };
        backUpTimer.schedule(this.backUpTask, BACK_UP_DELAY);
    }

    /**
     * This utility method handles Up navigation intents by searching for a parent activity and
     * navigating there if defined. When using this for an activity make sure to define both the
     * native parentActivity as well as the AppCompat one when supporting API levels less than 16.
     * when the activity has a single parent activity. If the activity doesn't have a single parent
     * activity then don't define one and this method will use back button functionality. If "Up"
     * functionality is still desired for activities without parents then use
     * {@code syntheticParentActivity} to define one dynamically.
     * <p/>
     * Note: Up navigation intents are represented by a back arrow in the top left of the Toolbar
     * in Material Design guidelines.
     *
     * @param currentActivity         Activity in use when navigate Up action occurred.
     * @param syntheticParentActivity Parent activity to use when one is not already configured.
     */
    public static void navigateUpOrBack(Activity currentActivity,
                                        Class<? extends Activity> syntheticParentActivity) {
        // Retrieve parent activity from AndroidManifest.
        Intent intent = NavUtils.getParentActivityIntent(currentActivity);

        // Synthesize the parent activity when a natural one doesn't exist.
        if (intent == null && syntheticParentActivity != null) {
            try {
                intent = NavUtils.getParentActivityIntent(currentActivity, syntheticParentActivity);
            } catch (PackageManager.NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        if (intent == null) {
            // No parent defined in manifest. This indicates the activity may be used by
            // in multiple flows throughout the app and doesn't have a strict parent. In
            // this case the navigation up button should act in the same manner as the
            // back button. This will result in users being forwarded back to other
            // applications if currentActivity was invoked from another application.
            currentActivity.onBackPressed();
        } else {
            if (NavUtils.shouldUpRecreateTask(currentActivity, intent)) {
                // Need to synthesize a backstack since currentActivity was probably invoked by a
                // different app. The preserves the "Up" functionality within the app according to
                // the activity hierarchy defined in AndroidManifest.xml via parentActivity
                // attributes.
                TaskStackBuilder builder = TaskStackBuilder.create(currentActivity);
                builder.addNextIntentWithParentStack(intent);
                builder.startActivities();
            } else {
                // Navigate normally to the manifest defined "Up" activity.
                NavUtils.navigateUpTo(currentActivity, intent);
            }
        }
    }

}
