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

package com.andrada.sitracker.tasks;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.backup.BackupManager;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.R;
import com.andrada.sitracker.db.manager.SiDBHelper;
import com.andrada.sitracker.events.ImportUpdates;
import com.andrada.sitracker.reader.SiteDetector;
import com.andrada.sitracker.reader.SiteStrategy;
import com.andrada.sitracker.ui.ImportAuthorsActivity_;
import com.andrada.sitracker.ui.SiMainActivity_;
import com.andrada.sitracker.util.AnalyticsHelper;
import com.andrada.sitracker.util.SamlibPageHelper;
import com.j256.ormlite.android.apptools.OpenHelperManager;

import org.androidannotations.annotations.EService;
import org.androidannotations.annotations.SystemService;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import de.greenrobot.event.EventBus;

import static com.andrada.sitracker.util.LogUtils.LOGW;
import static com.andrada.sitracker.util.LogUtils.makeLogTag;

@SuppressLint("Registered")
@EService
public class ImportAuthorsTask extends IntentService {

    public static final String AUTHOR_LIST_EXTRA = "authorsList";

    private final static int NOTIFICATION_ID = 628986143;

    private static final String TAG = makeLogTag(ImportAuthorsTask.class);

    private final IBinder mBinder = new ImportAuthorsBinder();

    @SystemService
    ConnectivityManager connectivityManager;

    @SystemService
    NotificationManager notificationManager;

    private volatile boolean shouldCancel = false;

    private SiDBHelper helper;

    @NotNull
    private List<String> authorsList = new ArrayList<String>();

    private ImportProgress importProgress;

    public ImportAuthorsTask() {
        super(ImportAuthorsTask.class.getSimpleName());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        helper = OpenHelperManager.getHelper(this, SiDBHelper.class);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        OpenHelperManager.releaseHelper();
    }

    @NotNull
    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    protected void onHandleIntent(@Nullable Intent intent) {
        if (intent == null) {
            return;
        }
        setupExtras(intent);

        this.importProgress = new ImportProgress(authorsList.size());

        //Filter out duplicates right away
        try {
            List<String> urlIds = helper.getAuthorDao().getAuthorsUrlIds();
            Map<String, String> prospectAuthorIds = new HashMap<String, String>();
            for (String auth : authorsList) {
                prospectAuthorIds.put(SamlibPageHelper.getUrlIdFromCompleteUrl(auth), auth);
            }
            for (String urlId : urlIds) {
                if (prospectAuthorIds.containsKey(urlId)) {
                    authorsList.remove(prospectAuthorIds.get(urlId));
                    this.importProgress.importFail(urlId);
                }
            }
            if (authorsList.size() == 0) {
                EventBus.getDefault().post(new ImportUpdates(this.importProgress));
            }
        } catch (SQLException e) {
            LOGW(TAG, "Failed to filter out duplicate authors", e);
        }

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(android.R.drawable.stat_notify_sync)
                        .setOngoing(true)
                        .setAutoCancel(false)
                        .setContentTitle(
                                getResources().getString(R.string.notification_import_title))
                        .setAutoCancel(true);

        // Creates an explicit intent for an Activity in your app
        Intent resultIntent = new Intent(this, ImportAuthorsActivity_.class);
        // The stack builder object will contain an artificial back stack for the
        // started Activity.
        // This ensures that navigating backward from the Activity leads out of
        // your application to the Home screen.
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        // Adds the back stack for the Intent (but not the Intent itself)
        stackBuilder.addParentStack(ImportAuthorsActivity_.class);
        // Adds the Intent that starts the Activity to the top of the stack
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent =
                stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        mBuilder.setContentIntent(resultPendingIntent);

        for (String authUrl : authorsList) {
            try {
                if (shouldCancel) {
                    //Make sure to cancel it here as well
                    notificationManager.cancel(NOTIFICATION_ID);
                    break;
                }
                SiteStrategy strategy = SiteDetector.chooseStrategy(authUrl, helper);
                if (strategy == null) {
                    this.importProgress.importFail(authUrl);
                } else {
                    int returnMsg = strategy.addAuthorForUrl(authUrl);
                    if (returnMsg == -1) {
                        this.importProgress.importSuccess();
                    } else {
                        this.importProgress.importFail(authUrl);
                    }
                }

                if (shouldCancel) {
                    //Make sure to cancel it here as well
                    notificationManager.cancel(NOTIFICATION_ID);
                    break;
                }
                EventBus.getDefault()
                        .post(new ImportUpdates(new ImportProgress(this.importProgress)));
                mBuilder.setContentText(getResources()
                        .getString(R.string.notification_import_progress,
                                importProgress.getTotalProcessed(),
                                importProgress.getTotalAuthors()))
                        .setAutoCancel(false)
                        .setProgress(importProgress.getTotalAuthors(),
                                importProgress.getTotalProcessed(), false);
                notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
                //Sleep for 5 seconds to avoid ban
                Thread.sleep(5000);

                if (!shouldCancel) {
                    EventBus.getDefault().post(new ImportUpdates(this.importProgress));
                }
            } catch (InterruptedException e) {
                LOGW(TAG, "Importing was forcibly stopped", e);
            }
        }
        if (!shouldCancel) {
            Intent finishIntent = SiMainActivity_.intent(this)
                    .authorsProcessed(importProgress.getTotalAuthors())
                    .authorsSuccessfullyImported(importProgress.getSuccessfullyImported())
                    .get();
            // The stack builder object will contain an artificial back stack for the
            // started Activity.
            // This ensures that navigating backward from the Activity leads out of
            // your application to the Home screen.
            stackBuilder = TaskStackBuilder.create(this);
            // Adds the back stack for the Intent (but not the Intent itself)
            stackBuilder.addParentStack(SiMainActivity_.class);
            // Adds the Intent that starts the Activity to the top of the stack
            stackBuilder.addNextIntent(finishIntent);

            BackupManager bm = new BackupManager(this);
            bm.dataChanged();

            AnalyticsHelper.getInstance().sendEvent(
                    Constants.GA_ADMIN_CATEGORY,
                    Constants.GA_EVENT_AUTHOR_IMPORT,
                    Constants.GA_EVENT_IMPORT_COMPLETE,
                    importProgress.getTotalAuthors());

            mBuilder.setProgress(0, 0, false)
                    .setOngoing(false)
                    .setAutoCancel(true)
                    .setContentText(getResources().getString(R.string.notification_import_complete))
                    .setContentIntent(
                            stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT));
            notificationManager.notify(NOTIFICATION_ID, mBuilder.build());
        }
    }

    public void cancelImport() {
        this.shouldCancel = true;
        notificationManager.cancel(NOTIFICATION_ID);
    }

    public ImportProgress getCurrentProgress() {
        return this.importProgress;
    }

    @NotNull
    public List<String> getAuthorsList() {
        return authorsList;
    }

    private void setupExtras(@NotNull Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null) {
            if (extras.containsKey(AUTHOR_LIST_EXTRA)) {
                authorsList = (List<String>) extras.getSerializable(AUTHOR_LIST_EXTRA);
            }
        }
    }

    public static class ImportProgress {

        private int totalAuthors = 0;

        private int successfullyImported = 0;

        private int failedImport = 0;

        private int totalProcessed = 0;

        private List<String> failedAuthors = new ArrayList<String>();

        public ImportProgress(int totalAuthors) {
            this.totalAuthors = totalAuthors;
        }

        public ImportProgress(@NotNull ImportProgress copy) {
            this.totalAuthors = copy.totalAuthors;
            this.successfullyImported = copy.successfullyImported;
            this.failedImport = copy.failedImport;
            this.totalProcessed = copy.totalProcessed;
            this.failedAuthors.addAll(copy.getFailedAuthors());
        }

        public int getTotalProcessed() {
            return totalProcessed;
        }

        public int getTotalAuthors() {
            return totalAuthors;
        }

        public int getSuccessfullyImported() {
            return successfullyImported;
        }

        public int getFailedImport() {
            return failedImport;
        }

        @NotNull
        public List<String> getFailedAuthors() {
            return failedAuthors;
        }

        void importSuccess() {
            this.totalProcessed++;
            this.successfullyImported++;
        }

        void importFail(String authorUrl) {
            this.totalProcessed++;
            this.failedImport++;
            this.failedAuthors.add(authorUrl);
        }
    }

    public class ImportAuthorsBinder extends Binder {

        @NotNull
        public ImportAuthorsTask getService() {
            return ImportAuthorsTask.this;
        }
    }
}
