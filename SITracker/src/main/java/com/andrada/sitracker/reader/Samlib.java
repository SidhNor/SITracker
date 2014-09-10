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

package com.andrada.sitracker.reader;

import android.util.Log;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.R;
import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.db.dao.AuthorDao;
import com.andrada.sitracker.db.dao.PublicationDao;
import com.andrada.sitracker.db.manager.SiDBHelper;
import com.andrada.sitracker.exceptions.AddAuthorException;
import com.andrada.sitracker.util.AnalyticsHelper;
import com.andrada.sitracker.util.LogUtils;
import com.andrada.sitracker.util.SamlibPageHelper;
import com.github.kevinsawicki.http.HttpRequest;
import com.j256.ormlite.dao.ForeignCollection;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.concurrent.Callable;

class Samlib implements SiteStrategy {

    private final SiDBHelper helper;

    public Samlib(SiDBHelper helper) {
        this.helper = helper;
    }

    @Override
    public int addAuthorForUrl(String url) {
        Author author = null;
        int message = -1;
        try {
            if (url.equals("") || !url.matches(Constants.SIMPLE_URL_REGEX)) {
                throw new MalformedURLException();
            }
            url = url.replace("zhurnal.lib.ru", "samlib.ru");

            if (!url.endsWith(Constants.AUTHOR_PAGE_URL_ENDING_WO_SLASH) && !url.endsWith(Constants.AUTHOR_PAGE_ALT_URL_ENDING_WO_SLASH)) {
                url = (url.endsWith("/")) ? url + Constants.AUTHOR_PAGE_URL_ENDING_WO_SLASH : url + Constants.AUTHOR_PAGE_URL_ENDING_WI_SLASH;
            }

            if (!url.startsWith(Constants.HTTP_PROTOCOL) && !url.startsWith(Constants.HTTPS_PROTOCOL)) {
                url = Constants.HTTP_PROTOCOL + url;
            }
            String urlId = SamlibPageHelper.getUrlIdFromCompleteUrl(url);
            if (helper.getAuthorDao().queryBuilder().where().eq("urlId", urlId).query().size() != 0) {
                throw new AddAuthorException(AddAuthorException.AuthorAddErrors.AUTHOR_ALREADY_EXISTS);
            }

            HttpRequest request = HttpRequest.get(new URL(url));
            if (request.code() == 404) {
                throw new MalformedURLException();
            }
            AuthorPageReader reader = new SamlibAuthorPageReader(request.body());
            author = reader.getAuthor(url);
            helper.getAuthorDao().create(author);
            final List<Publication> items = reader.getPublications(author);
            if (items.size() == 0) {
                helper.getAuthorDao().delete(author);
                throw new AddAuthorException(AddAuthorException.AuthorAddErrors.AUTHOR_NO_PUBLICATIONS);
            }

            helper.getPublicationDao().callBatchTasks(new Callable<Object>() {
                @Override
                public Object call() throws Exception {
                    for (Publication publication : items) {
                        helper.getPublicationDao().create(publication);
                    }
                    return null;
                }
            });

        } catch (HttpRequest.HttpRequestException e) {
            message = R.string.cannot_add_author_network;
        } catch (MalformedURLException e) {
            message = R.string.cannot_add_author_malformed;
        } catch (SQLException e) {
            if (author != null) {
                try {
                    helper.getAuthorDao().delete(author);
                } catch (SQLException e1) {
                    //Swallow the exception as the author just wasn't saved
                }
            }
            message = R.string.cannot_add_author_internal;
        } catch (AddAuthorException e) {
            switch (e.getError()) {
                case AUTHOR_ALREADY_EXISTS:
                    message = R.string.cannot_add_author_already_exits;
                    break;
                case AUTHOR_DATE_NOT_FOUND:
                    message = R.string.cannot_add_author_no_update_date;
                    break;
                case AUTHOR_NAME_NOT_FOUND:
                    message = R.string.cannot_add_author_no_name;
                    break;
                case AUTHOR_NO_PUBLICATIONS:
                    message = R.string.cannot_add_author_no_publications;
                    break;
                default:
                    message = R.string.cannot_add_author_unknown;
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return message;
    }

    @Override
    public boolean updateAuthor(Author author) throws SQLException {
        boolean authorUpdated = false;
        HttpRequest request;
        AuthorPageReader reader;
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
            reader = new SamlibAuthorPageReader(request.body());
            //We go a blank response but no exception, skip author
            if (reader.isPageBlank()) {
                return false;
            }
            AnalyticsHelper.getInstance().sendEvent(
                    Constants.GA_BGR_CATEGORY,
                    Constants.GA_EVENT_AUTHOR_UPDATE,
                    author.getName());
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
        AuthorDao authorDao = null;
        PublicationDao publicationsDao = null;

        try {
            publicationsDao = helper.getDao(Publication.class);
        } catch (SQLException e) {
            Log.e("Samlib", "Could not create DAO publicationsDao", e);
        }
        try {
            authorDao = helper.getDao(Author.class);
        } catch (SQLException e) {
            Log.e("Samlib", "Could not create DAO authorDao", e);
        }

        assert authorDao != null;
        assert publicationsDao != null;

        String authImgUrl = reader.getAuthorImageUrl(author.getUrl());
        String authDescription = reader.getAuthorDescription();
        if (authImgUrl != null) author.setAuthorImageUrl(authImgUrl);
        if (authDescription != null) author.setAuthorDescription(authDescription);

        authorDao.update(author);

        ForeignCollection<Publication> oldItems = author.getPublications();
        List<Publication> newItems = reader.getPublications(author);

        HashMap<String, Publication> oldItemsMap = new HashMap<String, Publication>();
        for (Publication oldPub : oldItems) {
            oldItemsMap.put(oldPub.getUrl(), oldPub);
        }

        if (newItems.size() == 0 && oldItemsMap.size() > 1) {
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

    private void trackException(String message) {
        AnalyticsHelper.getInstance().sendException(message);
    }
}
