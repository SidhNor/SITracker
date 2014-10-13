/*
 * Copyright 2014 Gleb Godonoga.
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

import android.content.Context;

import com.andrada.sitracker.BuildConfig;
import com.andrada.sitracker.R;
import com.google.android.gms.analytics.GoogleAnalytics;
import com.google.android.gms.analytics.HitBuilders;
import com.google.android.gms.analytics.StandardExceptionParser;
import com.google.android.gms.analytics.Tracker;

import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.Map;

public class AnalyticsHelper {

    private static AnalyticsHelper instance;
    private final Map<TrackerName, Tracker> mTrackers = new HashMap<TrackerName, Tracker>();
    private final Context defaultContext;

    private AnalyticsHelper(Context context) {
        this.defaultContext = context;
    }

    public static void initHelper(Context context) {
        if (instance == null) {
            if (BuildConfig.DEBUG) {
                GoogleAnalytics.getInstance(context).setDryRun(true);
            }
            instance = new AnalyticsHelper(context);
        }
    }

    public static synchronized AnalyticsHelper getInstance() {
        if (instance == null) {
            throw new RuntimeException("Analytics Helper not initialized");
        }
        return instance;
    }

    public void sendView(String viewName) {
        Tracker t = getTracker(TrackerName.APP_TRACKER);
        t.setScreenName(viewName);
        t.send(new HitBuilders.AppViewBuilder().build());
    }

    public void sendEvent(String category, String action, String label) {

        Tracker t = getTracker(TrackerName.APP_TRACKER);

        // This event will also be sent with &cd=Home%20Screen.
        // Build and send an Event.
        t.send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action)
                .setLabel(label)
                .build());

    }

    public void sendEvent(String category, String action, String label, long value) {

        Tracker t = getTracker(TrackerName.APP_TRACKER);

        // This event will also be sent with &cd=Home%20Screen.
        // Build and send an Event.
        t.send(new HitBuilders.EventBuilder()
                .setCategory(category)
                .setAction(action)
                .setLabel(label)
                .setValue(value)
                .build());

    }

    public void sendException(String message, Exception e) {
        Tracker t = getTracker(TrackerName.APP_TRACKER);
        t.send(new HitBuilders.ExceptionBuilder()
                        .setDescription(message + new StandardExceptionParser(defaultContext, null)
                                .getDescription(Thread.currentThread().getName(), e))
                        .setFatal(false)
                        .build()
        );
    }

    public void sendException(Exception e) {
        Tracker t = getTracker(TrackerName.APP_TRACKER);
        t.send(new HitBuilders.ExceptionBuilder()
                        .setDescription(new StandardExceptionParser(defaultContext, null)
                                .getDescription(Thread.currentThread().getName(), e))
                        .setFatal(false)
                        .build()
        );
    }

    public void sendException(@Nullable String message) {
        if (message == null) {
            message = "";
        }
        Tracker t = getTracker(TrackerName.APP_TRACKER);
        t.send(new HitBuilders.ExceptionBuilder()
                        .setDescription(message)
                        .setFatal(false)
                        .build()
        );
    }

    public synchronized Tracker getTracker(TrackerName trackerId) {
        if (!mTrackers.containsKey(trackerId)) {
            GoogleAnalytics analytics = GoogleAnalytics.getInstance(defaultContext);
            Tracker t = (trackerId == TrackerName.APP_TRACKER) ? analytics.newTracker(R.xml.analytics)
                    : analytics.newTracker(R.xml.global_tracker);
            mTrackers.put(trackerId, t);
        }
        return mTrackers.get(trackerId);
    }

    public enum TrackerName {
        APP_TRACKER,
        GLOBAL_TRACKER
    }
}
