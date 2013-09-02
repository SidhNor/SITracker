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

package com.andrada.sitracker.tasks;

import android.app.IntentService;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.contracts.SIPrefs_;
import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.db.dao.AuthorDao;
import com.andrada.sitracker.db.dao.PublicationDao;
import com.andrada.sitracker.db.manager.SiDBHelper;
import com.andrada.sitracker.tasks.messages.UpdateFailedIntentMessage;
import com.andrada.sitracker.tasks.messages.UpdateSuccessfulIntentMessage;
import com.andrada.sitracker.util.LogUtils;
import com.andrada.sitracker.util.SamlibPageParser;
import com.github.kevinsawicki.http.HttpRequest;
import com.google.analytics.tracking.android.EasyTracker;
import com.j256.ormlite.dao.ForeignCollection;

import org.androidannotations.annotations.EService;
import org.androidannotations.annotations.OrmLiteDao;
import org.androidannotations.annotations.SystemService;
import org.androidannotations.annotations.sharedpreferences.Pref;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

@EService
public class UpdateAuthorsTask extends IntentService {

    @OrmLiteDao(helper = SiDBHelper.class, model = Author.class)
    AuthorDao authorDao;

    @OrmLiteDao(helper = SiDBHelper.class, model = Publication.class)
    PublicationDao publicationsDao;

    @Pref
    SIPrefs_ prefs;

    @SystemService
    ConnectivityManager connectivityManager;

    private int updatedAuthors;

    public UpdateAuthorsTask() {
        super(UpdateAuthorsTask.class.getSimpleName());
    }

    /**
     * The IntentService calls this method from the default worker thread with
     * the intent that started the service. When this method returns, IntentService
     * stops the service, as appropriate.
     */
    @Override
    protected void onHandleIntent(Intent intent) {

        boolean isNetworkIgnore = intent.getBooleanExtra(Constants.UPDATE_IGNORES_NETWORK, false);

        EasyTracker.getInstance().setContext(this.getApplicationContext());

        //Check for updates
        this.updatedAuthors = 0;
        try {
            List<Author> authors = authorDao.queryForAll();
            for (Author author : authors) {
                boolean useWiFiOnly = prefs.updateOnlyWiFi().get();
                if (this.isConnected() &&
                        (isNetworkIgnore ||
                                (!useWiFiOnly || this.isConnectedToWiFi()))) {
                    if (updateAuthor(author)) {
                        this.updatedAuthors++;
                    }
                }
                //Sleep for 5 seconds to avoid ban
                Thread.sleep(5000);
            }

            //Success
            //Do a broadcast
            broadCastResult(true);

        } catch (SQLException e) {
            //Error
            //Do a broadcast
            broadCastResult(false);
            trackException(e.getMessage());
        } catch (InterruptedException e) {
            //Ignore
            trackException(e.getMessage());
        }
    }

    private boolean updateAuthor(Author author) throws SQLException {
        boolean authorUpdated = false;
        HttpRequest request;
        String body;
        try {
            URL authorURL = new URL(author.getUrl());
            request = HttpRequest.get(authorURL);
            if (request.code() == 404) {
                //skip this author
                //Not available atm
                return false;
            }
            if (!authorURL.getHost().equals(request.url().getHost())) {
                //We are being redirected hell knows where.
                //Skip
                return false;
            }
            body = SamlibPageParser.sanitizeHTML(request.body());

            EasyTracker.getTracker().sendEvent(
                    Constants.GA_BGR_CATEGORY,
                    Constants.GA_EVENT_AUTHOR_UPDATE,
                    author.getName(), null);
            EasyTracker.getInstance().dispatch();
        } catch (MalformedURLException e) {
            //Just swallow exception, as this is unlikely to happen
            //Skip author
            trackException(e.getMessage());
            return false;
        } catch (HttpRequest.HttpRequestException e) {
            //Author currently inaccessible or no internet
            //Skip author
            trackException(e.getMessage());
            return false;
        }

        String authImgUrl = SamlibPageParser.getAuthorImageUrl(body, author.getUrl());
        String authDescription = SamlibPageParser.getAuthorDescription(body);
        if (authImgUrl != null) author.setAuthorImageUrl(authImgUrl);
        if (authDescription != null) author.setAuthorDescription(authDescription);
        authorDao.update(author);

        ForeignCollection<Publication> oldItems = author.getPublications();
        List<Publication> newItems = SamlibPageParser.getPublications(body, author);

        HashMap<String, Publication> oldItemsMap = new HashMap<String, Publication>();
        for (Publication oldPub : oldItems) {
            oldItemsMap.put(oldPub.getUrl(), oldPub);
        }

        if (newItems.size() == 0 && oldItemsMap.size() > 1) {
            EasyTracker.getTracker().sendException(
                    "Something went wrong. Publications are empty.", false);
            LogUtils.LOGW(Constants.APP_TAG, "Something went wrong. No publications found for author that already exists");
            //Just skip for now to be on the safe side.
            return false;
        }

        for (Publication pub : newItems) {
            //Find pub in oldItems
            if (oldItemsMap.containsKey(pub.getUrl())) {
                Publication old = oldItemsMap.get(pub.getUrl());
                //Check size/name/description
                if (pub.getSize() != old.getSize() ||
                        !pub.getDescription().equals(old.getDescription()) ||
                        !pub.getName().equals(old.getName())) {
                    //if something differs
                    //Store the old size
                    pub.setOldSize(old.getSize());
                    //Swap the ids, do an update in DB
                    pub.setId(old.getId());
                    pub.setNew(true);
                    authorUpdated = true;
                    publicationsDao.update(pub);
                    //Mark author new, update in DB
                    author.setUpdateDate(new Date());
                    author.setNew(true);
                    authorDao.update(author);
                }
            } else {
                //Mark author new, update in DB
                author.setUpdateDate(new Date());
                author.setNew(true);
                authorDao.update(author);
                //Mark publication new, create in DB
                pub.setNew(true);
                authorUpdated = true;
                publicationsDao.create(pub);
            }
        }

        return authorUpdated;
    }

    private void broadCastResult(boolean success) {
        Intent broadcastIntent = new Intent();
        if (success) {
            broadcastIntent.setAction(UpdateSuccessfulIntentMessage.SUCCESS_MESSAGE);
            broadcastIntent.putExtra(Constants.NUMBER_OF_UPDATED_AUTHORS, this.updatedAuthors);
        } else {
            broadcastIntent = broadcastIntent.setAction(UpdateFailedIntentMessage.FAILED_MESSAGE);
        }
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
        EasyTracker.getTracker().sendException(message, false);
    }

}
