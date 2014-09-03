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


import android.app.ActivityManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.graphics.Color;
import android.os.Bundle;
import android.os.IBinder;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.R;
import com.andrada.sitracker.events.ImportUpdates;
import com.andrada.sitracker.tasks.ImportAuthorsTask;
import com.andrada.sitracker.tasks.ImportAuthorsTask_;
import com.andrada.sitracker.tasks.io.AuthorFileImportContext;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EActivity;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.UiThread;
import org.androidannotations.annotations.ViewById;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;
import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;

@EActivity(R.layout.activity_import)
public class ImportAuthorsActivity extends BaseActivity {

    @ViewById
    ViewGroup progressPanel;

    @ViewById
    ViewGroup buttonPanel;

    @ViewById
    Button chooseFileButton;

    @ViewById
    Button performImportButton;

    @ViewById
    ProgressBar progressBar;

    @ViewById
    ListView list;

    @ViewById
    ProgressBar importProgressBar;

    @ViewById
    TextView progressValue;

    @SystemService
    ActivityManager activityManager;

    ImportAuthorsTask importTask;
    private boolean isBound = false;
    private List<String> authorsToImport;

    private ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className,
                                       IBinder service) {
            // We've bound to LocalService, cast the IBinder and get LocalService instance
            ImportAuthorsTask.ImportAuthorsBinder binder = (ImportAuthorsTask.ImportAuthorsBinder) service;
            importTask = binder.getService();
            isBound = true;
            authorsToImport = importTask.getAuthorsList();
            if (list.getAdapter() == null || list.getAdapter().getCount() == 0) {
                showParseResults(authorsToImport);
            }
            toggleButtonAndProgressPanels(true);
            onEventMainThread(new ImportUpdates(importTask.getCurrentProgress()));
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
        final Intent chooserIntent = new Intent(getApplicationContext(), DirectoryChooserActivity.class);
        chooserIntent.putExtra(DirectoryChooserActivity.EXTRA_NEW_DIR_NAME, "Books");
        chooserIntent.putExtra(DirectoryChooserActivity.EXTRA_IS_DIRECTORY_CHOOSER, false);
        startActivityForResult(chooserIntent, Constants.REQUEST_DIRECTORY);
    }

    @Click(R.id.performImportButton)
    void importParsedAuthors() {
        if (authorsToImport != null) {
            progressValue.setText(getResources().getString(R.string.import_progress_indication,
                    0, authorsToImport.size()));

            Intent importSvc = ImportAuthorsTask_.intent(getApplicationContext()).get();
            importSvc.putStringArrayListExtra(ImportAuthorsTask.AUTHOR_LIST_EXTRA, new ArrayList<String>(authorsToImport));
            getApplicationContext().startService(importSvc);
            getApplicationContext().bindService(importSvc, mConnection, Context.BIND_AUTO_CREATE);
        }
    }

    @Click(R.id.cancelImportButton)
    void importCancelRequested() {
        if (isBound) {
            this.importTask.cancelImport();
            getApplicationContext().unbindService(mConnection);
            isBound = false;
        }
        getApplicationContext().stopService(ImportAuthorsTask_.intent(getApplicationContext()).get());
        toggleButtonAndProgressPanels(false);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == Constants.REQUEST_DIRECTORY) {
            if (resultCode == DirectoryChooserActivity.RESULT_CODE_DIR_SELECTED) {
                String fileToImport = data.getStringExtra(DirectoryChooserActivity.RESULT_SELECTED_DIR);
                tryParseOutTheChosenFile(fileToImport);
                progressBar.setVisibility(View.VISIBLE);
            }
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
    void tryParseOutTheChosenFile(String fileName) {
        showParseResults(new AuthorFileImportContext().getAuthorListFromFile(fileName));
    }

    public void onEventMainThread(ImportUpdates event) {
        if (event.isFinished()) {
            toggleButtonAndProgressPanels(false);
            //TODO send back to home activity
        } else {
            //Update progress
            importProgressBar.setMax(event.getImportProgress().getTotalAuthors());
            importProgressBar.setProgress(event.getImportProgress().getTotalProcessed());
            progressValue.setText(getResources().getString(R.string.import_progress_indication,
                    event.getImportProgress().getTotalProcessed(),
                    event.getImportProgress().getTotalAuthors()));
        }
    }

    private void toggleButtonAndProgressPanels(boolean inProgress) {
        if (inProgress) {
            this.buttonPanel.setVisibility(View.GONE);
            this.progressPanel.setVisibility(View.VISIBLE);
        } else {
            this.buttonPanel.setVisibility(View.VISIBLE);
            this.progressPanel.setVisibility(View.GONE);
        }
    }

    @UiThread
    void showParseResults(List<String> authorLinks) {
        if (authorLinks != null && authorLinks.size() > 0) {
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
            authorsToImport = null;
            performImportButton.setEnabled(false);
            Crouton.makeText(this, getResources().getString(R.string.cannot_import_authors_from_file), Style.ALERT).show();
        }
        progressBar.setVisibility(View.GONE);
    }

    private boolean isImportServiceRunning(Class<?> serviceClass) {
        for (ActivityManager.RunningServiceInfo service : activityManager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }
}
