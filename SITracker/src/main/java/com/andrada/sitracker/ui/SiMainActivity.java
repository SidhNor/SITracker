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
import android.app.Activity;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.provider.Settings;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.view.View;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.andrada.sitracker.R;
import com.andrada.sitracker.contracts.AppUriContract;
import com.andrada.sitracker.contracts.SIPrefs_;
import com.andrada.sitracker.events.AuthorSelectedEvent;
import com.andrada.sitracker.ui.fragment.AboutDialog;
import com.andrada.sitracker.ui.fragment.AuthorsFragment;
import com.andrada.sitracker.ui.fragment.AuthorsFragment_;
import com.andrada.sitracker.util.UpdateServiceHelper;
import com.andrada.sitracker.util.permission.Permissions;
import com.andrada.sitracker.util.permission.RuntimePermissionsInteraction;
import com.andrada.sitracker.util.permission.RuntimePermissionsUtils;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.Extra;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.jetbrains.annotations.NotNull;

import de.greenrobot.event.EventBus;

@SuppressLint("Registered")
@EActivity
public class SiMainActivity extends BaseActivity implements RuntimePermissionsInteraction {

    @ViewById(R.id.fragment_holder)
    View fragmentHolder;

    public static final String AUTHORS_PROCESSED_EXTRA = "authors_total_processed";
    public static final String AUTHORS_SUCCESSFULLY_IMPORTED_EXTRA = "authors_successfully_imported";
    @Extra(AUTHORS_PROCESSED_EXTRA)
    int authorsProcessed = -1;
    @Extra(AUTHORS_SUCCESSFULLY_IMPORTED_EXTRA)
    int authorsSuccessfullyImported = -1;

    @Pref
    SIPrefs_ prefs;

    RuntimePermissionsUtils permissionsUtils = new RuntimePermissionsUtils();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        int priority = 1;
        EventBus.getDefault().register(this, priority);

        setContentView(R.layout.activity_si_main);
        ensureUpdatesAreRunningOnSchedule();

        permissionsUtils.requestPermissionIfNeed(Permissions.WRITE_PERMISSION, this);
    }

    @AfterViews
    protected void afterViews() {
        Fragment fragment = getFragmentManager().findFragmentByTag("currentFrag");
        if (fragment == null) {
            //Bootstrap app with initial fragment
            FragmentTransaction transaction = getFragmentManager().beginTransaction();
            AuthorsFragment authFrag = AuthorsFragment_.builder().build();
            currentFragment = authFrag;
            transaction.replace(R.id.fragment_holder, authFrag, "currentFrag");
            transaction.setCustomAnimations(0, 0);
            transaction.commit();
        }
        super.afterViews();
        attemptToShowWhatsNew();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }


    @Override
    protected void onResume() {
        super.onResume();
        invalidateOptionsMenu();
        attemptToShowImportProgress();
    }

    @Override
    protected void onNewIntent(@NotNull Intent intent) {
        super.onNewIntent(intent);
        Bundle extras = intent.getExtras();
        if (extras != null) {
            if (extras.containsKey(AUTHORS_PROCESSED_EXTRA)) {
                authorsProcessed = extras.getInt(AUTHORS_PROCESSED_EXTRA);
            }
            if (extras.containsKey(AUTHORS_SUCCESSFULLY_IMPORTED_EXTRA)) {
                authorsSuccessfullyImported = extras.getInt(AUTHORS_SUCCESSFULLY_IMPORTED_EXTRA);
            }
        }
    }

    public void onEvent(AuthorSelectedEvent event) {
        Intent intent = new Intent(Intent.ACTION_VIEW,
                AppUriContract.buildAuthorUri(event.authorId, event.authorName), this,
                AuthorDetailsActivity_.class);
        ActivityCompat.startActivity(this, intent, null);
    }

    public void ensureUpdatesAreRunningOnSchedule() {
        boolean isSyncing = prefs.updatesEnabled().get();

        boolean updateServiceUp = UpdateServiceHelper.isServiceScheduled(this);
        if (isSyncing && !updateServiceUp) {
            UpdateServiceHelper.scheduleUpdates(this);
        } else if (!isSyncing && updateServiceUp) {
            UpdateServiceHelper.cancelUpdates(this);
        }
    }

    private void attemptToShowWhatsNew() {
        PackageManager pm = getPackageManager();
        String packageName = getPackageName();
        try {
            PackageInfo info = pm.getPackageInfo(packageName, 0);
            int currentVersionCode = info.versionCode;
            if (currentVersionCode > prefs.lastVersionViewed().get()) {
                //Show dialog
                AboutDialog.showWhatsNew(this);
                //Update last version viewed
                prefs.lastVersionViewed().put(currentVersionCode);
            }
        } catch (PackageManager.NameNotFoundException e) {
            //Ignore
        }
    }

    private void attemptToShowImportProgress() {
        if (authorsProcessed != -1 && authorsSuccessfullyImported != -1) {
            View view = getLayoutInflater().inflate(R.layout.widget_import_result, null);
            TextView totalTextV = (TextView) view.findViewById(R.id.totalAuthorsText);
            totalTextV.setText(getResources().getString(R.string.author_import_total_crouton_message,
                    authorsProcessed));
            TextView successTextV = (TextView) view.findViewById(R.id.successAuthorsText);
            successTextV.setText(getResources().getString(R.string.author_import_processed_crouton_message,
                    authorsSuccessfullyImported));
            TextView failedTextV = (TextView) view.findViewById(R.id.failedAuthorsText);
            failedTextV.setText(getResources().getString(R.string.author_import_failed_crouton_message,
                    authorsProcessed - authorsSuccessfullyImported));

            new MaterialDialog.Builder(this)
                    .customView(view, false)
                    .positiveText(R.string.ok)
                    .autoDismiss(true)
                    .show();

            //Remove extras to avoid reinitialization on config change
            getIntent().removeExtra(AUTHORS_PROCESSED_EXTRA);
            getIntent().removeExtra(AUTHORS_SUCCESSFULLY_IMPORTED_EXTRA);
            authorsSuccessfullyImported = -1;
            authorsProcessed = -1;
        }
    }

    @Override
    public Activity getActivity() {
        return this;
    }

    @Override
    public void showExplanationDialog(Permissions permission) {
        new MaterialDialog.Builder(this)
                .content(permission.explanationMessageResourceId)
                .negativeText(android.R.string.cancel)
                .positiveText(R.string.action_settings)
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        getActivity().startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,
                                Uri.fromParts("package", getPackageName(), null)));
                    }
                })
                .build().show();
    }

    @Override
    public void permissionGranted() {

    }

    @Override
    public void permissionRevoked() {

    }
}
