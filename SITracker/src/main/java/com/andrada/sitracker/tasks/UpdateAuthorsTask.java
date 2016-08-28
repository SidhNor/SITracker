/*
 *
 * Copyright 2016 Gleb Godonoga.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package com.andrada.sitracker.tasks;

import android.annotation.SuppressLint;
import android.app.IntentService;
import android.app.backup.BackupManager;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.contracts.SIPrefs_;
import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.db.dao.AuthorDao;
import com.andrada.sitracker.db.manager.SiDBHelper;
import com.andrada.sitracker.reader.SiteDetector;
import com.andrada.sitracker.reader.SiteStrategy;
import com.andrada.sitracker.tasks.messages.AuthorsUpToDateIntentMessage;
import com.andrada.sitracker.tasks.messages.UpdateFailedIntentMessage;
import com.andrada.sitracker.tasks.messages.UpdateSuccessfulIntentMessage;
import com.andrada.sitracker.analytics.AnalyticsManager;
import com.j256.ormlite.android.apptools.OpenHelperManager;

import org.androidannotations.annotations.EService;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.sharedpreferences.Pref;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@SuppressLint("Registered")
@EService
public class UpdateAuthorsTask extends IntentService {

    @Pref
    SIPrefs_ prefs;
    @SystemService
    ConnectivityManager connectivityManager;
    private SiDBHelper siDBHelper;
    private int updatedAuthors;

    public UpdateAuthorsTask() {
        super(UpdateAuthorsTask.class.getSimpleName());
    }

    @Override
    public void onCreate() {
        super.onCreate();
        siDBHelper = OpenHelperManager.getHelper(this, SiDBHelper.class);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        OpenHelperManager.releaseHelper();
    }

    /**
     * The IntentService calls this method from the default worker thread with
     * the intent that started the service. When this method returns, IntentService
     * stops the service, as appropriate.
     */
    @Override
    protected void onHandleIntent(@NotNull Intent intent) {

        boolean isNetworkIgnore = intent.getBooleanExtra(Constants.UPDATE_IGNORES_NETWORK, false);

        final long timeDiff = new Date().getTime() - prefs.lastUpdateDateTime().get();
        if (timeDiff < Constants.MIN_UPDATE_SPAN) {
            broadcastUpToDate();
            return;
        }


        //Check for updates
        this.updatedAuthors = 0;
        ArrayList<String> authorsUpdatedInThisSession = new ArrayList<String>();
        try {
            AuthorDao dao = siDBHelper.getAuthorDao();
            if (dao == null) {
                //Something went wrong.
                broadcastResult(false, null);
                return;
            }
            List<Author> authors = dao.queryForAll();
            for (Author author : authors) {
                boolean useWiFiOnly = prefs.updateOnlyWiFi().get();
                if (this.isConnected() &&
                        (isNetworkIgnore ||
                                (!useWiFiOnly || this.isConnectedToWiFi()))) {
                    SiteStrategy strategy = SiteDetector.chooseStrategy(author.getUrl(), siDBHelper);
                    if (strategy != null && strategy.updateAuthor(author)) {
                        this.updatedAuthors++;
                        authorsUpdatedInThisSession.add(author.getName());
                        //Do a broadcast for each update author - notification will update itself
                        broadcastResult(true, authorsUpdatedInThisSession);
                    }
                }
                //Sleep for 5 seconds to avoid ban
                Thread.sleep(5000);
            }
            prefs.lastUpdateDateTime().put(new Date().getTime());

        } catch (SQLException e) {
            //Error
            //Do a broadcast
            broadcastResult(false, null);
            trackException(e.getMessage());
        } catch (InterruptedException e) {
            //Ignore
            trackException(e.getMessage());
        }
    }

    private void broadcastResult(boolean success, ArrayList<String> updateAuthorsInThisSession) {
        Intent broadcastIntent = new Intent();
        if (success) {
            broadcastIntent.setAction(UpdateSuccessfulIntentMessage.SUCCESS_MESSAGE);
            broadcastIntent.putExtra(Constants.NUMBER_OF_UPDATED_AUTHORS, this.updatedAuthors);
            broadcastIntent.putStringArrayListExtra(Constants.AUTHOR_NAMES_UPDATED_IN_SESSION, updateAuthorsInThisSession);
            BackupManager bm = new BackupManager(this.getApplicationContext());
            bm.dataChanged();
        } else {
            broadcastIntent = broadcastIntent.setAction(UpdateFailedIntentMessage.FAILED_MESSAGE);
        }
        sendOrderedBroadcast(broadcastIntent, null);
    }

    private void broadcastUpToDate() {
        Intent broadcastIntent = new Intent();
        broadcastIntent = broadcastIntent.setAction(AuthorsUpToDateIntentMessage.UP_TO_DATE_MESSAGE);
        sendOrderedBroadcast(broadcastIntent, null);
    }

    private boolean isConnected() {
        final NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return (activeNetwork != null && activeNetwork.isConnected());
    }

    private boolean isConnectedToWiFi() {
        final NetworkInfo activeNetwork = connectivityManager.getActiveNetworkInfo();
        return (activeNetwork != null &&
                activeNetwork.isConnected() &&
                activeNetwork.getType() == ConnectivityManager.TYPE_WIFI);
    }

    private void trackException(String message) {
        AnalyticsManager.getInstance().sendException(message);
    }

}
