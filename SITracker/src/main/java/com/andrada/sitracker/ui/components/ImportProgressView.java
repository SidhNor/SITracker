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

package com.andrada.sitracker.ui.components;

import android.content.Context;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.andrada.sitracker.R;
import com.andrada.sitracker.events.CancelImportEvent;
import com.andrada.sitracker.tasks.ImportAuthorsTask;

import org.androidannotations.annotations.Click;
import org.androidannotations.annotations.EViewGroup;
import org.androidannotations.annotations.ViewById;
import org.jetbrains.annotations.NotNull;

import de.greenrobot.event.EventBus;

@EViewGroup(R.layout.import_progress_view)
public class ImportProgressView extends RelativeLayout {

    @ViewById
    ProgressBar importProgressBar;

    @ViewById
    TextView progressValue;

    @ViewById
    TextView progressTitle;

    public ImportProgressView(@NotNull Context context) {
        super(context);
    }

    public void updateProgress(@NotNull ImportAuthorsTask.ImportProgress importProgress) {
        if (importProgress.getTotalProcessed() == 0) {
            importProgressBar.setIndeterminate(true);
            progressTitle.setText(getResources().getString(R.string.import_message_connecting_title));
            progressValue.setVisibility(GONE);
        } else {
            importProgressBar.setIndeterminate(false);
            progressTitle.setText(getResources().getString(R.string.import_message_title));
            progressValue.setVisibility(VISIBLE);
            importProgressBar.setMax(importProgress.getTotalAuthors());
            importProgressBar.setProgress(importProgress.getTotalProcessed());
            progressValue.setText(getResources().getString(R.string.import_progress_indication,
                    importProgress.getTotalProcessed(),
                    importProgress.getTotalAuthors()));
        }
    }

    @Click(R.id.cancelImportButton)
    void importCancelRequested() {
        EventBus.getDefault().post(new CancelImportEvent());
    }

}
