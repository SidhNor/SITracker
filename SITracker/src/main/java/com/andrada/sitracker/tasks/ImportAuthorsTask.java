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

package com.andrada.sitracker.tasks;

import android.app.IntentService;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Binder;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;

import com.andrada.sitracker.R;
import com.andrada.sitracker.db.manager.SiDBHelper;
import com.andrada.sitracker.events.ImportUpdates;
import com.andrada.sitracker.reader.SiteDetector;
import com.andrada.sitracker.reader.SiteStrategy;
import com.andrada.sitracker.ui.HomeActivity_;
import com.andrada.sitracker.ui.ImportAuthorsActivity_;
import com.j256.ormlite.android.apptools.OpenHelperManager;
import com.j256.ormlite.support.DatabaseConnection;

import org.androidannotations.annotations.EService;
import org.androidannotations.annotations.SystemService;

import java.util.ArrayList;
import java.util.List;

import de.greenrobot.event.EventBus;

import static com.andrada.sitracker.util.LogUtils.LOGW;
import static com.andrada.sitracker.util.LogUtils.makeLogTag;

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
    private List<String> authorsList;
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

    @Override
    public IBinder onBind(Intent intent) {
        return mBinder;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (intent == null) {
            return;
        }
        setupExtras(intent);

        this.importProgress = new ImportProgress(authorsList.size());

        NotificationCompat.Builder mBuilder =
                new NotificationCompat.Builder(this)
                        .setSmallIcon(android.R.drawable.stat_notify_sync)
                        .setOngoing(true)
                        .setAutoCancel(false)
                        .setContentTitle(getResources().getString(R.string.notification_import_title))
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

        DatabaseConnection conn = null;
        for (String authUrl : authorsList) {
            try {
                if (shouldCancel) {
                    break;
                }
                SiteStrategy strategy = SiteDetector.chooseStrategy(authUrl, helper);
                int returnMsg = strategy.addAuthorForUrl(authUrl);
                if (returnMsg == -1) {
                    this.importProgress.importSuccess();
                } else {
                    this.importProgress.importFail(authUrl);
                }
                EventBus.getDefault().post(new ImportUpdates(new ImportProgress(this.importProgress)));
                mBuilder.setContentText(getResources()
                        .getString(R.string.notification_import_progress, importProgress.getTotalProcessed(), importProgress.getTotalAuthors()))
                        .setAutoCancel(false)
                        .setProgress(importProgress.getTotalAuthors(), importProgress.getTotalProcessed(), false);
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
            Intent finishIntent = HomeActivity_.intent(this)
                    .authorsProcessed(importProgress.getTotalAuthors())
                    .authorsSuccessfullyImported(importProgress.getSuccessfullyImported())
                    .get();
            // The stack builder object will contain an artificial back stack for the
            // started Activity.
            // This ensures that navigating backward from the Activity leads out of
            // your application to the Home screen.
            stackBuilder = TaskStackBuilder.create(this);
            // Adds the back stack for the Intent (but not the Intent itself)
            stackBuilder.addParentStack(HomeActivity_.class);
            // Adds the Intent that starts the Activity to the top of the stack
            stackBuilder.addNextIntent(finishIntent);

            mBuilder.setProgress(0, 0, false)
                    .setOngoing(false)
                    .setAutoCancel(true)
                    .setContentText(getResources().getString(R.string.notification_import_complete))
                    .setContentIntent(stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT));
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

    public List<String> getAuthorsList() {
        return authorsList;
    }

    private void setupExtras(Intent intent) {
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

        public ImportProgress(ImportProgress copy) {
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
        public ImportAuthorsTask getService() {
            return ImportAuthorsTask.this;
        }
    }
}
