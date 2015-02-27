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

import android.net.Uri;
import android.text.TextUtils;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.contracts.AppUriContract;
import com.andrada.sitracker.db.beans.SearchedAuthor;
import com.andrada.sitracker.exceptions.SearchException;
import com.github.kevinsawicki.http.HttpRequest;

import java.net.MalformedURLException;
import java.text.Collator;
import java.text.RuleBasedCollator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.andrada.sitracker.util.LogUtils.LOGD;
import static com.andrada.sitracker.util.LogUtils.makeLogTag;

public class SamlibCgiSearchStrategyImpl implements SearchStrategy {
    private static final String TAG = makeLogTag(SamlibCgiSearchStrategyImpl.class);

    private static final String AUTHOR_SEARCH_URL = "http://samlib.ru/cgi-bin/areader?q=alpha&anum=%s&page=%d&pagelen=%d";
    private static final int AUTHOR_PAGE_SIZE = 500;
    private static final int MAX_RESULTS = 50;
    //We are ok for 10 days with this cache.
    private static final long MAX_STALE_CACHE = 60 * 60 * 24 * 10;

    private static final Map<String, String> CHAR_MAP = new HashMap<String, String>() {
        {
            put("0", "048");
            put("1", "049");
            put("2", "050");
            put("3", "051");
            put("4", "052");
            put("5", "053");
            put("6", "054");
            put("7", "055");
            put("8", "056");
            put("9", "057");
            put("A", "065");
            put("B", "066");
            put("C", "067");
            put("D", "068");
            put("E", "069");
            put("F", "070");
            put("G", "071");
            put("H", "072");
            put("I", "073");
            put("J", "074");
            put("K", "075");
            put("L", "076");
            put("M", "077");
            put("N", "078");
            put("O", "079");
            put("P", "080");
            put("Q", "081");
            put("R", "082");
            put("S", "083");
            put("T", "084");
            put("U", "085");
            put("V", "086");
            put("W", "087");
            put("X", "088");
            put("Y", "089");
            put("Z", "090");
            put("А", "225");
            put("Б", "226");
            put("В", "247");
            put("Г", "231");
            put("Д", "228");
            put("Е", "229");
            put("Ё", "179");
            put("Ж", "246");
            put("З", "250");
            put("И", "233");
            put("Й", "234");
            put("К", "235");
            put("Л", "236");
            put("М", "237");
            put("Н", "238");
            put("О", "239");
            put("П", "240");
            put("Р", "242");
            put("С", "243");
            put("Т", "244");
            put("У", "245");
            put("Ф", "230");
            put("Х", "232");
            put("Ц", "227");
            put("Ч", "254");
            put("Ш", "251");
            put("Щ", "253");
            put("Ъ", "255");
            put("Ы", "249");
            put("Ь", "248");
            put("Э", "252");
            put("Ю", "224");
            put("Я", "241");
        }

        private static final long serialVersionUID = -782460774377989714L;
    };
    private List<SearchedAuthor> mAuthors = new ArrayList<SearchedAuthor>();

    private boolean forceStopped = false;

    @Override
    public List<SearchedAuthor> searchForQuery(Uri searchUri) throws MalformedURLException, SearchException, InterruptedException {
        String searchString = AppUriContract.getSanitizedSearchQuery(searchUri);
        String firstChar = searchString.substring(0, 1).toUpperCase();
        int currentPage = 1;
        boolean finished = false;

        RuleBasedCollator ru_RUCollator = (RuleBasedCollator) Collator
                .getInstance(new Locale("ru", "RU", ""));

        while (!finished) {
            if (forceStopped) {
                return mAuthors;
            }
            String urlToUse = String.format(AUTHOR_SEARCH_URL, CHAR_MAP.get(firstChar), currentPage, AUTHOR_PAGE_SIZE);
            LOGD(TAG, "Loading search result page: " + currentPage);
            HttpRequest request = HttpRequest.get(urlToUse);
            request.getConnection().addRequestProperty("Cache-Control", "max-stale=" + MAX_STALE_CACHE);
            if (request.code() == 200) {
                String body = request.body(Constants.DEFAULT_SAMLIB_ENCODING);
                String[] lines = body.split("\n");
                if (lines.length < AUTHOR_PAGE_SIZE) {
                    //This is the last page - finish in any case
                    finished = true;
                }
                Map<String, CgiSearchResultVO> authorMap = new HashMap<String, CgiSearchResultVO>();
                for (String line : lines) {
                    try {
                        CgiSearchResultVO item = new CgiSearchResultVO(line);
                        //ensure key is not null
                        if (item.name != null) {
                            authorMap.put(item.name, item);
                        }
                    } catch (IllegalArgumentException ignored) {
                    }
                }

                //Search in map for our query
                String[] authNames = authorMap.keySet().toArray(new String[1]);
                LOGD(TAG, "Got unique authors from page: " + authNames.length);
                Arrays.sort(authNames, ru_RUCollator);
                int startIdx = Arrays.binarySearch(authNames, searchString, ru_RUCollator);
                if (startIdx < 0) {
                    startIdx = -startIdx - 1;
                }
                for (int i = startIdx; i < authNames.length; i++) {
                    String name = authNames[i];
                    if (name.toLowerCase().startsWith(searchString.toLowerCase())) {
                        CgiSearchResultVO result = authorMap.get(name);
                        SearchedAuthor authToAdd = new SearchedAuthor(result.url, result.name,
                                TextUtils.isEmpty(result.description) ? result.title : result.description);
                        mAuthors.add(authToAdd);

                        if (mAuthors.size() == MAX_RESULTS) {
                            return mAuthors;
                        }
                    }
                }
            } else {
                finished = true;
            }
            currentPage++;
        }

        return mAuthors;
    }

    @Override
    public void cancelAnyRunningTasks() {
        forceStopped = true;
    }

    private class CgiSearchResultVO {
        public String url;
        public String name;
        public String title;
        public String description;
        public String count;

        public CgiSearchResultVO(String line) {
            String str = line + " |";
            String[] components = str.split("\\|");
            if (components.length < 7) {
                throw new IllegalArgumentException("Wrong line");
            }
            this.url = "http://samlib.ru/" + components[0];
            this.name = components[1];
            this.title = components[2];
            this.count = components[7];
            try {
                int number = Integer.valueOf(this.count);
                if (number == 0) {
                    throw new IllegalArgumentException("No publications");
                }
            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("No publications");
            }
        }
    }
}
