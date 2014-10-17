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

package com.andrada.sitracker.util;

import android.text.TextUtils;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.exceptions.RatingException;
import com.github.kevinsawicki.http.HttpRequest;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class RatingUtil {

    private static final String SAMLIB_VOTE_URL = "http://samlib.ru/cgi-bin/votecounter";

    public static String submitRatingForPublication(int ratingToSubmit, String publicationUrl)
            throws RatingException {


        Map<String, String> dataMap = submitFirstStepForm(ratingToSubmit, publicationUrl, "");

        String pageContent = dataMap.get("pageContent");
        String cookieHeader = dataMap.get("cookieHeader");
        if (cookieHeader == null) {
            throw new RatingException("Rating submission: error submitting first part of rating, no cookie header");
        }
        String cookieToUse = cookieHeader.split(";")[0];

        if (TextUtils.isEmpty(cookieToUse)) {
            throw new RatingException("Could not get vote Cookie");
        }

        Pattern pattern = Pattern.compile(Constants.PUB_RATING_LINK_EXTRACT_REGEX, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
        Matcher matcher = pattern.matcher(pageContent);

        if (!matcher.find()) {
            throw new RatingException("Rating submission: Could not find link information in first part response");
        }
        String redirectUrl = matcher.group(1);
        if (TextUtils.isEmpty(redirectUrl)) {
            throw new RatingException("Rating submission: Link was not correct in first part response");
        }
        String newUrl = "http://samlib.ru/" + redirectUrl;

        //This request is the one that actually submits the rating
        int finalCode = submitSecondRatingStep(newUrl, cookieToUse);
        if (finalCode != HttpURLConnection.HTTP_OK) {
            throw new RatingException("Rating submission: Samlib did not accept final rating submission");
        }

        return cookieToUse;
    }

    public static void updateRatingForPublicaiton(int newRating, String publicationUrl, String votingCookie)
            throws RatingException {
        submitFirstStepForm(newRating, publicationUrl, votingCookie);
    }

    private static Map<String, String> submitFirstStepForm(int ratingToSubmit, String publicationUrl,
                                                           String possibleCookie) throws RatingException {
        if (TextUtils.isEmpty(publicationUrl)) {
            throw new RatingException("Rating submission: publication URL is empty");
        }
        String urlCopy = publicationUrl.replace(".shtml", "");
        urlCopy = urlCopy.replaceFirst(".*?samlib.ru/", "");
        String[] urlParts = urlCopy.split("/");
        if (urlParts.length != 3) {
            throw new RatingException("Rating submission: url has a wrong structure");
        }
        String authorId = urlParts[0] + "/" + urlParts[1];
        String fileName = urlParts[2];

        HttpRequest request;
        Map<String, String> result = new HashMap<String, String>();


        try {
            request = HttpRequest.post(SAMLIB_VOTE_URL)
                    .accept("text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .acceptEncoding("gzip,deflate")
                    .contentType("application/x-www-form-urlencoded")
                    .header("Cookie", possibleCookie)
                    .header("Host", "samlib.ru")
                    .header("Origin", "http://samlib.ru")
                    .header("Referer", publicationUrl)
                    .header("User-Agent", "SiTracker/Android")
                    .form("BALL", String.valueOf(ratingToSubmit), null)
                    .form("FILE", fileName, null)
                    .form("DIR", authorId, null);

            int code = request.code();
            String cookieHeader = request.header("Set-Cookie");
            if (code != HttpURLConnection.HTTP_OK) {
                throw new RatingException("Rating submission: error submitting first part of rating, not ok");
            }
            String pageContent = request.body(Constants.DEFAULT_SAMLIB_ENCODING);
            result.put("cookieHeader", cookieHeader);
            result.put("pageContent", pageContent);

        } catch (HttpRequest.HttpRequestException e) {
            throw new RatingException("Rating submission: http error occurred");
        }

        return result;
    }

    private static int submitSecondRatingStep(String url, String cookieToUse) throws RatingException {
        int code;

        try {
            HttpRequest finalRequest = HttpRequest.get(url)
                    .header("Cookie", cookieToUse);
            code = finalRequest.code();
        } catch (HttpRequest.HttpRequestException e) {
            throw new RatingException("Rating submission: http error occurred");
        }

        return code;
    }

}
