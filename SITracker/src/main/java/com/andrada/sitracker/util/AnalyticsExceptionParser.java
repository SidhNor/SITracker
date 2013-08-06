package com.andrada.sitracker.util;

import com.google.analytics.tracking.android.ExceptionParser;

import org.apache.commons.lang3.exception.ExceptionUtils;

/**
 * Created by ggodonoga on 05/08/13.
 */
public class AnalyticsExceptionParser implements ExceptionParser {
    /*
     * (non-Javadoc)
     * @see com.google.analytics.tracking.android.ExceptionParser#getDescription(java.lang.String, java.lang.Throwable)
     */
    public String getDescription(String p_thread, Throwable p_throwable) {
        return "Thread: " + p_thread + ", Exception: " + ExceptionUtils.getStackTrace(p_throwable);
    }
}
