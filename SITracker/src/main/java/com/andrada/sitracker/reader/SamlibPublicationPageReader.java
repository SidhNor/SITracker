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

import android.text.TextUtils;
import android.util.Pair;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SamlibPublicationPageReader implements PublicationPageReader {

    public static final String SAMLIB_URL_PREFIX = "http://samlib.ru/";

    private static final String IMAGE_EXTRACTION_REGEX = "<table .*?<img src=(.*?)\\s.*?<br>\\s*(.*?)<br>";

    @NotNull
    @Override
    public List<Pair<String, String>> readPublicationImageUrlsAndDescriptions(String pageContent) {
        List<Pair<String, String>> result = new ArrayList<Pair<String, String>>();

        Pattern pattern = Pattern.compile(IMAGE_EXTRACTION_REGEX, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(pageContent);
        while (matcher.find()) {
            String imageUrl = matcher.group(1) == null ? "" : matcher.group(1);
            String imgDesc = matcher.group(2) == null ? "" : matcher.group(2);
            if (!TextUtils.isEmpty(imageUrl)) {
                result.add(new Pair<String, String>(SAMLIB_URL_PREFIX + imageUrl.trim(), imgDesc.trim()));
            }
        }
        return result;
    }
}
