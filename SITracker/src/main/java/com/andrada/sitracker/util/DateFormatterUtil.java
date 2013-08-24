package com.andrada.sitracker.util;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * Created by Gleb on 03.06.13.
 */
public final class DateFormatterUtil {

    public static String getFriendlyDateRelativeToToday(Date date, Locale currentLocale) {

        Date today = new Date();

        String localeBasedFmt = currentLocale.getLanguage().equals("ru") ? "d MMM" : "MMM d";

        //Check if the same day this year
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
        //Check if the say year
        SimpleDateFormat fmtYear = new SimpleDateFormat("yyyy");
        SimpleDateFormat formatToUse;
        if (fmt.format(today).equals(fmt.format(date))) {
            formatToUse = new SimpleDateFormat("HH:mm");
        } else if (fmtYear.format(today).equals(fmtYear.format(date))) {
            formatToUse = new SimpleDateFormat(localeBasedFmt);
        } else {
            formatToUse = new SimpleDateFormat("dd/MM/yyyy");
        }
        return formatToUse.format(date);
    }

}
