package com.andrada.sitracker.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Gleb on 03.06.13.
 */
public final class DateFormatterUtil {

    public static String getFriendlyDateRelativeToToday(Date date) {

        Date today = new Date();
        //Check if the same day
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
        SimpleDateFormat formatToUse;
        if (fmt.format(today).equals(fmt.format(date))) {
            formatToUse = new SimpleDateFormat("HH:mm");
        } else {
            formatToUse = new SimpleDateFormat("MMM dd");
        }
        return formatToUse.format(date);
    }

}
