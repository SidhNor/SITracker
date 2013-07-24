package com.andrada.sitracker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v4.widget.SlidingPaneLayout;

import com.actionbarsherlock.app.SherlockFragmentActivity;
import com.actionbarsherlock.view.Menu;
import com.andrada.sitracker.fragment.AuthorsFragment;
import com.andrada.sitracker.fragment.AuthorsFragment.OnAuthorSelectedListener;
import com.andrada.sitracker.fragment.PublicationsFragment;
import com.andrada.sitracker.tasks.UpdateAuthorsTask_;
import com.andrada.sitracker.tasks.filters.MarkAsReadMessageFilter;
import com.andrada.sitracker.tasks.filters.UpdateStatusMessageFilter;
import com.andrada.sitracker.tasks.receivers.MarkedAsReadReceiver;
import com.andrada.sitracker.tasks.receivers.UpdateStatusReceiver;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.FragmentById;
import org.androidannotations.annotations.OptionsMenu;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.ViewById;


@EActivity(R.layout.activity_main)
@OptionsMenu(R.menu.main_menu)
public class MainActivity extends SherlockFragmentActivity implements
        OnAuthorSelectedListener {

    @FragmentById(R.id.fragment_publications)
    PublicationsFragment mPubFragment;

    @FragmentById(R.id.fragment_authors)
    AuthorsFragment mAuthorsFragment;

    @ViewById
    SlidingPaneLayout fragment_container;

    @SystemService
    AlarmManager alarmManager;

    PendingIntent updatePendingIntent;

    @Override
    public void onAuthorSelected(long id) {
        // Capture the publications fragment from the activity layout
        mPubFragment.updatePublicationsView(id);
    }

    @AfterViews
    public void afterViews() {
        fragment_container.openPane();
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
    private BroadcastReceiver markAsReadReceiver;

    @Override
    protected void onResume() {
        super.onResume();
        if (updateStatusReceiver == null) {
            //AuthorsFragment is the callback here
            updateStatusReceiver = new UpdateStatusReceiver(mAuthorsFragment);
            updateStatusReceiver.setOrderedHint(true);
        }
        if (markAsReadReceiver == null) {
            markAsReadReceiver = new MarkedAsReadReceiver(mPubFragment, mAuthorsFragment);
        }

        UpdateStatusMessageFilter filter = new UpdateStatusMessageFilter();
        filter.setPriority(1);
        registerReceiver(updateStatusReceiver, filter);
        LocalBroadcastManager.getInstance(getApplicationContext())
                .registerReceiver(markAsReadReceiver, new MarkAsReadMessageFilter());
    }

    @Override
    protected void onPause() {
        super.onPause();
        unregisterReceiver(updateStatusReceiver);
        LocalBroadcastManager.getInstance(getApplicationContext()).unregisterReceiver(markAsReadReceiver);
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
}
