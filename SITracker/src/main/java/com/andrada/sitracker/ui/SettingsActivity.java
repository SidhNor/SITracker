/*
 * Copyright 2014 Gleb Godonoga.
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
import android.app.AlertDialog;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceActivity;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.R;
import com.andrada.sitracker.contracts.SIPrefs_;
import com.andrada.sitracker.events.AuthorSortMethodChanged;
import com.andrada.sitracker.tasks.ClearPublicationCacheTask;
import com.andrada.sitracker.tasks.UpdateAuthorsTask_;
import com.andrada.sitracker.util.ShareHelper;
import com.andrada.sitracker.util.UIUtils;
import com.google.analytics.tracking.android.EasyTracker;

import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.sharedpreferences.Pref;

import de.greenrobot.event.EventBus;

@SuppressWarnings("deprecation")
@SuppressLint("Registered")
@EActivity
public class SettingsActivity extends PreferenceActivity implements
        SharedPreferences.OnSharedPreferenceChangeListener {

    @SystemService
    AlarmManager alarmManager;

    @Pref
    SIPrefs_ prefs;

    @SuppressLint("NewApi")
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getPreferenceManager().setSharedPreferencesName("SIPrefs");
        if (getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
            addPreferencesFromResource(R.xml.preferences);
        } else {
            addPreferencesFromResource(R.xml.preferences_no3g);
        }

        setUpdateIntervalSummary(prefs.updateInterval().get());
        setAuthorSortSummary(prefs.authorsSortType().get());
        setDownloadFolderSummary(prefs.downloadFolder().get());
        if (UIUtils.hasHoneycomb()) {
            getActionBar().setDisplayHomeAsUpEnabled(true);
        }
    }

    private final Preference.OnPreferenceClickListener clickListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            Intent updateIntent = new Intent(getApplicationContext(), ClearPublicationCacheTask.class);
            getApplicationContext().startService(updateIntent);
            return true;
        }
    };

    private final Preference.OnPreferenceClickListener dirChooserClickListener = new Preference.OnPreferenceClickListener() {
        @Override
        public boolean onPreferenceClick(Preference preference) {
            final Intent chooserIntent = new Intent(getApplicationContext(), DirectoryChooserActivity.class);
            chooserIntent.putExtra(DirectoryChooserActivity.EXTRA_NEW_DIR_NAME, "Books");
            // REQUEST_DIRECTORY is a constant integer to identify the request, e.g. 0
            startActivityForResult(chooserIntent, Constants.REQUEST_DIRECTORY);
            return true;
        }
    };

    @Override
    protected void onResume() {
        super.onResume();
        getSharedPreferences(Constants.SI_PREF_NAME, MODE_MULTI_PROCESS).registerOnSharedPreferenceChangeListener(this);
        Preference pref = findPreference(Constants.PREF_CLEAR_SAVED_PUBS_KEY);
        if (pref != null) {
            pref.setOnPreferenceClickListener(clickListener);
        }
        Preference dirChooserPref = findPreference(Constants.CONTENT_DOWNLOAD_FOLDER_KEY);
        if (dirChooserPref != null) {
            dirChooserPref.setOnPreferenceClickListener(dirChooserClickListener);
        }
        EasyTracker.getInstance().activityStart(this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        getSharedPreferences(Constants.SI_PREF_NAME, MODE_MULTI_PROCESS).unregisterOnSharedPreferenceChangeListener(this);
        Preference pref = findPreference(Constants.PREF_CLEAR_SAVED_PUBS_KEY);
        if (pref != null) {
            pref.setOnPreferenceClickListener(null);
            EasyTracker.getInstance().activityStop(this);
        }
    }


    @Override
    public void onSharedPreferenceChanged(SharedPreferences preference, String key) {

        if (key == null) return;

        Intent intent = UpdateAuthorsTask_.intent(this.getApplicationContext()).get();
        PendingIntent pi = PendingIntent.getService(this.getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

        if (key.equals(Constants.UPDATES_ENABLED_KEY)) {
            Boolean isSyncing = prefs.updatesEnabled().get();
            if (!isSyncing) {
                alarmManager.cancel(pi);
            }
        } else if (key.equals(Constants.UPDATE_INTERVAL_KEY)) {
            alarmManager.cancel(pi);
            long updateInterval = Long.parseLong(prefs.updateInterval().get());
            alarmManager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis(), updateInterval, pi);
            setUpdateIntervalSummary(prefs.updateInterval().get());
            EasyTracker.getTracker().sendEvent(
                    Constants.GA_UI_CATEGORY,
                    Constants.GA_EVENT_CHANGED_UPDATE_INTERVAL,
                    Constants.GA_EVENT_CHANGED_UPDATE_INTERVAL, updateInterval);
            EasyTracker.getInstance().dispatch();
        } else if (key.equals(Constants.AUTHOR_SORT_TYPE_KEY)) {
            EventBus.getDefault().post(new AuthorSortMethodChanged());
            setAuthorSortSummary(prefs.authorsSortType().get());
        } else if (key.equals(Constants.CONTENT_DOWNLOAD_FOLDER_KEY)) {
            String path = prefs.downloadFolder().get();
            //Perform validation
            if (ShareHelper.getExternalDirectoryBasedOnPath(path) != null) {
                setDownloadFolderSummary(path);
            } else {
                final AlertDialog.Builder builder = new AlertDialog.Builder(this);
                builder.setTitle("Invalid directory");
                builder.setMessage("Your directory is invalid");
                builder.setPositiveButton(android.R.string.ok, null);
                builder.show();
            }
        }
    }

    private void setDownloadFolderSummary(String newValue) {
        Preference pref = findPreference(Constants.CONTENT_DOWNLOAD_FOLDER_KEY);
        if (newValue == null || newValue.length() == 0) {
            pref.setSummary(getResources().getString(R.string.pref_content_download_summ));
        } else {
            pref.setSummary(getResources().getString(R.string.pref_content_download_summ_short) + " " + newValue);
        }
    }

    private void setUpdateIntervalSummary(String newValue) {
        ListPreference updateInterval = (ListPreference) findPreference(Constants.UPDATE_INTERVAL_KEY);
        // Set summary to be the user-description for the selected value
        int index = updateInterval.findIndexOfValue(newValue);
        if (index >= 0 && index < updateInterval.getEntries().length)
            updateInterval.setSummary(updateInterval.getEntries()[index]);
    }

    private void setAuthorSortSummary(String newValue) {
        ListPreference authorsSortType = (ListPreference) findPreference(Constants.AUTHOR_SORT_TYPE_KEY);
        int sortType = Integer.parseInt(newValue);
        authorsSortType.setSummary(authorsSortType.getEntries()[sortType]);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constants.REQUEST_DIRECTORY) {
            if (resultCode == DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED) {
                String absoluteDir = data.getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR);
                SharedPreferences.Editor editor = getPreferenceManager().getSharedPreferences().edit();
                editor.putString(Constants.CONTENT_DOWNLOAD_FOLDER_KEY, absoluteDir);
                editor.commit();
                setDownloadFolderSummary(absoluteDir);
            }
        }
    }
}
