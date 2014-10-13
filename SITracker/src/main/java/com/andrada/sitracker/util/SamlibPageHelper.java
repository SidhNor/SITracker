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

public class SamlibPageHelper {

    public static String stripDescriptionOfImages(String value) {
        value = value.replaceAll("(<a[^>]*>)?\\s*?<img[^>]*>\\s?(</a>)?", "")
                .replaceAll("<br/?>", "")
                .replaceAll("<a[^>]*>\\s?Иллюстрации.[^>]*a>", "");
        return value;
    }

    @NotNull
    public static String getUrlIdFromCompleteUrl(@NotNull String completeUrl) {
        String result = completeUrl.toLowerCase()
                .replace("http://", "")
                .replace("indexdate.shtml", "")
                .replace("indextitle.shtml", "")
                .replace("/", "_")
                .replace(".", "_")
                .replace("\\", "_")
                .trim();
        if (result.endsWith("_")) {
            result = result.substring(0, result.length() - 1);
        }
        return result;
    }
}
