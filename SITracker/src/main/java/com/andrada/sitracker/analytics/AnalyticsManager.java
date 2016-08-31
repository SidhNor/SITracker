/*
 * Copyright 2016 Gleb Godonoga.
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

package com.andrada.sitracker.analytics;

import android.content.Context;
import android.os.Bundle;

import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.crash.FirebaseCrash;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public class AnalyticsManager {

    private static AnalyticsManager instance;
    private final FirebaseAnalytics firebaseAnalytics;

    private AnalyticsManager(Context context) {
        this.firebaseAnalytics = FirebaseAnalytics.getInstance(context);
    }

    public static void initHelper(Context context) {
        if (instance == null) {
            instance = new AnalyticsManager(context);
        }
    }

    private Bundle toBundle(Map<String, String> map) {
        Bundle bundle = new Bundle();
        for (Map.Entry<String, String> entry : map.entrySet()) {
            String value = "";
            if (entry.getValue() != null) {
                value = StringUtils.left(value
                        .replaceFirst("http://", "")
                        .replaceAll("budclub.ru/", "")
                        .replaceAll("samlib.ru/", ""), 36);
            }
            bundle.putString(StringUtils.left(entry.getKey(), 24), value);
        }
        return bundle;
    }

    public static synchronized AnalyticsManager getInstance() {
        if (instance == null) {
            throw new RuntimeException("Analytics Helper not initialized");
        }
        return instance;
    }

    public void sendView(String viewName) {
        FBAEvent viewEvent = new FBAEvent(FirebaseAnalytics.Event.VIEW_ITEM);
        viewEvent.getParamMap().put(FirebaseAnalytics.Param.ITEM_NAME, viewName);
        logEvent(viewEvent);
    }

    public void logEvent(FBAEvent event) {
        firebaseAnalytics.logEvent(event.getName(), toBundle(event.getParamMap()));
    }

    public void sendException(String message, Exception e) {
        FirebaseCrash.report(new Exception(message, e));
    }

    public void sendException(Exception e) {
        FirebaseCrash.report(e);
    }

    public void sendException(@Nullable String message) {
        if (message == null) {
            message = "";
        }
        FirebaseCrash.log(message);
    }

    public void setOptOut(boolean optOut) {
        firebaseAnalytics.setAnalyticsCollectionEnabled(!optOut);
    }
}
