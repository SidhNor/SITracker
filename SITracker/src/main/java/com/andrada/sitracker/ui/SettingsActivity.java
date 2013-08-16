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
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.PreferenceActivity;
import android.preference.PreferenceManager;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.R;
import com.andrada.sitracker.events.AuthorSortMethodChanged;
import com.andrada.sitracker.tasks.UpdateAuthorsTask_;
import com.andrada.sitracker.util.UIUtils;
import com.google.analytics.tracking.android.EasyTracker;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.SystemService;

import de.greenrobot.event.EventBus;

@EActivity
public class SettingsActivity extends PreferenceActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    @SystemService
    AlarmManager alarmManager;

    @SuppressLint("NewApi")
    @SuppressWarnings("deprecation")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            addPreferencesFromResource(R.xml.preferences);
        } else {
            addPreferencesFromResource(R.xml.preferences_no3g);
        }

        setUpdateIntervalSummary();
        setAuthorSortSummary();
        if (UIUtils.hasHoneycomb()) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }

    }

    @Override
    protected void onResume() {
        super.onResume();
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);
        EasyTracker.getInstance().activityStart(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        PreferenceManager.getDefaultSharedPreferences(this).unregisterOnSharedPreferenceChangeListener(this);
        EasyTracker.getInstance().activityStop(this);
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        Boolean isSyncing = sharedPreferences.getBoolean(Constants.UPDATE_PREFERENCE_KEY, true);
        Intent intent = UpdateAuthorsTask_.intent(this.getApplicationContext()).get();
        PendingIntent pi = PendingIntent.getService(this.getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        alarmManager.cancel(pi);
        long updateInterval = Long.parseLong(sharedPreferences.getString(Constants.UPDATE_INTERVAL_KEY, "14400000"));
        if (isSyncing) {
            alarmManager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis(), updateInterval, pi);
        }

        if (key.equals(Constants.AUTHOR_SORT_TYPE_KEY)) {
            EventBus.getDefault().post(new AuthorSortMethodChanged());
            setAuthorSortSummary();
        }

        if (key.equals(Constants.UPDATE_INTERVAL_KEY)) {
            setUpdateIntervalSummary();
            EasyTracker.getTracker().sendEvent(
                    Constants.GA_UI_CATEGORY,
                    Constants.GA_EVENT_CHANGED_UPDATE_INTERVAL,
                    Constants.GA_EVENT_CHANGED_UPDATE_INTERVAL, updateInterval);
            EasyTracker.getInstance().dispatch();
        }
    }

    private void setUpdateIntervalSummary() {
        ListPreference updateInterval = (ListPreference) findPreference(Constants.UPDATE_INTERVAL_KEY);
        // Set summary to be the user-description for the selected value
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        int index = updateInterval.findIndexOfValue(sharedPreferences.getString(Constants.UPDATE_INTERVAL_KEY, ""));
        if (index >= 0 && index < updateInterval.getEntries().length)
            updateInterval.setSummary(updateInterval.getEntries()[index]);
    }

    private void setAuthorSortSummary() {
        ListPreference authorsSortType = (ListPreference) findPreference(Constants.AUTHOR_SORT_TYPE_KEY);
        int sortType = Integer.parseInt(
                PreferenceManager.getDefaultSharedPreferences(this)
                        .getString(Constants.AUTHOR_SORT_TYPE_KEY, "0"));
        authorsSortType.setSummary(authorsSortType.getEntries()[sortType]);
    }
}
