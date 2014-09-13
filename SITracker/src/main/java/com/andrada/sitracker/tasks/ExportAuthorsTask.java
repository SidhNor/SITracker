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

import android.content.Context;
import android.os.AsyncTask;

import com.andrada.sitracker.R;
import com.andrada.sitracker.db.manager.SiDBHelper;
import com.andrada.sitracker.events.AuthorsExported;
import com.andrada.sitracker.util.ShareHelper;
import com.j256.ormlite.android.apptools.OpenHelperManager;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.SQLException;
import java.util.List;

import de.greenrobot.event.EventBus;

public class ExportAuthorsTask extends AsyncTask<String, Integer, String> {
    private final Context context;
    private SiDBHelper helper;


    public ExportAuthorsTask(Context context) {
        this.context = context;
    }

    @Override
    protected String doInBackground(String... args) {
        String result = "";
        String dir = args[0];
        PrintWriter writer = null;
        try {
            File exportDir = new File(dir);
            File exportFile = new File(exportDir, ShareHelper.getTimestampFilename("authors-", ".txt"));
            writer = new PrintWriter(exportFile);
            List<String> authors = helper.getAuthorDao().getAuthorsUrls();
            for (String url : authors) {
                writer.println(url);
            }
            writer.println();
        } catch (IOException e) {
            if (context == null) {
                result = "Error writing to file";
            } else {
                result = context.getResources().getString(R.string.cannot_write_export_file);
            }
        } catch (SQLException e) {
            result = "Error reading authors";
        } finally {
            if (writer != null) {
                writer.close();
            }
        }
        return result;
    }

    @Override
    protected void onPreExecute() {
        this.helper = OpenHelperManager.getHelper(this.context, SiDBHelper.class);
    }

    @Override
    protected void onPostExecute(String result) {
        OpenHelperManager.releaseHelper();
        EventBus.getDefault().post(new AuthorsExported(result));
    }
}
