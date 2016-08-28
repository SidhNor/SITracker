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

import android.annotation.SuppressLint;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.widget.Toolbar;
import android.view.View;

import com.afollestad.materialdialogs.MaterialDialog;
import com.andrada.sitracker.Constants;
import com.andrada.sitracker.R;
import com.andrada.sitracker.analytics.AnalyticsManager;
import com.andrada.sitracker.analytics.FBAEvent;
import com.andrada.sitracker.contracts.SIPrefs_;
import com.andrada.sitracker.events.AuthorSortMethodChanged;
import com.andrada.sitracker.tasks.ClearPublicationCacheTask;
import com.andrada.sitracker.tasks.UpdateAuthorsTask_;
import com.andrada.sitracker.util.ShareHelper;
import com.google.firebase.analytics.FirebaseAnalytics;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import de.greenrobot.event.EventBus;

@SuppressLint("Registered")
@EActivity(R.layout.activity_settings)
public class SettingsActivity extends BaseActivity {

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState == null) {
            getFragmentManager().beginTransaction()
                    .add(R.id.container, SettingsActivity_.SettingsFragment_.builder().build())
                    .commit();
        }
    }

    @AfterViews
    protected void afterViews() {
        Toolbar toolbar = getActionBarToolbar();
        toolbar.setTitle(R.string.title_settings);
        toolbar.setNavigationIcon(R.drawable.ic_up);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateUpOrBack(SettingsActivity.this, SiMainActivity_.class);
            }
        });
    }


    @EFragment
    public static class SettingsFragment extends PreferenceFragment
            implements SharedPreferences.OnSharedPreferenceChangeListener, DirectoryChooserController.DirectoryChooserResultListener {

        private DirectoryChooserController dirChooserController;
        @NotNull
        public static final String PREF_NAME = "SIPrefs";
        @Nullable
        private final Preference.OnPreferenceClickListener clickListener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {
                Intent updateIntent = new Intent(getActivity().getApplicationContext(), ClearPublicationCacheTask.class);
                getActivity().getApplicationContext().startService(updateIntent);
                return true;
            }
        };
        private final Preference.OnPreferenceClickListener dirChooserClickListener = new Preference.OnPreferenceClickListener() {
            @Override
            public boolean onPreferenceClick(Preference preference) {

                dirChooserController.showDialog(SettingsFragment.this);
                return true;
            }
        };
        @SystemService
        AlarmManager alarmManager;
        @Pref
        SIPrefs_ prefs;


        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            getPreferenceManager().setSharedPreferencesName(PREF_NAME);
            if (getActivity().getPackageManager().hasSystemFeature(PackageManager.FEATURE_TELEPHONY)) {
                addPreferencesFromResource(R.xml.preferences);
            } else {
                addPreferencesFromResource(R.xml.preferences_no3g);
            }

            setUpdateIntervalSummary(prefs.updateInterval().get());
            setAuthorSortSummary(prefs.authorsSortType().get());
            setDownloadFolderSummary(prefs.downloadFolder().get());

            dirChooserController = new DirectoryChooserController(getActivity(), prefs.downloadFolder().get(), true);
            dirChooserController.setListener(this);
        }

        @Override
        public void onResume() {
            super.onResume();
            getActivity().getSharedPreferences(Constants.SI_PREF_NAME, MODE_MULTI_PROCESS).registerOnSharedPreferenceChangeListener(this);

            Preference pref = findPreference(Constants.PREF_CLEAR_SAVED_PUBS_KEY);
            if (pref != null) {
                pref.setOnPreferenceClickListener(clickListener);
            }
            Preference dirChooserPref = findPreference(Constants.CONTENT_DOWNLOAD_FOLDER_KEY);
            if (dirChooserPref != null) {
                dirChooserPref.setOnPreferenceClickListener(dirChooserClickListener);
            }
        }

        @Override
        public void onPause() {
            super.onPause();
            getActivity().getSharedPreferences(Constants.SI_PREF_NAME, MODE_MULTI_PROCESS).unregisterOnSharedPreferenceChangeListener(this);
            Preference pref = findPreference(Constants.PREF_CLEAR_SAVED_PUBS_KEY);
            if (pref != null) {
                pref.setOnPreferenceClickListener(null);
            }
        }


        @Override
        public void onDirectoryChooserResult(int resultCode, String absoluteDir) {
            if (resultCode == DirectoryChooserController.RESULT_CODE_DIR_SELECTED) {
                SharedPreferences.Editor editor = getPreferenceManager().getSharedPreferences().edit();
                editor.putString(Constants.CONTENT_DOWNLOAD_FOLDER_KEY, absoluteDir);
                editor.apply();
                setDownloadFolderSummary(absoluteDir);
            }
        }


        @Override
        public void onSharedPreferenceChanged(SharedPreferences preference, String key) {

            Intent intent = UpdateAuthorsTask_.intent(this.getActivity().getApplicationContext()).get();
            PendingIntent pi = PendingIntent.getService(this.getActivity().getApplicationContext(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);

            if (Constants.UPDATES_ENABLED_KEY.equals(key)) {
                Boolean isSyncing = prefs.updatesEnabled().get();
                if (!isSyncing) {
                    alarmManager.cancel(pi);
                }
            } else if (Constants.UPDATE_INTERVAL_KEY.equals(key)) {
                alarmManager.cancel(pi);
                long updateInterval = Long.parseLong(prefs.updateInterval().get());
                alarmManager.setInexactRepeating(AlarmManager.RTC, System.currentTimeMillis(), updateInterval, pi);
                setUpdateIntervalSummary(prefs.updateInterval().get());

                FBAEvent event = new FBAEvent(Constants.GA_EVENT_CHANGED_UPDATE_INTERVAL);
                event.getParamMap().put(FirebaseAnalytics.Param.VALUE, prefs.updateInterval().get());
                AnalyticsManager.getInstance().logEvent(event);
            } else if (Constants.AUTHOR_SORT_TYPE_KEY.equals(key)) {
                EventBus.getDefault().post(new AuthorSortMethodChanged());
                setAuthorSortSummary(prefs.authorsSortType().get());
            } else if (Constants.CONTENT_DOWNLOAD_FOLDER_KEY.equals(key)) {
                String path = prefs.downloadFolder().get();
                //Perform validation
                if (ShareHelper.getExternalDirectoryBasedOnPath(path) != null) {
                    setDownloadFolderSummary(path);
                } else {
                    final MaterialDialog.Builder builder = new MaterialDialog.Builder(
                            this.getActivity());
                    builder.title("Invalid directory");
                    builder.content("Your directory is invalid");
                    builder.positiveText(android.R.string.ok);
                    builder.show();
                }
            } else if (Constants.PREF_USAGE_OPT_OUT_KEY.equals(key)) {
                AnalyticsManager.getInstance().setOptOut(prefs.optOutUsageStatistics().get());
            }
        }

        private void setDownloadFolderSummary(@Nullable String newValue) {
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

    }


}
