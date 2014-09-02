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

package com.andrada.sitracker.tasks;

import android.app.IntentService;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.os.Bundle;

import com.andrada.sitracker.db.beans.Author;
import com.andrada.sitracker.db.beans.Publication;
import com.andrada.sitracker.db.dao.AuthorDao;
import com.andrada.sitracker.db.dao.PublicationDao;
import com.andrada.sitracker.db.manager.SiDBHelper;

import org.androidannotations.annotations.EService;
import org.androidannotations.annotations.OrmLiteDao;
import org.androidannotations.annotations.SystemService;

import java.util.List;

@EService
public class ImportAuthorsTask extends IntentService {

    public static final String CLEAR_CURRENT_EXTRA = "clearCurrentAuthors";
    public static final String AUTHOR_LIST_EXTRA = "authorsList";

    @OrmLiteDao(helper = SiDBHelper.class, model = Author.class)
    AuthorDao authorDao;
    @OrmLiteDao(helper = SiDBHelper.class, model = Publication.class)
    PublicationDao publicationsDao;

    @SystemService
    ConnectivityManager connectivityManager;
    @SystemService
    NotificationManager notificationManager;

    private List<String> authorsList;
    private boolean clearCurrentAuthors;

    public ImportAuthorsTask() {
        super(ImportAuthorsTask.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        setupExtras(intent);
    }

    private void setupExtras(Intent intent) {
        Bundle extras = intent.getExtras();
        if (extras != null) {
            if (extras.containsKey(CLEAR_CURRENT_EXTRA)) {
                clearCurrentAuthors = extras.getBoolean(CLEAR_CURRENT_EXTRA);
            }
            if (extras.containsKey(AUTHOR_LIST_EXTRA)) {
                authorsList = (List<String>) extras.getSerializable(AUTHOR_LIST_EXTRA);
            }
        }
    }
}
