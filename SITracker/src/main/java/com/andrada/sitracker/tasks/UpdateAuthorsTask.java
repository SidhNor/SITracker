package com.andrada.sitracker.tasks;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;

import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.db.manager.SiDBHelper;
import com.andrada.sitracker.tasks.messages.UpdateFailedIntentMessage;
import com.andrada.sitracker.tasks.messages.UpdateSuccessfulIntentMessage;
import com.andrada.sitracker.util.SamlibPageParser;
import com.github.kevinsawicki.http.HttpRequest;
import com.j256.ormlite.dao.Dao;
import com.j256.ormlite.dao.ForeignCollection;

import org.androidannotations.annotations.EService;
import org.androidannotations.annotations.OrmLiteDao;

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
public class UpdateAuthorsTask extends IntentService  {

    @OrmLiteDao(helper = SiDBHelper.class, model = Author.class)
    Dao<Author, Integer> authorDao;

    @OrmLiteDao(helper = SiDBHelper.class, model = Publication.class)
    Dao<Publication, Integer> publicationsDao;

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
        //Check for updates
        try {
            List<Author> authors = authorDao.queryForAll();
            for (Author author : authors) {
                HttpRequest request = null;
                try {
                    request = HttpRequest.get(new URL(author.getUrl()));
                } catch (MalformedURLException e) {
                    //Just swallow exception, as this is unlikely to happen
                    //SKip author
                    continue;
                }
                if (request.code() == 404) {
                    //skip this author
                    //Not available atm
                    continue;
                }
                String body = SamlibPageParser.sanitizeHTML(request.body());

                ForeignCollection<Publication> oldItems = author.getPublications();
                List<Publication> newItems = SamlibPageParser.getPublications(body, author);

                HashMap<String, Publication> oldItemsMap = new HashMap<String, Publication>();
                for (Publication oldPub : oldItems) {
                    oldItemsMap.put(oldPub.getUrl(), oldPub);
                }

                for (Publication pub : newItems) {
                    //Find pub in oldItems
                    if (oldItemsMap.containsKey(pub.getUrl())) {
                        Publication old = oldItemsMap.get(pub.getUrl());
                        //Check size/name/description
                        if (pub.getSize() != old.getSize() ||
                            !pub.getDescription().equals(old.getDescription()) ||
                            !pub.getName().equals(old.getName()) ) {
                            //if something differs
                            //Store the old size
                            pub.setOldSize(old.getSize());
                            //Swap the ids, do an update in DB
                            pub.setId(old.getId());
                            pub.setNew(true);
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
                        publicationsDao.create(pub);
                    }
                }

                if (oldItems.size() != newItems.size()) {
                    //Find any old publications to remove
                    for (Publication oldItem : oldItems) {
                        if (!newItems.contains(oldItem)) {
                            //Remove from DB
                            publicationsDao.delete(oldItem);
                        }
                    }
                }


            }
            //Success
            //Do a broadcast
            broadCastResult(true);

        } catch (SQLException e) {
            //Error
            //Do a broadcast
            broadCastResult(false);
        }
    }

    private void broadCastResult(boolean success) {
        Intent broadcastIntent;
        if (success) broadcastIntent = new UpdateSuccessfulIntentMessage();
        else broadcastIntent = new UpdateFailedIntentMessage();
        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(broadcastIntent);
    }

}
