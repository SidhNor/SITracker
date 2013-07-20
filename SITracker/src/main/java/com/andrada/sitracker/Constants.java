package com.andrada.sitracker;

public class Constants {

    public final static String PUBLICATIONS_REGEX =
            "<DL>\\s*<DT>\\s*<li>.*?<A HREF=(.*?)><b>\\s*(.*?)\\s*</b></A>.*?<b>(\\d+)k</b>.*?<small>(?:Оценка:<b>((\\d+(?:\\.\\d+)?).*?)</b>.*?)?\\s*\\\"(.*?)\\\"\\s*(.*?)?\\s*(?:<A HREF=\\\"(.*?)\\\">Комментарии:\\s*((\\d+).*?)</A>\\s*)?</small>.*?(?:<br>\\s*<dd>\\s*<font.*?>(.*?)</font>)";

    public final static String SIMPLE_URL_REGEX =
            "^(https?:\\/\\/)?([\\da-z\\.-]+)\\.([a-z\\.]{2,6})([\\/\\w \\.-]*)*\\/?$";

    public final static String AUTHOR_UPDATE_DATE_REGEX = "Обновлялось:</font></a></b>\\s*(.*?)\\s*$";

    public final static String AUTHOR_UPDATE_DATE_FORMAT = "dd/MM/yyyy";

    public final static String DIALOG_ADD_AUTHOR = "DIALOG_ADD_AUTHOR";

    public final static String AUTHOR_PAGE_URL_ENDING_WO_SLASH = "indexdate.shtml";
    public final static String AUTHOR_PAGE_URL_ENDING_WI_SLASH = "/indexdate.shtml";
    public final static String HTTP_PROTOCOL = "http://";
    public final static String HTTPS_PROTOCOL = "https://";

}
