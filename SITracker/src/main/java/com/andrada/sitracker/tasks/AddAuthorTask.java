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

import android.content.Context;
import android.os.AsyncTask;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.R;
import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.db.manager.SiDBHelper;
import com.andrada.sitracker.events.AuthorAddedEvent;
import com.andrada.sitracker.exceptions.AddAuthorException;
import com.andrada.sitracker.util.SamlibPageParser;
import com.github.kevinsawicki.http.HttpRequest;
import com.github.kevinsawicki.http.HttpRequest.HttpRequestException;
import com.j256.ormlite.android.apptools.OpenHelperManager;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;

import de.greenrobot.event.EventBus;

public class AddAuthorTask extends AsyncTask<String, Integer, String> {

    private final Context context;
    private SiDBHelper helper;


    public AddAuthorTask(Context context) {
        this.context = context;
    }

    @Override
    protected String doInBackground(String... args) {
        String message = "";
        for (String url : args) {
            message = this.processAuthorAdd(url);
        }
        return message;
    }

    @Override
    protected void onPreExecute() {
        this.helper = OpenHelperManager.getHelper(this.context, SiDBHelper.class);
    }

    @Override
    protected void onPostExecute(String result) {
        OpenHelperManager.releaseHelper();
        EventBus.getDefault().post(new AuthorAddedEvent(result));
    }

    private String processAuthorAdd(String url) {
        Author author = null;
        String message = "";
        try {
            if (url.equals("") || !url.matches(Constants.SIMPLE_URL_REGEX)) {
                throw new MalformedURLException();
            }

            if (!url.endsWith(Constants.AUTHOR_PAGE_URL_ENDING_WO_SLASH)) {
                url = (url.endsWith("/")) ? url + Constants.AUTHOR_PAGE_URL_ENDING_WO_SLASH : url + Constants.AUTHOR_PAGE_URL_ENDING_WI_SLASH;
            }

            if (!url.startsWith(Constants.HTTP_PROTOCOL) && !url.startsWith(Constants.HTTPS_PROTOCOL)) {
                url = Constants.HTTP_PROTOCOL + url;
            }

            if (helper.getAuthorDao().queryBuilder().where().eq("url", url).query().size() != 0) {
                throw new AddAuthorException(AddAuthorException.AuthorAddErrors.AUTHOR_ALREADY_EXISTS);
            }

            HttpRequest request = HttpRequest.get(new URL(url));
            if (request.code() == 404) {
                throw new MalformedURLException();
            }
            String body = SamlibPageParser.sanitizeHTML(request.body());
            author = SamlibPageParser.getAuthor(body, url);
            helper.getAuthorDao().create(author);
            List<Publication> items = SamlibPageParser.getPublications(body, author);
            if (items.size() == 0) {
                helper.getAuthorDao().delete(author);
                throw new AddAuthorException(AddAuthorException.AuthorAddErrors.AUTHOR_NO_PUBLICATIONS);
            }
            for (Publication publication : items) {
                helper.getPublicationDao().create(publication);
            }
        } catch (HttpRequestException e) {
            message = context.getResources().getString(R.string.cannot_add_author_network);
        } catch (MalformedURLException e) {
            message = context.getResources().getString(R.string.cannot_add_author_malformed);
        } catch (SQLException e) {
            if (author != null) {
                try {
                    helper.getAuthorDao().delete(author);
                } catch (SQLException e1) {
                    //Swallow the exception as the author just wasn't saved
                }
            }
            message = context.getResources().getString(R.string.cannot_add_author_internal);
        } catch (AddAuthorException e) {
            switch (e.getError()) {
                case AUTHOR_ALREADY_EXISTS:
                    message = context.getResources().getString(R.string.cannot_add_author_already_exits);
                    break;
                case AUTHOR_DATE_NOT_FOUND:
                    message = context.getResources().getString(R.string.cannot_add_author_no_update_date);
                    break;
                case AUTHOR_NAME_NOT_FOUND:
                    message = context.getResources().getString(R.string.cannot_add_author_no_name);
                    break;
                case AUTHOR_NO_PUBLICATIONS:
                    message = context.getResources().getString(R.string.cannot_add_author_no_publications);
                    break;
                default:
                    message = context.getResources().getString(R.string.cannot_add_author_unknown);
                    break;
            }
        }
        return message;
    }
}
