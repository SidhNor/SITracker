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

import android.app.IntentService;
import android.content.Intent;

import com.andrada.sitracker.R;
import com.andrada.sitracker.exceptions.SharePublicationException;
import com.andrada.sitracker.util.AnalyticsHelper;
import com.andrada.sitracker.util.ShareHelper;

import java.io.File;

public class ClearPublicationCacheTask extends IntentService {

    public ClearPublicationCacheTask() {
        super(ClearPublicationCacheTask.class.getSimpleName());
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String errorMessage = "";

        int failedFiles = 0;
        try {
            File dir = ShareHelper.getPublicationStorageDirectory(this);

            if (dir == null) {
                throw new SharePublicationException(
                        SharePublicationException.SharePublicationErrors.STORAGE_NOT_ACCESSIBLE_FOR_PERSISTANCE);
            }
            for (File file : dir.listFiles()) {
                if (!file.delete()) failedFiles++;
            }
        } catch (SharePublicationException e) {
            errorMessage = this.getResources().getString(R.string.publication_error_storage);
        }
        if (!errorMessage.equals("")) {
            AnalyticsHelper.getInstance().sendException("Failed to remove files.");
        }
    }
}
