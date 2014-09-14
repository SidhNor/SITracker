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

package com.andrada.sitracker;

import android.app.Application;

import com.andrada.sitracker.util.AnalyticsExceptionParser;
import com.andrada.sitracker.util.AnalyticsHelper;
import com.google.android.gms.analytics.ExceptionReporter;

public class SITrackerApp extends Application {

    /*
      * (non-Javadoc)
      * @see android.app.Application#onCreate()
      */
    public void onCreate() {

        AnalyticsHelper.initHelper(this);
        ExceptionReporter myReporter = new ExceptionReporter(
                // Currently used Tracker.
                AnalyticsHelper.getInstance().getTracker(AnalyticsHelper.TrackerName.APP_TRACKER),
                // Current default uncaught exception handler.
                Thread.getDefaultUncaughtExceptionHandler(),
                // Context of the application.
                this.getApplicationContext());
        myReporter.setExceptionParser(new AnalyticsExceptionParser());
        Thread.setDefaultUncaughtExceptionHandler(myReporter);
    }
}
