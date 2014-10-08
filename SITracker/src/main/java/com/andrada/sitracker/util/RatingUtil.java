package com.andrada.sitracker.util;

import android.text.TextUtils;

import com.andrada.sitracker.Constants;
import com.github.kevinsawicki.http.HttpRequest;

import java.net.HttpURLConnection;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static com.andrada.sitracker.util.LogUtils.LOGE;

public class RatingUtil {

    private static final String SAMLIB_VOTE_URL = "http://samlib.ru/cgi-bin/votecounter";

    public static String submitRatingForPublication(int ratingToSubmit, String publicationUrl) {
        String result = "";
        if (TextUtils.isEmpty(publicationUrl)) {
            return "Rating submission: publication URL is empty";
        }
        try {
            String urlCopy = publicationUrl.replace(".shtml", "");
            urlCopy = urlCopy.replaceFirst(".*?samlib.ru/", "");
            String[] urlParts = urlCopy.split("/");
            if (urlParts.length != 3) {
                throw new Exception("Rating submission: url has a wrong structure");
            }
            String authorId = urlParts[0] + "/" + urlParts[1];
            String fileName = urlParts[2];

            HttpRequest request = HttpRequest.post(SAMLIB_VOTE_URL)
                    .accept("text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,*/*;q=0.8")
                    .acceptEncoding("gzip,deflate")
                    .contentType("application/x-www-form-urlencoded")
                    .header("Host", "samlib.ru")
                    .header("Origin", "http://samlib.ru")
                    .header("Referer", publicationUrl)
                    .header("User-Agent", "SiTracker/Android")
                    .form("BALL", String.valueOf(ratingToSubmit), null)
                    .form("FILE", fileName, null)
                    .form("DIR", authorId, null);

            int code = request.code();

            if (code != HttpURLConnection.HTTP_OK || request.header("Set-Cookie") == null) {
                throw new Exception("Rating submission: error submitting first part of rating, not ok or no cookie");
            }

            //Get new url from body that does rating submit
            String pageContent = request.body();
            String cookieToUse = request.header("Set-Cookie");
            Pattern pattern = Pattern.compile(Constants.PUB_RATING_LINK_EXTRACT_REGEX, Pattern.CASE_INSENSITIVE | Pattern.DOTALL);
            Matcher matcher = pattern.matcher(pageContent);

            if (!matcher.find()) {
                throw new Exception("Rating submission: Could not find link information in first part response");
            }
            String redirectUrl = matcher.group(1);
            if (TextUtils.isEmpty(redirectUrl)) {
                throw new Exception("Rating submission: Link was not correct in first part response");
            }
            String newUrl = "http://samlib.ru/" + redirectUrl;
            HttpRequest finalRequest = HttpRequest.get(newUrl)
                    .header("Cookie", cookieToUse);
            //This request is the one that actually submits the rating
            int finalCode = finalRequest.code();
            if (finalCode != HttpURLConnection.HTTP_OK) {
                throw new Exception("Rating submission: Samlib did not accept final rating submission");
            }

            //If we reached here - that means this is a success

        } catch (Exception e) {
            result = e.getMessage();
            LOGE("SiTracker", "Could not submit rating", e);
        }
        return result;
    }

}
