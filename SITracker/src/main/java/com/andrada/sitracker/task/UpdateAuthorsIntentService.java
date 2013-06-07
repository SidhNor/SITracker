package com.andrada.sitracker.task;

import android.app.IntentService;
import android.content.Intent;

import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.db.manager.SiDBHelper;
import com.andrada.sitracker.task.receivers.UpdateBroadcastReceiver;
import com.andrada.sitracker.util.SamlibPageParser;
import com.github.kevinsawicki.http.HttpRequest;
import com.j256.ormlite.dao.Dao;

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
public class UpdateAuthorsIntentService extends IntentService  {

    @OrmLiteDao(helper = SiDBHelper.class, model = Author.class)
    Dao<Author, Integer> authorDao;

    @OrmLiteDao(helper = SiDBHelper.class, model = Publication.class)
    Dao<Publication, Integer> publicationsDao;

    public UpdateAuthorsIntentService() {
        super(UpdateAuthorsIntentService.class.getSimpleName());
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
                    e.printStackTrace();
                }
                if (request.code() == 404) {
                    //skip this author
                    //Not available atm
                    continue;
                }
                String body = SamlibPageParser.sanitizeHTML(request.body());

                List<Publication> oldItems = publicationsDao.queryBuilder().
                        where().eq("authorID", author.getId()).query();
                List<Publication> newItems = SamlibPageParser.getPublications(body, author.getUrl(), author.getId());

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
                            author.setUpdated(true);
                            author.setUpdateDate(new Date());
                            authorDao.update(author);
                        }
                    } else {
                        //Mark author new, update in DB
                        author.setUpdated(true);
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
            broadCastResult();

        } catch (SQLException e) {
            //Error
            //Do a broadcast
            broadCastResult();
            e.printStackTrace();
        }
    }

    private void broadCastResult() {
        Intent broadcastIntent = new Intent();
        broadcastIntent.setAction(UpdateBroadcastReceiver.UPDATE_RECEIVER_ACTION);
        broadcastIntent.addCategory(Intent.CATEGORY_DEFAULT);
        sendBroadcast(broadcastIntent);
    }

}
