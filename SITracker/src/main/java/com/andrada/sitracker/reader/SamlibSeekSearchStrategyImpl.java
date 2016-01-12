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

package com.andrada.sitracker.reader;

import android.net.Uri;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.contracts.AppUriContract;
import com.andrada.sitracker.db.beans.SearchedAuthor;
import com.andrada.sitracker.exceptions.SearchException;
import com.github.kevinsawicki.http.HttpRequest;

import org.androidannotations.api.BackgroundExecutor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLEncoder;
import java.nio.CharBuffer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.andrada.sitracker.util.LogUtils.LOGD;
import static com.andrada.sitracker.util.LogUtils.LOGE;
import static com.andrada.sitracker.util.LogUtils.makeLogTag;

public class SamlibSeekSearchStrategyImpl implements SearchStrategy {

    private static final String TAG = makeLogTag(SamlibSeekSearchStrategyImpl.class);

    private static final String SEARCH_URL = "http://samlib.ru/cgi-bin/seek?DIR=%s&FIND=%s&PLACE=index&JANR=%d&TYPE=%d&PAGE=%d";
    private static final String DEFAULT_DIR = "";
    private static final String BUFF_READER_ID = "bufferedReader";
    private static final int DEFAULT_GENRE = 0;
    private static final int DEFAULT_TYPE = 0;
    /**
     * Use search cache for 1 day only
     */
    private static final long MAX_STALE_CACHE = 60 * 60 * 24 * 1;
    volatile boolean finishedLoading = false;
    private List<SearchedAuthor> mAuthors = new ArrayList<SearchedAuthor>();

    private void readData(BufferedReader reader, StringBuffer appendable) throws IOException {
        try {
            final CharBuffer buffer = CharBuffer.allocate(8192);
            int read;
            while ((read = reader.read(buffer)) != -1) {
                buffer.rewind();
                appendable.append(buffer, 0, read);
                buffer.rewind();
            }
        } catch (IOException e) {
            LOGE(TAG, "Could not read data", e);
        } finally {
            try {
                reader.close();
            } catch (IOException ignored) {

            }
            finishedLoading = true;
        }
    }

    @Override
    public List<SearchedAuthor> searchForQuery(Uri searchUri)
            throws MalformedURLException, SearchException, InterruptedException {

        String searchString = AppUriContract.getSanitizedSearchQuery(searchUri);
        try {
            searchString = URLEncoder.encode(searchString, Constants.DEFAULT_SAMLIB_ENCODING);
        } catch (UnsupportedEncodingException ignored) {
            //Try to just search without encoding the query
        }
        String url = String.format(SEARCH_URL, DEFAULT_DIR, searchString, DEFAULT_GENRE, DEFAULT_TYPE, 1);

        Map<String, SearchedAuthor> hashAuthors = new HashMap<String, SearchedAuthor>();

        final long requestStart = new Date().getTime();
        final HttpRequest request = HttpRequest.get(new URL(url));
        //Tolerate 1 day
        request.getConnection().addRequestProperty("Cache-Control", "max-stale=" + MAX_STALE_CACHE);
        if (request.code() == 404) {
            throw new MalformedURLException();
        }

        if (request.code() == 500) {
            throw new SearchException(SearchException.SearchErrors.SAMLIB_BUSY);
        }

        final StringBuffer buffer = new StringBuffer();
        final BufferedReader reader = request.bufferedReader();
        finishedLoading = false;
        LOGD(TAG, "Starting search: " + requestStart);
        BackgroundExecutor.execute(new BackgroundExecutor.Task(BUFF_READER_ID, 0, "") {
            @Override
            public void execute() {
                try {
                    readData(reader, buffer);
                } catch (Throwable e) {
                    Thread.getDefaultUncaughtExceptionHandler().uncaughtException(Thread.currentThread(), e);
                }
            }
        });
        Thread.sleep(500);
        long currentMils;
        boolean authNumberCriteriaSatisfied;
        boolean timeCriteriaSatisfied;
        do {
            currentMils = new Date().getTime();
            Collection<SearchedAuthor> authors = new SamlibAuthorSearchReader().getUniqueAuthorsFromPage(buffer.toString());
            for (SearchedAuthor auth : authors) {
                if (!hashAuthors.containsKey(auth.getAuthorUrl())) {
                    hashAuthors.put(auth.getAuthorUrl(), auth);
                }
            }
            LOGD(TAG, "Check for result availability. Mils passed: " + (currentMils - requestStart) + ". Unique authors got: " + hashAuthors.size());
            authNumberCriteriaSatisfied = hashAuthors.size() > 10;
            timeCriteriaSatisfied = (currentMils - requestStart) > 30000 && hashAuthors.size() != 0;
            Thread.sleep(500);
        } while (!finishedLoading && !authNumberCriteriaSatisfied && !timeCriteriaSatisfied);

        if (!finishedLoading) {
            LOGD(TAG, "Search conditions satisfied. Force stopping current request with " + hashAuthors.size() + " authors");
            finishedLoading = true;
            BackgroundExecutor.cancelAll(BUFF_READER_ID, true);
        }
        mAuthors.addAll(hashAuthors.values());
        final String unencodedQuery = AppUriContract.getSanitizedSearchQuery(searchUri).toLowerCase();
        Collections.sort(mAuthors, new Comparator<SearchedAuthor>() {
            @Override
            public int compare(SearchedAuthor searchedAuthor, SearchedAuthor searchedAuthor2) {
                return searchedAuthor.weightedCompare(searchedAuthor2, unencodedQuery);
            }
        });

        return mAuthors;
    }

    @Override
    public void cancelAnyRunningTasks() {
        finishedLoading = true;
        BackgroundExecutor.cancelAll(BUFF_READER_ID, true);
    }
}
