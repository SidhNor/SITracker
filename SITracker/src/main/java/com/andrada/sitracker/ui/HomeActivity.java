/*
 * Copyright 2013 Gleb Godonoga.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andrada.sitracker.ui;

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.support.v4.app.FragmentManager;
import android.support.v4.widget.SlidingPaneLayout;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ProgressBar;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.R;
import com.andrada.sitracker.contracts.SIPrefs_;
import com.andrada.sitracker.events.ProgressBarToggleEvent;
import com.andrada.sitracker.tasks.UpdateAuthorsTask_;
import com.andrada.sitracker.tasks.filters.UpdateStatusMessageFilter;
import com.andrada.sitracker.tasks.receivers.UpdateStatusReceiver;
import com.andrada.sitracker.ui.fragment.AuthorsFragment;
import com.andrada.sitracker.ui.fragment.PublicationsFragment;
import com.andrada.sitracker.util.ImageLoader;
import com.andrada.sitracker.util.UIUtils;
import com.google.analytics.tracking.android.EasyTracker;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.StringRes;
import org.androidannotations.annotations.sharedpreferences.Pref;

import de.greenrobot.event.EventBus;
import de.keyboardsurfer.android.widget.crouton.Crouton;


@SuppressLint("Registered")
@EActivity(R.layout.activity_main)
@OptionsMenu(R.menu.main_menu)
public class HomeActivity extends BaseActivity implements ImageLoader.ImageLoaderProvider {

    @FragmentById(R.id.fragment_publications)
    PublicationsFragment mPubFragment;

    @FragmentById(R.id.fragment_authors)
    AuthorsFragment mAuthorsFragment;

    @ViewById(R.id.fragment_container)
    SlidingPaneLayout slidingPane;

    @ViewById
    ProgressBar globalProgress;

    @SystemService
    AlarmManager alarmManager;

    @Pref
    SIPrefs_ prefs;

    PendingIntent updatePendingIntent;

    private ImageLoader mImageLoader;

    @StringRes(R.string.app_name)
    String mAppName;

    @AfterViews
    public void afterViews() {
        //Make sure the authors are opened
        slidingPane.openPane();
        slidingPane.setParallaxDistance(100);


        slidingPane.getViewTreeObserver().addOnGlobalLayoutListener(globalLayoutListener);
        Intent intent = UpdateAuthorsTask_.intent(this.getApplicationContext()).get();
        this.updatePendingIntent = PendingIntent.getService(this.getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        ensureUpdatesAreRunningOnSchedule();

        mImageLoader = new ImageLoader(this, R.drawable.blank_book)
                .setMaxImageSize(getResources().getDimensionPixelSize(R.dimen.publication_pixel_size))
                .setFadeInImage(UIUtils.hasHoneycombMR1());
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        //Do not show menu in actionbar if authors are updating
        return mAuthorsFragment == null || !mAuthorsFragment.isUpdating();
    }

    private BroadcastReceiver updateStatusReceiver;

    @Override
    protected void onCreate(android.os.Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);
        if (isFinishing()) {
            return;
        }
        UIUtils.enableDisableActivitiesByFormFactor(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (updateStatusReceiver == null) {
            //AuthorsFragment is the callback here
            updateStatusReceiver = new UpdateStatusReceiver(mAuthorsFragment);
            updateStatusReceiver.setOrderedHint(true);
        }
        slidingPane.setPanelSlideListener(slidingPaneListener);
        getSupportFragmentManager().addOnBackStackChangedListener(backStackListener);

        UpdateStatusMessageFilter filter = new UpdateStatusMessageFilter();
        filter.setPriority(1);
        registerReceiver(updateStatusReceiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();
        slidingPane.setPanelSlideListener(null);
        unregisterReceiver(updateStatusReceiver);
        getSupportFragmentManager().removeOnBackStackChangedListener(backStackListener);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
        Crouton.cancelAllCroutons();
    }

    public void ensureUpdatesAreRunningOnSchedule() {
        Boolean isSyncing = prefs.updatesEnabled().get();
        alarmManager.cancel(this.updatePendingIntent);
        if (isSyncing) {
            long updateInterval = Long.parseLong(prefs.updateInterval().get());
            alarmManager.setInexactRepeating(AlarmManager.RTC_WAKEUP,
                    System.currentTimeMillis(),
                    updateInterval,
                    this.updatePendingIntent);
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        /*
         * The action bar up action should open the slider if it is currently
         * closed, as the left pane contains content one level up in the
         * navigation hierarchy.
         */
        if (item.getItemId() == android.R.id.home && !slidingPane.isOpen()) {
            slidingPane.openPane();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @OptionsItem(R.id.action_settings)
    void menuSettingsSelected() {
        startActivity(com.andrada.sitracker.ui.SettingsActivity_.intent(this).get());
    }

    @OptionsItem(R.id.action_import)
    void menuImportSelected() {
        startActivity(com.andrada.sitracker.ui.ImportAuthorsActivity_.intent(this).get());
    }

    /**
     * This global layout listener is used to fire an event after first layout
     * occurs and then it is removed. This gives us a chance to configure parts
     * of the UI that adapt based on available space after they have had the
     * opportunity to measure and layout.
     */
    final ViewTreeObserver.OnGlobalLayoutListener globalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @SuppressLint("NewApi")
        @Override
        public void onGlobalLayout() {

            if (UIUtils.hasJellyBean()) {
                slidingPane.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            } else {
                //noinspection deprecation
                slidingPane.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            }

            if (slidingPane.isSlideable() && !slidingPane.isOpen()) {
                updateActionBarWithoutLandingNavigation();
            } else {
                updateActionBarWithHomeBackNavigation();
            }
        }
    };

    /**
     * This back stack listener is used to simulate standard fragment backstack behavior
     * for back button when panes are slid back and forth.
     */
    final FragmentManager.OnBackStackChangedListener backStackListener = new FragmentManager.OnBackStackChangedListener() {
        @Override
        public void onBackStackChanged() {
            if (slidingPane.isSlideable() &&
                    !slidingPane.isOpen() &&
                    getSupportFragmentManager().getBackStackEntryCount() == 0) {
                slidingPane.openPane();
            }
        }
    };


    final SlidingPaneLayout.SimplePanelSlideListener slidingPaneListener = new SlidingPaneLayout.SimplePanelSlideListener() {

        public void onPanelOpened(View view) {
            EasyTracker.getTracker().sendView(Constants.GA_SCREEN_AUTHORS);
            if (slidingPane.isSlideable()) {
                updateActionBarWithHomeBackNavigation();
                getSupportFragmentManager().popBackStack();
            }
        }

        public void onPanelClosed(View view) {
            EasyTracker.getTracker().sendView(Constants.GA_SCREEN_PUBLICATIONS);
            //This is called only on phones and 7 inch tablets in portrait
            updateActionBarWithoutLandingNavigation();
            getSupportFragmentManager().beginTransaction().addToBackStack(null).commit();
        }
    };

    private void updateActionBarWithoutLandingNavigation() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        getSupportActionBar().setHomeButtonEnabled(true);
        mAuthorsFragment.setHasOptionsMenu(false);
        String authorTitle = mAuthorsFragment.getCurrentSelectedAuthorName();
        getSupportActionBar().setTitle(authorTitle.equals("") ? mAppName : authorTitle);
    }

    private void updateActionBarWithHomeBackNavigation() {
        getSupportActionBar().setDisplayHomeAsUpEnabled(false);
        getSupportActionBar().setHomeButtonEnabled(false);
        mAuthorsFragment.setHasOptionsMenu(true);
        getSupportActionBar().setTitle(mAppName);
    }

    public void onEventMainThread(ProgressBarToggleEvent event) {
        if (event.showProgress) {
            this.globalProgress.setVisibility(View.VISIBLE);
        } else {
            this.globalProgress.setVisibility(View.GONE);
        }
    }

    public AuthorsFragment getAuthorsFragment() {
        return mAuthorsFragment;
    }

    public PublicationsFragment getPubFragment() {
        return mPubFragment;
    }

    @Override
    public ImageLoader getImageLoaderInstance() {
        return mImageLoader;
    }
}
