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

package com.andrada.sitracker;

import android.app.Application;
import android.net.http.HttpResponseCache;

import com.andrada.sitracker.analytics.AnalyticsManager;
import com.google.firebase.FirebaseApp;

import java.io.File;

import de.greenrobot.event.EventBus;

import static com.andrada.sitracker.util.LogUtils.LOGD;
import static com.andrada.sitracker.util.LogUtils.LOGE;

public class SITrackerApp extends Application {

    /*
      * (non-Javadoc)
      * @see android.app.Application#onCreate()
      */
    @Override
    public void onCreate() {

        super.onCreate();

        if (!FirebaseApp.getApps(this).isEmpty()) {

            //Setup cache if possible
            try {
                File httpCacheDir = new File(this.getCacheDir(), "http");
                long httpCacheSize = 1024 * 1024; // 1 MiB
                HttpResponseCache.install(httpCacheDir, httpCacheSize);
                LOGD("SiTracker", "Cache installed");

            } catch (Exception ignored) {
                LOGE("SiTracker", "Cache installed failed");
            }

            EventBus.builder()
                    .throwSubscriberException(BuildConfig.DEBUG)
                    .installDefaultEventBus();

            AnalyticsManager.initHelper(this);
        }


    }
}
