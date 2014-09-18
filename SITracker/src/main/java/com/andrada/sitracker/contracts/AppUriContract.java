package com.andrada.sitracker.contracts;

import android.net.Uri;

import java.util.List;

public class AppUriContract {

    private static final String PATH_SEARCH_SAMLIB = "search";
    private static final String PATH_AUTHORS = "authors";

    public static final String CONTENT_AUTHORITY = "com.andrada.sitracker";
    public static final Uri BASE_CONTENT_URI = Uri.parse("content://" + CONTENT_AUTHORITY);
    public static final Uri CONTENT_URI = BASE_CONTENT_URI.buildUpon().appendPath(PATH_AUTHORS).build();


    public static Uri buildSamlibSearchUri(String query) {
        if (query == null) {
            query = "";
        }
        // convert "lorem ipsum dolor sit" to "lorem* ipsum* dolor* sit*"
        query = query.replaceAll(" +", " *") + "*";
        return CONTENT_URI.buildUpon()
                .appendPath(PATH_SEARCH_SAMLIB).appendPath(query).build();
    }

    public static String getSearchQuery(Uri uri) {
        List<String> segments = uri.getPathSegments();
        if (2 < segments.size()) {
            return segments.get(2);
        }
        return null;
    }

    public static String getSanitizedSearchQuer(Uri uri) {
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
