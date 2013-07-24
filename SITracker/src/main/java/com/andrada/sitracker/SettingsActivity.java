package com.andrada.sitracker;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceManager;

import com.actionbarsherlock.app.SherlockPreferenceActivity;
import com.andrada.sitracker.tasks.UpdateAuthorsTask_;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.SystemService;

/**
 * Created by ggodonoga on 22/07/13.
 */
@EActivity
public class SettingsActivity extends SherlockPreferenceActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    @SystemService
    AlarmManager alarmManager;

    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        addPreferencesFromResource(R.xml.preferences);
        setSummary();

    }

    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Boolean isSyncing = sharedPreferences.getBoolean(Constants.UPDATE_PREFERENCE_KEY, true);
        Intent intent = UpdateAuthorsTask_.intent(this.getApplicationContext()).get();
        PendingIntent pi = PendingIntent.getService(this.getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.cancel(pi);

        if (isSyncing) {
            long updateInterval = Long.getLong(sharedPreferences.getString(Constants.UPDATE_INTERVAL_KEY, "3600000L"), 3600000L);
            alarmManager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis(), updateInterval, pi);
        }

        if (key.equals(Constants.UPDATE_INTERVAL_KEY)) {
            setSummary();
        }
    }

    private void setSummary() {
        ListPreference updateInterval = (ListPreference) findPreference(Constants.UPDATE_INTERVAL_KEY);
        // Set summary to be the user-description for the selected value
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        int index = updateInterval.findIndexOfValue(sharedPreferences.getString(Constants.UPDATE_INTERVAL_KEY, ""));
        if (index >= 0 && index < updateInterval.getEntries().length)
            updateInterval.setSummary(updateInterval.getEntries()[index]);
    }
}
