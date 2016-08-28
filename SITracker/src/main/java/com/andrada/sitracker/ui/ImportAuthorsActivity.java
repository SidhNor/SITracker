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
import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.support.design.widget.Snackbar;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.R;
import com.andrada.sitracker.analytics.ImportAuthorsEvent;
import com.andrada.sitracker.analytics.ImportEvent;
import com.andrada.sitracker.events.CancelImportEvent;
import com.andrada.sitracker.events.ImportUpdates;
import com.andrada.sitracker.tasks.ImportAuthorsTask;
import com.andrada.sitracker.tasks.ImportAuthorsTask_;
import com.andrada.sitracker.tasks.io.AuthorFileImportContext;
import com.andrada.sitracker.ui.components.ImportProgressView;
import com.andrada.sitracker.ui.components.ImportProgressView_;
import com.andrada.sitracker.analytics.AnalyticsManager;

import org.androidannotations.annotations.AfterViews;
import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

@SuppressLint("Registered")
@EActivity(R.layout.activity_import)
public class ImportAuthorsActivity extends BaseActivity implements DirectoryChooserController.DirectoryChooserResultListener {

    ImportProgressView progressPanel;

    @ViewById
    ViewGroup buttonPanel;

    @ViewById
    ViewGroup actionsContainer;

    @ViewById
    Button chooseFileButton;

    @ViewById
    Button performImportButton;

    @ViewById
    ProgressBar progressBar;

    @ViewById
    ListView list;

    @SystemService
    ActivityManager activityManager;

    @Nullable
    ImportAuthorsTask importTask;
    private boolean isBound = false;
    @NotNull
    private List<String> authorsToImport = new ArrayList<>();

    private DirectoryChooserController dirChooserController;

    @NotNull
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            ImportAuthorsTask.ImportAuthorsBinder binder = (ImportAuthorsTask.ImportAuthorsBinder) service;
            importTask = binder.getService();
            isBound = true;
            ImportAuthorsTask.ImportProgress progress;
            if (importTask.getCurrentProgress() != null) {
                authorsToImport = importTask.getAuthorsList();
                progress = importTask.getCurrentProgress();
                if (list.getAdapter() == null || list.getAdapter().getCount() == 0) {
                    showParseResults(authorsToImport);
                }
            } else {
                progress = new ImportAuthorsTask.ImportProgress(authorsToImport.size());
            }

            toggleButtonAndProgressPanels(true);
            onEventMainThread(new ImportUpdates(progress));
        }

        @Override
        public void onServiceDisconnected(ComponentName arg0) {
            importTask = null;
            isBound = false;
        }
    };

    @Click(R.id.chooseFileButton)
    void chooseFileClicked() {
        //Make sure the authors are opened
        dirChooserController.showDialog(this);
    }

    @Click(R.id.performImportButton)
    void importParsedAuthors() {

        AnalyticsManager.getInstance().logEvent(new ImportAuthorsEvent());
        //Inflate
        Intent importSvc = ImportAuthorsTask_.intent(getApplicationContext()).get();
        importSvc.putStringArrayListExtra(ImportAuthorsTask.AUTHOR_LIST_EXTRA, new ArrayList<>(authorsToImport));
        getApplicationContext().startService(importSvc);
        getApplicationContext().bindService(importSvc, mConnection, Context.BIND_AUTO_CREATE);
    }

    public void onEvent(CancelImportEvent event) {
        if (isBound) {
            if (importTask != null) {
                importTask.cancelImport();
            }
            getApplicationContext().unbindService(mConnection);
            isBound = false;
        }
        AnalyticsManager.getInstance().logEvent(new ImportEvent(true, null));
        getApplicationContext().stopService(ImportAuthorsTask_.intent(getApplicationContext()).get());
        toggleButtonAndProgressPanels(false);
    }

    @Override
    public void onDirectoryChooserResult(int resultCode, String fileToImport) {
        if (resultCode == DirectoryChooserController.RESULT_CODE_DIR_SELECTED) {
            tryParseOutTheChosenFile(fileToImport);
            progressBar.setVisibility(View.VISIBLE);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (this.isImportServiceRunning(ImportAuthorsTask_.class)) {
            Intent importSvc = ImportAuthorsTask_.intent(getApplicationContext()).get();
            getApplicationContext().bindService(importSvc, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EventBus.getDefault().register(this);

        dirChooserController = new DirectoryChooserController(this, null, false);
        dirChooserController.setListener(this);
    }

    @AfterViews
    protected void afterViews() {
        Toolbar toolbar = getActionBarToolbar();
        toolbar.setTitle(R.string.title_import);
        toolbar.setNavigationIcon(R.drawable.ic_up);
        toolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                navigateUpOrBack(ImportAuthorsActivity.this, SiMainActivity_.class);
            }
        });
    }

    @Override
    public void onStop() {
        super.onStop();
        if (isBound) {
            getApplicationContext().unbindService(mConnection);
            isBound = false;
        }
    }

    @Background
    void tryParseOutTheChosenFile(@NotNull String fileName) {
        showParseResults(new AuthorFileImportContext().getAuthorListFromFile(fileName));
    }

    public void onEventMainThread(@NotNull ImportUpdates event) {
        if (event.isFinished()) {
            //toggleButtonAndProgressPanels(false);
            SiMainActivity_.intent(this)
                    .authorsProcessed(event.getImportProgress().getTotalAuthors())
                    .authorsSuccessfullyImported(event.getImportProgress().getSuccessfullyImported())
                    .start();
            this.finish();
        } else if (progressPanel != null) {
            //Update progress
            progressPanel.updateProgress(event.getImportProgress());
        }
    }

    private void toggleButtonAndProgressPanels(boolean inProgress) {
        if (inProgress) {
            this.buttonPanel.setVisibility(View.GONE);
            if (this.progressPanel == null) {
                this.progressPanel = ImportProgressView_.build(this);
                this.actionsContainer.addView(progressPanel);
            }
            this.progressPanel.setVisibility(View.VISIBLE);
        } else {
            this.buttonPanel.setVisibility(View.VISIBLE);
            this.progressPanel.setVisibility(View.GONE);
        }
    }

    @UiThread
    void showParseResults(@NotNull List<String> authorLinks) {
        if (authorLinks.size() > 0) {
            authorsToImport = authorLinks;
            list.setAdapter(new ArrayAdapter<String>(getApplicationContext(), android.R.layout.simple_list_item_1, authorLinks) {
                @Override
                public View getView(int position, View convertView,
                                    ViewGroup parent) {
                    View view = super.getView(position, convertView, parent);
                    TextView textView = (TextView) view.findViewById(android.R.id.text1);
                    textView.setTextColor(Color.BLACK);
                    return view;
                }
            });
            performImportButton.setEnabled(true);
        } else {
            list.setAdapter(null);
            authorsToImport.clear();
            performImportButton.setEnabled(false);
            Snackbar.make(findViewById(R.id.main_content),
                    getResources().getString(R.string.cannot_import_authors_from_file),
                    Snackbar.LENGTH_LONG)
                    .show();
        }
        progressBar.setVisibility(View.GONE);
    }

    private boolean isImportServiceRunning(@NotNull Class<?> serviceClass) {
        for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
