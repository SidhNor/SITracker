package com.andrada.sitracker.tasks;

import android.app.IntentService;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.preference.PreferenceManager;
import android.util.Log;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.db.dao.AuthorDao;
import com.andrada.sitracker.db.dao.PublicationDao;
import com.andrada.sitracker.db.manager.SiDBHelper;
import com.andrada.sitracker.tasks.messages.UpdateFailedIntentMessage;
import com.andrada.sitracker.tasks.messages.UpdateSuccessfulIntentMessage;
import com.andrada.sitracker.util.SamlibPageParser;
import com.github.kevinsawicki.http.HttpRequest;
import com.google.analytics.tracking.android.EasyTracker;
import com.j256.ormlite.dao.ForeignCollection;

import org.androidannotations.annotations.EService;
import org.androidannotations.annotations.OrmLiteDao;
import org.androidannotations.annotations.SystemService;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

/**
 * Created by Gleb on 03.06.13.
 */

@EService
public class UpdateAuthorsTask extends IntentService {

    @OrmLiteDao(helper = SiDBHelper.class, model = Author.class)
    AuthorDao authorDao;

    @OrmLiteDao(helper = SiDBHelper.class, model = Publication.class)
    PublicationDao publicationsDao;

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
                boolean useWiFiOnly = PreferenceManager.getDefaultSharedPreferences(this)
                        .getBoolean(Constants.UPDATE_NETWORK_KEY, false);
                if (this.isConnected() &&
                        (isNetworkIgnore ||
                                (!useWiFiOnly || this.isConnectedToWiFi()))) {
                    updateAuthor(author);
                }
                //Sleep for 5 seconds to avoid ban from samlib
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
        HttpRequest request;
        try {
            request = HttpRequest.get(new URL(author.getUrl()));
            if (request.code() == 404) {
                //skip this author
                //Not available atm
                return false;
            }
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
        String body = SamlibPageParser.sanitizeHTML(request.body());

        ForeignCollection<Publication> oldItems = author.getPublications();
        List<Publication> newItems = SamlibPageParser.getPublications(body, author);

        HashMap<String, Publication> oldItemsMap = new HashMap<String, Publication>();
        for (Publication oldPub : oldItems) {
            oldItemsMap.put(oldPub.getUrl(), oldPub);
        }

        if (newItems.size() == 0 && oldItemsMap.size() > 1) {
            EasyTracker.getTracker().sendException(
                    "Publications are empty. Response code: " + request.code() +
                            ". Response size:" + request.body().getBytes().length, false);
            Log.w(Constants.APP_TAG, "Something went wrong. No publications found for author that already exists");
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
                    this.updatedAuthors++;
                    publicationsDao.update(pub);
                    //Mark author new, update in DB
                    author.setUpdateDate(new Date());
                    authorDao.update(author);
                }
            } else {
                //Mark author new, update in DB
                author.setUpdateDate(new Date());
                authorDao.update(author);
                //Mark publication new, create in DB
                pub.setNew(true);
                this.updatedAuthors++;
                publicationsDao.create(pub);
            }
        }

        return true;
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
