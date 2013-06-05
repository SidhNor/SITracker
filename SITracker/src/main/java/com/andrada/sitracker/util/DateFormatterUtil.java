package com.andrada.sitracker.util;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Created by Gleb on 03.06.13.
 */
public final class DateFormatterUtil {

    public static String getFriendlyDateRelativeToToday(Date date) {

        Date today = new Date();
        //Check if the same day this year
        SimpleDateFormat fmt = new SimpleDateFormat("yyyyMMdd");
        //Check if the say year
        SimpleDateFormat fmtYear = new SimpleDateFormat("yyyy");
        SimpleDateFormat formatToUse;
        if (fmt.format(today).equals(fmt.format(date))) {
            formatToUse = new SimpleDateFormat("HH:mm");
        } else if (fmtYear.format(today).equals(fmtYear.format(date))){
            formatToUse = new SimpleDateFormat("MMM dd");
        } else {
            formatToUse = new SimpleDateFormat("dd/MM/yyyy");
        }
        return formatToUse.format(date);
    }

}
