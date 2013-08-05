package com.andrada.sitracker;

public class Constants {

    public final static String APP_TAG = "sitracker";

    public final static String PUBLICATIONS_REGEX =
            "<DL>\\s*<DT>\\s*<li>.*?<A HREF=(.*?)><b>\\s*(.*?)\\s*</b></A>.*?<b>(\\d+)k</b>.*?<small>(?:Оценка:<b>((\\d+(?:\\.\\d+)?).*?)</b>.*?)?\\s*\\\"(.*?)\\\"\\s*(.*?)?\\s*(?:<A HREF=\\\"(.*?)\\\">Комментарии:\\s*((\\d+).*?)</A>\\s*)?</small>.*?(?:<br>\\s*<dd>\\s*<font.*?>(.*?)</font>)?</DL>";

    public final static String SIMPLE_URL_REGEX =
            "^(https?:\\/\\/)?([\\da-z\\.-]+)\\.([a-z\\.]{2,6})([\\/\\w \\.-]*)*\\/?$";

    public final static String AUTHOR_UPDATE_DATE_REGEX = "Обновлялось:</font></a></b>\\s*(.*?)\\s*$";

    public final static String AUTHOR_UPDATE_DATE_FORMAT = "dd/MM/yyyy";

    public final static String DIALOG_ADD_AUTHOR = "DIALOG_ADD_AUTHOR";

    public final static String AUTHOR_PAGE_URL_ENDING_WO_SLASH = "indexdate.shtml";
    public final static String AUTHOR_PAGE_URL_ENDING_WI_SLASH = "/indexdate.shtml";
    public final static String HTTP_PROTOCOL = "http://";
    public final static String HTTPS_PROTOCOL = "https://";

    public final static String UPDATE_PREFERENCE_KEY = "pref_updates";
    public final static String UPDATE_INTERVAL_KEY = "pref_update_period";
    public static final String UPDATE_IGNORES_NETWORK = "update_service_ignores_network_constraints";
    public final static String UPDATE_NETWORK_KEY = "pref_wifi_only";

    public static final String NUMBER_OF_UPDATED_AUTHORS = "number_of_updated_authors";

    //Analytics category names
    public static final String GA_UI_CATEGORY = "ui_action";
    public static final String GA_BGR_CATEGORY = "bgr_action";

    //Analytics screen names
    public static final String GA_SCREEN_PUBLICATIONS = "Publications";
    public static final String GA_SCREEN_AUTHORS = "Authors";
    public static final String GA_SCREEN_ADD_DIALOG = "Add author dialog";

    //Analytics event names
    public static final String GA_EVENT_AUTHOR_ADDED = "author_added";
    public static final String GA_EVENT_AUTHOR_UPDATE = "author_update";
    public static final String GA_EVENT_AUTHORS_MANUAL_REFRESH = "author_manual_refresh";
    public static final String GA_EVENT_AUTHOR_REMOVED = "authors_removed";
    public static final String GA_EVENT_AUTHOR_MANUAL_READ = "author_manual_marked_read";
    public static final String GA_EVENT_AUTHOR_PUB_OPEN = "publication_opened";
    public static final String GA_EVENT_CHANGED_UPDATE_INTERVAL = "update_interval_changed";
}
