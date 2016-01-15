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

package com.andrada.sitracker.tasks;

import android.content.Context;
import android.os.AsyncTask;

import com.andrada.sitracker.R;
import com.andrada.sitracker.db.manager.SiDBHelper;
import com.andrada.sitracker.events.AuthorAddedEvent;
import com.andrada.sitracker.reader.SiteDetector;
import com.andrada.sitracker.reader.SiteStrategy;
import com.j256.ormlite.android.apptools.OpenHelperManager;

import org.jetbrains.annotations.NotNull;

import de.greenrobot.event.EventBus;

public class AddAuthorTask extends AsyncTask<String, Integer, String> {

    private final Context context;
    private SiDBHelper helper;
    private String authUrl;


    public AddAuthorTask(Context context) {
        this.context = context;
    }

    @Override
    protected String doInBackground(@NotNull String... args) {
        String message = "";
        for (String url : args) {
            authUrl = url;
            SiteStrategy strategy = SiteDetector.chooseStrategy(url, helper);
            if (strategy == null) {
                message = context.getResources().getString(R.string.supported_urls);
                return message;
            }
            int returnMsg = strategy.addAuthorForUrl(url);
            if (returnMsg != -1) {
                message = context.getResources().getString(returnMsg);
            }
        }
        return message;
    }

    @Override
    protected void onPreExecute() {
        this.helper = OpenHelperManager.getHelper(this.context, SiDBHelper.class);
    }

    @Override
    protected void onPostExecute(String result) {
        OpenHelperManager.releaseHelper();
        EventBus.getDefault().post(new AuthorAddedEvent(result, authUrl));
    }
}
