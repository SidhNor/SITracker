/*
 * Copyright 2013 Gleb Godonoga.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.andrada.sitracker.util;

import org.jetbrains.annotations.NotNull;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public final class DateFormatterUtil {

    public static String getFriendlyDateRelativeToToday(Date date, @NotNull Locale currentLocale) {

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
