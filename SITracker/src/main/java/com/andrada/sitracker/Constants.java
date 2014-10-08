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

package com.andrada.sitracker;

public class Constants {

    public final static String APP_TAG = "sitracker";

    public final static long STARTUP_UPDATE_DELAY = 600000L;

    public final static String PUBLICATIONS_REGEX =
            "<DL><DT><li>(?:<font.*?>.*?</font>)?\\s*(<b>(.*?)\\s*</b>\\s*)?<A HREF=(.*?)><b>\\s*(.*?)\\s*</b></A>.*?<b>(\\d+)k</b>.*?<small>(?:Оценка:<b>((\\d+(?:\\.\\d+)?).*?)</b>.*?)?\\s*\\\"(.*?)\\\"\\s*(.*?)?\\s*(?:<A HREF=\\\"(.*?)\\\">Комментарии:\\s*((\\d+).*?)</A>\\s*)?</small>.*?(?:<br><DD>(.*?)(?:<small><a href=(.*?)>Иллюстрации/приложения.*?</a></small>)?)?</DL>";

    public static final String SAMLIB_AUTHOR_SEARCH_REGEX = "<td.*?><p><a href=(.*?)><font.*?>(.*?)</font>.+?</font><br>\\s*(.*?)</td></tr></tbody>";

    public final static String SIMPLE_URL_REGEX =
            "^(https?:\\/\\/)?([\\da-z\\.-]+)\\.([a-z\\.]{2,6})([\\/\\w \\.-]*)*\\/?$";

    public final static String AUTHOR_UPDATE_DATE_REGEX = "Обновлялось:</font></a></b>\\s*(.*?)\\s*$";

    public final static String AUTHOR_DESCRIPTION_TEXT_REGEX = "Об авторе:</font></b><i>(.*?)</i>";

    public final static String AUTHOR_IMAGE_REGEX = "<font color=\"#555555\">\\s*(<a href=about.shtml>)?\\s*<img src=(.*?) .*\\s*(</a>)?";

    public final static String AUTHOR_UPDATE_DATE_FORMAT = "dd/MM/yyyy";

    public final static String DIALOG_ADD_AUTHOR = "DIALOG_ADD_AUTHOR";

    public final static String AUTHOR_PAGE_URL_ENDING_WO_SLASH = "indextitle.shtml";
    public static final String AUTHOR_PAGE_ALT_URL_ENDING_WO_SLASH = "indexdate.shtml";
    public final static String AUTHOR_PAGE_URL_ENDING_WI_SLASH = "/indextitle.shtml";
    public final static String AUTHOR_PAGE_ALT_URL_ENDING_WI_SLASH = "/indexdate.shtml";
    public final static String HTTP_PROTOCOL = "http://";
    public final static String HTTPS_PROTOCOL = "https://";

    public final static String SI_PREF_NAME = "SIPrefs";

    public final static String UPDATES_ENABLED_KEY = "updatesEnabled";
    public final static String CONTENT_DOWNLOAD_FOLDER_KEY = "downloadFolder";
    public final static String PREF_CLEAR_SAVED_PUBS_KEY = "clearSavedPubs";
    public final static String UPDATE_INTERVAL_KEY = "updateInterval";
    public final static String PREF_USAGE_OPT_OUT_KEY = "optOutUsageStatistics";
    public final static String UPDATE_IGNORES_NETWORK = "update_service_ignores_network_constraints";
    public final static String AUTHOR_SORT_TYPE_KEY = "authorsSortType";

    public static final String NUMBER_OF_UPDATED_AUTHORS = "number_of_updated_authors";

    //Analytics category names
    public static final String GA_EXPLORE_CATEGORY = "explore_action";
    public static final String GA_READ_CATEGORY = "read_action";
    public static final String GA_ADMIN_CATEGORY = "admin_action";

    //Analytics screen names
    public static final String GA_SCREEN_PUBLICATIONS = "Publications";
    public static final String GA_SCREEN_AUTHORS = "Authors";
    public static final String GA_SCREEN_ADD_DIALOG = "Add author dialog";
    public static final String GA_SCREEN_IMPORT_PROGRESS = "Import progress";
    public static final String GA_SCREEN_EXPORT_DIALOG = "Export authors dialog";
    public static final String GA_SCREEN_PREFS_DOWNLOAD_DIALOG = "Download folder setting dialog";
    public static final String GA_SCREEN_RATING_DIALOG = "Publication Rating";
    public static final String GA_SCREEN_ABOUT_DIALOG = "About";

    //Analytics event names
    public static final String GA_EVENT_AUTHOR_ADDED = "author_added";
    public static final String GA_EVENT_AUTHOR_UPDATE = "author_update";
    public static final String GA_EVENT_AUTHORS_MANUAL_REFRESH = "author_manual_refresh";
    public static final String GA_EVENT_PUB_MANUAL_REFRESH = "pub_manual_refresh";
    public static final String GA_EVENT_PUB_RATED = "pub_rated";
    public static final String GA_EVENT_FAB_CLICK = "fab_used";
    public static final String GA_EVENT_LONG_TAP = "long_tap_used";
    public static final String GA_EVENT_AUTHOR_REMOVED = "authors_removed";
    public static final String GA_EVENT_AUTHOR_MANUAL_READ = "author_manual_marked_read";
    public static final String GA_EVENT_AUTHOR_PUB_OPEN = "publication_opened";
    public static final String GA_EVENT_CHANGED_UPDATE_INTERVAL = "update_interval_changed";
    public static final String GA_EVENT_AUTHOR_IMPORT = "author_import";
    public static final String GA_EVENT_IMPORT_COMPLETE = "import_complete";
    public static final String GA_EVENT_IMPORT_CANCELED = "import_canceled";
    public static final String GA_EVENT_CLEAR_CACHED_PUBS = "clear_cached_publications";

    public static final int REQUEST_DIRECTORY = 1681;
    public static final int REQUEST_EXPORT_DIRECTORY = 16811;
}
