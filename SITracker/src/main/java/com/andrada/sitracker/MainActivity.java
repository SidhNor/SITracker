package com.andrada.sitracker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.preference.PreferenceManager;
import android.support.v4.widget.SlidingPaneLayout;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.ProgressBar;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.actionbarsherlock.view.MenuItem;
import com.andrada.sitracker.fragment.AuthorsFragment;
import com.andrada.sitracker.fragment.PublicationsFragment;
import com.andrada.sitracker.tasks.UpdateAuthorsTask_;
import com.andrada.sitracker.tasks.filters.UpdateStatusMessageFilter;
import com.andrada.sitracker.tasks.receivers.UpdateStatusReceiver;
import com.google.analytics.tracking.android.EasyTracker;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.OptionsItem;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.StringRes;

import de.keyboardsurfer.android.widget.crouton.Crouton;


@EActivity(R.layout.activity_main)
@OptionsMenu(R.menu.main_menu)
public class MainActivity extends SherlockFragmentActivity implements
        AuthorsFragment.OnAuthorsUpdatingListener {

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

    PendingIntent updatePendingIntent;

    @StringRes(R.string.app_name)
    private String mAppName;

    @AfterViews
    public void afterViews() {
        slidingPane.setPanelSlideListener(slidingPaneListener);
        //Make sure the authors are opened
        slidingPane.openPane();
        slidingPane.setParallaxDistance(100);


        slidingPane.getViewTreeObserver().addOnGlobalLayoutListener(globalLayoutListener);
        mAuthorsFragment.setUpdatingListener(this);
        Intent intent = UpdateAuthorsTask_.intent(this.getApplicationContext()).get();
        this.updatePendingIntent = PendingIntent.getService(this.getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        ensureUpdatesAreRunningOnSchedule(PreferenceManager.getDefaultSharedPreferences(this));
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        //Do not show menu in actionbar if authors are updating
        if (mAuthorsFragment != null) {
            return !mAuthorsFragment.isUpdating();
        }
        return true;
    }

    private BroadcastReceiver updateStatusReceiver;

    @Override
    protected void onStart() {
        super.onStart();
        Integer i = 10 + 2;
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (updateStatusReceiver == null) {
            //AuthorsFragment is the callback here
            updateStatusReceiver = new UpdateStatusReceiver(mAuthorsFragment);
            updateStatusReceiver.setOrderedHint(true);
        }

        UpdateStatusMessageFilter filter = new UpdateStatusMessageFilter();
        filter.setPriority(1);
        registerReceiver(updateStatusReceiver, filter);
        EasyTracker.getInstance().activityStart(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(updateStatusReceiver);
        EasyTracker.getInstance().activityStop(this);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        Crouton.cancelAllCroutons();
    }

    public void ensureUpdatesAreRunningOnSchedule(SharedPreferences sharedPreferences) {
        Boolean isSyncing = sharedPreferences.getBoolean(Constants.UPDATE_PREFERENCE_KEY, true);
        alarmManager.cancel(this.updatePendingIntent);
        if (isSyncing) {
            long updateInterval = Long.getLong(sharedPreferences.getString(Constants.UPDATE_INTERVAL_KEY, "3600000L"), 3600000L);
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
        startActivity(SettingsActivity_.intent(this).get());
    }

    /**
     * This global layout listener is used to fire an event after first layout
     * occurs and then it is removed. This gives us a chance to configure parts
     * of the UI that adapt based on available space after they have had the
     * opportunity to measure and layout.
     */
    ViewTreeObserver.OnGlobalLayoutListener globalLayoutListener = new ViewTreeObserver.OnGlobalLayoutListener() {
        @Override
        public void onGlobalLayout() {
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.JELLY_BEAN) {
                slidingPane.getViewTreeObserver().removeGlobalOnLayoutListener(this);
            } else {
                slidingPane.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            }
            if (slidingPane.isSlideable() && !slidingPane.isOpen()) {
                updateActionBarWithoutLandingNavigation();
            } else {
                updateActionBarWithHomeBackNavigation();
            }
        }
    };


    SlidingPaneLayout.SimplePanelSlideListener slidingPaneListener = new SlidingPaneLayout.SimplePanelSlideListener() {

        public void onPanelOpened(View view) {
            EasyTracker.getTracker().sendView(Constants.GA_SCREEN_AUTHORS);
            if (slidingPane.isSlideable()) {
                updateActionBarWithHomeBackNavigation();
            }
        }

        public void onPanelClosed(View view) {
            EasyTracker.getTracker().sendView(Constants.GA_SCREEN_PUBLICATIONS);
            //This is called only on phones and 7 inch tablets in portrait
            updateActionBarWithoutLandingNavigation();
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

    @Override
    public void onUpdateStarted() {
        this.globalProgress.setVisibility(View.VISIBLE);
    }

    @Override
    public void onUpdateStopped() {
        this.globalProgress.setVisibility(View.GONE);
    }
}
