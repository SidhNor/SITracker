package com.andrada.sitracker;

import android.app.Application;

import com.andrada.sitracker.util.AnalyticsExceptionParser;
import com.google.analytics.tracking.android.EasyTracker;
import com.google.analytics.tracking.android.ExceptionReporter;

/**
 * Created by ggodonoga on 05/08/13.
 */
public class SITrackerApp extends Application {
    /*
      * (non-Javadoc)
      * @see android.app.Application#onCreate()
      */
    public void onCreate() {
        EasyTracker.getInstance().setContext(this);

        // Change uncaught exception parser...
        // Note: Checking uncaughtExceptionHandler type can be useful if clearing ga_trackingId during development to disable analytics - avoid NullPointerException.
        Thread.UncaughtExceptionHandler uncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        if (uncaughtExceptionHandler instanceof ExceptionReporter) {
            ExceptionReporter exceptionReporter = (ExceptionReporter) uncaughtExceptionHandler;
            exceptionReporter.setExceptionParser(new AnalyticsExceptionParser());

        }
    }
}
