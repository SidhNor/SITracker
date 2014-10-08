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

package com.andrada.sitracker.ui.fragment;

import android.os.Bundle;
import android.support.v4.app.DialogFragment;
import android.text.TextUtils;
import android.view.View;
import android.widget.RatingBar;
import android.widget.TextView;

import com.andrada.sitracker.Constants;
import com.andrada.sitracker.R;
import com.andrada.sitracker.util.AnalyticsHelper;
import com.github.kevinsawicki.http.HttpRequest;

import org.androidannotations.annotations.Background;
import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EFragment;
import org.androidannotations.annotations.FragmentArg;
import org.androidannotations.annotations.ViewById;
import org.androidannotations.annotations.res.StringArrayRes;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;

@EFragment(R.layout.fragment_rate_publication)
public class RatePublicationDialog extends DialogFragment implements RatingBar.OnRatingBarChangeListener {

    public static final String FRAGMENT_TAG = "rate_pub_dialog";
    private static final String SAMLIB_VOTE_URL = "http://samlib.ru/cgi-bin/votecounter";

    @FragmentArg
    String publicationUrl;

    @ViewById(R.id.rating_bar)
    RatingBar mRatingBar;

    @ViewById(R.id.rating_description)
    TextView mRatingDescription;

    @ViewById(R.id.submit_rating_button)
    View mSubmitRatingBtn;

    @StringArrayRes(R.array.publication_rating_description)
    String[] ratingDescriptions;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(DialogFragment.STYLE_NO_TITLE, 0);
    }

    @Override
    public void onStart() {
        super.onStart();
        mRatingBar.setOnRatingBarChangeListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();
        mRatingBar.setOnRatingBarChangeListener(null);
    }

    @Override
    public void onResume() {
        super.onResume();
        onRatingChanged(mRatingBar, mRatingBar.getRating(), false);
    }

    @Override
    public void onRatingChanged(RatingBar ratingBar, float rating, boolean fromUser) {
        int normalizedValue = (int) (rating * 2);
        if (normalizedValue >= 0 && normalizedValue < ratingDescriptions.length) {
            mRatingDescription.setText(ratingDescriptions[normalizedValue]);
        }
    }

    @Click(R.id.submit_rating_button)
    void submitRatingClicked() {
        int rating = (int) (mRatingBar.getRating() * 2);
        submitRatingSilently(rating, publicationUrl);
        this.dismiss();
    }

    @Background
    void submitRatingSilently(int ratingToSubmit, String publicationUrl) {
        if (TextUtils.isEmpty(publicationUrl)) {
            return;
        }

        try {
            String urlCopy = publicationUrl.replace(".shtml", "");
            urlCopy = urlCopy.replaceFirst(".*?samlib.ru/", "");
            String[] urlParts = urlCopy.split("/");
            if (urlParts.length != 3) {
                return;
            }
            String authorId = urlParts[0] + "/" + urlParts[1];
            String fileName = urlParts[2];

            Map<String, String> formData = new HashMap<String, String>();
            formData.put("BALL", String.valueOf(ratingToSubmit));
            formData.put("FILE", fileName);
            formData.put("DIR", authorId);
            formData.put("OK", "ОК");

            HttpRequest request = HttpRequest.post(SAMLIB_VOTE_URL)
                    .header("Host", "samlib.ru")
                    .header("Origin", "http://samlib.ru")
                    .header("Referer", "publicationUrl")
                    .form(formData, "windows-1251");

            int code = request.code();
            //We expect a 200
            if (code == HttpURLConnection.HTTP_OK) {
                //Vote submitted successfully
                AnalyticsHelper.getInstance().sendEvent(
                        Constants.GA_EXPLORE_CATEGORY,
                        Constants.GA_EVENT_PUB_RATED,
                        Constants.GA_EVENT_PUB_RATED);
            } else {
                //Send analytics error
                AnalyticsHelper.getInstance().sendException("Could not submit rating for publicaiton");
            }
        } catch (Exception e) {
            //Eat stuff
        }

    }
}
