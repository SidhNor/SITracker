/*
 * Copyright 2014 Gleb Godonoga.
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

package com.andrada.sitracker.reader;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.R;
import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.db.manager.SiDBHelper;
import com.andrada.sitracker.exceptions.AddAuthorException;
import com.andrada.sitracker.util.SamlibPageHelper;
import com.github.kevinsawicki.http.HttpRequest;

import org.jetbrains.annotations.Nullable;

import java.net.MalformedURLException;
import java.net.URL;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Callable;

public class CgiSamlib implements SiteStrategy {

    private static final String CGI_AUTHOR_URL_ROOT = "http://samlib.ru/cgi-bin/areader?q=razdel&order=date&object=";

    private final SiDBHelper helper;

    public CgiSamlib(SiDBHelper helper) {
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
            //Get author root id from url

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

            String reducedAuthorName = SamlibPageHelper.getReducedUrlFromCompleteUrl(url);
            String cgiUrl = CGI_AUTHOR_URL_ROOT + reducedAuthorName;

            HttpRequest request = HttpRequest.get(new URL(cgiUrl));
            if (request.code() == 404) {
                throw new MalformedURLException();
            }
            AuthorPageReader reader = new CgiSamlibAuthorPageReader(request.body(Constants.DEFAULT_SAMLIB_ENCODING));
            author = reader.getAuthor(url);
            helper.getAuthorDao().create(author);
            final List<Publication> items = reader.getPublications(author);
            if (items.size() == 0) {
                helper.getAuthorDao().delete(author);
                throw new AddAuthorException(AddAuthorException.AuthorAddErrors.AUTHOR_NO_PUBLICATIONS);
            }

            helper.getPublicationDao().callBatchTasks(new Callable<Object>() {
                @Nullable
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
        return false;
    }
}
