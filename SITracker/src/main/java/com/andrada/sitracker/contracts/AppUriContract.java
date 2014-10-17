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

package com.andrada.sitracker.contracts;

import android.net.Uri;

import java.util.List;

public class AppUriContract {

    private static final String PATH_SEARCH_SAMLIB = "search";
    private static final String PATH_AUTHORS = "authors";
    private static final String PATH_PUBLICATIONS = "publicaitons";

    public static final String CONTENT_AUTHORITY = "com.andrada.sitracker";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final Uri AUTHOR_CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_AUTHORS).build();
    public static final Uri PUBLICATION_CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_PUBLICATIONS).build();


    public static Uri buildSamlibSearchUri(String query, int searchType) {
        if (query == null) {
            query = "";
        }
        // convert "lorem ipsum dolor sit" to "lorem* ipsum* dolor* sit*"
        query = query.replaceAll(" +", " *") + "*";
        return AUTHOR_CONTENT_URI.buildUpon()
                .appendPath(PATH_SEARCH_SAMLIB).appendPath(query)
                .appendQueryParameter("type", String.valueOf(searchType)).build();
    }

    public static Uri buildPublicationUri(long publicationId) {
        return PUBLICATION_CONTENT_URI.buildUpon().appendPath(String.valueOf(publicationId)).build();
    }

    public static long getPublicationId(Uri uri) {
        return Long.valueOf(uri.getPathSegments().get(1));
    }

    public static String getSearchQuery(Uri uri) {
        List<String> segments = uri.getPathSegments();
        if (2 < segments.size()) {
            return segments.get(2);
        }
        return null;
    }

    public static int getSearchTypeParam(Uri uri) {
        return Integer.valueOf(uri.getQueryParameter("type"));
    }

    public static String getSanitizedSearchQuery(Uri uri) {
        String query = getSearchQuery(uri);
        if (query != null) {
            query = query.replaceAll("\\*", "");
        }
        return query;
    }

    public static boolean isSearchUri(Uri uri) {
        List<String> pathSegments = uri.getPathSegments();
        return pathSegments.size() >= 2 && PATH_SEARCH_SAMLIB.equals(pathSegments.get(1));
    }
}
